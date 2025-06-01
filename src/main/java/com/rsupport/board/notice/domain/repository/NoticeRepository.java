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

    // 조회수만 증가(updatedAt 갱신 x)
    // todo: 조회수가 커지면 ...캐싱..?
    @Modifying(clearAutomatically = true) // 영속성 초기화
    @Query("UPDATE Notice n SET n.viewCount = n.viewCount + 1 WHERE n.id = :id")
    int incrementViewCountOnly(@Param("id") Long id);

}
