package com.rsupport.board.notice.infra;

import com.rsupport.board.notice.domain.entity.Attachment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 로컬에 파일 저장
 * application.properties에서 지정한 경로에 업로드
 * URL은 (/files/{yyyyMMdd}/{uuid} + 확장자) 형태로 반환
 * todo: s3 ??
 */
@Service
public class LocalFileStorageImpl implements FileStorageService {
    @Value("${upload.path}") // properties 에 설정한 로컬 폴더 경로
    private String uploadRootPath;

    // 하위 디렉토리 날짜 포맷팅
    private static final DateTimeFormatter DATE_FOLDER_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public Attachment store(MultipartFile file) {
        String originFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String ext = "";
        int idx = originFilename.lastIndexOf('.');

        if (idx > 0) {
            ext = originFilename.substring(idx);
        }

        String dateFolder = LocalDateTime.now().format(DATE_FOLDER_FORMAT);
        String uuid = UUID.randomUUID().toString();
        String storedFilename = uuid + ext;

        // 최종 저장 경로(ex. uploadRootPath/20250531/uuid.png)
        Path targetDir = Paths.get(uploadRootPath, dateFolder);
        try {
            // 하위 디렉토리가 없으면 생성
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            // 실제 파일 복사
            Path destination = targetDir.resolve(storedFilename);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            // 웹에서 접근 가능한 URL 생성(ex. /files/20250531/uuid.png)
            String fileUrl = "/files/" + dateFolder + "/" + storedFilename;

            // Attachment 엔티티 생성(uploadedAt은 BaseTimeEntity에서 자동 설정)
            return Attachment.builder()
                    .filename(originFilename)
                    .url(fileUrl)
                    .build();
        }
        catch (IOException e) {
            throw new RuntimeException("파일 저장에 실패했습니다: " + originFilename, e);
        }
    }
}
