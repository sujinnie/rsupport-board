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

    @BeforeEach
    void setUp() { // 공통으로 사용할 더미 데이터 생성
        LocalDateTime baseTime = LocalDateTime.of(2025, 6, 10, 12, 0, 0);

        sampleMember = Member.builder()
                .id(1L)
                .name("테스트유저")
                .email("read-test@example.com")
                .password("pwd123")
                .build();

        sampleNotice = Notice.testBuilder()
                .id(100L)
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

        refreshedNotice = Notice.testBuilder()
                .id(100L)
                .title("공지상세조회테스트")
                .content("공지상세조회테스트 내용내용")
                .startAt(baseTime.minusDays(1))
                .endAt(baseTime.plusDays(1))
                .viewCount(6)
                .member(sampleMember)
                .createdAt(baseTime.minusDays(2))
                .updatedAt(baseTime.minusDays(2))
                .build();
        Attachment att2 = Attachment.testBuilder()
                .id(55L)
                .filename("file1.txt")
                .url("/uploads/file1.txt")
                .uploadedAt(baseTime.minusDays(1))
                .build();
        refreshedNotice.addAttachment(att2);
    }

    @Test
    @DisplayName("1. 멤버+공지 모두 존재 -> 조회수 증가 후 반환")
    void getNotice_success() {
        // given
        // 호출 시 생성해둔 샘플 데이터 반환
        when(memberRepository.findById(1L)).thenReturn(Optional.of(sampleMember));
        when(noticeRepository.findWithMemberAndAttachmentsById(100L)).thenReturn(Optional.of(sampleNotice));
        when(noticeRepository.findWithMemberAndAttachmentsById(100L)).thenReturn(Optional.of(refreshedNotice));

        // when
        NoticeResponseDTO dto = noticeService.getNotice(1L, 100L);

        // then
        // 메서드 호출 검증
        verify(memberRepository, times(1)).findById(1L);
        verify(noticeRepository, times(1)).incrementViewCountOnly(100L);
        // 조회수 증가 전, 후 두 번 호출된거 확인
        verify(noticeRepository, times(2)).findWithMemberAndAttachmentsById(100L);

        // 최종 응답 DTO 검증
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(100L);
        assertThat(dto.getTitle()).isEqualTo("공지상세조회테스트");
        assertThat(dto.getContent()).isEqualTo("공지상세조회테스트 내용내용");
        assertThat(dto.getViewCount()).isEqualTo(6L);
        assertThat(dto.getAuthor().getId()).isEqualTo(1L);
        assertThat(dto.getAttachments()).hasSize(1);
        assertThat(dto.getAttachments().get(0).getFilename()).isEqualTo("file1.txt");
    }

    @Test
    @DisplayName("2. 존재하지 않는 userId -> MEMBER_NOT_FOUND 예외 발생")
    void getNotice_memberNotFound() {
        // given
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        // when+then
        assertThatThrownBy(() -> noticeService.getNotice(1L, 100L))
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
        when(memberRepository.findById(1L)).thenReturn(Optional.of(sampleMember));
        when(noticeRepository.findWithMemberAndAttachmentsById(100L)).thenReturn(Optional.empty());

        // when+then
        assertThatThrownBy(() -> noticeService.getNotice(1L, 100L))
                .isInstanceOf(CustomExceptionHandler.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTICE_NOT_FOUND);

        // 조회수 증가는 호출되지 않아야 함
        verify(noticeRepository, never()).incrementViewCountOnly(anyLong());
    }
}
