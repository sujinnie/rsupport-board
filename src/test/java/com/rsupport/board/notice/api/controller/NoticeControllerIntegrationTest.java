package com.rsupport.board.notice.api.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 통합 테스트
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")  // application-test.properties를 사용
class NoticeControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("1. 공지 생성 시 제목이 비어 있으면 400 에러 반환, @Valid 검증")
    void createNotice_titleBlank_returnsBadRequest() throws Exception {
        mockMvc.perform(
                    multipart("/v1/notices")
                        // note: 텍스트 필드들은 param() 으로 보내야 한다
                        .param("userId", "1")
                        .param("title", "") // 빈 문자열 -> @NotBlank 에러
                        .param("content", "테스트 내용")
                        .param("startAt", "2025-06-20T10:00:00")
                        .param("endAt", "2025-06-25T10:00:00")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                // 반환된 JSON 포맷 검증
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.exception.code").value("C0001"))
                .andExpect(jsonPath("$.exception.message").value("제목은 필수입니다."));
    }
}
