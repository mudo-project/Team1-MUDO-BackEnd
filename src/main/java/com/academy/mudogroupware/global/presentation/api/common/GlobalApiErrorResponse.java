package com.academy.mudogroupware.global.presentation.api.common;
import com.academy.mudogroupware.global.domain.common.exception.*; import com.fasterxml.jackson.annotation.JsonFormat; import java.time.LocalDateTime; import java.util.Map;
public record GlobalApiErrorResponse(@JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss") LocalDateTime timestamp,int status,String code,String message,String traceId,Map<String,Object> details){
 public static GlobalApiErrorResponse of(ApplicationException e,String t){return of(e.getErrorCode(),e.getMessage(),t,e.getContext());}
 public static GlobalApiErrorResponse of(ErrorCode e,String t){return of(e,e.getMessage(),t,Map.of());}
 public static GlobalApiErrorResponse of(ErrorCode e,String t,Map<String,Object>d){return of(e,e.getMessage(),t,d);}
 public static GlobalApiErrorResponse of(ErrorCode e,String m,String t,Map<String,Object>d){return new GlobalApiErrorResponse(LocalDateTime.now(),e.getHttpStatus().value(),e.getCode(),m,t,d==null?Map.of():Map.copyOf(d));}
}
