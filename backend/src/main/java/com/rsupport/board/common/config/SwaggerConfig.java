package com.rsupport.board.common.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI springOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RSUPPORT NOTICE REST API")
                        .version("1.0.0")
                        .description("공지사항 관리 REST API 명세서 입니다")
                        .contact(new Contact().name("sujin").email("sujin5262@naver.com"))
                        .license(new License().name("sujin"))
                )
                .externalDocs(new ExternalDocumentation()
                        .description("GitHub Repository")
                        .url("https://github.com/sujinnie/rsupport-board")
                );
    }

    @Bean
    public GroupedOpenApi noticeApiGroup() {
        return GroupedOpenApi.builder()
                .group("notice-api")
                .pathsToMatch("/v1/notices/**")
                .pathsToExclude("/test/**")
                .build();
    }
}
