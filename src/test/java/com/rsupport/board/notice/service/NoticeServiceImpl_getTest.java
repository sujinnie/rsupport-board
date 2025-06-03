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
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 공지 상세 조회 서비스 단위테스트 (조회수 배치 업데이트 로직 포함)
 */
@ExtendWith(MockitoExtension.class)
public class NoticeServiceImpl_getTest {
    @Mock
    private MemberRepository memberRepository;

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private RedisTemplate<String, Long> redisTemplate;

    @Mock
    private ValueOperations<String, Long> valueOperations;
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
    }

    @Test
    @DisplayName("1. 멤버+공지 모두 존재, redis에 누적값 없을 떄 -> 조회수 1 증가 후 반환")
    void getNotice_withOutRedisDelta_success() {
        // given
        // 호출 시 생성해둔 샘플 데이터 반환
        when(memberRepository.findById(SAMPLE_USER_ID)).thenReturn(Optional.of(sampleMember));
        when(noticeRepository.findWithMemberAndAttachmentsById(SAMPLE_NOTICE_ID)).thenReturn(Optional.of(sampleNotice));

        String redisKey = "notice:view:" + SAMPLE_NOTICE_ID;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(redisKey, 1L)).thenReturn(1L); // 증가했으니 redis에 0+1 (key,1)
        when(valueOperations.get(redisKey)).thenReturn(1L);

        // when
        NoticeResponseDTO dto = noticeService.getNotice(SAMPLE_USER_ID, SAMPLE_NOTICE_ID);

        // then
        // 메서드 호출 검증
        verify(memberRepository, times(1)).findById(SAMPLE_USER_ID);

        // redis opsForValue -> increment -> get 순서로 호출됐는지 확인
        InOrder inOrder = inOrder(redisTemplate, valueOperations);
        inOrder.verify(redisTemplate).opsForValue();
        inOrder.verify(valueOperations).increment(redisKey, 1L);
        inOrder.verify(valueOperations).get(redisKey);

        // 최종 응답 DTO 검증
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(SAMPLE_NOTICE_ID);
        assertThat(dto.getTitle()).isEqualTo("공지상세조회테스트");
        assertThat(dto.getContent()).isEqualTo("공지상세조회테스트 내용내용");
        assertThat(dto.getViewCount()).isEqualTo(6L); //viewCount: 기존 5 + redis누적값 1 = 6
        assertThat(dto.getAuthor().getId()).isEqualTo(SAMPLE_USER_ID);
        assertThat(dto.getAttachments()).hasSize(1);
        assertThat(dto.getAttachments().get(0).getFilename()).isEqualTo("file1.txt");
    }

    @Test
    @DisplayName("2. 멤버+공지 모두 존재, redis 누적값 3 -> 조회수 4(=3+1) 증가 후 반환")
    void getNotice_withRedisDelta_success() {
        // given
        // 호출 시 생성해둔 샘플 데이터 반환
        when(memberRepository.findById(SAMPLE_USER_ID)).thenReturn(Optional.of(sampleMember));
        when(noticeRepository.findWithMemberAndAttachmentsById(SAMPLE_NOTICE_ID)).thenReturn(Optional.of(sampleNotice));

        String redisKey = "notice:view:" + SAMPLE_NOTICE_ID;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(redisKey, 1L)).thenReturn(4L); // 증가했으니 redis에 3+1 (key,4)
        when(valueOperations.get(redisKey)).thenReturn(4L);

        // when
        NoticeResponseDTO dto = noticeService.getNotice(SAMPLE_USER_ID, SAMPLE_NOTICE_ID);

        // then
        // 메서드 호출 검증
        verify(memberRepository, times(1)).findById(SAMPLE_USER_ID);

        // redis opsForValue -> increment -> get 순서로 호출됐는지 확인
        InOrder inOrder = inOrder(redisTemplate, valueOperations);
        inOrder.verify(redisTemplate).opsForValue();
        inOrder.verify(valueOperations).increment(redisKey, 1L);
        inOrder.verify(valueOperations).get(redisKey);

        // 최종 응답 DTO 검증
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(SAMPLE_NOTICE_ID);
        assertThat(dto.getTitle()).isEqualTo("공지상세조회테스트");
        assertThat(dto.getContent()).isEqualTo("공지상세조회테스트 내용내용");
        assertThat(dto.getViewCount()).isEqualTo(9L); //viewCount: 기존 5 + redis 원래 누적값 3 + 증가 1 = 9
        assertThat(dto.getAuthor().getId()).isEqualTo(SAMPLE_USER_ID);
        assertThat(dto.getAttachments()).hasSize(1);
        assertThat(dto.getAttachments().get(0).getFilename()).isEqualTo("file1.txt");
    }

    @Test
    @DisplayName("3. 존재하지 않는 userId -> MEMBER_NOT_FOUND 예외 발생")
    void getNotice_memberNotFound() {
        // given
        Long nonExistingUserId = 2L;
        when(memberRepository.findById(nonExistingUserId)).thenReturn(Optional.empty());

        // when+then
        assertThatThrownBy(() -> noticeService.getNotice(nonExistingUserId, SAMPLE_NOTICE_ID))
                .isInstanceOf(CustomExceptionHandler.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);

        // 공지는 아예 조회되지 않아야함, redis 도 호출되지 않아야함
        verify(noticeRepository, never()).findWithMemberAndAttachmentsById(anyLong());
        verify(redisTemplate, never()).opsForValue();
//        verify(noticeRepository, never()).incrementViewCountOnly(anyLong());
    }

    @Test
    @DisplayName("4. 존재하지 않는 noticeId -> NOTICE_NOT_FOUND 예외 발생")
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

        // redis 는 아예 호출되지 않아야함
        verify(redisTemplate, never()).opsForValue();
    }
}
