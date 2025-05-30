package com.rsupport.board.notice.repository;

import com.rsupport.board.common.config.JpaAuditingConfig;
import com.rsupport.board.member.domain.Member;
import com.rsupport.board.member.repository.MemberRepository;
import com.rsupport.board.notice.domain.Attachment;
import com.rsupport.board.notice.domain.Notice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class AttachmentRepositoryTest {

    @Autowired
    private MemberRepository memberRepo;

    @Autowired
    private NoticeRepository noticeRepo;

    @Autowired
    private AttachmentRepository attachmentRepo;

    @Test
    @DisplayName("Attachment 업로드 및 조회 시 Notice 매핑")
    void saveAndFindAttachment() {
        // 1) Member 저장
        Member m = Member.builder()
                .name("테스트")
                .email("test@example.com")
                .password("pwd123")
                .build();
        memberRepo.save(m);

        // 2) Notice 저장
        Notice n = Notice.builder()
                .member(m)
                .title("제목제목")
                .content("내용")
                .startAt(LocalDateTime.now())
                .endAt(LocalDateTime.now().plusDays(1))
                .build();
        noticeRepo.save(n);

        // 3) Attachment 저장
        Attachment att = Attachment.builder()
                .filename("test.txt")
                .url("/upload/test.txt")
                .build();

        // 3-2) Notice와 매핑
        n.addAttachment(att);
//        noticeRepo.save(n);
        Notice savedNotice = noticeRepo.saveAndFlush(n);
        Attachment savedAtt = savedNotice.getAttachments().get(0);

        // 4) Attachment 조회 + 검증
        Attachment found = attachmentRepo.findById(savedAtt.getId()).orElseThrow();
        assertThat(found.getFilename()).isEqualTo("test.txt");
        assertThat(found.getNotice().getId()).isEqualTo(n.getId());
        assertThat(found.getUploadedAt()).isNotNull();
    }
}
