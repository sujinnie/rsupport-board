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
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 공지 상세 조회 서비스 통합 테스트
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")  // application-test.properties 를 사용
public class NoticeController_getIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    private Member sampleMember;
    private Notice sampleNotice;
    private Attachment sampleAtt1, sampleAtt2;

    @BeforeEach
    void setUp() {
        // DB 초기화: 저장된 데이터 미리 삭제
        attachmentRepository.deleteAll();
        noticeRepository.deleteAll();
        memberRepository.deleteAll();

        // 테스트용 멤버 생성
        sampleMember = Member.builder()
                .name("테스트유저")
                .email("testuser@example.com")
                .password("pwd123")
                .build();
        sampleMember = memberRepository.save(sampleMember);

        // 테스트용 공지 생성
        LocalDateTime now = LocalDateTime.of(2025, 6, 10, 14, 30, 0);
        sampleNotice = Notice.testBuilder()
                .member(sampleMember)
                .title("공지 상세 조회 통합테스트")
                .content("공지 상세 조회 통합테스트 내용내용")
                .startAt(now.minusDays(1))
                .endAt(now.plusDays(1))
                .viewCount(5)
                .build();
//        sampleNotice = noticeRepository.save(sampleNotice);

        // 테스트용 첨부파일 2개 생성 
        sampleAtt1 = Attachment.builder()
                .filename("attach1.png")
                .url("/uploads/attach1.png")
                .uploadedAt(now.minusHours(5))
                .build();
        sampleAtt2 = Attachment.builder()
                .filename("attach2.pdf")
                .url("/uploads/attach2.pdf")
                .uploadedAt(now.minusHours(3))
                .build();
        sampleNotice.addAttachment(sampleAtt1);
        sampleNotice.addAttachment(sampleAtt2);

        sampleNotice = noticeRepository.save(sampleNotice);
    }

    @Test
    @DisplayName("1. 멤버+공지+첨부파일 모두 존재 -> 성공, 반환")
    void getNotice_success() throws Exception {
        // given
        Long userId = sampleMember.getId();
        Long noticeId = sampleNotice.getId();

        mockMvc.perform(
                        get("/v1/notices/{noticeId}", noticeId)
                                .param("userId", String.valueOf(userId))
                                .accept(MediaType.APPLICATION_JSON_VALUE +";charset=UTF-8")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"))
                // JSON 필드 검증 - {"status": "success", "data": {...}, "exception": null}
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.exception").isEmpty())
                // data 검증
                .andExpect(jsonPath("$.data.id").value(noticeId.intValue()))
                .andExpect(jsonPath("$.data.title").value("공지 상세 조회 통합테스트"))
                .andExpect(jsonPath("$.data.content").value("공지 상세 조회 통합테스트 내용내용"))
                // 멤버정보 검증
                .andExpect(jsonPath("$.data.author.id").value(userId.intValue()))
                .andExpect(jsonPath("$.data.author.name").value("테스트유저"))
                // 첨부파일 검증
                .andExpect(jsonPath("$.data.attachments.length()").value(2))
                .andExpect(jsonPath("$.data.attachments[0].filename").value("attach1.png"))
                .andExpect(jsonPath("$.data.attachments[0].url").value("/uploads/attach1.png"))
                .andExpect(jsonPath("$.data.attachments[1].filename").value("attach2.pdf"))
                .andExpect(jsonPath("$.data.attachments[1].url").value("/uploads/attach2.pdf"));
    }

    @Test
    @DisplayName("2. userId가 존재하지 않는 경우 -> 404 MEMBER_NOT_FOUND 예외 처리")
    void getNotice_memberNotFound_throwsException() throws Exception {
        Long badUserId = 9999L;
        Long noticeId = sampleNotice.getId();

        mockMvc.perform(
                        get("/v1/notices/{noticeId}", noticeId)
                                .param("userId", String.valueOf(badUserId))
                                .accept(MediaType.APPLICATION_JSON_VALUE +";charset=UTF-8")
                )
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"))
                // JSON
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.data").isEmpty())
                // ErrorCode
                .andExpect(jsonPath("$.exception.code").value(ErrorCode.MEMBER_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.exception.message").value(ErrorCode.MEMBER_NOT_FOUND.getMessage()));
    }

    @Test
    @DisplayName("3. noticeId가 존재하지 않는 경우 -> 404 NOTICE_NOT_FOUND 예외처리")
    void getNotice_noticeNotFound_throwsException() throws Exception {
        Long userId = sampleMember.getId();
        Long badNoticeId = 8888L;

        mockMvc.perform(
                        get("/v1/notices/{noticeId}", badNoticeId)
                                .param("userId", String.valueOf(userId))
                                .accept(MediaType.APPLICATION_JSON_VALUE +";charset=UTF-8")
                )
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"))
                // JSON
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.data").isEmpty())
                // ErrorCode
                .andExpect(jsonPath("$.exception.code").value(ErrorCode.NOTICE_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.exception.message").value(ErrorCode.NOTICE_NOT_FOUND.getMessage()));
    }
}
