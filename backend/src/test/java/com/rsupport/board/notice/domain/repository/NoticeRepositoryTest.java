package com.rsupport.board.notice.domain.repository;

import com.rsupport.board.common.config.JpaAuditingConfig;
import com.rsupport.board.member.domain.entity.Member;
import com.rsupport.board.member.domain.repository.MemberRepository;
import com.rsupport.board.notice.domain.entity.Notice;

import com.rsupport.board.notice.domain.repository.NoticeRepository;
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
@ActiveProfiles("test") // application-test.properties 사용
class NoticeRepositoryTest {
    @Autowired
    NoticeRepository noticeRepo;
    @Autowired MemberRepository memberRepo;

    @Test
    @DisplayName("공지 등록 후 조회 -> 제목, 작성자 체크")
    void saveAndFind() {
        Member m = Member.builder()
                .name("테스트")
                .email("test@example.com")
                .password("pw")
                .build();
        memberRepo.save(m);

        Notice n = Notice.builder()
                .member(m)
                .title("제목")
                .content("내용")
                .startAt(LocalDateTime.now())
                .endAt(LocalDateTime.now().plusDays(1))
                .build();
        noticeRepo.save(n);

        Notice found = noticeRepo.findById(n.getId()).orElseThrow();
        assertThat(found.getTitle()).isEqualTo("제목");
        assertThat(found.getMember().getEmail()).isEqualTo("test@example.com");
    }
}
