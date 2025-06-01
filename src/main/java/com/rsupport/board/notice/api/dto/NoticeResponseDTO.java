package com.rsupport.board.notice.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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

//    @Data
//    @NoArgsConstructor
//    @AllArgsConstructor
//    public static class AttachmentDTO {
//        private Long id;
//        private String filename;
//        private String url;
//        private LocalDateTime uploadedAt;
//    }
}
