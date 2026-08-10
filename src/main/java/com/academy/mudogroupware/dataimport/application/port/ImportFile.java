package com.academy.mudogroupware.dataimport.application.port;

import java.util.Arrays;

public record ImportFile(
        ImportFileRole role,
        String fileName,
        byte[] content
) {

    public ImportFile {
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        content = content != null ? Arrays.copyOf(content, content.length) : new byte[0];
    }

    public static ImportFile student(String fileName, byte[] content) {
        return new ImportFile(ImportFileRole.STUDENT, fileName, content);
    }

    public static ImportFile lecture(String fileName, byte[] content) {
        return new ImportFile(ImportFileRole.LECTURE, fileName, content);
    }

    public static ImportFile enrollment(String fileName, byte[] content) {
        return new ImportFile(ImportFileRole.ENROLLMENT, fileName, content);
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }
}
