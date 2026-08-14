package com.academy.mudogroupware.sharedfile.infrastructure.external.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.academy.mudogroupware.sharedfile.application.port.DriveBinary;
import com.academy.mudogroupware.sharedfile.application.port.DriveItem;
import com.academy.mudogroupware.sharedfile.application.port.DrivePage;
import com.academy.mudogroupware.sharedfile.application.port.GoogleWorkspaceExportFormat;
import com.academy.mudogroupware.sharedfile.application.port.GoogleWorkspaceFileType;
import com.academy.mudogroupware.sharedfile.domain.exception.SharedFileDriveFailureException;
import com.fasterxml.jackson.databind.ObjectMapper;

class GoogleDriveAdapterTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final GoogleDriveAdapter adapter = new GoogleDriveAdapter(builder.build(), new ObjectMapper());

    @Test
    void getItemReturnsDriveItemWithBearerTokenWhenFound() {
        server.expect(requestTo(containsString("/files/item-id")))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andRespond(withSuccess("""
                        {
                          "id": "item-id",
                          "name": "자리.pdf",
                          "mimeType": "application/pdf",
                          "parents": ["root-id"],
                          "webViewLink": "https://drive.google.com/file/item-id",
                          "modifiedTime": "2026-08-01T00:00:00.000Z",
                          "trashed": false,
                          "capabilities": {"canDownload": true}
                        }
                        """, MediaType.APPLICATION_JSON));

        Optional<DriveItem> result = adapter.getItem("access-token", "item-id");

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("item-id");
        assertThat(result.get().name()).isEqualTo("자리.pdf");
        assertThat(result.get().parentIds()).containsExactly("root-id");
        assertThat(result.get().downloadable()).isTrue();
        assertThat(result.get().trashed()).isFalse();
    }

    @Test
    void getItemReturnsEmptyWhenDriveRespondsNotFound() {
        server.expect(requestTo(containsString("/files/missing-id")))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(adapter.getItem("access-token", "missing-id")).isEmpty();
    }

    @Test
    void getItemThrowsDriveFailureWhenDriveRespondsForbidden() {
        server.expect(requestTo(containsString("/files/forbidden-id")))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> adapter.getItem("access-token", "forbidden-id"))
                .isInstanceOf(SharedFileDriveFailureException.class);
    }

    @Test
    void getItemThrowsDriveFailureWhenDriveRespondsServerError() {
        server.expect(requestTo(containsString("/files/error-id")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adapter.getItem("access-token", "error-id"))
                .isInstanceOf(SharedFileDriveFailureException.class);
    }

    @Test
    void listChildrenReturnsPageWithItemsAndNextCursor() {
        server.expect(requestTo(containsString("/files?")))
                .andExpect(requestTo(containsString("q=")))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andRespond(withSuccess("""
                        {
                          "files": [
                            {"id": "child-1", "name": "폴더", "mimeType": "application/vnd.google-apps.folder",
                             "parents": ["parent-id"], "trashed": false}
                          ],
                          "nextPageToken": "next-cursor"
                        }
                        """, MediaType.APPLICATION_JSON));

        DrivePage page = adapter.listChildren("access-token", "parent-id", null, 20);

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().get(0).id()).isEqualTo("child-1");
        assertThat(page.nextCursor()).isEqualTo("next-cursor");
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    void searchByNameEncodesAmpersandInsteadOfSplittingTheQueryParam() {
        server.expect(requestTo(containsString("/files?")))
                // '&'가 인코딩(%26)되지 않고 그대로 있으면 q 파라미터가 여기서 끊기고 새 파라미터로 오인된다.
                .andExpect(requestTo(containsString("A%26B")))
                .andExpect(requestTo(org.hamcrest.Matchers.not(containsString("A&B"))))
                .andRespond(withSuccess("""
                        {"files": [], "nextPageToken": null}
                        """, MediaType.APPLICATION_JSON));

        DrivePage page = adapter.searchByName("access-token", "A&B", null, 20);

        assertThat(page.items()).isEmpty();
    }

    @Test
    void searchByNameDoesNotThrowWhenKeywordContainsCurlyBraces() {
        server.expect(requestTo(containsString("/files?")))
                .andRespond(withSuccess("""
                        {"files": [], "nextPageToken": null}
                        """, MediaType.APPLICATION_JSON));

        assertThatCode(() -> adapter.searchByName("access-token", "A{B}C", null, 20))
                .doesNotThrowAnyException();
    }

    @Test
    void createFolderSendsFolderMimeTypeAndParent() {
        server.expect(requestTo(containsString("/files?")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andExpect(content().string(containsString("\"mimeType\":\"application/vnd.google-apps.folder\"")))
                .andExpect(content().string(containsString("\"parents\":[\"parent-id\"]")))
                .andExpect(content().string(containsString("\"name\":\"새 폴더\"")))
                .andRespond(withSuccess("""
                        {"id": "new-folder-id", "name": "새 폴더", "mimeType": "application/vnd.google-apps.folder",
                         "parents": ["parent-id"], "trashed": false}
                        """, MediaType.APPLICATION_JSON));

        DriveItem created = adapter.createFolder("access-token", "parent-id", "새 폴더");

        assertThat(created.id()).isEqualTo("new-folder-id");
        assertThat(created.mimeType()).isEqualTo("application/vnd.google-apps.folder");
    }

    @Test
    void createRootFolderSendsRequestWithoutParents() {
        server.expect(requestTo(containsString("/files?")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("\"mimeType\":\"application/vnd.google-apps.folder\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("\"parents\""))))
                .andRespond(withSuccess("""
                        {"id": "root-folder-id", "name": "이음 그룹웨어 - 공유파일",
                         "mimeType": "application/vnd.google-apps.folder", "trashed": false}
                        """, MediaType.APPLICATION_JSON));

        DriveItem created = adapter.createRootFolder("access-token", "이음 그룹웨어 - 공유파일");

        assertThat(created.id()).isEqualTo("root-folder-id");
    }

    @Test
    void createWorkspaceFileMapsDocsTypeToGoogleMimeType() {
        server.expect(requestTo(containsString("/files?")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("\"mimeType\":\"application/vnd.google-apps.document\"")))
                .andExpect(content().string(containsString("\"parents\":[\"parent-id\"]")))
                .andRespond(withSuccess("""
                        {"id": "docs-id", "name": "새 문서", "mimeType": "application/vnd.google-apps.document",
                         "parents": ["parent-id"], "trashed": false}
                        """, MediaType.APPLICATION_JSON));

        DriveItem created = adapter.createWorkspaceFile("access-token", "parent-id", "새 문서", GoogleWorkspaceFileType.DOCS);

        assertThat(created.id()).isEqualTo("docs-id");
    }

    @Test
    void uploadSendsMultipartRelatedBodyWithMetadataAndContent() {
        server.expect(requestTo(containsString("/upload/drive/v3/files")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andExpect(header("Content-Type", org.hamcrest.Matchers.containsString("multipart/related")))
                .andExpect(content().string(containsString("\"name\":\"파일.txt\"")))
                .andExpect(content().string(containsString("\"parents\":[\"parent-id\"]")))
                .andExpect(content().string(containsString("text/plain")))
                .andExpect(content().string(containsString("파일 내용")))
                .andRespond(withSuccess("""
                        {"id": "uploaded-id", "name": "파일.txt", "mimeType": "text/plain",
                         "parents": ["parent-id"], "trashed": false}
                        """, MediaType.APPLICATION_JSON));

        DriveItem uploaded = adapter.upload("access-token", "parent-id", "파일.txt", "text/plain",
                "파일 내용".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertThat(uploaded.id()).isEqualTo("uploaded-id");
    }

    @Test
    void uploadDoesNotUseAFixedBoundary() {
        // 고정 boundary였다면 파일 내용에 그 값이 섞여 있을 때 파트 경계가 깨질 수 있다.
        server.expect(requestTo(containsString("/upload/drive/v3/files")))
                .andExpect(header("Content-Type",
                        org.hamcrest.Matchers.not(containsString("boundary=shared_file_upload_boundary"))))
                .andRespond(withSuccess("""
                        {"id": "uploaded-id", "name": "a.txt", "mimeType": "text/plain", "trashed": false}
                        """, MediaType.APPLICATION_JSON));

        adapter.upload("access-token", "parent-id", "a.txt", "text/plain", "content".getBytes());
    }

    @Test
    void uploadFallsBackToOctetStreamWhenContentTypeIsInvalid() {
        server.expect(requestTo(containsString("/upload/drive/v3/files")))
                .andExpect(content().string(containsString("Content-Type: application/octet-stream")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("X-Injected"))))
                .andRespond(withSuccess("""
                        {"id": "uploaded-id", "name": "a.txt", "mimeType": "application/octet-stream", "trashed": false}
                        """, MediaType.APPLICATION_JSON));

        adapter.upload("access-token", "parent-id", "a.txt",
                "text/plain\r\nX-Injected: evil", "content".getBytes());
    }

    @Test
    void updateItemSendsPatchWithNewNameOnly() {
        server.expect(requestTo(containsString("/files/item-id")))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andExpect(content().string(containsString("\"name\":\"바뀐 이름.pdf\"")))
                .andExpect(requestTo(org.hamcrest.Matchers.not(containsString("addParents"))))
                .andRespond(withSuccess("""
                        {"id": "item-id", "name": "바뀐 이름.pdf", "mimeType": "application/pdf",
                         "parents": ["parent-id"], "trashed": false}
                        """, MediaType.APPLICATION_JSON));

        DriveItem renamed = adapter.updateItem("access-token", "item-id", "바뀐 이름.pdf", null, null);

        assertThat(renamed.name()).isEqualTo("바뀐 이름.pdf");
    }

    @Test
    void updateItemSendsPatchWithAddAndRemoveParentsOnly() {
        server.expect(requestTo(containsString("addParents=to-parent")))
                .andExpect(requestTo(containsString("removeParents=from-parent")))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(content().string(""))
                .andRespond(withSuccess("""
                        {"id": "item-id", "name": "파일.pdf", "mimeType": "application/pdf",
                         "parents": ["to-parent"], "trashed": false}
                        """, MediaType.APPLICATION_JSON));

        DriveItem moved = adapter.updateItem("access-token", "item-id", null, "from-parent", "to-parent");

        assertThat(moved.parentIds()).containsExactly("to-parent");
    }

    // 이름변경+이동 원자성의 핵심 증거: 두 변경이 같은 PATCH 요청 하나에 함께 실린다(호출이 2번이 아니라 1번).
    @Test
    void updateItemSendsNameAndParentsInASinglePatchRequest() {
        server.expect(requestTo(containsString("/files/item-id")))
                .andExpect(requestTo(containsString("addParents=to-parent")))
                .andExpect(requestTo(containsString("removeParents=from-parent")))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(content().string(containsString("\"name\":\"바뀐 이름.pdf\"")))
                .andRespond(withSuccess("""
                        {"id": "item-id", "name": "바뀐 이름.pdf", "mimeType": "application/pdf",
                         "parents": ["to-parent"], "trashed": false}
                        """, MediaType.APPLICATION_JSON));

        DriveItem updated = adapter.updateItem("access-token", "item-id", "바뀐 이름.pdf", "from-parent", "to-parent");

        assertThat(updated.name()).isEqualTo("바뀐 이름.pdf");
        assertThat(updated.parentIds()).containsExactly("to-parent");
    }

    @Test
    void trashSendsPatchWithTrashedTrue() {
        server.expect(requestTo(containsString("/files/item-id")))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(content().string(containsString("\"trashed\":true")))
                .andRespond(withSuccess("""
                        {"id": "item-id", "name": "파일.pdf", "mimeType": "application/pdf",
                         "parents": ["parent-id"], "trashed": true}
                        """, MediaType.APPLICATION_JSON));

        adapter.trash("access-token", "item-id");
    }

    @Test
    void downloadOriginalReturnsBinaryWithMetadataNameAndContentType() {
        server.expect(requestTo(containsString("/files/item-id")))
                .andExpect(requestTo(containsString("fields=")))
                .andRespond(withSuccess("""
                        {"id": "item-id", "name": "원본.pdf", "mimeType": "application/pdf",
                         "parents": ["parent-id"], "trashed": false}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("alt=media")))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andRespond(withSuccess("binary-content".getBytes(), MediaType.APPLICATION_OCTET_STREAM));

        DriveBinary binary = adapter.downloadOriginal("access-token", "item-id");

        assertThat(binary.filename()).isEqualTo("원본.pdf");
        assertThat(binary.contentType()).isEqualTo("application/pdf");
        assertThat(new String(binary.content())).isEqualTo("binary-content");
    }

    @Test
    void exportReturnsBinaryWithExportMimeTypeAndExtension() {
        server.expect(requestTo(containsString("/files/item-id")))
                .andExpect(requestTo(containsString("fields=")))
                .andRespond(withSuccess("""
                        {"id": "item-id", "name": "문서", "mimeType": "application/vnd.google-apps.document",
                         "parents": ["parent-id"], "trashed": false}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/files/item-id/export")))
                .andExpect(requestTo(containsString("mimeType=application/pdf")))
                .andRespond(withSuccess("pdf-bytes".getBytes(), MediaType.APPLICATION_PDF));

        DriveBinary binary = adapter.export("access-token", "item-id", GoogleWorkspaceExportFormat.DOCS_PDF);

        assertThat(binary.filename()).isEqualTo("문서.pdf");
        assertThat(binary.contentType()).isEqualTo("application/pdf");
    }
}
