package com.rsupport.board.notice.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 공지사항 목록
 *
 * {
 *   "noticeList": [{...}, {...}, ...],
 *   "pageInfo": {...}
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoticeListResDTO {
    private List<NoticeListItemDTO> noticeList;  // 실제 조회된 목록
    private PageInfo pageInfo; // 페이징 정보

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageInfo {
        private int pageNumber; // 현재 페이지 번호(0부터 시작)
        private int pageSize; // 한 페이지당 보여줄 개수
        private long totalElements;
        private int totalPages;
        private boolean first;
        private boolean last;
    }
}
