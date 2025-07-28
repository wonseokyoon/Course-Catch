package com.Catch_Course.global.springDoc;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "API 서버"))
public class SpringDocConfig {

    @Bean
    public GroupedOpenApi groupApiV1() {
        return GroupedOpenApi.builder()
                .group("api")
                .pathsToMatch("/api/**")
                .build();
    }
//    @Bean
//    public GroupedOpenApi groupController() {
//        return GroupedOpenApi.builder()
//                .group("controller")
//                .pathsToExclude("/api/**")
//                .build();
//    }
}