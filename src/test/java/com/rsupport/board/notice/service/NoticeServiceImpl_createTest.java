package com.rsupport.board.notice.service;

import com.rsupport.board.common.exception.CustomExceptionHandler;
import com.rsupport.board.common.exception.ErrorCode;
import com.rsupport.board.member.domain.entity.Member;
import com.rsupport.board.member.domain.repository.MemberRepository;
import com.rsupport.board.notice.api.dto.AttachmentInfoDTO;
import com.rsupport.board.notice.api.dto.NoticeCreateReqDTO;
import com.rsupport.board.notice.api.dto.NoticeResponseDTO;
import com.rsupport.board.notice.domain.entity.Attachment;
import com.rsupport.board.notice.domain.entity.Notice;
import com.rsupport.board.notice.domain.repository.NoticeRepository;
import com.rsupport.board.notice.infra.FileStorageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 공지 생성 서비스 단위테스트
 */
@ExtendWith(MockitoExtension.class)
class NoticeServiceImpl_createTest {
    @Mock
    private MemberRepository memberRepository;

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private FileStorageService fileStorageService;

    // Note: @InjectMocks
    // Mockito가 NoticeServiceImpl 생성자를 찾아서
    // memberRepository, noticeRepository, fileStorageService mock객체를 주입하여 인스턴스 생성
    @InjectMocks
    private NoticeServiceImpl noticeService;

    private Member sampleMember;
    private LocalDateTime now, end;

    private final Long SAMPLE_USER_ID = 1L;

    @BeforeEach
    void setUp() { // 공통으로 사용할 더미 데이터 생성
        now = LocalDateTime.of(2025, 6, 15, 10, 0, 0);
        end = now.plusDays(3);

        sampleMember = Member.builder()
                .id(SAMPLE_USER_ID)
                .name("테스트유저")
                .email("create-test@example.com")
                .password("pwd123")
                .build();
    }

    @Test
    @DisplayName("1. 유저존재 + 첨부파일 없이 공지 생성 -> 성공")
    void createNotice_withoutFiles_success() {
        // given
        NoticeCreateReqDTO req = NoticeCreateReqDTO.builder()
                .userId(SAMPLE_USER_ID)
                .title("공지생성 테스트1 - 파일없음")
                .content("첨부파일 없는 공지 생성 테스트")
                .startAt(now)
                .endAt(end)
                .files(null)  // 첨부파일 없음
                .build();

        // 생성한 샘플 유저를 반환하도록 세팅
        when(memberRepository.findById(SAMPLE_USER_ID)).thenReturn(Optional.of(sampleMember));
        
        // 서비스 로직이 Notice를 저장한 뒤 id=100L이 붙었다고 세팅
        when(noticeRepository.save(any(Notice.class))).thenAnswer(invocation -> {
            Notice arg = invocation.getArgument(0);
            arg.setId(100L);
            return arg;
        });

        // when
        NoticeResponseDTO responseDTO = noticeService.createNotice(req);

        // then
        // 작성자 조회 검증
        verify(memberRepository, times(1)).findById(SAMPLE_USER_ID);

        // noticeRepository.save(...) 호출 여부 검증
        ArgumentCaptor<Notice> noticeCaptor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeRepository, times(1)).save(noticeCaptor.capture());
        Notice savedNotice = noticeCaptor.getValue();

        // Notice에 입력된 값이 요청 그대로 들어갔는지 검증
        assertThat(savedNotice.getMember()).isEqualTo(sampleMember);
        assertThat(savedNotice.getTitle()).isEqualTo("공지생성 테스트1 - 파일없음");
        assertThat(savedNotice.getContent()).isEqualTo("첨부파일 없는 공지 생성 테스트");
        assertThat(savedNotice.getStartAt()).isEqualTo(now);
        assertThat(savedNotice.getEndAt()).isEqualTo(end);
        assertThat(savedNotice.getAttachments()).isEmpty(); // 첨부파일이 없는 시나리오 -> attachments 리스트가 비어 있어야함

        // 최종 응답 DTO 검증
        assertThat(responseDTO).isNotNull();
        assertThat(responseDTO.getId()).isEqualTo(100L);
        assertThat(responseDTO.getTitle()).isEqualTo("공지생성 테스트1 - 파일없음");
        assertThat(responseDTO.getContent()).isEqualTo("첨부파일 없는 공지 생성 테스트");
        assertThat(responseDTO.getAuthor().getId()).isEqualTo(SAMPLE_USER_ID);
        assertThat(responseDTO.getAuthor().getName()).isEqualTo("테스트유저");
        assertThat(responseDTO.getAttachments()).isEmpty();
    }

    @Test
    @DisplayName("2. 유저존재 + 첨부파일 여러개 업로드 -> 성공")
    void createNotice_withMultipleFiles_success() throws Exception {
        // given
        // 더미 MultipartFile 2개 생성
        MultipartFile file1 = mock(MultipartFile.class);
        when(file1.isEmpty()).thenReturn(false);

        MultipartFile file2 = mock(MultipartFile.class);
        when(file2.isEmpty()).thenReturn(false);

        NoticeCreateReqDTO req = NoticeCreateReqDTO.builder()
                .userId(SAMPLE_USER_ID)
                .title("공지생성 테스트2 - 파일여러개")
                .content("첨부파일 여러개 생성 테스트")
                .startAt(now)
                .endAt(end)
                .files(new MultipartFile[]{file1, file2})
                .build();

        when(memberRepository.findById(SAMPLE_USER_ID)).thenReturn(Optional.of(sampleMember));

        // note: thenAnswer
        // FileStorageService.store(...)가 호출될 때마다 다른 Attachment를 반환하도록 세팅
        when(fileStorageService.store(file1)).thenAnswer(invocation -> {
            Attachment a = Attachment.testBuilder()
                    .id(11L)
                    .filename("testImg1.png")
                    .url("/uploads/testImg1.png")
                    .uploadedAt(now.plusMinutes(1))
                    .build();
            return a;
        });

        when(fileStorageService.store(file2)).thenAnswer(invocation -> {
            Attachment a = Attachment.testBuilder()
                    .id(12L)
                    .filename("testDoc1.pdf")
                    .url("/uploads/testDoc1.pdf")
                    .uploadedAt(now.plusMinutes(2))
                    .build();
            return a;
        });

        // 서비스 로직이 Notice를 저장한 뒤 id=200L이 붙었다고 세팅
        when(noticeRepository.save(any(Notice.class))).thenAnswer(invocation -> {
            Notice n = invocation.getArgument(0);
            n.setId(200L);
            return n;
        });

        // when
        NoticeResponseDTO responseDTO = noticeService.createNotice(req);

        // then
        // 작성자 조회 검증
        verify(memberRepository, times(1)).findById(SAMPLE_USER_ID);

        // 파일 저장 서비스가 2번 호출되었는지 검증
        verify(fileStorageService, times(1)).store(file1);
        verify(fileStorageService, times(1)).store(file2);

        // noticeRepository.save(...) 호출 여부 검증
        ArgumentCaptor<Notice> noticeCaptor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeRepository, times(1)).save(noticeCaptor.capture());
        Notice savedNotice = noticeCaptor.getValue();

        // 저장된 Notice에 입력된 값이 요청 그대로 들어갔는지 검증
        assertThat(savedNotice.getMember()).isEqualTo(sampleMember);
        assertThat(savedNotice.getTitle()).isEqualTo("공지생성 테스트2 - 파일여러개");
        assertThat(savedNotice.getContent()).isEqualTo("첨부파일 여러개 생성 테스트");
        assertThat(savedNotice.getStartAt()).isEqualTo(now);
        assertThat(savedNotice.getEndAt()).isEqualTo(end);
        
        // 저장된 Notice의 첨부파일 리스트 크기 확인 -> 2
        assertThat(savedNotice.getAttachments()).hasSize(2);

        // 첫번째 파일 확인
        Attachment savedAtt1 = savedNotice.getAttachments().get(0);
        assertThat(savedAtt1.getFilename()).isEqualTo("testImg1.png");
        assertThat(savedAtt1.getUrl()).isEqualTo("/uploads/testImg1.png");

        // 두번째 파일 확인
        Attachment savedAtt2 = savedNotice.getAttachments().get(1);
        assertThat(savedAtt2.getFilename()).isEqualTo("testDoc1.pdf");
        assertThat(savedAtt2.getUrl()).isEqualTo("/uploads/testDoc1.pdf");

        // 최종 응답 DTO 검증
        assertThat(responseDTO).isNotNull();
        assertThat(responseDTO.getId()).isEqualTo(200L);
        assertThat(responseDTO.getTitle()).isEqualTo("공지생성 테스트2 - 파일여러개");
        assertThat(responseDTO.getContent()).isEqualTo("첨부파일 여러개 생성 테스트");
        assertThat(responseDTO.getAuthor().getId()).isEqualTo(SAMPLE_USER_ID);
        assertThat(responseDTO.getAuthor().getName()).isEqualTo("테스트유저");
        assertThat(responseDTO.getAttachments()).hasSize(2);

        // DTO 내부의 Attachment 검증
        AttachmentInfoDTO attDTO1 = responseDTO.getAttachments().get(0);
        assertThat(attDTO1.getId()).isEqualTo(11L);
        assertThat(attDTO1.getFilename()).isEqualTo("testImg1.png");
        assertThat(attDTO1.getUrl()).isEqualTo("/uploads/testImg1.png");
        assertThat(attDTO1.getUploadedAt()).isEqualTo(now.plusMinutes(1));

        AttachmentInfoDTO attDTO2 = responseDTO.getAttachments().get(1);
        assertThat(attDTO2.getId()).isEqualTo(12L);
        assertThat(attDTO2.getFilename()).isEqualTo("testDoc1.pdf");
        assertThat(attDTO2.getUrl()).isEqualTo("/uploads/testDoc1.pdf");
        assertThat(attDTO2.getUploadedAt()).isEqualTo(now.plusMinutes(2));
    }

    @Test
    @DisplayName("3. 존재하지 않는 유저 -> MEMBER_NOT_FOUND 예외 발생")
    void createNotice_memberNotFound_throwsException() {
        // given
        NoticeCreateReqDTO req = NoticeCreateReqDTO.builder()
                .userId(99L) // 존재하지 않는 ID
                .title("공지생성 테스트3 - 존재하지 않는 유저")
                .content("존재하지 않는 유저 테스트")
                .startAt(now)
                .endAt(end)
                .files(null)
                .build();

        // 유저 조회 시 빈 값을 반환하도록 세팅
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        // when+then
        assertThatThrownBy(() -> noticeService.createNotice(req))
                .isInstanceOf(CustomExceptionHandler.class) //예외 타입이 CustomExceptionHandler인지 확인
                .extracting("errorCode") // 에러코드 가져와서
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND); // MEMBER_NOT_FOUND 인지 확인..
    }

    @Test
    @DisplayName("4. 시작일<종료일 -> INVALID_DATE_RANGE 예외 발생")
    void createNotice_invalidDateRange_throwsException() {
        // given
        NoticeCreateReqDTO req = NoticeCreateReqDTO.builder()
                .userId(SAMPLE_USER_ID)
                .title("공지생성 테스트4 - 종료일이 시작일보다 빠를때")
                .content("시작일<종료일 테스트")
                .startAt(now)
                .endAt(now.minusDays(2)) // 시작일 < 종료일로 세팅
                .files(null)
                .build();

        when(memberRepository.findById(SAMPLE_USER_ID)).thenReturn(Optional.of(sampleMember));

        // when+then
        assertThatThrownBy(() -> noticeService.createNotice(req))
                .isInstanceOf(CustomExceptionHandler.class) //예외 타입이 CustomExceptionHandler인지 확인
                .extracting("errorCode") // 에러코드 가져와서
                .isEqualTo(ErrorCode.INVALID_DATE_RANGE); // INVALID_DATE_RANGE 인지 확인..
    }
}
