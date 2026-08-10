package com.academy.mudogroupware.users.domain.model;

public record Permission(Long id, String code, String resource, String action, String description) {
}
