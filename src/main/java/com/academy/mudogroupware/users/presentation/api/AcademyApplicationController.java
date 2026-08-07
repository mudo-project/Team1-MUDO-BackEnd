package com.academy.mudogroupware.users.presentation.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.users.application.usecase.ListAcademyApplicationsUseCase;
import com.academy.mudogroupware.users.presentation.api.common.AcademyApplicationResponseCode;
import com.academy.mudogroupware.users.presentation.api.response.AcademyApplicationResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/academy-applications")
@RequiredArgsConstructor
public class AcademyApplicationController {

    private final ListAcademyApplicationsUseCase listAcademyApplicationsUseCase;

    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<AcademyApplicationResponse>>> list() {
        List<AcademyApplicationResponse> data = listAcademyApplicationsUseCase.listApplications().stream()
                .map(AcademyApplicationResponse::from)
                .toList();
        return ResponseEntity.ok(
                GlobalApiResponse.ok(AcademyApplicationResponseCode.ACADEMY_APPLICATION_LIST_FOUND, data));
    }
}
