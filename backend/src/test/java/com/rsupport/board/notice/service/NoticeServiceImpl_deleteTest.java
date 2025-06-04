package com.rsupport.board.notice.service;

import com.rsupport.board.common.exception.CustomExceptionHandler;
import com.rsupport.board.common.exception.ErrorCode;
import com.rsupport.board.member.domain.entity.Member;
import com.rsupport.board.member.domain.repository.MemberRepository;
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
 * 공지 삭제 서비스 단위테스트
 */
@ExtendWith(MockitoExtension.class)
public class NoticeServiceImpl_deleteTest {
    @Mock
    MemberRepository memberRepository;

    @Mock
    NoticeRepository noticeRepository;

    @InjectMocks
    NoticeServiceImpl noticeService;

    private Notice sampleNotice;
    private Member sampleMember;
    private LocalDateTime now;

    private final Long SAMPLE_USER_ID = 1L;
    private final Long SAMPLE_NOTICE_ID = 100L;

    @BeforeEach
    void setUp() {// 공통으로 사용할 더미 데이터 생성
        now = LocalDateTime.of(2025, 6, 15, 10, 0, 0);

        sampleMember = Member.builder()
                .id(SAMPLE_USER_ID)
                .name("테스트유저")
                .email("delete-test@example.com")
                .password("pwd123")
                .build();

        sampleNotice = Notice.testBuilder()
                .id(SAMPLE_NOTICE_ID)
                .member(sampleMember)
                .title("공제 삭제 테스트")
                .content("공제 삭제 테스트 내용내용")
                .startAt(now.minusDays(1))
                .endAt(now.plusDays(1))
                .viewCount(0)
                .build();
    }

    @Test
    @DisplayName("1. 삭제요청자==작성자, 공지가 존재 -> 삭제 성공")
    void deleteNotice_success() {
        // given
        when(memberRepository.findById(SAMPLE_USER_ID)).thenReturn(Optional.of(sampleMember));
        when(noticeRepository.findById(SAMPLE_NOTICE_ID)).thenReturn(Optional.of(sampleNotice));

        // when
        noticeService.deleteNotice(SAMPLE_USER_ID, SAMPLE_NOTICE_ID);

        // then
        // NoticeRepository.delete(notice) 호출 검증
        verify(noticeRepository, times(1)).delete(sampleNotice);
    }

    @Test
    @DisplayName("2. 존재하지 않는 유저 -> MEMBER_NOT_FOUND 예외 발생")
    void deleteNotice_memberNotFound_throwsException() {
        // given
        Long nonExistingUserId = 2L; // 존재하지 않는 유저id -> empty 반환하도록 세팅
        when(memberRepository.findById(nonExistingUserId)).thenReturn(Optional.empty());

        // when+then
        // 에러코드 검증
        assertThatThrownBy(() -> noticeService.deleteNotice(nonExistingUserId, SAMPLE_NOTICE_ID))
                .isInstanceOf(CustomExceptionHandler.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);

        // 멤버조회부터 에러 -> 공지조회, 삭제 모두 호출 x
        verify(noticeRepository, never()).findById(anyLong());
        verify(noticeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("3. 존재하지 않는 공지 -> NOTICE_NOT_FOUND 예외 발생")
    void deleteNotice_noticeNotFound_throwsException() {
        // given
        when(memberRepository.findById(SAMPLE_USER_ID)).thenReturn(Optional.of(sampleMember));

        Long nonExistingNoticeId = 101L;
        when(noticeRepository.findById(nonExistingNoticeId)).thenReturn(Optional.empty());

        // when+then
        assertThatThrownBy(() -> noticeService.deleteNotice(SAMPLE_USER_ID, nonExistingNoticeId))
                .isInstanceOf(CustomExceptionHandler.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTICE_NOT_FOUND);

        // 삭제 호출 x
        verify(noticeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("4. 공지 존재, 요청자 != 작성자 -> ACCESS_DENIED 예외 발생")
    void deleteNotice_forbiddenUser_throwsException() {
        // given
        // 샘플과 다른 유저를 반환하도록 세팅
        Long otherUserId = 777L; // 실제 작성자 ID는 1L
        when(memberRepository.findById(otherUserId)).thenReturn(
                Optional.of(Member.builder()
                        .id(otherUserId)
                        .name("다른 테스트유저")
                        .email("delete-test2@example.com")
                        .password("pwd123")
                        .build()
                )
        );
        when(noticeRepository.findById(SAMPLE_NOTICE_ID)).thenReturn(Optional.of(sampleNotice));

        // when+then
        assertThatThrownBy(() -> noticeService.deleteNotice(otherUserId, SAMPLE_NOTICE_ID))
                .isInstanceOf(CustomExceptionHandler.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);

        // 삭제 호출 x
        verify(noticeRepository, never()).delete(any());
    }
}
