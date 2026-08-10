package com.academy.mudogroupware.approval.application.port;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface LeaveRequestSubmissionPort {

    void submit(Long documentId, Long requesterId, LocalDate startDate, LocalDate endDate,
                LocalDateTime submittedAt);
}
