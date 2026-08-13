package com.academy.mudogroupware.revenuereport.application.port;

import java.time.LocalDateTime;

public interface ExpenseSummaryPort {

    ExpenseSummary summarize(LocalDateTime from, LocalDateTime to);
}
