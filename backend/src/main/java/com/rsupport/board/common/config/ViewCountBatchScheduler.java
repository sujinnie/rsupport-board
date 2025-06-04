package com.rsupport.board.common.config;

import com.rsupport.board.notice.domain.repository.NoticeRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Redis에 캐싱된 조회수 카운트를 모아서 주기적으로 DB 반영하는 스케줄러
 */
@Component
@RequiredArgsConstructor
public class ViewCountBatchScheduler {

    private final RedisTemplate<String, Long> redisTemplate;
    private final NoticeRepository noticeRepository;

    /**
     * 1분마다 실행
     * - redis에 남아있는 모든 notice:view:{noticeId} 키 조회
     * - 조회수의 delta값 가져오고나서
     * - redis 키 삭제 or 초기화
     * - DB에 배치 UPDATE로 한번에 반영
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void flushViewCountsToDB() {
        // 1. 남아 있는 키들 전체 조회
        Set<String> keys = redisTemplate.keys("notice:view:*");
        if (keys == null || keys.isEmpty()) {
            return;
        }

        // 2. 각 키 별로 저장된 delta 값 가져옴
        Map<Long, Long> viewIncrementCnt = new HashMap<>();
        for (String key : keys) {
            Long delta = redisTemplate.opsForValue().get(key);
            if (delta != null && delta > 0) {
                Long noticeId = Long.parseLong(key.substring("notice:view:".length()));
                viewIncrementCnt.put(noticeId, delta);
            }
        }

        if (viewIncrementCnt.isEmpty()) {
            return;
        }

        // 3. 해당 키들 삭제
        redisTemplate.delete(keys);

        // 4. DB에 배치 UPDATE로 반영
        noticeRepository.batchIncreaseViewCount(viewIncrementCnt);
    }
}
