package com.academy.mudogroupware.global;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse; import org.junit.jupiter.api.Test; import static org.assertj.core.api.Assertions.assertThat;
class GlobalApiResponseTest { @Test void successShape(){GlobalApiResponse<String> r=GlobalApiResponse.ok("TEST_200","성공","data");assertThat(r.status()).isEqualTo(200);assertThat(r.code()).isEqualTo("TEST_200");assertThat(r.message()).isEqualTo("성공");assertThat(r.data()).isEqualTo("data");} }
