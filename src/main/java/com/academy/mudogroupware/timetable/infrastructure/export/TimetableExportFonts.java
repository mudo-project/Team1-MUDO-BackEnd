package com.academy.mudogroupware.timetable.infrastructure.export;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;

final class TimetableExportFonts {

    static final String REGULAR_RESOURCE = "/fonts/NanumGothic-Regular.ttf";
    static final String BOLD_RESOURCE = "/fonts/NanumGothic-Bold.ttf";

    static final Font AWT_REGULAR = loadAwtFont(REGULAR_RESOURCE);
    static final Font AWT_BOLD = loadAwtFont(BOLD_RESOURCE);

    private TimetableExportFonts() {
    }

    static byte[] readBytes(String resourcePath) {
        try (InputStream in = TimetableExportFonts.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("폰트 리소스를 찾을 수 없습니다: " + resourcePath);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("내보내기용 한글 폰트를 읽는 데 실패했습니다: " + resourcePath, e);
        }
    }

    private static Font loadAwtFont(String resourcePath) {
        try (InputStream in = TimetableExportFonts.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("폰트 리소스를 찾을 수 없습니다: " + resourcePath);
            }
            return Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (FontFormatException | IOException e) {
            throw new IllegalStateException("내보내기용 한글 폰트를 로드하는 데 실패했습니다: " + resourcePath, e);
        }
    }
}
