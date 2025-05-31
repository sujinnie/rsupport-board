package com.rsupport.board.common.exception;

import org.springframework.http.HttpStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 전체 에러 코드 정의
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 공통 에러
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C0001", "잘못된 입력 값입니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C0002", "잘못된 타입입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C0003", "허용되지 않은 메서드입니다."), // 405
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C0004", "존재하지 않는 엔티티입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C0005", "서버 오류가 발생했습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "C0006", "접근이 거부되었습니다."), // 403

    // Notice 관련 에러
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "N0001", "해당 공지를 찾을 수 없습니다."),
    ATTACHMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "N0002", "첨부파일을 찾을 수 없습니다."),

    // Member 관련 에러
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M0001", "해당 회원을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
