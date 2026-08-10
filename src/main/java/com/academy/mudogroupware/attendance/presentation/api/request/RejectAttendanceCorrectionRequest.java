package com.academy.mudogroupware.attendance.presentation.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectAttendanceCorrectionRequest(
        @NotBlank @Size(max = 500) String reason) {
}
