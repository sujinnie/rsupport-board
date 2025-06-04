package com.rsupport.board.notice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 공지사항 목록 조회(+검색) 요청 DTO
 */
@Data
@NoArgsConstructor
public class NoticeListReqDTO {
    @Schema(description = "키워드", example = "검색할 키워드입니다. (ex.점검)")
    @Nullable
    private String keyword;

    @Schema(description = "키워드 검색 범위", example = "true: 제목만, false: 제목+내용")
    @Nullable
    private Boolean titleOnly;

    @Schema(description = "검색 기간 시작일", example = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Nullable
    private LocalDate fromDate;

    @Schema(description = "검색 기간 종료일", example = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Nullable
    private LocalDate toDate;

    @Override
    public String toString() { // 로그
        return "NoticeSearchReqDTO{" +
                "q='" + keyword + '\'' +
                ", titleOnly=" + titleOnly +
                ", fromDate=" + fromDate +
                ", toDate=" + toDate +
                '}';
    }
}
