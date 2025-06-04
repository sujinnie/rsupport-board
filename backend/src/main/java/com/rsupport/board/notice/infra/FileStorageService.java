package com.rsupport.board.notice.infra;

import com.rsupport.board.notice.domain.entity.Attachment;
import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 저장
 * : 스토리지(현재는 로컬)에 저장 -> Attachment 엔티티 생성 후 반환
 * todo: s3?
 */
public interface FileStorageService {
    Attachment store(MultipartFile file);
}
