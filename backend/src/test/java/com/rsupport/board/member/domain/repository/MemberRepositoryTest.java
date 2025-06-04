package com.rsupport.board.member.domain.repository;

import com.rsupport.board.common.config.JpaAuditingConfig;
import com.rsupport.board.member.domain.entity.Member;

import com.rsupport.board.member.domain.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test") // application-test.properties 사용
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepo;

    @Test
    @DisplayName("회원 저장 후 이메일로 조회")
    void saveAndFindByEmail() {
        // given
        Member m = Member.builder()
                .name("테스트")
                .email("test@example.com")
                .password("pwd123")
                .build();
        memberRepo.save(m);

        // when
        Optional<Member> found = memberRepo.findByEmail("test@example.com");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("테스트");
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("존재하지 않는 이메일 조회 시 Optional.empty 반환")
    void findByEmailNotFound() {
        // when
        Optional<Member> found = memberRepo.findByEmail("noone@example.com");

        // then
        assertThat(found).isNotPresent();
    }
}
