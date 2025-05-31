package com.rsupport.board.notice.domain.entity;

import com.rsupport.board.common.entity.BaseTimeEntity;
import com.rsupport.board.member.domain.entity.Member;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 공지 엔티티
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "notice")
public class Notice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private Member member;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    // Notice : Attachment = 1 : n
    @OneToMany(mappedBy = "notice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attachment> attachments = new ArrayList<>();

    @Builder
    public Notice(Member member, String title, String content, LocalDateTime startAt, LocalDateTime endAt) {
        this.member = member;
        this.title = title;
        this.content = content;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public void update(String title, String content, LocalDateTime startAt, LocalDateTime endAt) {
        this.title = title;
        this.content = content;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    /**
     * 단위테스트용..
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 첨부파일 추가
     *
     * @param attachment
     */
    public void addAttachment(Attachment attachment) {
        this.attachments.add(attachment);
        attachment.setNotice(this);
    }

    /**
     * 조회수 증가
     */
    public void increaseViewCount() {
        this.viewCount++;
    }

}
