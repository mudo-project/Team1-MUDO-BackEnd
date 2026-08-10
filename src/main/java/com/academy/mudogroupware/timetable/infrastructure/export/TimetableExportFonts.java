package com.academy.mudogroupware.timetable.infrastructure.export;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;

final class TimetableExportFonts {

    // Inter는 한글 글리프가 없는 라틴 전용 폰트라, 한글은 나눔고딕으로 대체(폴백) 렌더링한다.
    static final String INTER_RESOURCE = "/fonts/Inter-Regular.ttf";
    static final String KOREAN_REGULAR_RESOURCE = "/fonts/NanumGothic-Regular.ttf";
    static final String KOREAN_BOLD_RESOURCE = "/fonts/NanumGothic-Bold.ttf";

    static final Font AWT_INTER = loadAwtFont(INTER_RESOURCE);
    static final Font AWT_KOREAN_REGULAR = loadAwtFont(KOREAN_REGULAR_RESOURCE);
    static final Font AWT_KOREAN_BOLD = loadAwtFont(KOREAN_BOLD_RESOURCE);

    private TimetableExportFonts() {
    }

    static byte[] readBytes(String resourcePath) {
        try (InputStream in = TimetableExportFonts.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("폰트 리소스를 찾을 수 없습니다: " + resourcePath);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("내보내기용 폰트를 읽는 데 실패했습니다: " + resourcePath, e);
        }
    }

    private static Font loadAwtFont(String resourcePath) {
        try (InputStream in = TimetableExportFonts.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("폰트 리소스를 찾을 수 없습니다: " + resourcePath);
            }
            return Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (FontFormatException | IOException e) {
            throw new IllegalStateException("내보내기용 폰트를 로드하는 데 실패했습니다: " + resourcePath, e);
        }
    }
}
