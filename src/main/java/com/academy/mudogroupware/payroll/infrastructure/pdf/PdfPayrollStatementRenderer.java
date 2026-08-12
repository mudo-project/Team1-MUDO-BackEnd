package com.academy.mudogroupware.payroll.infrastructure.pdf;

import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementRenderer;
import com.academy.mudogroupware.payroll.application.result.PayrollDetailResult;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class PdfPayrollStatementRenderer implements PayrollStatementRenderer {
  private static final BaseFont FONT = loadFont();

  @Override public byte[] render(PayrollDetailResult payroll) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    Document document = new Document(PageSize.A4, 40, 40, 40, 40);
    try {
      PdfWriter.getInstance(document, output);
      document.open();
      document.add(new Paragraph("%d년 %d월 급여명세서".formatted(
          payroll.yearMonth().getYear(), payroll.yearMonth().getMonthValue()), font(18, Font.BOLD)));
      document.add(new Paragraph("직원: " + payroll.employee().name(), font(11, Font.NORMAL)));
      document.add(new Paragraph("지급 예정일: " + payroll.scheduledPayDate(), font(11, Font.NORMAL)));
      document.add(Chunk.NEWLINE);
      document.add(table("지급 항목", payroll.earnings()));
      document.add(Chunk.NEWLINE);
      document.add(table("공제 항목", payroll.deductions()));
      document.add(Chunk.NEWLINE);
      document.add(new Paragraph("지급 합계: " + money(payroll.totalEarnings()), font(11, Font.BOLD)));
      document.add(new Paragraph("공제 합계: " + money(payroll.totalDeductions()), font(11, Font.BOLD)));
      document.add(new Paragraph("차인지급 예정액: " + money(payroll.netPay()), font(13, Font.BOLD)));
    } catch (DocumentException e) {
      throw new IllegalStateException("급여명세서 PDF 생성에 실패했습니다.", e);
    } finally {
      document.close();
    }
    return output.toByteArray();
  }

  private PdfPTable table(String title, java.util.List<PayrollDetailResult.Item> items) {
    PdfPTable table = new PdfPTable(new float[] {3, 2});
    table.setWidthPercentage(100);
    PdfPCell titleCell = new PdfPCell(new Phrase(title, font(12, Font.BOLD)));
    titleCell.setColspan(2);
    table.addCell(titleCell);
    table.addCell(cell("항목"));
    table.addCell(cell("금액"));
    for (PayrollDetailResult.Item item : items) {
      table.addCell(cell(item.name()));
      table.addCell(cell(money(item.amount())));
    }
    return table;
  }

  private PdfPCell cell(String text) { return new PdfPCell(new Phrase(text, font(10, Font.NORMAL))); }
  private Font font(float size, int style) { return new Font(FONT, size, style); }
  private String money(BigDecimal amount) {
    return amount == null ? "-" : NumberFormat.getNumberInstance(Locale.KOREA).format(amount) + "원";
  }
  private static BaseFont loadFont() {
    String path = "/fonts/NanumGothic-Regular.ttf";
    try (InputStream in = PdfPayrollStatementRenderer.class.getResourceAsStream(path)) {
      if (in == null) throw new IllegalStateException("급여명세서 한글 폰트를 찾을 수 없습니다.");
      return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, false,
          in.readAllBytes(), null);
    } catch (IOException | DocumentException e) {
      throw new IllegalStateException("급여명세서 한글 폰트를 읽을 수 없습니다.", e);
    }
  }
}
