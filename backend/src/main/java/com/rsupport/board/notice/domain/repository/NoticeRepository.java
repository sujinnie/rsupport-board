package com.rsupport.board.notice.domain.repository;

import com.rsupport.board.notice.domain.entity.Notice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 공지 CRUD 및 검색
 */
public interface NoticeRepository extends JpaRepository<Notice, Long>, NoticeRepositoryCustom {
    // 조회 테스트 용
    List<Notice> findAllByOrderByCreatedAtDesc();
}
