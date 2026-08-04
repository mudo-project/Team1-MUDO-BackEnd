package com.academy.mudogroupware.global.infrastructure.config;

/* Comment
 *   사용할 각 도메인에 ModelStruct 사용하려면 이를 구체화하여 사용
 *   @Mapper(config = MapStructConfig.class) <- 이거 추가
 * */

import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

@MapperConfig(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface MapStructConfig {
}
