package com.rsupport.board.notice.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

/**
 * 공지 목록 조회(검색) 서비스 통합 테스트
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")  // application-test.properties 를 사용
class NoticeController_getListIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Member savedMember;

    @BeforeEach
    void setUp() {
        // DB 초기화: 저장된 데이터 미리 삭제
        attachmentRepository.deleteAll();
        noticeRepository.deleteAll();
        memberRepository.deleteAll();

        // 테스트용 유저 생성
        Member m = Member.builder()
                .name("테스트유저")
                .email("testuser@example.com")
                .password("password123")
                .build();
        savedMember = memberRepository.save(m);

        // 테스트용 공지 3개 생성
        // 공지1: 작성일 2025-05-01, 첨부파일 없음
        Notice notice1 = Notice.testBuilder()
                .member(savedMember)
                .title("찐공지테스트1")
                .content("2025-05-01 첨부파일 없음 공지내용1")
                .startAt(LocalDateTime.of(2025, 5, 1, 0, 0))
                .endAt(LocalDateTime.of(2025, 5, 2, 0, 0))
                .viewCount(10)
                .createdAt(LocalDateTime.of(2025, 5, 1, 8, 0))
                .updatedAt(LocalDateTime.of(2025, 5, 1, 8, 0))
                .build();
        notice1 = noticeRepository.save(notice1);

        // 공지2: 작성일 2025-05-10, 첨부파일 1개
        Notice notice2 = Notice.testBuilder()
                .member(savedMember)
                .title("공지테스트2")
                .content("2025-05-10 첨부파일1개 공지내용2")
                .startAt(LocalDateTime.of(2025, 5, 10, 0, 0))
                .endAt(LocalDateTime.of(2025, 5, 11, 0, 0))
                .viewCount(5)
                .createdAt(LocalDateTime.of(2025, 5, 10, 9, 30))
                .updatedAt(LocalDateTime.of(2025, 5, 10, 9, 30))
                .build();
        notice2 = noticeRepository.save(notice2);

        Attachment att2 = Attachment.testBuilder()
                .filename("test2.pdf")
                .url("/uploads/test2.pdf")
                .uploadedAt(LocalDateTime.of(2025, 5, 10, 9, 31))
                .notice(notice2)
                .build();
        attachmentRepository.save(att2);

        // 공지3: 작성일 2025-06-01, 첨부파일 2개
        Notice notice3 = Notice.testBuilder()
                .member(savedMember)
                .title("공지테스트3")
                .content("2025-06-01 첨부파일2개 공지내용3.")
                .startAt(LocalDateTime.of(2025, 6, 1, 0, 0))
                .endAt(LocalDateTime.of(2025, 6, 2, 0, 0))
                .viewCount(7)
                .createdAt(LocalDateTime.of(2025, 6, 1, 12, 0))
                .updatedAt(LocalDateTime.of(2025, 6, 1, 12, 0))
                .build();
        notice3 = noticeRepository.save(notice3);

        Attachment att31 = Attachment.testBuilder()
                .filename("test31.txt")
                .url("/uploads/test31.txt")
                .uploadedAt(LocalDateTime.of(2025, 6, 1, 12, 1))
                .notice(notice3)
                .build();
        Attachment att32 = Attachment.testBuilder()
                .filename("test32.png")
                .url("/uploads/test32.png")
                .uploadedAt(LocalDateTime.of(2025, 6, 1, 12, 2))
                .notice(notice3)
                .build();
        attachmentRepository.save(att31);
        attachmentRepository.save(att32);
    }

    @Test
    @DisplayName("1. [기본조회] 공지 목록 조회 -> 최신 등록순으로 정렬되어 반환")
    void getNoticeList_DefaultSorting_ShowsHasAttachmentCorrectly() throws Exception {
        /*
         * 테스트 목표:
         *  - 저장된 공지3 -> 2 -> 1 순서대로 나와야 함
         *  - 각각의 hasAttachment 값이 false, true, true 여야함
         */
        mockMvc.perform(
                        get("/v1/notices").accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON + ";charset=UTF-8"))
                .andExpect(jsonPath("$.status").value("success"))
                // body.data.noticeList 길이: 3
                .andExpect(jsonPath("$.data.noticeList.length()").value(3))

                // 공지3 부터 나왔는지 확인 (idx:0)
                .andExpect(jsonPath("$.data.noticeList[0].id").value(noticeRepository.findAllByOrderByCreatedAtDesc().get(0).getId()))
                .andExpect(jsonPath("$.data.noticeList[0].title").value("공지테스트3"))
                .andExpect(jsonPath("$.data.noticeList[0].hasAttachment").value(true))
                .andExpect(jsonPath("$.data.noticeList[0].viewCount").value(7))
                .andExpect(jsonPath("$.data.noticeList[0].author.id").value(savedMember.getId().intValue()))
                .andExpect(jsonPath("$.data.noticeList[0].author.name").value("테스트유저"))

                // 공지2 (idx:1)
                .andExpect(jsonPath("$.data.noticeList[1].title").value("공지테스트2"))
                .andExpect(jsonPath("$.data.noticeList[1].hasAttachment").value(true))

                // 공지1 (idx:2)
                .andExpect(jsonPath("$.data.noticeList[2].title").value("찐공지테스트1"))
                .andExpect(jsonPath("$.data.noticeList[2].hasAttachment").value(false))

                // pageInfo 확인: 총개수:3, 총 페이지: 1, first=true, last=true
                .andExpect(jsonPath("$.data.pageInfo.totalElements").value(3))
                .andExpect(jsonPath("$.data.pageInfo.totalPages").value(1))
                .andExpect(jsonPath("$.data.pageInfo.first").value(true))
                .andExpect(jsonPath("$.data.pageInfo.last").value(true))
                .andExpect(jsonPath("$.data.pageInfo.pageSize").value(20)) // default
                .andExpect(jsonPath("$.data.pageInfo.pageNumber").value(0)); // default
    }

    @Test
    @DisplayName("2. [키워드, 날짜 필터링 테스트] 검색 키워드: ‘3’ + 날짜 범위: 2025-06-01~2025-06-30 로 조회 -> 공지3만 조회됨")
    void getNoticeList_WithKeywordAndDateFilter_ReturnsFilteredResults() throws Exception {
        /*
         * 테스트 목표:
         *  - 공지3만 조회되어야 함
         */
        mockMvc.perform(
                        get("/v1/notices")
                                .queryParam("keyword", "3")
                                .queryParam("titleOnly", "false")
                                .queryParam("fromDate", "2025-06-01")
                                .queryParam("toDate", "2025-06-30")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON + ";charset=UTF-8"))

                // 결과가 1개만 나와야 함
                .andExpect(jsonPath("$.data.noticeList.length()").value(1))
                .andExpect(jsonPath("$.data.noticeList[0].title").value("공지테스트3"))
                .andExpect(jsonPath("$.data.noticeList[0].hasAttachment").value(true))

                // pageInfo: 총 개수 1, 총 페이지 1, first=true, last=true
                .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1))
                .andExpect(jsonPath("$.data.pageInfo.totalPages").value(1))
                .andExpect(jsonPath("$.data.pageInfo.first").value(true))
                .andExpect(jsonPath("$.data.pageInfo.last").value(true));
    }

    @Test
    @DisplayName("3. [제목만 검색(titleOnly=true)테스트] 검색키워드: ‘찐’ 조회 -> 공지1만 조회됨")
    void getNoticeList_TitleOnlyTrue_ReturnsOnlyTitleMatches() throws Exception {
        /*
         * 테스트 목표:
         *  - 공지1 만 조회됨
         */

        mockMvc.perform(
                        get("/v1/notices")
                                .queryParam("keyword", "찐")
                                .queryParam("titleOnly", "true")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON + ";charset=UTF-8"))

                // 결과가 1개만 나와야 함
                .andExpect(jsonPath("$.data.noticeList.length()").value(1))
                .andExpect(jsonPath("$.data.noticeList[0].title").value("찐공지테스트1"))
                .andExpect(jsonPath("$.data.noticeList[0].hasAttachment").value(false))

                // pageInfo: 총 개수 1, 총 페이지 1, first=true, last=true
                .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1))
                .andExpect(jsonPath("$.data.pageInfo.totalPages").value(1))
                .andExpect(jsonPath("$.data.pageInfo.first").value(true))
                .andExpect(jsonPath("$.data.pageInfo.last").value(true));
    }

    @Test
    @DisplayName("4. [페이징 테스트] page=1, size=1 조회 -> 두 번째 페이지에 공지2가 나와야함")
    void getNoticeList_PagingSecondPage_ReturnsSecondItem() throws Exception {
        /*
         * 테스트 목표:
         *  - 기본 순서: 공지3 -> 2 -> 1
         *  - page=1, size=1 으로 요청 → 첫페이지(0)는 공지3, 두번째 페이지(1)는 공지2 가 나와야함
         */
        mockMvc.perform(
                        get("/v1/notices")
                                .queryParam("page", "1")
                                .queryParam("size", "1")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON + ";charset=UTF-8"))

                // noticeList 길이는 1
                .andExpect(jsonPath("$.data.noticeList.length()").value(1))

                // 두 번째 페이지이므로 공지2가 나와야 함
                .andExpect(jsonPath("$.data.noticeList[0].title").value("공지테스트2"))
                .andExpect(jsonPath("$.data.noticeList[0].hasAttachment").value(true))

                // pageInfo: 총 개수 3, 총 페이지 3(size=1 이니까 3페이지), first=false, last=false
                .andExpect(jsonPath("$.data.pageInfo.totalElements").value(3))
                .andExpect(jsonPath("$.data.pageInfo.totalPages").value(3))
                .andExpect(jsonPath("$.data.pageInfo.pageNumber").value(1))
                .andExpect(jsonPath("$.data.pageInfo.pageSize").value(1))
                .andExpect(jsonPath("$.data.pageInfo.first").value(false))
                .andExpect(jsonPath("$.data.pageInfo.last").value(false));
    }
}
