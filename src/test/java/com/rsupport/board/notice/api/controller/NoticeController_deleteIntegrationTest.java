package com.rsupport.board.notice.api.controller;

import com.rsupport.board.common.exception.ErrorCode;
import com.rsupport.board.member.domain.entity.Member;
import com.rsupport.board.member.domain.repository.MemberRepository;
import com.rsupport.board.notice.domain.entity.Attachment;
import com.rsupport.board.notice.domain.entity.Notice;
import com.rsupport.board.notice.domain.repository.AttachmentRepository;
import com.rsupport.board.notice.domain.repository.NoticeRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

/**
 * 공지 삭제 서비스 통합 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NoticeController_deleteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    private Member sampleMember;
    private Member otherMember;
    private Notice sampleNotice;
    private Attachment sampleAtt;

    @BeforeEach
    void setUp() {
        // DB 초기화: 저장된 데이터 미리 삭제
        attachmentRepository.deleteAll();
        noticeRepository.deleteAll();
        memberRepository.deleteAll();

        // 테스트용 유저 생성
        Member m1 = Member.builder()
                .name("테스트유저")
                .email("testuser@example.com")
                .password("pwd123")
                .build();
        sampleMember = memberRepository.save(m1);

        Member m2 = Member.builder()
                .name("타인")
                .email("other@test.com")
                .password("pw")
                .build();
        otherMember = memberRepository.save(m2);

        // 테스트용 공지 생성
        LocalDateTime now = LocalDateTime.of(2025, 6, 10, 14, 30, 0);
        sampleNotice = Notice.testBuilder()
                .member(sampleMember)
                .title("공지 삭제 통합테스트")
                .content("공지 삭제 통합테스트 내용내용")
                .startAt(now.minusDays(1))
                .endAt(now.plusDays(1))
                .viewCount(0)
                .build();

        // 테스트용 첨부파일 생성
        sampleAtt = Attachment.builder()
                .filename("attach1.png")
                .url("/uploads/attach1.png")
                .uploadedAt(now.minusHours(5))
                .build();
        sampleNotice.addAttachment(sampleAtt);

        sampleNotice = noticeRepository.save(sampleNotice);
    }

    @Test
    @DisplayName("1. 요청자 == 작성자 -> 삭제 성공")
    void deleteNotice_success() throws Exception {
        Long noticeId = sampleNotice.getId();
        Long userId = sampleMember.getId();

        mockMvc.perform(
                delete("/v1/notices/{noticeId}", noticeId)
                        .param("userId", userId.toString())
                        .accept(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
        )
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"))
        .andExpect(jsonPath("$.status").value("success"))
        .andExpect(jsonPath("$.data").isEmpty());

        // DB에서도 실제로 삭제되었는지 추가 검증
        assertThat(noticeRepository.findById(noticeId)).isEmpty();
        assertThat(attachmentRepository.findById(sampleAtt.getId())).isEmpty();
    }

    @Test
    @DisplayName("2. noticeId가 존재하지 않는 경우 -> 404 NOTICE_NOT_FOUND 예외처리")
    void deleteNotice_notFound_throwsException() throws Exception {
        Long nonExistentId = 999L;
        Long userId = sampleMember.getId();

        mockMvc.perform(
                delete("/v1/notices/{noticeId}", nonExistentId)
                        .param("userId", userId.toString())
                        .accept(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
        )
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"))
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.exception.code").value(ErrorCode.NOTICE_NOT_FOUND.getCode()))
        .andExpect(jsonPath("$.exception.message").value(ErrorCode.NOTICE_NOT_FOUND.getMessage()));
    }

    @Test
    @DisplayName("3. 공지존재, 요청자 != 작성자 -> 403 FORBIDDEN 예외처리")
    void deleteNotice_forbidden_throwsException() throws Exception {
        Long noticeId = sampleNotice.getId();
        Long userId   = otherMember.getId(); // 작성자가 아님

        mockMvc.perform(
                delete("/v1/notices/{noticeId}", noticeId)
                        .param("userId", userId.toString())
                        .accept(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
        )
        .andExpect(status().isForbidden())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"))
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.exception.code").value(ErrorCode.ACCESS_DENIED.getCode()))
        .andExpect(jsonPath("$.exception.message").value(ErrorCode.ACCESS_DENIED.getMessage()));

        // DB에는 여전히 공지와 첨부파일이 남아 있어야 함
        assertThat(noticeRepository.findById(noticeId)).isPresent();
        assertThat(attachmentRepository.findById(sampleAtt.getId())).isPresent();
    }
}
