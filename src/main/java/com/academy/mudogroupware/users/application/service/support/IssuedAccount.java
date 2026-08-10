package com.academy.mudogroupware.users.application.service.support;

import com.academy.mudogroupware.users.domain.model.User;

public record IssuedAccount(User user, String passwordSetupLink) {
}
