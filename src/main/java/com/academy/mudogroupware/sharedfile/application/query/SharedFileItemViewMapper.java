package com.academy.mudogroupware.sharedfile.application.query;

import com.academy.mudogroupware.sharedfile.application.port.DriveItem;

// 목록·검색·상세 조회 서비스가 공통으로 쓰는 DriveItem → SharedFileItemView 변환.
public final class SharedFileItemViewMapper {

    private SharedFileItemViewMapper() {
    }

    public static SharedFileItemView toView(DriveItem item) {
        return new SharedFileItemView(
                item.id(), item.name(), item.mimeType(), item.viewUrl(), item.downloadable(), item.modifiedAt());
    }
}
