package com.rsupport.board.notice.domain.repository;

import com.rsupport.board.notice.api.dto.NoticeListItemDTO;
import com.rsupport.board.notice.api.dto.NoticeListReqDTO;

import com.rsupport.board.notice.domain.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface NoticeRepositoryCustom {
    /**
     * 검색 조건(NoticeListReqDTO)에 맞춰 공지 목록 조회, 페이지네이션된 결과 반환
     *
     * @param req 검색 조건(keyword, titleOnly, fromDate, toDate)
     * @param pageable  페이징 조건(page, size, sort ...)
     * @return 페이지네이션된 공지 리스트
     */
    Page<NoticeListItemDTO> findAllBySearchCondition(NoticeListReqDTO req, Pageable pageable);

    /**
     * 공지 상세 조회 (멤버, 첨부파일 정보 함께 가져오기, n+1방지)
     *
     * @param noticeId 조회할 공지 id
     * @return 공지
     */
    Optional<Notice> findWithMemberAndAttachmentsById(Long noticeId);
}
