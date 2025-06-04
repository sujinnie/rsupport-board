package com.rsupport.board.common.config;

import com.rsupport.board.common.utils.KoreanTitleUtil;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 애플리케이션 실행 시 더미 데이터 생성 (이미 존재하면 해당 작업은 진행x)
 *
 * 더미 데이터 내용
 * - member 테이블: 100,000건
 * - notice 테이블: 500,000건
 * - notice_attachment 테이블: 공지당 0~3개 랜덤 첨부파일 (샘플 파일을 upload.path 아래로 복사..)
 */
@Component
@RequiredArgsConstructor
public class DummyDataLoader implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

    @Value("${upload.path}") // properties 에 설정한 로컬 폴더 경로
    private String uploadPath;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 1) 'ID = 1인 테스트 회원'을 무조건 삽입 (존재하면 건너뜀)
        jdbcTemplate.update(
                "INSERT INTO member (id, name, email, password, created_at, updated_at) " +
                        "SELECT 1, '테스트유저', 'testuser@example.com', 'password123', NOW(), NOW() " +
                        "WHERE NOT EXISTS (SELECT 1 FROM member WHERE id = 1)"
        );

        // 2) member 테이블에 1외의 데이터가 있으면 스킵
        Integer memberCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM member", Integer.class);
        if (memberCount != null && memberCount > 1) {
            System.out.println(
                    ">>> [DummyDataLoader] 기존에 ID=1외 데이터가 있으므로 더미 로딩 스킵 (memberCount=" + memberCount + ")"
            );
            return;
        }

        // 3) 샘플 파일 가져오기
        Resource[] sampleResources = resourceResolver.getResources("classpath:/dummy-files/*");
        if (sampleResources == null || sampleResources.length == 0) {
            System.out.println(">>> [DummyDataLoader] resources/dummy-files 안에 샘플 파일이 없습니다. 첨부파일 로딩은 건너뜁니다.");
        }
        else {
            System.out.println(">>> [DummyDataLoader] 샘플 파일 " + sampleResources.length + "개 로드 성공!");
        }

        // 4) 유저 더미 insert: 100,000건
        insertDummyMembers(100_000);
        // 제대로 insert 됐는지 확인..
        List<Long> existingMemberIds = jdbcTemplate.query(
                "SELECT id FROM member",
                (rs, rowNum) -> rs.getLong("id")
        );
        if (existingMemberIds.isEmpty()) {
            throw new IllegalStateException("회원 데이터가 적재되지 않았습니다.");
        }

        //5) 공지 더미 insert: 100,000건
        insertDummyNotices(100_000, existingMemberIds);
        // 제대로 insert 됐는지 확인
        List<Long> existingNoticeIds = jdbcTemplate.query(
                "SELECT id FROM notice",
                (rs, rowNum) -> rs.getLong("id")
        );
        if (existingNoticeIds.isEmpty()) {
            throw new IllegalStateException("공지 데이터가 적재되지 않았습니다.");
        }

        // 6) 공지당 첨부파일 0~3개 랜덤 생성해서 메타정보 insert
        if (sampleResources != null && sampleResources.length > 0) {
            insertDummyAttachments(existingNoticeIds, sampleResources);
        }

//        // 추가.. 파일 없는 공지 400,000건
//        insertDummyNotices(400_000, existingMemberIds);

        System.out.println(">>> [DummyDataLoader] 더미 데이터 삽입 완료!");
    }

    // 1) member 테이블에 100,000건 batch insert
    private void insertDummyMembers(int totalUsers) {
        System.out.println(">>> [DummyDataLoader] 회원 데이터 100,000건 생성 시작...");

        final int batchSize = 1_000;
        List<Object[]> batchParams = new ArrayList<>(batchSize);
        Faker faker = new Faker(new Locale("ko"));

        for (int i = 1; i <= totalUsers; i++) {
            // DataFaker로 이름, 비번 랜덤 생성
            String name = faker.name().fullName();
            // 비밀번호: 길이 8~12, 대문자 필수, 특수문자 없이
            String password = faker.internet().password(8, 12, true, false);
            String email = name + "_" + i + "@example.com";

            batchParams.add(new Object[]{
                    name,
                    email,
                    password,
                    Timestamp.valueOf(java.time.LocalDateTime.now()),
                    Timestamp.valueOf(java.time.LocalDateTime.now())
            });

            if (i % batchSize == 0) {
                jdbcTemplate.batchUpdate(
                        "INSERT INTO member (name, email, password, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
                        batchParams
                );
                batchParams.clear();
                System.out.println("  - [DummyDataLoader] 회원 " + i + "명 삽입 완료...");
            }
        }

        // 남은 파라미터 처리
        if (!batchParams.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    "INSERT INTO member (name, email, password, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
                    batchParams
            );
        }
        System.out.println(">>> [DummyDataLoader] 회원 데이터 100,000건 생성 완료!");
    }

    // 2) notice 테이블에 100,000건 batch insert
    private void insertDummyNotices(int totalNotices, List<Long> existingMemberIds) {
        System.out.println(">>> [DummyDataLoader] 공지 100,000건 생성 시작...");

        final int batchSize = 1_000;
        List<Object[]> batchParams = new ArrayList<>(batchSize);
        Random rnd = new Random();
        Faker faker = new Faker(new Locale("ko"));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeMonthsAgo = now.minusMonths(3);

        // 최근 3개월의 초 단위 계산 (createdAt을 랜덤하게 작성하기 위해.. 정렬테스트 필요하니까)
        // note: epochSecond - 1970-01-01 UTC부터 몇 초가 지났는지
        long startEpochSec = threeMonthsAgo.atZone(ZoneId.systemDefault()).toEpochSecond();
        long endEpochSec = now.atZone(ZoneId.systemDefault()).toEpochSecond();

        for (int i = 1; i <= totalNotices; i++) {
            // DataFaker로 제목, 내용 랜덤 생성
            String title = KoreanTitleUtil.randomKoreanTitle();
            String content = faker.lorem().paragraph(3);

            // userId: 생성된 userId 중에서 랜덤
            int idx = rnd.nextInt(existingMemberIds.size());
            long userId = existingMemberIds.get(idx);

            // createdAt, updatedAt: 최근 3개월 내에서 랜덤
            long randomEpoch = startEpochSec + (long) (rnd.nextDouble() * (endEpochSec - startEpochSec));
            LocalDateTime randomCreatedAt = LocalDateTime.ofEpochSecond(randomEpoch, 0, ZoneId.systemDefault().getRules().getOffset(now));
            Timestamp createdAt = Timestamp.valueOf(randomCreatedAt);

            // startAt: createdAt 기준으로 1일 뒤, endAt: startAt 기준으로 1~7일 뒤 랜덤
            LocalDateTime startAt = now.plusDays(1);
            LocalDateTime endAt = now.plusDays(rnd.nextInt(7)+1);

            batchParams.add(new Object[]{
                    title,
                    content,
                    userId,
                    Timestamp.valueOf(startAt),
                    Timestamp.valueOf(endAt),
                    createdAt,
                    createdAt,
                    0L // view_count 초기값: 0
            });

            if (i % batchSize == 0) {
                jdbcTemplate.batchUpdate(
                        "INSERT INTO notice (title, content, user_id, start_at, end_at, created_at, updated_at, view_count) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        batchParams
                );
                batchParams.clear();
                System.out.println("  - [DummyDataLoader] 공지 " + i + "건 삽입 완료...");
            }
        }

        // 남은 파라미터 처리
        if (!batchParams.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    "INSERT INTO notice (title, content, user_id, start_at, end_at, created_at, updated_at, view_count) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    batchParams
            );
        }
        System.out.println(">>> [DummyDataLoader] 공지 100,000건 생성 완료!");
    }

    // 3) attachment 테이블에 공지당 0~3개 랜덤으로 batch insert + 실제파일복사
    private void insertDummyAttachments(List<Long> existingNoticeIds, Resource[] sampleResources) throws IOException {
        System.out.println(">>> [DummyDataLoader] 첨부파일 생성 시작 (공지당 0~3개 랜덤)...");

        final int batchSize = 1_000;
        List<Object[]> batchParams = new ArrayList<>(batchSize);
        Random rnd = new Random();

        // 날짜별 폴더 생성용 포맷 (ex. 2025/06/02)
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        String today = LocalDate.now().format(dtf);
        Path baseUploadDir = Paths.get(uploadPath, today);
        Files.createDirectories(baseUploadDir); // 디렉토리가 없으면 생성

        for (long noticeId : existingNoticeIds) {
            int attCnt = rnd.nextInt(4); // 0~3개 랜덤
            for (int k = 0; k < attCnt; k++) {
                // sampleResources 중 랜덤으로 파일 하나 선택
                Resource sample = sampleResources[rnd.nextInt(sampleResources.length)];
                String originalFilename = sample.getFilename();

                // 확장자만 추출
                String ext = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    ext = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String storedFilename = UUID.randomUUID().toString() + ext; // 실제 저장할 UUID 파일명
                Path destFile = baseUploadDir.resolve(storedFilename); // 저장할 경로

                // 실제 파일 복사(classpath:/dummy-files -> 로컬 디스크 destFile)
                try (InputStream is = sample.getInputStream()) {
                    Files.copy(is, destFile);
                }

                // 파일 메타정보 추출
                long fileSize = Files.size(destFile);
                String contentType = Files.probeContentType(destFile);  // "image/jpeg" 같은..
                String uploadPathUrl = "/uploads/" + today + "/" + storedFilename;

                Timestamp now = Timestamp.valueOf(java.time.LocalDateTime.now());

                batchParams.add(new Object[]{
                        (long) noticeId,
                        originalFilename,
//                        storedFilename,
//                        contentType,
//                        fileSize,
                        uploadPathUrl,
                        now
                });

                if (batchParams.size() >= batchSize) {
                    jdbcTemplate.batchUpdate(
                            "INSERT INTO attachment (notice_id, filename, url, uploaded_at) VALUES (?, ?, ?, ?)",
                            batchParams
                    );
                    batchParams.clear();
                    System.out.println("  - [DummyDataLoader] 첨부파일 ~" + noticeId + "건 처리...");
                }
            }
        }

        // 남아 있는 파라미터 처리
        if (!batchParams.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    "INSERT INTO attachment (notice_id, filename, url, uploaded_at) VALUES (?, ?, ?, ?)",
                    batchParams
            );
        }

        System.out.println(">>> [DummyDataLoader] 첨부파일 생성 완료!");
    }
}
