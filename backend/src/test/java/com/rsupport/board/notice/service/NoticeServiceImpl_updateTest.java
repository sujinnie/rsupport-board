package com.rsupport.board.notice.service;

import com.rsupport.board.common.exception.CustomExceptionHandler;
import com.rsupport.board.common.exception.ErrorCode;
import com.rsupport.board.member.domain.entity.Member;
import com.rsupport.board.member.domain.repository.MemberRepository;
import com.rsupport.board.notice.api.dto.NoticeResponseDTO;
import com.rsupport.board.notice.api.dto.NoticeUpdateReqDTO;
import com.rsupport.board.notice.domain.entity.Attachment;
import com.rsupport.board.notice.domain.entity.Notice;
import com.rsupport.board.notice.domain.repository.AttachmentRepository;
import com.rsupport.board.notice.domain.repository.NoticeRepository;
import com.rsupport.board.notice.infra.FileStorageService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 공지 수정 서비스 단위테스트
 */
@ExtendWith(MockitoExtension.class)
public class NoticeServiceImpl_updateTest {
    @Mock
    MemberRepository memberRepository;

    @Mock
    NoticeRepository noticeRepository;

    @Mock
    AttachmentRepository attachmentRepository;

    @Mock
    FileStorageService fileStorageService;
    @Mock private RedisTemplate<String, Long> redisTemplate;
    @Mock private ValueOperations<String, Long> valueOperations;

    @InjectMocks
    NoticeServiceImpl noticeService;

    private Member sampleMember;
    private Notice sampleNotice;
    private Attachment sampleAtt1;
    private Attachment sampleAtt2;
    private LocalDateTime now;
    private LocalDateTime end;

    private final Long SAMPLE_USER_ID = 1L;
    private final Long SAMPLE_NOTICE_ID = 100L;

    @BeforeEach
    void setUp() { // 공통으로 사용할 더미 데이터 생성
        now = LocalDateTime.of(2025, 6, 15, 10, 0, 0);
        end = now.minusDays(1);

        sampleMember = Member.builder()
                .id(SAMPLE_USER_ID)
                .name("테스트유저")
                .email("update-test@example.com")
                .password("pwd123")
                .build();

        sampleNotice = Notice.testBuilder()
                .id(SAMPLE_NOTICE_ID)
                .member(sampleMember)
                .title("원본 제목")
                .content("원본 내용")
                .startAt(now)
                .endAt(now.plusDays(3))
                .build();

        sampleAtt1 = Attachment.testBuilder()
                .id(11L)
                .filename("file1.png")
                .url("/uploads/file1.png")
                .uploadedAt(now.minusDays(1))
                .build();
        sampleAtt2 = Attachment.testBuilder()
                .id(12L)
                .filename("file2.pdf")
                .url("/uploads/file2.pdf")
                .uploadedAt(now.minusDays(1))
                .build();
        sampleNotice.addAttachment(sampleAtt1);
        sampleNotice.addAttachment(sampleAtt2);
    }

    @Test
    @DisplayName("1. 수정자=작성자, 날짜 범위가 정상, 존재하는 첨부파일 1개 삭제, 새 파일 1개 추가 -> 수정 성공")
    void updateNotice_withoutFiles_success() {
        // given
        // 삭제할 파일 id 세팅(att1만 삭제)
        List<Long> removeIds = List.of(11L);

        // 새로 추가할 파일 세팅
        MultipartFile newFile = mock(MultipartFile.class);
        when(newFile.isEmpty()).thenReturn(false);
//        when(newFile.getOriginalFilename()).thenReturn("new-image.jpg");

        // 수정 요청 DTO 세팅(제목, 내용, 날짜 범위, removeAttachmentIds, newFiles)
        NoticeUpdateReqDTO req = new NoticeUpdateReqDTO();
        req.setUserId(SAMPLE_USER_ID);
        req.setTitle("수정된 제목");
        req.setContent("수정된 내용");
        req.setStartAt(now.plusDays(1));
        req.setEndAt(now.plusDays(5));
        req.setRemoveAttachmentIds(removeIds);
        req.setNewFiles(new MultipartFile[]{ newFile });

        // 메서드 호출 시 샘플데이터 반환하도록 세팅
        when(memberRepository.findById(SAMPLE_USER_ID)).thenReturn(Optional.of(sampleMember));
        when(noticeRepository.findWithMemberAndAttachmentsById(SAMPLE_NOTICE_ID)).thenReturn(Optional.of(sampleNotice));

        // fileStorageService.store(...) 호출 시 새로운 Attachment 하나 반환하게 세팅(== new file)
        when(fileStorageService.store(newFile)).thenAnswer(invocation -> {
            Attachment a = Attachment.testBuilder()
                    .id(99L)
                    .filename("new-image.jpg")
                    .url("/uploads/new-image.jpg")
                    .uploadedAt(now)
                    .build();
            return a;
        });

        // redis npe 에러 방지
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(0L);  // 조회수 기본값을 리턴

        // noticeRepository.save(...) 호출 시 파라미터로 들어온 notice 그대로 반환하게 새팅
        when(noticeRepository.save(any(Notice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        NoticeResponseDTO responseDTO = noticeService.updateNotice(SAMPLE_USER_ID, SAMPLE_NOTICE_ID, req);

        // then
        verify(memberRepository, times(1)).findById(SAMPLE_USER_ID);
        verify(noticeRepository, times(1)).findWithMemberAndAttachmentsById(SAMPLE_NOTICE_ID);

        // 삭제요청한 파일id가 attachmentRepository.deleteAllByIdIn(...) 호출에 포함되었는지 확인
        ArgumentCaptor<List<Long>> removeCaptor = ArgumentCaptor.forClass(List.class);
        verify(attachmentRepository, times(1)).deleteAllByIdIn(removeCaptor.capture());
        assertThat(removeCaptor.getValue()).containsExactly(11L);

        // fileStorageService.store(newFile) 호출 검증
        verify(fileStorageService, times(1)).store(newFile);

        // noticeRepository.save(...) 호출된 Notice 검증
        ArgumentCaptor<Notice> noticeCaptor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeRepository, times(1)).save(noticeCaptor.capture());
        Notice savedNotice = noticeCaptor.getValue();
        // 작성자 정보 그대로인지
        assertThat(savedNotice.getMember()).isEqualTo(sampleMember);
        // 제목, 내용, startAt, endAt 이 요청대로 변경되었는지
        assertThat(savedNotice.getTitle()).isEqualTo("수정된 제목");
        assertThat(savedNotice.getContent()).isEqualTo("수정된 내용");
        assertThat(savedNotice.getStartAt()).isEqualTo(now.plusDays(1));
        assertThat(savedNotice.getEndAt()).isEqualTo(now.plusDays(5));
        // att1 잘 삭제됐는지 -> 남아 있는 첨부파일이 att2와 newFile 뿐이다
        List<Attachment> remainingAtts = savedNotice.getAttachments();
        assertThat(remainingAtts).hasSize(2);
        assertThat(remainingAtts.stream().map(Attachment::getId)).containsExactlyInAnyOrder(12L, 99L);

        // 최종 응답 DTO 검증
        assertThat(responseDTO).isNotNull();
        assertThat(responseDTO.getId()).isEqualTo(100L);
        assertThat(responseDTO.getTitle()).isEqualTo("수정된 제목");
        assertThat(responseDTO.getContent()).isEqualTo("수정된 내용");
        assertThat(responseDTO.getAuthor().getId()).isEqualTo(1L);
        assertThat(responseDTO.getAuthor().getName()).isEqualTo("테스트유저");
        assertThat(responseDTO.getAttachments()).hasSize(2);
    }

    @Test
    @DisplayName("2. 시작일 > 종료일 -> INVALID_DATE_RANGE 예외 발생")
    void updateNotice_invalidDateRange_throwsException() {
        // given
        NoticeUpdateReqDTO req = new NoticeUpdateReqDTO();
        req.setUserId(SAMPLE_USER_ID);
        // startAt > endAt 으로 세팅
        req.setStartAt(now.plusDays(5));
        req.setEndAt(now.plusDays(2));

        when(memberRepository.findById(SAMPLE_USER_ID)).thenReturn(Optional.of(sampleMember));
        when(noticeRepository.findWithMemberAndAttachmentsById(SAMPLE_NOTICE_ID)).thenReturn(Optional.of(sampleNotice));

        // when+then
        // 에러코드 확인
        assertThatThrownBy(() -> noticeService.updateNotice(SAMPLE_USER_ID, SAMPLE_NOTICE_ID, req))
                .isInstanceOf(CustomExceptionHandler.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_DATE_RANGE);

        // 날짜 검증에서 걸리니까 파일삭제, 파일저장, 공지저장은 호출 안 됨
        verify(attachmentRepository, never()).deleteAllByIdIn(anyList());
        verify(fileStorageService, never()).store(any());
        verify(noticeRepository, never()).save(any());
    }

    @Test
    @DisplayName("3. 삭제할 파일 중 존재하지 않는 id가 있음 -> ATTACHMENT_NOT_FOUND 예외 발생")
    void updateNotice_removeInvalidAttachment_throwsException() {
        // given
        NoticeUpdateReqDTO req = new NoticeUpdateReqDTO();
        req.setUserId(SAMPLE_USER_ID);
        // 존재하지 않는 파일id 삭제 요청 세팅
        req.setRemoveAttachmentIds(List.of(999L));

        when(memberRepository.findById(SAMPLE_USER_ID)).thenReturn(Optional.of(sampleMember));
        when(noticeRepository.findWithMemberAndAttachmentsById(SAMPLE_NOTICE_ID)).thenReturn(Optional.of(sampleNotice));

        // when+then
        // 에러코드 확인
        assertThatThrownBy(() -> noticeService.updateNotice(SAMPLE_USER_ID, SAMPLE_NOTICE_ID, req))
                .isInstanceOf(CustomExceptionHandler.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ATTACHMENT_NOT_FOUND);

        // 파일삭제, 공지저장은 호출되지 않음
        verify(attachmentRepository, never()).deleteAllByIdIn(anyList());
        verify(noticeRepository, never()).save(any());
    }

    @Test
    @DisplayName("4. 공지 존재, 수정자 != 공지 작성자 -> ACCESS_DENIED 예외 발생")
    void updateNotice_forbiddenUser_throwsException() {
        // given
        Long otherUserId   = 999L; // 실제 작성자 ID는 1L
        NoticeUpdateReqDTO req = new NoticeUpdateReqDTO();
        req.setUserId(otherUserId);

        // 샘플과 다른 유저를 반환하도록 세팅
        when(memberRepository.findById(otherUserId)).thenReturn(
                Optional.of(Member.builder()
                        .id(999L)
                        .name("타인")
                        .email("other@ex.com")
                        .password("pw")
                        .build())
        );
        when(noticeRepository.findWithMemberAndAttachmentsById(SAMPLE_NOTICE_ID)).thenReturn(Optional.of(sampleNotice));

        // when+then
        // 에러코드 확인
        assertThatThrownBy(() -> noticeService.updateNotice(otherUserId, SAMPLE_NOTICE_ID, req))
                .isInstanceOf(CustomExceptionHandler.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);

        // 삭제/날짜 검증 전에 권한 검사에서 걸림 -> 삭제, 저장 등 호출 안함
        verify(attachmentRepository, never()).deleteAllByIdIn(anyList());
        verify(noticeRepository, never()).save(any());
    }

    @Test
    @DisplayName("5. 존재하지 않는 공지 -> NOTICE_NOT_FOUND 예외 발생")
    void updateNotice_notFoundNotice_throwsException() {
        // given
        Long nonExistingNoticeId = 222L;
        NoticeUpdateReqDTO req = new NoticeUpdateReqDTO();
        req.setUserId(SAMPLE_USER_ID);

        when(memberRepository.findById(SAMPLE_USER_ID)).thenReturn(Optional.of(sampleMember));
        when(noticeRepository.findWithMemberAndAttachmentsById(nonExistingNoticeId)).thenReturn(Optional.empty());

        // when+then
        // 에러코드 확인
        assertThatThrownBy(() -> noticeService.updateNotice(SAMPLE_USER_ID, nonExistingNoticeId, req))
                .isInstanceOf(CustomExceptionHandler.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTICE_NOT_FOUND);

        // 공지조히에서 에외발생 -> 삭제, 저장 등 호출 안함
        verify(attachmentRepository, never()).deleteAllByIdIn(anyList());
        verify(noticeRepository, never()).save(any());
    }

    @Test
    @DisplayName("6. 존재하지 않는 유저 -> MEMBER_NOT_FOUND 예외 발생")
    void updateNotice_memberNotFound_throwsException() {
        // given
        Long nonExistingUserId = 777L;
        NoticeUpdateReqDTO req = new NoticeUpdateReqDTO();
        req.setUserId(nonExistingUserId);

        when(memberRepository.findById(nonExistingUserId)).thenReturn(Optional.empty());

        // when+then
        // 에러코드 확인
        assertThatThrownBy(() -> noticeService.updateNotice(nonExistingUserId, SAMPLE_NOTICE_ID, req))
                .isInstanceOf(CustomExceptionHandler.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);

        // 유저 검사에서 예외발생 -> 공지 조회도 안함
        verify(noticeRepository, never()).findWithMemberAndAttachmentsById(anyLong());
        verify(attachmentRepository, never()).deleteAllByIdIn(anyList());
        verify(noticeRepository, never()).save(any());
    }
}
