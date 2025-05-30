package com.rsupport.board.notice.repository;

import com.rsupport.board.notice.domain.Notice;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 공지 CRUD 및 검색
 * todo: 페이징·정렬
 */
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    // List<Notice> findByMemberId(Long memberId);
}
