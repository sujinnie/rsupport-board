package com.rsupport.board.notice.service;

import com.rsupport.board.notice.api.dto.*;
import org.springframework.data.domain.Pageable;

public interface NoticeService {

    /**
     * 공지 등록 (create)
     *
     * @param req 등록할 공지 정보
     * @return 생성된 공지 id + 정보
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

    /**
     * 공지 상세 조회 (read)
     *
     * @param userId 유저 id
     * @param noticeId 공지 id
     * @return 공지 상세 내용
     */
    NoticeResponseDTO getNotice(Long userId, Long noticeId);

    /**
     * 공지 수정 (update)
     *
     * @param userId 수정자 id
     * @param req 수정할 공지 정보
     * @return 수정된 공지 내용
     */
    NoticeResponseDTO updateNotice(Long userId, Long noticeId, NoticeUpdateReqDTO req);

    // todo: 삭제
}
