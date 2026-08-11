package com.academy.mudogroupware.sharedfile.application.port;

// 다운로드/export 결과 바이너리. Controller가 이 값으로 Content-Disposition·Content-Type을 채운다.
public record DriveBinary(byte[] content, String filename, String contentType) {
}
