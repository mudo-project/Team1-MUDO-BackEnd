package com.academy.mudogroupware.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import com.academy.mudogroupware.approval.presentation.api.ApprovalController;
import com.academy.mudogroupware.approval.presentation.api.ApprovalTemplateController;
import com.academy.mudogroupware.lecture.presentation.api.LectureController;
import com.academy.mudogroupware.notice.presentation.api.NoticeController;
import com.academy.mudogroupware.rollcall.presentation.api.MessageTemplateController;
import com.academy.mudogroupware.rollcall.presentation.api.RollcallController;
import com.academy.mudogroupware.student.presentation.api.StudentController;

class DomainPermissionPolicyTest {

    @Test
    void studentManagementUsesSingleBusinessPermission() {
        assertPolicy(StudentController.class, "createStudent", "hasAuthority('STUDENT:MANAGE')");
        assertPolicy(StudentController.class, "getStudents", "hasAuthority('STUDENT:MANAGE')");
        assertPolicy(StudentController.class, "getStudentDetail", "hasAuthority('STUDENT:MANAGE')");
        assertPolicy(StudentController.class, "updateStudent", "hasAuthority('STUDENT:MANAGE')");
        assertPolicy(StudentController.class, "deleteStudent", "hasAuthority('STUDENT:MANAGE')");
        assertPolicy(StudentController.class, "enroll", "hasAuthority('STUDENT:MANAGE')");
        assertPolicy(StudentController.class, "endEnrollment", "hasAuthority('STUDENT:MANAGE')");
    }

    @Test
    void lectureReadAndManagePermissionsAreSeparatedByBusinessTask() {
        assertPolicy(LectureController.class, "createLecture", "hasAuthority('LECTURE:MANAGE')");
        assertPolicy(
                LectureController.class,
                "getLectures",
                "hasAnyAuthority('LECTURE:READ', 'LECTURE:MANAGE')");
        assertPolicy(
                LectureController.class,
                "getLectureDetail",
                "hasAnyAuthority('LECTURE:READ', 'LECTURE:MANAGE')");
    }

    @Test
    void rollcallManagementUsesOnePermissionButTemplateManagementIsSeparated() {
        assertPolicy(RollcallController.class, "getRoster", "hasAuthority('ROLLCALL:MANAGE')");
        assertPolicy(RollcallController.class, "saveEntries", "hasAuthority('ROLLCALL:MANAGE')");
        assertPolicy(RollcallController.class, "exportSheet", "hasAuthority('ROLLCALL:MANAGE')");
        assertPolicy(RollcallController.class, "getMessageCandidates", "hasAuthority('ROLLCALL:MANAGE')");
        assertPolicy(RollcallController.class, "sendMessages", "hasAuthority('ROLLCALL:MANAGE')");

        assertPolicy(
                MessageTemplateController.class,
                "getTemplates",
                "hasAnyAuthority('ROLLCALL:MANAGE', 'ROLLCALL:TEMPLATE_MANAGE')");
        assertPolicy(MessageTemplateController.class, "createTemplate", "hasAuthority('ROLLCALL:TEMPLATE_MANAGE')");
        assertPolicy(MessageTemplateController.class, "updateTemplate", "hasAuthority('ROLLCALL:TEMPLATE_MANAGE')");
        assertPolicy(MessageTemplateController.class, "deleteTemplate", "hasAuthority('ROLLCALL:TEMPLATE_MANAGE')");
    }

    @Test
    void approvalSubmitTemplateManagementAndReadAllHaveSeparatePermissions() {
        assertPolicy(ApprovalController.class, "createDocument", "hasAuthority('APPROVAL:SUBMIT')");
        assertPolicy(ApprovalController.class, "getAllApprovals", "hasAuthority('APPROVAL:READ_ALL')");
        assertPolicy(ApprovalController.class, "resubmit", "hasAuthority('APPROVAL:SUBMIT')");

        assertPolicy(
                ApprovalTemplateController.class,
                "getTemplates",
                "hasAnyAuthority('APPROVAL:SUBMIT', 'APPROVAL:TEMPLATE_MANAGE')");
        assertPolicy(
                ApprovalTemplateController.class,
                "getTemplateDetail",
                "hasAnyAuthority('APPROVAL:SUBMIT', 'APPROVAL:TEMPLATE_MANAGE')");
        assertPolicy(
                ApprovalTemplateController.class,
                "createTemplate",
                "hasAuthority('APPROVAL:TEMPLATE_MANAGE')");
        assertPolicy(
                ApprovalTemplateController.class,
                "updateTemplate",
                "hasAuthority('APPROVAL:TEMPLATE_MANAGE')");
        assertPolicy(
                ApprovalTemplateController.class,
                "deleteTemplate",
                "hasAuthority('APPROVAL:TEMPLATE_MANAGE')");
    }

    @Test
    void noticeWriteAndPinPermissionsAreSeparatedFromOwnerOnlyDelete() {
        assertPolicy(NoticeController.class, "createNotice", "hasAuthority('NOTICE:WRITE')");
        assertPolicy(NoticeController.class, "updateNotice", "hasAuthority('NOTICE:WRITE')");
        assertPolicy(NoticeController.class, "pinNotice", "hasAuthority('NOTICE:PIN')");
        assertPolicy(NoticeController.class, "unpinNotice", "hasAuthority('NOTICE:PIN')");
        assertNoPolicy(NoticeController.class, "deleteNotice");
    }

    private static void assertPolicy(Class<?> controllerType, String methodName, String expectedExpression) {
        PreAuthorize preAuthorize = method(controllerType, methodName).getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
                .as("%s.%s @PreAuthorize", controllerType.getSimpleName(), methodName)
                .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(expectedExpression);
    }

    private static void assertNoPolicy(Class<?> controllerType, String methodName) {
        PreAuthorize preAuthorize = method(controllerType, methodName).getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
                .as("%s.%s should rely on owner validation only", controllerType.getSimpleName(), methodName)
                .isNull();
    }

    private static Method method(Class<?> controllerType, String methodName) {
        return Arrays.stream(controllerType.getDeclaredMethods())
                .filter(method -> method.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        controllerType.getSimpleName() + "." + methodName + " method not found"));
    }
}
