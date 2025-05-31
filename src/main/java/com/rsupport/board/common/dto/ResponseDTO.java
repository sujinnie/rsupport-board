package com.rsupport.board.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 공통 응답 포맷
 *
 * {
 *   "status": "success" or "error",
 *   "data": {...} or null,
 *   "exception": {
 *       "code": "N0001",
 *       "message": "해당 공지를 찾을 수 없습니다."
 *   } or null
 * }
 *
 * @param <T>  성공 시 반환할 데이터 타입
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDTO<T> {
    private String status;
    private T data;
    private ExceptionDTO exception;

    public static <T> ResponseDTO<T> success(T data) {
        return new ResponseDTO<>("success", data, null);
    }

    public static <T> ResponseDTO<T> error(String code, String message) {
        return new ResponseDTO<>("error", null, new ExceptionDTO(code, message));
    }
}
