package com.rsupport.board.notice.domain.repository;

import com.rsupport.board.notice.api.dto.NoticeListItemDTO;
import com.rsupport.board.notice.api.dto.NoticeListReqDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NoticeRepositoryCustom {
    /**
     * 검색 조건(NoticeListReqDTO)에 맞춰 공지 목록 조회, 페이지네이션된 결과 반환
     *
     * @param req 검색 조건(keyword, titleOnly, fromDate, toDate)
     * @param pageable  페이징 조건(page, size, sort ...)
     * @return 페이지네이션된 공지 리스트
     */
    Page<NoticeListItemDTO> findAllBySearchCondition(NoticeListReqDTO req, Pageable pageable);
}
