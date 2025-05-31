package com.rsupport.board.notice.domain.repository;

import com.rsupport.board.notice.domain.entity.Attachment;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 첨부파일 CRUD
 */
public interface AttachmentRepository extends JpaRepository <Attachment, Long> {
}
