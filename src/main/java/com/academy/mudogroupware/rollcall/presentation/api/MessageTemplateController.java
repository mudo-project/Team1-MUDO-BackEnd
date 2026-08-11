package com.academy.mudogroupware.rollcall.presentation.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.rollcall.application.usecase.CreateMessageTemplateUseCase;
import com.academy.mudogroupware.rollcall.application.usecase.DeleteMessageTemplateUseCase;
import com.academy.mudogroupware.rollcall.application.usecase.MessageTemplateQueryUseCase;
import com.academy.mudogroupware.rollcall.application.usecase.UpdateMessageTemplateUseCase;
import com.academy.mudogroupware.rollcall.presentation.api.common.RollcallResponseCode;
import com.academy.mudogroupware.rollcall.presentation.api.request.CreateMessageTemplateRequest;
import com.academy.mudogroupware.rollcall.presentation.api.request.UpdateMessageTemplateRequest;
import com.academy.mudogroupware.rollcall.presentation.api.response.MessageTemplateCreateResponse;
import com.academy.mudogroupware.rollcall.presentation.api.response.MessageTemplateResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "문자 템플릿", description = "출결 상태별 문자 템플릿 CRUD API")
@RestController
@RequestMapping("/api/rollcall/message-templates")
@RequiredArgsConstructor
public class MessageTemplateController {

    private final CreateMessageTemplateUseCase createMessageTemplateUseCase;
    private final MessageTemplateQueryUseCase messageTemplateQueryUseCase;
    private final UpdateMessageTemplateUseCase updateMessageTemplateUseCase;
    private final DeleteMessageTemplateUseCase deleteMessageTemplateUseCase;

    @Operation(summary = "문자 템플릿 생성", description = "출결 상태 1개당 템플릿 1개만 만들 수 있다. 이미 있으면 409.")
    @PreAuthorize("hasAuthority('ROLLCALL:TEMPLATE_MANAGE')")
    @PostMapping
    public ResponseEntity<GlobalApiResponse<MessageTemplateCreateResponse>> createTemplate(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CreateMessageTemplateRequest request) {
        Long templateId = createMessageTemplateUseCase.createTemplate(
                request.toCommand(authUser.userId()));
        MessageTemplateCreateResponse data = MessageTemplateCreateResponse.from(templateId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(RollcallResponseCode.TEMPLATE_CREATED, data));
    }

    @Operation(summary = "문자 템플릿 목록 조회", description = "요청자 소속 학원의 문자 템플릿을 전부 조회한다.")
    @PreAuthorize("hasAnyAuthority('ROLLCALL:MANAGE', 'ROLLCALL:TEMPLATE_MANAGE')")
    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<MessageTemplateResponse>>> getTemplates() {
        List<MessageTemplateResponse> responses = messageTemplateQueryUseCase.getTemplates()
                .stream()
                .map(MessageTemplateResponse::from)
                .toList();
        return ResponseEntity.ok(GlobalApiResponse.ok(RollcallResponseCode.TEMPLATE_LIST_RETRIEVED, responses));
    }

    @Operation(summary = "문자 템플릿 수정", description = "이름과 내용을 수정한다. 상태는 변경할 수 없다.")
    @PreAuthorize("hasAuthority('ROLLCALL:TEMPLATE_MANAGE')")
    @PatchMapping("/{templateId}")
    public ResponseEntity<Void> updateTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody UpdateMessageTemplateRequest request) {
        updateMessageTemplateUseCase.updateTemplate(request.toCommand(templateId));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "문자 템플릿 삭제")
    @PreAuthorize("hasAuthority('ROLLCALL:TEMPLATE_MANAGE')")
    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable Long templateId) {
        deleteMessageTemplateUseCase.deleteTemplate(templateId);
        return ResponseEntity.noContent().build();
    }
}
