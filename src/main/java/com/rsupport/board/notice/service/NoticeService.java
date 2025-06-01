package com.rsupport.board.notice.service;

import com.rsupport.board.notice.api.dto.*;
import org.springframework.data.domain.Pageable;

public interface NoticeService {

    /**
     * 공지 등록(create)
     *
     * @param req 등록할 공지 정보
     * @return 생성된 공지id + 정보
     */
    NoticeResponseDTO createNotice(NoticeCreateReqDTO req);

    /**
     * 공지 목록 조회 (검색+페이지네이션)
     *
     * @param req 검색 조건 (keyword, titleOnly, fromDate, toDate)
     * @param pageable  페이지네이션 정보 (page, size, sort 정보)
     * @return 검색된 공지 목록
     */
    NoticeListResDTO getNoticeList(NoticeListReqDTO req, Pageable pageable);

    // todo: 상세조회 수정 삭제
}
