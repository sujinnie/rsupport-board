package com.rsupport.board.notice.api.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 공지사항 목록 아이템(하나)
 *
 * {
 *   "id": 123,
 *   "title": "test",
 *   "hasAttachment": false,
 *   "createdAt": "2025-06-01T18:00:00",
 *   "startAt": "2025-06-01T18:00:00",
 *   "endAt": "2025-06-05T18:00:00",
 *   "viewCount": 777,
 *   "author": {
 *     "id": 1,
 *     "name": "테스트"
 *   }
 * }
 */
@Data
@NoArgsConstructor
public class NoticeListItemDTO {
    private Long id;
    private String title;
    private Boolean hasAttachment;
    private LocalDateTime createdAt;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer viewCount;
    private AuthorInfoDTO author;

    @QueryProjection
    public NoticeListItemDTO(Long id,  String title, Boolean hasAttachment, LocalDateTime createdAt, LocalDateTime startAt,
                             LocalDateTime endAt, Integer viewCount, Long authorId, String authorName) {
        this.id = id;
        this.title = title;
        this.hasAttachment = hasAttachment;
        this.createdAt = createdAt;
        this.startAt = startAt;
        this.endAt = endAt;
        this.viewCount = viewCount;
        this.author = new AuthorInfoDTO(authorId, authorName);
    }
}
