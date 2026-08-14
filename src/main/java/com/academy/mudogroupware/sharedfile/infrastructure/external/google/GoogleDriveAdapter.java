package com.academy.mudogroupware.sharedfile.infrastructure.external.google;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import com.academy.mudogroupware.sharedfile.application.port.DriveBinary;
import com.academy.mudogroupware.sharedfile.application.port.DriveItem;
import com.academy.mudogroupware.sharedfile.application.port.DrivePage;
import com.academy.mudogroupware.sharedfile.application.port.GoogleWorkspaceExportFormat;
import com.academy.mudogroupware.sharedfile.application.port.GoogleWorkspaceFileType;
import com.academy.mudogroupware.sharedfile.application.port.SharedFileDrivePort;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileDriveFailureException;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileItemNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

// SharedFileDrivePort의 실제 구현체. Google Drive REST API v3를 RestClient로 직접 호출하며,
// URL·쿼리 문법·MIME type 매핑 등 Drive 고유의 세부사항을 이 클래스 안에 가둔다.
@Component
@RequiredArgsConstructor
public class GoogleDriveAdapter implements SharedFileDrivePort {

    private static final String FILES_ENDPOINT = "https://www.googleapis.com/drive/v3/files";
    private static final String UPLOAD_ENDPOINT = "https://www.googleapis.com/upload/drive/v3/files";
    // Drive 응답에서 항상 요청하는 필드 목록. capabilities(canDownload)는 미리보기 미지원 파일 안내에 쓰인다.
    private static final String FILE_FIELDS =
            "id,name,mimeType,parents,webViewLink,modifiedTime,trashed,capabilities(canDownload)";

    private final RestClient googleDriveRestClient;
    private final ObjectMapper objectMapper;

    // files.get 호출. 404는 "없음"으로, 401/403/5xx/네트워크 오류는 예외로 구분해서 던진다 —
    // 404만 "루트 삭제됨"과 같은 상태 판단에 쓰이고 나머지는 단순 요청 실패이기 때문이다.
    @Override
    public Optional<DriveItem> getItem(String accessToken, String itemId) {
        try {
            URI uri = fileUriBuilder(itemId).queryParam("fields", FILE_FIELDS).build().encode().toUri();
            GoogleDriveFileResponse response = googleDriveRestClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(GoogleDriveFileResponse.class);
            return Optional.ofNullable(response).map(this::toDriveItem);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                return Optional.empty();
            }
            throw new SharedFileDriveFailureException(e);
        } catch (RestClientException e) {
            throw new SharedFileDriveFailureException(e);
        }
    }

    // files.list. parentId의 직계 하위만 조회하고, 휴지통에 있는 항목은 제외한다.
    @Override
    public DrivePage listChildren(String accessToken, String parentId, String cursor, int size) {
        String query = "'" + parentId + "' in parents and trashed = false";
        return listFiles(accessToken, query, cursor, size);
    }

    // files.list. 이름 부분일치로 전체를 검색한다(루트 하위 필터링은 SharedFileRootGuard가 호출자 쪽에서 수행).
    @Override
    public DrivePage searchByName(String accessToken, String keyword, String cursor, int size) {
        String escapedKeyword = keyword.replace("\\", "\\\\").replace("'", "\\'");
        String query = "name contains '" + escapedKeyword + "' and trashed = false";
        return listFiles(accessToken, query, cursor, size);
    }

    // files.create(부모 없음). 배포 단위의 시스템 루트를 Drive 최상위에 생성한다.
    @Override
    public DriveItem createRootFolder(String accessToken, String name) {
        return createMetadataOnly(accessToken, null, name, "application/vnd.google-apps.folder");
    }

    // files.create(부모 있음). 시스템 루트 하위 일반 폴더 생성용(Task4에서 실제 호출).
    @Override
    public DriveItem createFolder(String accessToken, String parentId, String name) {
        return createMetadataOnly(accessToken, parentId, name, "application/vnd.google-apps.folder");
    }

    // uploadType=multipart. RestClient는 multipart/related를 직접 지원하지 않아 본문을 수동으로 구성한다:
    // 메타데이터(JSON) 파트 + 파일 내용 파트를 하나의 boundary로 감싼다. boundary는 요청마다 새로 발급해
    // 파일 내용에 고정 문자열이 우연히 포함돼 파트 경계가 깨지는 것을 막고, contentType은 실제 media type으로
    // 파싱·정규화해 CRLF가 포함된 값이 헤더 인젝션으로 이어지지 않게 한다.
    @Override
    public DriveItem upload(String accessToken, String parentId, String name, String contentType, byte[] content) {
        try {
            String boundary = "shared_file_upload_" + java.util.UUID.randomUUID();
            String metadataJson = objectMapper.writeValueAsString(
                    new GoogleDriveCreateFileRequest(name, null, List.of(parentId)));
            byte[] body = buildMultipartRelatedBody(boundary, metadataJson, contentType, content);

            GoogleDriveFileResponse response = googleDriveRestClient.post()
                    .uri(UPLOAD_ENDPOINT + "?uploadType=multipart&fields=" + FILE_FIELDS)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.parseMediaType("multipart/related; boundary=" + boundary))
                    .body(body)
                    .retrieve()
                    .body(GoogleDriveFileResponse.class);
            return toDriveItem(response);
        } catch (JsonProcessingException e) {
            throw new SharedFileDriveFailureException(e);
        } catch (RestClientException e) {
            throw new SharedFileDriveFailureException(e);
        }
    }

    private byte[] buildMultipartRelatedBody(String boundary, String metadataJson, String contentType, byte[] content) {
        String header = "--" + boundary + "\r\n"
                + "Content-Type: application/json; charset=UTF-8\r\n\r\n"
                + metadataJson + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Type: " + sanitizeContentType(contentType) + "\r\n\r\n";
        String footer = "\r\n--" + boundary + "--";

        byte[] headerBytes = header.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] footerBytes = footer.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] body = new byte[headerBytes.length + content.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
        System.arraycopy(content, 0, body, headerBytes.length, content.length);
        System.arraycopy(footerBytes, 0, body, headerBytes.length + content.length, footerBytes.length);
        return body;
    }

    // 신뢰할 수 없는 contentType(예: CRLF가 섞인 값)이 그대로 헤더에 들어가는 것을 막는다.
    // 정상적인 media type으로 파싱되지 않으면 기본값으로 대체한다.
    private String sanitizeContentType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType).toString();
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }

    // files.create. Docs/Sheets/Slides 빈 파일 생성(Task4). Google MIME type 매핑은 이 클래스 안에 가둔다.
    @Override
    public DriveItem createWorkspaceFile(String accessToken, String parentId, String name, GoogleWorkspaceFileType type) {
        return createMetadataOnly(accessToken, parentId, name, toGoogleMimeType(type));
    }

    private String toGoogleMimeType(GoogleWorkspaceFileType type) {
        return switch (type) {
            case DOCS -> "application/vnd.google-apps.document";
            case SHEETS -> "application/vnd.google-apps.spreadsheet";
            case SLIDES -> "application/vnd.google-apps.presentation";
        };
    }

    // files.update(PATCH) 하나로 name 변경과 addParents/removeParents 이동을 함께 반영한다. name이
    // null이면 body 자체를 생략해 이름은 건드리지 않고, toParentId가 null이면 쿼리에 부모 교체 파라미터를
    // 안 붙여 부모는 그대로 둔다 — 두 변경을 별도 요청으로 나누면 그 사이에 부분 실패가 생길 수 있어서다.
    @Override
    public DriveItem updateItem(String accessToken, String itemId, String name, String fromParentId,
            String toParentId) {
        try {
            UriComponentsBuilder uriBuilder = fileUriBuilder(itemId).queryParam("fields", FILE_FIELDS);
            if (toParentId != null) {
                uriBuilder.queryParam("addParents", toParentId).queryParam("removeParents", fromParentId);
            }
            URI uri = uriBuilder.build().encode().toUri();
            GoogleDriveUpdateFileRequest request = name == null ? null : new GoogleDriveUpdateFileRequest(name, null);
            return patch(accessToken, uri, request);
        } catch (RestClientException e) {
            throw new SharedFileDriveFailureException(e);
        }
    }

    // files.update(PATCH, trashed=true). Drive 휴지통으로 이동할 뿐 영구 삭제하지 않는다(Task5).
    @Override
    public void trash(String accessToken, String itemId) {
        try {
            GoogleDriveUpdateFileRequest request = new GoogleDriveUpdateFileRequest(null, true);
            URI uri = fileUriBuilder(itemId).queryParam("fields", FILE_FIELDS).build().encode().toUri();
            patch(accessToken, uri, request);
        } catch (RestClientException e) {
            throw new SharedFileDriveFailureException(e);
        }
    }

    // 메타데이터를 먼저 조회해 파일명·MIME type을 얻은 뒤, files.get?alt=media로 원본 바이트를 받는다(Task5).
    @Override
    public DriveBinary downloadOriginal(String accessToken, String itemId) {
        DriveItem metadata = requireItem(accessToken, itemId);
        try {
            URI uri = fileUriBuilder(itemId).queryParam("alt", "media").build().encode().toUri();
            byte[] content = googleDriveRestClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(byte[].class);
            return new DriveBinary(content, metadata.name(), metadata.mimeType());
        } catch (RestClientException e) {
            throw new SharedFileDriveFailureException(e);
        }
    }

    // files.export. Google Workspace 파일을 요청한 형식(PDF/DOCX 등)으로 변환해 받는다(Task5).
    @Override
    public DriveBinary export(String accessToken, String itemId, GoogleWorkspaceExportFormat format) {
        DriveItem metadata = requireItem(accessToken, itemId);
        try {
            URI uri = fileUriBuilder(itemId).pathSegment("export")
                    .queryParam("mimeType", format.getExportMimeType())
                    .build().encode().toUri();
            byte[] content = googleDriveRestClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(byte[].class);
            String filename = metadata.name() + "." + format.getExtension();
            return new DriveBinary(content, filename, format.getExportMimeType());
        } catch (RestClientException e) {
            throw new SharedFileDriveFailureException(e);
        }
    }

    // getItem을 재사용해 대상이 없으면 404 예외를 던진다(다운로드·export는 메타데이터가 반드시 필요).
    private DriveItem requireItem(String accessToken, String itemId) {
        return getItem(accessToken, itemId).orElseThrow(() -> new SharedFileItemNotFoundException(itemId));
    }

    // getItem/updateItem/trash/downloadOriginal/export가 공유하는 "FILES_ENDPOINT/{itemId}" 빌더.
    // itemId를 문자열 연결로 이어붙이면 '&'·'#'·'=' 등이 섞였을 때 쿼리 구조가 깨지거나 의도치 않은
    // 파라미터가 주입될 수 있어(URI 인젝션), pathSegment()로 값 하나를 그대로 인코딩해 넣는다.
    private UriComponentsBuilder fileUriBuilder(String itemId) {
        return UriComponentsBuilder.fromUriString(FILES_ENDPOINT).pathSegment(itemId);
    }

    // updateItem/trash가 공유하는 PATCH 실행. request가 null이면 본문 없이 보낸다(updateItem이 이름은
    // 안 바꾸고 부모만 바꿀 때 사용).
    private DriveItem patch(String accessToken, URI uri, GoogleDriveUpdateFileRequest request) {
        RestClient.RequestBodySpec spec = googleDriveRestClient.patch()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        GoogleDriveFileResponse response = request == null
                ? spec.retrieve().body(GoogleDriveFileResponse.class)
                : spec.contentType(MediaType.APPLICATION_JSON).body(request).retrieve().body(GoogleDriveFileResponse.class);
        return toDriveItem(response);
    }

    // createRootFolder/createFolder/createWorkspaceFile이 공유하는 files.create 호출.
    // parentId가 null이면 parents 필드를 아예 생략해 Drive 최상위에 생성한다.
    private DriveItem createMetadataOnly(String accessToken, String parentId, String name, String mimeType) {
        try {
            List<String> parents = parentId == null ? null : List.of(parentId);
            GoogleDriveCreateFileRequest request = new GoogleDriveCreateFileRequest(name, mimeType, parents);
            GoogleDriveFileResponse response = googleDriveRestClient.post()
                    .uri(FILES_ENDPOINT + "?fields=" + FILE_FIELDS)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GoogleDriveFileResponse.class);
            return toDriveItem(response);
        } catch (RestClientException e) {
            throw new SharedFileDriveFailureException(e);
        }
    }

    // listChildren/searchByName이 공유하는 files.list 호출. cursor는 Drive의 nextPageToken을 그대로 전달한다.
    // query는 사용자 입력(검색어)을 포함할 수 있어 UriComponentsBuilder로 각 파라미터를 개별 인코딩한다 —
    // 문자열을 직접 이어붙이면 '&'가 파라미터를 끊고 '{'/'}'는 URI 템플릿으로 오인돼 예외가 난다.
    private DrivePage listFiles(String accessToken, String query, String cursor, int size) {
        try {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(FILES_ENDPOINT)
                    .queryParam("q", query)
                    .queryParam("pageSize", size)
                    .queryParam("fields", "nextPageToken,files(" + FILE_FIELDS + ")");
            if (cursor != null && !cursor.isBlank()) {
                uriBuilder.queryParam("pageToken", cursor);
            }
            URI uri = uriBuilder.build().encode().toUri();
            GoogleDriveFileListResponse response = googleDriveRestClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(GoogleDriveFileListResponse.class);
            List<DriveItem> items = response == null || response.files() == null
                    ? List.of()
                    : response.files().stream().map(this::toDriveItem).toList();
            String nextCursor = response == null ? null : response.nextPageToken();
            return new DrivePage(items, nextCursor);
        } catch (RestClientException e) {
            throw new SharedFileDriveFailureException(e);
        }
    }

    // Drive 응답 DTO를 도메인이 아는 값 객체(DriveItem)로 변환. null 필드는 안전한 기본값으로 치환한다.
    private DriveItem toDriveItem(GoogleDriveFileResponse response) {
        List<String> parentIds = response.parents() == null ? List.of() : response.parents();
        boolean downloadable = response.capabilities() != null
                && Boolean.TRUE.equals(response.capabilities().canDownload());
        LocalDateTime modifiedAt = response.modifiedTime() == null
                ? null
                : OffsetDateTime.parse(response.modifiedTime()).toLocalDateTime();
        boolean trashed = Boolean.TRUE.equals(response.trashed());
        return new DriveItem(
                response.id(), response.name(), response.mimeType(), parentIds,
                response.webViewLink(), downloadable, modifiedAt, trashed);
    }
}
