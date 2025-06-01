package com.rsupport.board.notice.api.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeCreateReqDTO {
    @Schema(description = "공지 제목", example = "공지제목입니다.")
    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @Schema(description = "공지 내용", example = "공지내용입니다. 공지내용입니다.")
    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    @Schema(description = "작성자 id", example = "1")
    @NotNull(message = "userId는 필수입니다.")
    private Long userId;

    @Schema(description = "공지 시작 일시", example = "2025-06-01T00:00:00", type="string", format="date-time")
    @NotNull(message = "시작일 지정은 필수입니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startAt;

    @Schema(description = "공지 종료 일시", example = "2025-06-01T00:00:00", type="string", format="date-time")
    @NotNull(message = "종료일 지정은 필수입니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endAt;

    @ArraySchema(
            schema = @Schema(
                    description = "첨부파일 리스트(0개 이상)",
                    type = "string",
                    format = "binary"
            ),
            arraySchema = @Schema(description = "첨부파일 배열")
    )
    private MultipartFile[] files;
}
