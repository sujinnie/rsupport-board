package com.rsupport.board.notice.api.controller;

import com.rsupport.board.common.exception.ErrorCode;
import com.rsupport.board.member.domain.entity.Member;
import com.rsupport.board.member.domain.repository.MemberRepository;
import com.rsupport.board.notice.domain.entity.Attachment;
import com.rsupport.board.notice.domain.entity.Notice;
import com.rsupport.board.notice.domain.repository.AttachmentRepository;
import com.rsupport.board.notice.domain.repository.NoticeRepository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 공지 수정 서비스 통합 테스트
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NoticeController_updateIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private EntityManager em;

    private Member savedMember;
    private Member otherMember;
    private Notice savedNotice;
    private Attachment savedAtt;

    private final LocalDateTime now = LocalDateTime.of(2025, 6, 2, 3, 0);

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
        savedMember = memberRepository.save(m1);

        Member m2 = Member.builder()
                .name("타인")
                .email("other@test.com")
                .password("pw")
                .build();
        otherMember = memberRepository.save(m2);

        // 공지 생성
        savedNotice = noticeRepository.save(Notice.testBuilder()
                .member(savedMember)
                .title("공지수정 통합테스트")
                .content("공지수정 통합테스트 내용내용")
                .startAt(now.minusDays(1))
                .endAt(now.plusDays(3))
                .viewCount(0)
                .build());

        // 첨부파일
        savedAtt = attachmentRepository.save(Attachment.testBuilder()
                .filename("att1.png")
                .url("/uploads/att1.png")
                .uploadedAt(now.minusDays(1))
                .notice(savedNotice)
                .build());

//        savedNotice.addAttachment(savedAtt);
//        noticeRepository.save(savedNotice);
    }

    @Test
    @DisplayName("1. 수정자=작성자, 날짜 범위가 정상, 존재하는 첨부파일 1개 삭제, 새 파일 1개 추가 -> 수정 성공")
    void patchNotice_success() throws Exception {
        Long noticeId = savedNotice.getId();
        Long userId   = savedMember.getId();
        Long removeAttId = savedAtt.getId(); // 삭제할 첨부파일 id

        // 새 더미파일 생성
        MockMultipartFile newFile = new MockMultipartFile(
                "newFiles", // dto 랑 이름같아야함..
                "newFile.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "dummy-image".getBytes(StandardCharsets.UTF_8)
        );

        // note: multipart()는 HTTP 메서드가 POST로 잡힘 -> PATCH로 override
        mockMvc.perform(
                multipart("/v1/notices/{noticeId}", noticeId)
                        .file(newFile)
                        // note: 텍스트 필드들은 param() 으로 보내야 한다
                        .param("userId", savedMember.getId().toString())
                        .param("title", "수정된 제목")
                        .param("content", "수정된 내용")
                        .param("startAt", "2025-06-01T10:00:00")
                        .param("endAt",   "2025-06-05T10:00:00")
                        .param("removeAttachmentIds", removeAttId.toString())
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
        )
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"))
        // JSON
        .andExpect(jsonPath("$.status").value("success"))
        .andExpect(jsonPath("$.data.id").value(noticeId.intValue()))
        .andExpect(jsonPath("$.data.title").value("수정된 제목"))
        .andExpect(jsonPath("$.data.content").value("수정된 내용"))
        // 기존에있던거 삭제 + 새로 추가 -> 추가한 거 1개만 있어야함
        .andExpect(jsonPath("$.data.attachments.length()").value(1))
        .andExpect(jsonPath("$.data.attachments[0].filename").value("newFile.jpg"));
    }

    @Test
    @DisplayName("2. 시작일 > 종료일 -> 400 INVALID_DATE_RANGE 반환")
    void patchNotice_invalidDate_throws() throws Exception {
        Long noticeId = savedNotice.getId();
        Long userId = savedMember.getId();

        MockMultipartFile dummyFile = new MockMultipartFile(
                "newFiles",
                "",
                MediaType.TEXT_PLAIN_VALUE,
                new byte[0]
        );

        mockMvc.perform(
                multipart("/v1/notices/{id}", savedNotice.getId())
                        .file(dummyFile)
                        .param("userId", userId.toString())
                        .param("title", "수정된 제목")
                        .param("content", "수정된 내용")
                        // startAt > endAt 세팅
                        .param("startAt", "2025-06-01T10:00:00")
                        .param("endAt",   "2025-05-01T10:00:00")
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
        )
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON + ";charset=UTF-8"))
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.exception.code").value(ErrorCode.INVALID_DATE_RANGE.getCode()))
        .andExpect(jsonPath("$.exception.message").value(ErrorCode.INVALID_DATE_RANGE.getMessage()));
    }

    @Test
    @DisplayName("3. 삭제할 파일 중 존재하지 않는 id가 있음 -> 404 ATTACHMENT_NOT_FOUND 반환")
    void patchNotice_removeInvalidAttachment_throws() throws Exception {
        Long noticeId = savedNotice.getId();
        Long userId = savedMember.getId();

        // 존재하지 않는 첨부파일 id 세팅
        String nonExistentId = "99999";

        MockMultipartFile dummyFile = new MockMultipartFile(
                "newFiles",
                "",
                MediaType.TEXT_PLAIN_VALUE,
                new byte[0]
        );

        mockMvc.perform(
                multipart("/v1/notices/{id}", savedNotice.getId())
                        .file(dummyFile)
                        .param("userId", userId.toString())
                        .param("title", "수정된 제목")
                        .param("content", "수정된 내용")
                        .param("startAt", "2025-06-01T10:00:00")
                        .param("endAt",   "2025-06-05T10:00:00")
                        .param("removeAttachmentIds", nonExistentId)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON + ";charset=UTF-8")
        )
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON + ";charset=UTF-8"))
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.exception.code").value(ErrorCode.ATTACHMENT_NOT_FOUND.getCode()))
        .andExpect(jsonPath("$.exception.message").value(ErrorCode.ATTACHMENT_NOT_FOUND.getMessage()));
    }

    @Test
    @DisplayName("수정자 != 공지 작성자 ->  403 FORBIDDEN 반환")
    void patchNotice_unauthorizedUser_throws() throws Exception {
        Long noticeId = savedNotice.getId();
        Long otherUserId = otherMember.getId();

        MockMultipartFile dummyFile = new MockMultipartFile(
                "newFiles",
                "",
                MediaType.TEXT_PLAIN_VALUE,
                new byte[0]
        );

        mockMvc.perform(
                multipart("/v1/notices/{id}", savedNotice.getId())
                        .file(dummyFile)
                        .param("userId", otherUserId.toString())
                        .param("title", "수정된 제목")
                        .param("content", "수정된 내용")
                        .param("startAt", "2025-06-01T10:00:00")
                        .param("endAt",   "2025-06-05T10:00:00")
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON + ";charset=UTF-8")
        )
        .andExpect(status().isForbidden())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON + ";charset=UTF-8"))
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.exception.code").value(ErrorCode.ACCESS_DENIED.getCode()))
        .andExpect(jsonPath("$.exception.message").value(ErrorCode.ACCESS_DENIED.getMessage()));
    }
}
