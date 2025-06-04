package com.rsupport.board.notice.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 공지사항 상세 조회 내용, 생성된 조회 정보
 *
 * {
 *   "id": 123,
 *   "title": "test",
 *   "content": "aaa",
 *   "createdAt": "2025-06-01T18:00:00",
 *   "updatedAt": "2025-06-01T18:00:00",
 *   "startAt": "2025-06-01T18:00:00",
 *   "endAt": "2025-06-05T18:00:00",
 *   "viewCount": 777,
 *   "author": {
 *     "id": 1,
 *     "name": "테스트"
 *   },
 *   "attachments": [
 *      {...}, {...}
 *   ]
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoticeResponseDTO {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long viewCount;
    private AuthorInfoDTO author; //작성자(==멤버,유저)
    private List<AttachmentInfoDTO> attachments;
}
