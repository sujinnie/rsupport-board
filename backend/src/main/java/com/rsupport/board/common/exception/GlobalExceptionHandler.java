package com.rsupport.board.common.exception;

import com.rsupport.board.common.dto.ResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.nio.file.AccessDeniedException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * CustomException 처리
     */
    @ExceptionHandler(CustomExceptionHandler.class)
    public ResponseEntity<ResponseDTO<?>> handleCustomException(CustomExceptionHandler e) {
        ErrorCode errorCode = e.getErrorCode();

        if(errorCode.getStatus().value() / 100 == 4) {
            log.error("\nBusinessException occurred caused by client: {}", errorCode);
            log.error("\n{}", Arrays.stream(e.getStackTrace()).findFirst().orElse(null));
        }
        else if(errorCode.getStatus().value() / 100 == 5) {
            log.error("\nBusinessException occurred caused by server: {}", errorCode.getMessage());
            log.error("\n{}", Arrays.stream(e.getStackTrace()).findFirst().orElse(null));
        }
        else {
            log.error("BusinessException", e);
        }

        ResponseDTO<?> body = ResponseDTO.error(errorCode.getCode(), e.getMessage());
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(body);
    }

    /**
     * @Valid 검증 실패 시 발생하는 예외 처리 (@NotBlank 등..)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseDTO<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("handleMethodArgumentNotValidException", e);

        // field() : 검증 실패한 필드명 (ex. title, content 등)
        // defaultMessage() : @NotBlank(message="...") 등에서 정의한 메시지
        // 이것만 추출해서 검증 하기
        List<String> errorMessages = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getDefaultMessage())
                .toList();

        String combinedMessage = String.join(", ", errorMessages);

        ResponseDTO<?> body = ResponseDTO.error(ErrorCode.INVALID_INPUT_VALUE.getCode(), combinedMessage);
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(body);
    }

    /**
     * 바인딩 실패 시 발생하는 예외 처리
     */
    @ExceptionHandler(BindException.class)
    protected ResponseEntity<ResponseDTO<?>> handleBindException(BindException e) {
        log.error("handleBindException", e);

        ResponseDTO<?> body = ResponseDTO.error(ErrorCode.INVALID_INPUT_VALUE.getCode(), e.getMessage());
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(body);
    }

    /**
     * enum type 일치하지 않아 binding 못할 경우 발생
     * 주로 @RequestParam enum으로 binding 못했을 경우 발생
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ResponseDTO<?>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.error("handleMethodArgumentTypeMismatchException", e);

        ResponseDTO<?> body = ResponseDTO.error(ErrorCode.INVALID_INPUT_VALUE.getCode(), e.getMessage());
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(body);
    }

    /**
     * 지원하지 않은 HTTP method 호출 시 발생하는 예외 처리
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ResponseDTO<?>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.error("handleHttpRequestMethodNotSupportedException", e);

        ResponseDTO<?> body = ResponseDTO.error(ErrorCode.METHOD_NOT_ALLOWED.getCode(), e.getMessage());
        return ResponseEntity
                .status(ErrorCode.METHOD_NOT_ALLOWED.getStatus())
                .body(body);
    }

    /**
     * 권한 관련 예외 처리
     */
    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<ResponseDTO<?>> handleAccessDeniedException(AccessDeniedException e) {
        log.error("AccessDeniedException", e);

        ResponseDTO<?> body = ResponseDTO.error(ErrorCode.ACCESS_DENIED.getCode(), e.getMessage());
        return ResponseEntity
                .status(ErrorCode.ACCESS_DENIED.getStatus())
                .body(body);
    }

    /**
     * 그 외 500 Internal Server Error로 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDTO<?>> handleException(Exception e) {
        log.error("Exception", e);

        ResponseDTO<?> body = ResponseDTO.error(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(body);
    }
}
