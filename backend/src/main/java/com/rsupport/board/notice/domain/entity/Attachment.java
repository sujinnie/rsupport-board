package com.rsupport.board.notice.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 첨부파일 엔티티
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "attachment")
@EntityListeners(AuditingEntityListener.class)
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // N:1 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "url", length = 2048, nullable = false)
    private String url;

    @CreatedDate
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @Builder
    public Attachment(String filename, String url, LocalDateTime uploadedAt){
        this.filename = filename;
        this.url = url;
        this.uploadedAt = uploadedAt;
    }

    @Builder(builderClassName = "TestBuilder", builderMethodName = "testBuilder")
    public Attachment(Long id, String filename, String url, LocalDateTime uploadedAt, Notice notice){
        this.id = id;
        this.filename = filename;
        this.url = url;
        this.uploadedAt = uploadedAt;
        this.notice = notice;
    }

    void setNotice(Notice notice) {
        this.notice = notice;
    }
}
