package com.academy.mudogroupware.payroll.infrastructure.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalPayrollStatementStorageAdapterTest {
  @TempDir Path directory;

  @Test
  void 급여명세서를_로컬에_저장하고_다시_읽는다() {
    var adapter = new LocalPayrollStatementStorageAdapter(directory.toString());
    byte[] pdf = "test-pdf".getBytes(StandardCharsets.UTF_8);
    String key = "tenants/local/payroll-statements/2026/08/test.pdf";

    adapter.upload(key, pdf, "application/pdf");

    assertThat(adapter.download(key)).isEqualTo(pdf);
    assertThat(adapter.generateDownloadUrl(key, 300)).startsWith("file:");
  }

  @Test
  void 저장소_밖으로_벗어나는_경로를_거절한다() {
    var adapter = new LocalPayrollStatementStorageAdapter(directory.toString());

    assertThatThrownBy(() -> adapter.upload("../outside.pdf", new byte[0], "application/pdf"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
