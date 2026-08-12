package com.academy.mudogroupware.users.application.result;

import java.util.List;

public record MemberPage(
        List<MemberListItem> content, int page, int size, long totalElements, int totalPages, boolean hasNext) {

    public static MemberPage of(List<MemberListItem> content, int page, int size, long totalElements,
            boolean hasNext) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new MemberPage(content, page, size, totalElements, totalPages, hasNext);
    }
}
