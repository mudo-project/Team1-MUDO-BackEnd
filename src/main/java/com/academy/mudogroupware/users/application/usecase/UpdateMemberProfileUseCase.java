package com.academy.mudogroupware.users.application.usecase;

import java.time.LocalDateTime;

public interface UpdateMemberProfileUseCase {
    void updateMemberProfile(Long academyId, Long userId, String name, String phone, String email,
                              LocalDateTime joinedAt);
}
