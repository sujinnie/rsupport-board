package com.rsupport.board.member.repository;

import com.rsupport.board.member.domain.Member;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * 회원 조회
 * todo: 회원 관리
 */
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
}
