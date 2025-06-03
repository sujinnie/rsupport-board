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
@Table(

        name = "notice",
        indexes = {
                // 정렬용: created_at 단일 인덱스(최신 등록일로 정렬)
                @Index(name = "idx_notice_created_at", columnList = "created_at"),
                // 범위 검색용: end_at 단일 인덱스(endAt만 들어왔을 때 범위필터용)
                @Index(name = "idx_notice_end_at", columnList = "end_at"),
                // 범위 검색용(복합): start_at + end_at (startAt만 혹은 startAt <= x <= endAt 일때)
                @Index(name = "idx_notice_start_end", columnList = "start_at, end_at"),
                // 키워드 검색용: title 단일 인덱스(포함관계 검색)
                // todo: content 까지 하려면.. fulltext사용하거나 elasticSearch 같은걸 도입해야한다... 지금은 서비스단에서 분리만ㄱㄱ
                @Index(name = "idx_notice_title", columnList = "title")
        }
)
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

    // 업데이트 메서드
    public void updateTitle(String title) { this.title = title; }
    public void updateContent(String content) { this.content = content; }
    public void updateStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public void updateEndAt(LocalDateTime endAt) { this.endAt = endAt; }

    // 단위테스트용 빌더..
    @Builder(builderClassName = "TestBuilder", builderMethodName = "testBuilder")
    public Notice(Long id, String title, String content, LocalDateTime startAt, LocalDateTime endAt,
                  int viewCount, Member member, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.startAt = startAt;
        this.endAt = endAt;
        this.viewCount = viewCount;
        this.member = member;
        super.setCreatedAt(createdAt);
        super.setUpdatedAt(updatedAt);
    }

    public void setId(long id) { this.id = id; }

    // 첨부파일 추가
    public void addAttachment(Attachment attachment) {
        this.attachments.add(attachment);
        attachment.setNotice(this);
    }
}
