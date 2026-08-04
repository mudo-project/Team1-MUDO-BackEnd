package com.academy.mudogroupware.approval.infrastructure.external.gemini;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record GeminiProperties(String apiKey, String model) {
  public GeminiProperties(
      @Value("${GEMINI_API_KEY:}") String apiKey,
      @Value("${GEMINI_MODEL:gemini-2.0-flash}") String model) {
    this.apiKey = apiKey;
    this.model = model;
  }
}
