package com.rsupport.board.notice.api.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 공지 생성/조회/수정/삭제 등에 필요한 작성자 정보
 */
@Data
@NoArgsConstructor
public class AuthorInfoDTO {
    private Long id;
    private String name;

    @QueryProjection
    public AuthorInfoDTO(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
