package com.rsupport.board.notice.api.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 공지 수정 요청 DTO
 *
 * - PATCH 메서드 사용
 * - null 또는 필드 생략: 기존 값 유지
 * - 빈스트링(""): 덮어씀
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeUpdateReqDTO {
    @Schema(description = "수정자 id", example = "1")
    @NotNull(message = "userId는 필수입니다.")
    private Long userId;

    @Schema(description = "새로운 제목(null 혹은 필드 생략 시 기존값 유지, 빈스트링 전송 시 초기화)",
            example = "수정된 공지 제목입니다.")
    private String title;

    @Schema(description = "새로운 내용(null 혹은 필드 생략 시 기존값 유지, 빈스트링 전송 시 초기화)",
            example = "수정된 공지 내용입니다.")
    private String content;

    @Schema(description = "새로운 시작 일시(yyyy-MM-dd'T'HH:mm:ss). (생략 시 기존값 유지)",
            example = "2025-06-10T09:00:00", type = "string", format = "date-time")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startAt;

    @Schema(description = "새로운 종료 일시(ISO-8601, yyyy-MM-dd'T'HH:mm:ss). (생략 시 기존값 유지)",
            example = "2025-06-15T18:00:00", type = "string", format = "date-time")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endAt;

    @ArraySchema(
            schema = @Schema(
                    description = "새로 추가할 첨부파일 리스트(0개 이상)",
                    type = "string",
                    format = "binary"
            ),
            arraySchema = @Schema(description = "첨부파일 배열")
    )
    private MultipartFile[] newFiles;

    @Schema(description = "삭제할 기존 첨부파일 ID 목록. 예시: removeAttachmentIds=11, removeAttachmentIds=13")
    private List<Long> removeAttachmentIds;
}
