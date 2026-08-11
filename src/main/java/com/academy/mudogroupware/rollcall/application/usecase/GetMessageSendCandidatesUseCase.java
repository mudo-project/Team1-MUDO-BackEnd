package com.academy.mudogroupware.rollcall.application.usecase;

import java.time.LocalDate;
import java.util.List;

import com.academy.mudogroupware.rollcall.application.query.MessageSendCandidateView;

public interface GetMessageSendCandidatesUseCase {

    List<MessageSendCandidateView> getCandidates(Long lectureId, LocalDate date);
}
