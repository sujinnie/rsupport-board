package com.rsupport.board.notice.service;

import com.rsupport.board.common.exception.CustomExceptionHandler;
import com.rsupport.board.common.exception.ErrorCode;
import com.rsupport.board.member.domain.entity.Member;
import com.rsupport.board.member.domain.repository.MemberRepository;
import com.rsupport.board.notice.api.dto.NoticeResponseDTO;
import com.rsupport.board.notice.domain.entity.Attachment;
import com.rsupport.board.notice.domain.entity.Notice;
import com.rsupport.board.notice.domain.repository.NoticeRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 공지 상세 조회 서비스 단위테스트
 */
@ExtendWith(MockitoExtension.class)
public class NoticeServiceImpl_getTest {
    @Mock
    private MemberRepository memberRepository;

    @Mock
    private NoticeRepository noticeRepository;

    @InjectMocks
    private NoticeServiceImpl noticeService;

    private Member sampleMember;
    private Notice sampleNotice;
    private Notice refreshedNotice;
    private final Long SAMPLE_USER_ID = 1L;
    private final Long SAMPLE_NOTICE_ID = 100L;

    @BeforeEach
    void setUp() { // 공통으로 사용할 더미 데이터 생성
        LocalDateTime baseTime = LocalDateTime.of(2025, 6, 10, 12, 0, 0);

        sampleMember = Member.builder()
                .id(SAMPLE_USER_ID)
                .name("테스트유저")
                .email("read-test@example.com")
                .password("pwd123")
                .build();

        sampleNotice = Notice.testBuilder()
                .id(SAMPLE_NOTICE_ID)
                .title("공지상세조회테스트")
                .content("공지상세조회테스트 내용내용")
                .startAt(baseTime.minusDays(1))
                .endAt(baseTime.plusDays(1))
                .viewCount(5)
                .member(sampleMember)
                .createdAt(baseTime.minusDays(2))
                .updatedAt(baseTime.minusDays(2))
                .build();
        Attachment att = Attachment.testBuilder()
                .id(55L)
                .filename("file1.txt")
                .url("/uploads/file1.txt")
                .uploadedAt(baseTime.minusDays(1))
                .build();
        sampleNotice.addAttachment(att);

        refreshedNotice = Notice.testBuilder() // 조회수만 증가된 notice
                .id(SAMPLE_NOTICE_ID)
                .title("공지상세조회테스트")
                .content("공지상세조회테스트 내용내용")
                .startAt(baseTime.minusDays(1))
                .endAt(baseTime.plusDays(1))
                .viewCount(6)
                .member(sampleMember)
                .createdAt(baseTime.minusDays(2))
                .updatedAt(baseTime.minusDays(2))
                .build();
        refreshedNotice.addAttachment(att);
    }

    @Test
    @DisplayName("1. 멤버+공지 모두 존재 -> 조회수 증가 후 반환")
    void getNotice_success() {
        // given
        // 호출 시 생성해둔 샘플 데이터 반환
        when(memberRepository.findById(SAMPLE_USER_ID)).thenReturn(Optional.of(sampleMember));
        when(noticeRepository.findWithMemberAndAttachmentsById(SAMPLE_NOTICE_ID)).thenReturn(Optional.of(sampleNotice));
        when(noticeRepository.findWithMemberAndAttachmentsById(SAMPLE_NOTICE_ID)).thenReturn(Optional.of(refreshedNotice));

        // when
        NoticeResponseDTO dto = noticeService.getNotice(SAMPLE_USER_ID, SAMPLE_NOTICE_ID);

        // then
        // 메서드 호출 검증
        verify(memberRepository, times(1)).findById(SAMPLE_USER_ID);
        verify(noticeRepository, times(1)).incrementViewCountOnly(SAMPLE_NOTICE_ID);
        // 조회수 증가 전, 후 두 번 호출된거 확인
        verify(noticeRepository, times(2)).findWithMemberAndAttachmentsById(SAMPLE_NOTICE_ID);

        // 최종 응답 DTO 검증
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(SAMPLE_NOTICE_ID);
        assertThat(dto.getTitle()).isEqualTo("공지상세조회테스트");
        assertThat(dto.getContent()).isEqualTo("공지상세조회테스트 내용내용");
        assertThat(dto.getViewCount()).isEqualTo(6L);
        assertThat(dto.getAuthor().getId()).isEqualTo(SAMPLE_USER_ID);
        assertThat(dto.getAttachments()).hasSize(1);
        assertThat(dto.getAttachments().get(0).getFilename()).isEqualTo("file1.txt");
    }

    @Test
    @DisplayName("2. 존재하지 않는 userId -> MEMBER_NOT_FOUND 예외 발생")
    void getNotice_memberNotFound() {
        // given
        Long nonExistingUserId = 2L;
        when(memberRepository.findById(nonExistingUserId)).thenReturn(Optional.empty());

        // when+then
        assertThatThrownBy(() -> noticeService.getNotice(nonExistingUserId, SAMPLE_NOTICE_ID))
                .isInstanceOf(CustomExceptionHandler.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);

        // 공지는 아예 조회되지 않아야 함
        verify(noticeRepository, never()).findWithMemberAndAttachmentsById(anyLong());
        verify(noticeRepository, never()).incrementViewCountOnly(anyLong());
    }

    @Test
    @DisplayName("3. 존재하지 않는 noticeId -> NOTICE_NOT_FOUND 예외 발생")
    void getNotice_noticeNotFound() {
        // given
        Long NonExistingNoticeId = 101L;
        when(memberRepository.findById(SAMPLE_USER_ID)).thenReturn(Optional.of(sampleMember));
        when(noticeRepository.findWithMemberAndAttachmentsById(NonExistingNoticeId)).thenReturn(Optional.empty());

        // when+then
        assertThatThrownBy(() -> noticeService.getNotice(SAMPLE_USER_ID, NonExistingNoticeId))
                .isInstanceOf(CustomExceptionHandler.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTICE_NOT_FOUND);

        // 조회수 증가는 호출되지 않아야 함
        verify(noticeRepository, never()).incrementViewCountOnly(anyLong());
    }
}
