package com.academy.mudogroupware.sharedfile.presentation.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.academy.mudogroupware.sharedfile.application.port.DriveBinary;
import com.academy.mudogroupware.sharedfile.application.query.ExportTargetFormat;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemType;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemView;
import com.academy.mudogroupware.sharedfile.application.usecase.CreateGoogleWorkspaceFileUseCase;
import com.academy.mudogroupware.sharedfile.application.usecase.CreateSharedFolderUseCase;
import com.academy.mudogroupware.sharedfile.application.usecase.DownloadSharedFileUseCase;
import com.academy.mudogroupware.sharedfile.application.usecase.GetSharedFileItemUseCase;
import com.academy.mudogroupware.sharedfile.application.usecase.GetSharedFileRootUseCase;
import com.academy.mudogroupware.sharedfile.application.usecase.ListSharedFileItemsUseCase;
import com.academy.mudogroupware.sharedfile.application.usecase.MoveSharedFileItemUseCase;
import com.academy.mudogroupware.sharedfile.application.usecase.RecreateSharedFileRootUseCase;
import com.academy.mudogroupware.sharedfile.application.usecase.RenameSharedFileItemUseCase;
import com.academy.mudogroupware.sharedfile.application.usecase.SearchSharedFileItemsUseCase;
import com.academy.mudogroupware.sharedfile.application.usecase.TrashSharedFileItemUseCase;
import com.academy.mudogroupware.sharedfile.application.usecase.UploadSharedFileUseCase;
import com.academy.mudogroupware.sharedfile.presentation.api.common.SharedFileResponseCode;
import com.academy.mudogroupware.sharedfile.presentation.api.request.CreateGoogleWorkspaceFileRequest;
import com.academy.mudogroupware.sharedfile.presentation.api.request.CreateSharedFolderRequest;
import com.academy.mudogroupware.sharedfile.presentation.api.request.UpdateSharedFileItemRequest;
import com.academy.mudogroupware.sharedfile.presentation.api.response.SharedFileItemResponse;
import com.academy.mudogroupware.sharedfile.presentation.api.response.SharedFileItemsResponse;
import com.academy.mudogroupware.sharedfile.presentation.api.response.SharedFileRootResponse;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Tag(name = "공유파일", description = "학원 공용 Google Drive 기반 공유파일 API")
@RestController
@RequestMapping("/api/shared-files")
@RequiredArgsConstructor
@Validated
public class SharedFileController {

    private static final String MANAGE_PERMISSION = "hasAuthority('SHAREDFILE:MANAGE')";
    private static final String ROOT_MANAGE_PERMISSION = "hasAuthority('SHAREDFILE:ROOT_MANAGE')";

    private final GetSharedFileRootUseCase getSharedFileRootUseCase;
    private final RecreateSharedFileRootUseCase recreateSharedFileRootUseCase;
    private final ListSharedFileItemsUseCase listSharedFileItemsUseCase;
    private final GetSharedFileItemUseCase getSharedFileItemUseCase;
    private final SearchSharedFileItemsUseCase searchSharedFileItemsUseCase;
    private final CreateSharedFolderUseCase createSharedFolderUseCase;
    private final UploadSharedFileUseCase uploadSharedFileUseCase;
    private final CreateGoogleWorkspaceFileUseCase createGoogleWorkspaceFileUseCase;
    private final RenameSharedFileItemUseCase renameSharedFileItemUseCase;
    private final MoveSharedFileItemUseCase moveSharedFileItemUseCase;
    private final TrashSharedFileItemUseCase trashSharedFileItemUseCase;
    private final DownloadSharedFileUseCase downloadSharedFileUseCase;

    @Operation(summary = "시스템 루트 상태 조회", description = "공유파일 시스템 루트를 지금 사용할 수 있는지 조회합니다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "조회 성공")})
    @PreAuthorize(MANAGE_PERMISSION)
    @GetMapping("/root")
    public ResponseEntity<GlobalApiResponse<SharedFileRootResponse>> getRoot() {
        SharedFileRootResponse response = SharedFileRootResponse.from(getSharedFileRootUseCase.getRoot());
        return ResponseEntity.ok(GlobalApiResponse.ok(SharedFileResponseCode.ROOT_RETRIEVED, response));
    }

    @Operation(summary = "시스템 루트 재생성",
            description = "시스템 루트 생성이 실패했거나 Google Drive에서 삭제가 확인된 경우 다시 생성합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "재생성 성공"),
        @ApiResponse(responseCode = "409", description = "이미 사용 가능한 시스템 루트가 존재함")
    })
    @PreAuthorize(ROOT_MANAGE_PERMISSION)
    @PostMapping("/root/recreation")
    public ResponseEntity<GlobalApiResponse<SharedFileRootResponse>> recreateRoot() {
        SharedFileRootResponse response =
                SharedFileRootResponse.from(recreateSharedFileRootUseCase.recreate());
        return ResponseEntity.ok(GlobalApiResponse.ok(SharedFileResponseCode.ROOT_RECREATED, response));
    }

    @Operation(summary = "현재 폴더 목록", description = "parentId 하위의 파일·폴더 목록을 cursor 기반으로 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "409", description = "시스템 루트가 아직 준비되지 않음")
    })
    @PreAuthorize(MANAGE_PERMISSION)
    @GetMapping("/items")
    public ResponseEntity<GlobalApiResponse<SharedFileItemsResponse>> getItems(
            @RequestParam(required = false) String parentId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        SharedFileItemsResponse response =
                SharedFileItemsResponse.from(listSharedFileItemsUseCase.list(parentId, cursor, size));
        return ResponseEntity.ok(GlobalApiResponse.ok(SharedFileResponseCode.ITEMS_LISTED, response));
    }

    @Operation(summary = "파일·폴더 상세 조회", description = "itemId 하나의 메타데이터를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "403", description = "시스템 루트 밖 항목"),
        @ApiResponse(responseCode = "404", description = "파일·폴더 없음")
    })
    @PreAuthorize(MANAGE_PERMISSION)
    @GetMapping("/items/{itemId}")
    public ResponseEntity<GlobalApiResponse<SharedFileItemResponse>> getItem(@PathVariable String itemId) {
        SharedFileItemView view = getSharedFileItemUseCase.get(itemId);
        return ResponseEntity.ok(
                GlobalApiResponse.ok(SharedFileResponseCode.ITEM_RETRIEVED, SharedFileItemResponse.from(view)));
    }

    @Operation(summary = "시스템 루트 전체 검색", description = "이름으로 검색하고, 시스템 루트 하위 결과만 반환합니다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "검색 성공")})
    @PreAuthorize(MANAGE_PERMISSION)
    @GetMapping("/items/search")
    public ResponseEntity<GlobalApiResponse<SharedFileItemsResponse>> searchItems(
            @RequestParam String keyword,
            @RequestParam(required = false) SharedFileItemType type,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        SharedFileItemsResponse response = SharedFileItemsResponse.from(
                searchSharedFileItemsUseCase.search(keyword, type, cursor, size));
        return ResponseEntity.ok(GlobalApiResponse.ok(SharedFileResponseCode.ITEMS_SEARCHED, response));
    }

    @Operation(summary = "하위 폴더 생성", description = "parentId 하위에 새 폴더를 만듭니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "생성 성공"),
        @ApiResponse(responseCode = "400", description = "이름이 올바르지 않음")
    })
    @PreAuthorize(MANAGE_PERMISSION)
    @PostMapping("/folders")
    public ResponseEntity<GlobalApiResponse<SharedFileItemResponse>> createFolder(
            @Valid @RequestBody CreateSharedFolderRequest request) {
        SharedFileItemView view = createSharedFolderUseCase.create(request.parentId(), request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                GlobalApiResponse.created(SharedFileResponseCode.FOLDER_CREATED, SharedFileItemResponse.from(view)));
    }

    @Operation(summary = "로컬 파일 업로드", description = "최대 100MB의 파일 한 개를 parentId 하위에 업로드합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "업로드 성공"),
        @ApiResponse(responseCode = "400", description = "이름이 올바르지 않거나 100MB를 초과함")
    })
    @PreAuthorize(MANAGE_PERMISSION)
    @PostMapping(value = "/items/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GlobalApiResponse<SharedFileItemResponse>> uploadItem(
            @RequestParam String parentId,
            @RequestPart MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 비어 있습니다.");
        }
        SharedFileItemView view = uploadSharedFileUseCase.upload(
                parentId, file.getOriginalFilename(), file.getContentType(), file.getBytes());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                GlobalApiResponse.created(SharedFileResponseCode.FILE_UPLOADED, SharedFileItemResponse.from(view)));
    }

    @Operation(summary = "Google 파일 생성", description = "Docs·Sheets·Slides 중 하나의 빈 파일을 생성합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "생성 성공"),
        @ApiResponse(responseCode = "400", description = "이름이 올바르지 않음")
    })
    @PreAuthorize(MANAGE_PERMISSION)
    @PostMapping("/google-files")
    public ResponseEntity<GlobalApiResponse<SharedFileItemResponse>> createGoogleFile(
            @Valid @RequestBody CreateGoogleWorkspaceFileRequest request) {
        SharedFileItemView view = createGoogleWorkspaceFileUseCase.create(
                request.parentId(), request.name(), request.type());
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalApiResponse.created(
                SharedFileResponseCode.GOOGLE_FILE_CREATED, SharedFileItemResponse.from(view)));
    }

    @Operation(summary = "이름 변경·이동", description = "name만 있으면 이름 변경, parentId만 있으면 이동, 둘 다 있으면 둘 다 적용합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "변경 성공"),
        @ApiResponse(responseCode = "400", description = "name과 parentId가 모두 없거나 이름이 올바르지 않음"),
        @ApiResponse(responseCode = "403", description = "시스템 루트 밖 항목이거나 시스템 루트 자체를 변경하려 함")
    })
    @PreAuthorize(MANAGE_PERMISSION)
    @PatchMapping("/items/{itemId}")
    public ResponseEntity<GlobalApiResponse<SharedFileItemResponse>> updateItem(
            @PathVariable String itemId,
            @RequestBody UpdateSharedFileItemRequest request) {
        if (!request.hasAnyChange()) {
            throw new IllegalArgumentException("name 또는 parentId 중 하나는 필수입니다.");
        }
        SharedFileItemView view = null;
        if (request.name() != null && !request.name().isBlank()) {
            view = renameSharedFileItemUseCase.rename(itemId, request.name());
        }
        if (request.parentId() != null && !request.parentId().isBlank()) {
            view = moveSharedFileItemUseCase.move(itemId, request.parentId());
        }
        return ResponseEntity.ok(
                GlobalApiResponse.ok(SharedFileResponseCode.ITEM_UPDATED, SharedFileItemResponse.from(view)));
    }

    @Operation(summary = "휴지통 삭제", description = "Google Drive 휴지통으로 이동합니다. 영구 삭제가 아닙니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "403", description = "시스템 루트 밖 항목이거나 시스템 루트 자체를 삭제하려 함")
    })
    @PreAuthorize(MANAGE_PERMISSION)
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> trashItem(@PathVariable String itemId) {
        trashSharedFileItemUseCase.trash(itemId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "원본·변환 다운로드",
            description = "format이 없으면 원본을, 있으면 지정한 형식으로 변환해 다운로드합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "다운로드 성공"),
        @ApiResponse(responseCode = "400", description = "지원하지 않는 변환 형식")
    })
    @PreAuthorize(MANAGE_PERMISSION)
    @GetMapping("/items/{itemId}/download")
    public ResponseEntity<byte[]> downloadItem(
            @PathVariable String itemId,
            @RequestParam(required = false) ExportTargetFormat format) {
        DriveBinary binary = downloadSharedFileUseCase.download(itemId, format);
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(binary.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(binary.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(binary.content());
    }
}
