package com.rsupport.board.notice.service;

import com.rsupport.board.notice.api.dto.NoticeListItemDTO;
import com.rsupport.board.notice.api.dto.NoticeListReqDTO;
import com.rsupport.board.notice.api.dto.NoticeListResDTO;
import com.rsupport.board.notice.domain.repository.NoticeRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 공지 목록 조회(검색) 서비스 단위테스트
 */
@ExtendWith(MockitoExtension.class)
class NoticeServiceImpl_getListTest {
    @Mock
    private NoticeRepository noticeRepository;

    @InjectMocks
    private NoticeServiceImpl noticeService;

    private NoticeListReqDTO sampleReq;
    private Pageable samplePageable;

    private NoticeListItemDTO buildReq(Long id, String title, Boolean hasAttachment, LocalDateTime createdAt,  LocalDateTime startAt,
                                       LocalDateTime endAt, Integer viewCount, Long authorId, String authorName) {
        return new NoticeListItemDTO(
                id,
                title,
                hasAttachment,
                createdAt,
                startAt,
                endAt,
                viewCount,
                authorId,
                authorName
        );
    }

    @BeforeEach
    void setUp() {
        // 기본 검색 조건 세팅
        sampleReq = new NoticeListReqDTO();
        sampleReq.setKeyword("test");
        sampleReq.setTitleOnly(false);
        sampleReq.setFromDate(LocalDate.of(2025, 5, 15));
        sampleReq.setToDate(LocalDate.of(2025, 6, 30));

        // 기본 Pageable 설정 (첫 페이지:0, 페이지 크기:10, createdAt 내림차순)
        samplePageable = PageRequest.of(0,10, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    @DisplayName("1. 검색 조건에 맞는 공지가 존재할 때 -> 올바른 결과 반환")
    void getNoticeList_WithResults_ReturnsCorrectPage() {
        // given
        // 더미 NoticeListItemDTO 2개 생성
        NoticeListItemDTO item1 = buildReq(
                100L, "첫번째 공지", true,
                LocalDateTime.of(2025, 5, 10, 9, 0, 0),
                LocalDateTime.of(2025, 5, 10, 9, 0, 0),
                LocalDateTime.of(2025, 5, 15, 9, 0, 0),
                5, 1L, "작성자A"
        );
        NoticeListItemDTO item2 = buildReq(
                101L, "두번째 공지", false,
                LocalDateTime.of(2025, 5, 5, 14, 30, 0),
                LocalDateTime.of(2025, 5, 5, 14, 30, 0),
                LocalDateTime.of(2025, 5, 7, 14, 30, 0),
                2, 2L, "작성자B"
        );
        List<NoticeListItemDTO> noticeList = List.of(item1, item2);

        long totalElements = 2L; // 전체개수: 2 세팅
        int totalPages = 1; // 페이지크기: 10, 전체개수: 2 -> 총 1페이지
        Page<NoticeListItemDTO> fakePage = new PageImpl<>(noticeList, samplePageable, totalElements);

        // 샘플 req 로 조회 시 세팅해둔 page 반환
        when(noticeRepository.findAllBySearchCondition(sampleReq, samplePageable)).thenReturn(fakePage);

        // when
        NoticeListResDTO responseDTO = noticeService.getNoticeList(sampleReq, samplePageable);

        // then
        // 정확한 파라미터로 호출됐는지 검증 -> argument captor..
        ArgumentCaptor<NoticeListReqDTO> reqCaptor = ArgumentCaptor.forClass(NoticeListReqDTO.class);
        ArgumentCaptor<Pageable> pageCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(noticeRepository, times(1)).findAllBySearchCondition(reqCaptor.capture(), pageCaptor.capture());
        NoticeListReqDTO capturedReq = reqCaptor.getValue();
        Pageable capturedPage = pageCaptor.getValue();

        // 요청값이 그대로 넘어갔는지 확인
        assertThat(capturedReq.getKeyword()).isEqualTo("test");
        assertThat(capturedReq.getFromDate()).isEqualTo(LocalDate.of(2025, 5, 15));
        assertThat(capturedReq.getToDate()).isEqualTo(LocalDate.of(2025, 6, 30));
        assertThat(capturedReq.getTitleOnly()).isFalse();

        // pageable의 정보도 잘 넘어갔는지 확인
        assertThat(capturedPage.getPageNumber()).isEqualTo(0);
        assertThat(capturedPage.getPageSize()).isEqualTo(10);
        assertThat(capturedPage.getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);

        // 반환된 noticeList 확인
        List<NoticeListItemDTO> returnedList = responseDTO.getNoticeList();
        assertThat(returnedList).hasSize(2);
        assertThat(returnedList.get(0).getId()).isEqualTo(100L);
        assertThat(returnedList.get(0).getTitle()).isEqualTo("첫번째 공지");
        assertThat(returnedList.get(0).getHasAttachment()).isTrue();
        assertThat(returnedList.get(0).getAuthor().getName()).isEqualTo("작성자A");

        assertThat(returnedList.get(1).getId()).isEqualTo(101L);
        assertThat(returnedList.get(1).getHasAttachment()).isFalse();
        assertThat(returnedList.get(1).getAuthor().getId()).isEqualTo(2L);

        // 반환된 pageInfo 확인
        NoticeListResDTO.PageInfo pageInfo = responseDTO.getPageInfo();
        assertThat(pageInfo.getTotalElements()).isEqualTo(2L);
        assertThat(pageInfo.getTotalPages()).isEqualTo(1);
        assertThat(pageInfo.isFirst()).isTrue();
        assertThat(pageInfo.isLast()).isTrue();
        assertThat(pageInfo.getPageNumber()).isEqualTo(0);
        assertThat(pageInfo.getPageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("2. 검색 조건에 맞는 공지가 없을떄 -> noticeList는 빈 리스트, pageInfo만 기본값으로 채워서 반환")
    void getNoticeList_NoResults_ReturnsEmptyContent() {
        // given
        List<NoticeListItemDTO> emptyList = Collections.emptyList();
        Page<NoticeListItemDTO> emptyPage = new PageImpl<>(emptyList, samplePageable, 0L );
        when(noticeRepository.findAllBySearchCondition(sampleReq, samplePageable)).thenReturn(emptyPage);

        // when
        NoticeListResDTO resultDto = noticeService.getNoticeList(sampleReq, samplePageable);

        // then
        // 호출됐는지만 확인. 파라미터 확인 x
        verify(noticeRepository, times(1)).findAllBySearchCondition(sampleReq, samplePageable);

        // noticeList는 비어있어야 함
        assertThat(resultDto.getNoticeList()).isEmpty();

        // pageInfo 확인
        NoticeListResDTO.PageInfo pageInfo = resultDto.getPageInfo();
        assertThat(pageInfo.getTotalElements()).isEqualTo(0L);
        assertThat(pageInfo.getTotalPages()).isEqualTo(0);
        assertThat(pageInfo.isFirst()).isTrue();
        assertThat(pageInfo.isLast()).isTrue();
        assertThat(pageInfo.getPageNumber()).isEqualTo(0);
        assertThat(pageInfo.getPageSize()).isEqualTo(10);
    }
}
