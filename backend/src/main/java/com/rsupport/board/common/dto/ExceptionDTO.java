package com.rsupport.board.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 에러 발생 시 응답에 포함될 에러 정보
 * {
 *   "code": "N0001",
 *   "message": "해당 공지를 찾을 수 없습니다."
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExceptionDTO {
    private String code;
    private String message;
}
