package com.academy.mudogroupware.users.application.usecase;

public interface ChangeMyPasswordUseCase {
    void changePassword(Long userId, String currentPassword, String newPassword);
}
