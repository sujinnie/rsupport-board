package com.rsupport.board.notice.service;

import com.rsupport.board.notice.api.dto.NoticeCreateReqDTO;
import com.rsupport.board.notice.api.dto.NoticeResponseDTO;

public interface NoticeService {
    // 공지 등록(create)
    NoticeResponseDTO createNotice(NoticeCreateReqDTO req);

    // todo: 목록조회 상세조회 수정 삭제
}
