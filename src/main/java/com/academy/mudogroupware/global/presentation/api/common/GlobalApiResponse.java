package com.academy.mudogroupware.global.presentation.api.common;

import org.springframework.http.HttpStatus;

public record GlobalApiResponse<T>(int status, String code, String message, T data) {
  public static <T> GlobalApiResponse<T> of(HttpStatus s, String c, String m, T d) {
    return new GlobalApiResponse<>(s.value(), c, m, d);
  }

  public static GlobalApiResponse<Void> of(HttpStatus s, String c, String m) {
    return of(s, c, m, null);
  }

  public static <T> GlobalApiResponse<T> ok(String c, String m, T d) {
    return of(HttpStatus.OK, c, m, d);
  }

  public static GlobalApiResponse<Void> ok(String c, String m) {
    return of(HttpStatus.OK, c, m);
  }

  public static <T> GlobalApiResponse<T> ok(ResponseCode c, T d) {
    return ok(c.getCode(), c.getMessage(), d);
  }

  public static GlobalApiResponse<Void> ok(ResponseCode c) {
    return ok(c.getCode(), c.getMessage());
  }

  public static <T> GlobalApiResponse<T> created(String c, String m, T d) {
    return of(HttpStatus.CREATED, c, m, d);
  }

  public static GlobalApiResponse<Void> created(String c, String m) {
    return of(HttpStatus.CREATED, c, m);
  }

  public static <T> GlobalApiResponse<T> created(ResponseCode c, T d) {
    return created(c.getCode(), c.getMessage(), d);
  }

  public static GlobalApiResponse<Void> created(ResponseCode c) {
    return created(c.getCode(), c.getMessage());
  }
}
