package com.rsupport.board.notice.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentInfoDTO {
    private Long id;
    private String filename;
    private String url;
    private LocalDateTime uploadedAt;
}
