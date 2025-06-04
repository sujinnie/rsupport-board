package com.rsupport.board.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 정적리소스 파일 가져오기
 * "/uploads/**" 로 들어오는 요청들을 로컬의 실제 디스크 경로(uploadPath + "/") 로 매핑
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    // application.properties 에 정의된 업로드 경로를 여기서 읽어오도록 수정
    @Value("${upload.path}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}
