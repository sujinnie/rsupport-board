package com.rsupport.board.notice.service;

import com.rsupport.board.common.exception.CustomExceptionHandler;
import com.rsupport.board.common.exception.ErrorCode;
import com.rsupport.board.member.domain.entity.Member;
import com.rsupport.board.notice.api.dto.*;
import com.rsupport.board.notice.domain.entity.Notice;
import com.rsupport.board.notice.domain.entity.Attachment;
import com.rsupport.board.member.domain.repository.MemberRepository;
import com.rsupport.board.notice.domain.repository.AttachmentRepository;
import com.rsupport.board.notice.domain.repository.NoticeRepository;
import com.rsupport.board.notice.infra.FileStorageService;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {
    private final MemberRepository memberRepository;
    private final NoticeRepository noticeRepository;
    private final AttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final RedisTemplate<String, Long> redisTemplate;

    /**
     * 공지 등록 서비스 (create)
     */
    @Override
    @Transactional
    @CacheEvict( // 새로운 공지 등록되면 캐싱해놨던 조회목록 초기화 해야함
            value = "latestNotices", // 같은 캐시 이름
            key = "'getNoticeList_0-20'", // 캐시 키 고정
            beforeInvocation = false // 메서드가 성공한 뒤에 캐시 삭제
    )
    public NoticeResponseDTO createNotice(NoticeCreateReqDTO req) {
        // 작성자 예외처리 (유저만 공지 등록 가능)
        Member author = memberRepository.findById(req.getUserId())
                .orElseThrow(()->new CustomExceptionHandler(ErrorCode.MEMBER_NOT_FOUND));

        // 시작일 < 종료일 예외처리
        if(req.getEndAt().isBefore(req.getStartAt()) || req.getEndAt().isEqual(req.getStartAt())) {
            throw new CustomExceptionHandler(ErrorCode.INVALID_DATE_RANGE);
        }

        Notice notice = Notice.builder()
                .member(author)
                .title(req.getTitle())
                .content(req.getContent())
                .startAt(req.getStartAt())
                .endAt(req.getEndAt())
                .build();

        // 첨부파일이 있을 경우
        MultipartFile[] files = req.getFiles();
        if (files != null && files.length > 0) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    Attachment att = fileStorageService.store(file); // 파일저장 + Attachment 엔티티 반환
                    notice.addAttachment(att);
                }
            }
        }

        Notice savedNotice = noticeRepository.save(notice);

        // 응답 DTO로 변환
        return convertToResDTO(savedNotice);
    }

    /**
     * 공지 -> response dto 로 변환
     */
    private NoticeResponseDTO convertToResDTO(Notice notice) {
        AuthorInfoDTO author = new AuthorInfoDTO(
                notice.getMember().getId(),
                notice.getMember().getName()
        );

        List<AttachmentInfoDTO> attachments = notice.getAttachments().stream()
                .map(attachment -> new AttachmentInfoDTO(
                        attachment.getId(),
                        attachment.getFilename(),
                        attachment.getUrl(),
                        attachment.getUploadedAt()
                ))
                .toList();

        // 조회수는 db값 + redis 에 캐싱된 값
         Long redisDelta = Optional.ofNullable(redisTemplate.opsForValue().get("notice:view:" + notice.getId())).orElse(0L);
         long totalViewCount = notice.getViewCount() + redisDelta;

        return new NoticeResponseDTO(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getStartAt(),
                notice.getEndAt(),
                notice.getCreatedAt(),
                notice.getUpdatedAt(),
                totalViewCount,
                author,
                attachments
        );
    }

    /**
     * 공지 목록 조회 서비스 (검색+페이지네이션)
     */
    @Override
    @Transactional
    @Cacheable( // 캐시먼저 확인
            value = "latestNotices",
            key = "#root.methodName + '_' + #pageable.pageNumber + '-' + #pageable.pageSize",
            condition = "#req.keyword == null && #req.fromDate == null && #req.toDate == null && #pageable.pageNumber == 0"
    )
    public NoticeListResDTO getNoticeList(NoticeListReqDTO req, Pageable pageable) {
        Page<NoticeListItemDTO> searchedNoticeList = noticeRepository.findAllBySearchCondition(req, pageable);
        return convertToNoticeListDTO(searchedNoticeList);
    }

    /**
     * Page<NoticeListItemDTO> -> NoticeListResDTO로 변환
     */
    public static NoticeListResDTO convertToNoticeListDTO(Page<NoticeListItemDTO> page) {
        NoticeListResDTO.PageInfo pageInfo = new NoticeListResDTO.PageInfo(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
        return new NoticeListResDTO(page.getContent(), pageInfo);
    }


    /**
     * 공지 상세 조회 서비스 (read)
     */
    @Override
    @Transactional
    public NoticeResponseDTO getNotice(Long userId, Long noticeId) {
        // 작성자 예외처리 (유저만 공지 조회 가능)
        Member member = memberRepository.findById(userId)
                .orElseThrow(()-> new CustomExceptionHandler(ErrorCode.MEMBER_NOT_FOUND));

        // 공지 id 확인
        Notice notice = noticeRepository.findWithMemberAndAttachmentsById(noticeId)
                .orElseThrow(()-> new CustomExceptionHandler(ErrorCode.NOTICE_NOT_FOUND));

        // 조회수 증가: redis INCR로 변경!!
        String redisKey = "notice:view:" + noticeId;
        //  redis에 키가 없으면 0에서 시작, 있으면 1씩 증가
        redisTemplate.opsForValue().increment(redisKey, 1L);
        
        // 응답 DTO로 변환
        return convertToResDTO(notice);
    }

    /**
     * 공지 수정 서비스 (update)
     */
    @Override
    @Transactional
    @CacheEvict( // 공지가 수정되면 캐싱해놨던 조회목록 초기화 해야함
            value = "latestNotices", // 같은 캐시 이름
            key = "'getNoticeList_0-20'", // 캐시 키 고정
            beforeInvocation = false // 메서드가 성공한 뒤에 캐시 삭제
    )
    public NoticeResponseDTO updateNotice(Long userId, Long noticeId, NoticeUpdateReqDTO req) {
        // 작성자 예외처리1 (회원여부)
        Member member = memberRepository.findById(userId)
                .orElseThrow(()-> new CustomExceptionHandler(ErrorCode.MEMBER_NOT_FOUND));

        // 수정할 공지 조회
        Notice notice = noticeRepository.findWithMemberAndAttachmentsById(noticeId)
                .orElseThrow(()-> new CustomExceptionHandler(ErrorCode.NOTICE_NOT_FOUND));

        // 작성자 예외처리2 (작성자만 공지 수정 가능)
        if(!req.getUserId().equals(notice.getMember().getId())) {
            throw new CustomExceptionHandler(ErrorCode.ACCESS_DENIED);
        }

        // 시작, 종료 일시 예외처리 (시작일 > 종료일 이면 에러)
        LocalDateTime finalStart = (req.getStartAt() != null) ? req.getStartAt() : notice.getStartAt();
        LocalDateTime finalEnd   = (req.getEndAt() != null) ? req.getEndAt() : notice.getEndAt();
        if (finalStart.isAfter(finalEnd)) {
            throw new CustomExceptionHandler(ErrorCode.INVALID_DATE_RANGE);
        }

        // 변경된 필드가 있을 때만 업데이트 !
        if (req.getTitle() != null && !req.getTitle().trim().isEmpty()) {
            notice.updateTitle(req.getTitle());
        }
        if (req.getContent() != null && !req.getContent().trim().isEmpty()) {
            notice.updateContent(req.getContent());
        }
        if (req.getStartAt() != null) {
            notice.updateStartAt(req.getStartAt());
        }
        if (req.getEndAt() != null) {
            notice.updateEndAt(req.getEndAt());
        }

        // 삭제 요청한 첨부파일 처리 (원래 공지에 없었으면 에러, 있으면 삭제)
        List<Long> removeIds = req.getRemoveAttachmentIds();
        if (removeIds != null && !removeIds.isEmpty()) {
            // 공지가 갖고 있는 파일 id list
            Set<Long> existingAttachmentIds = notice.getAttachments()
                    .stream()
                    .map(Attachment::getId)
                    .collect(Collectors.toSet());

            // 삭제하려는 파일이 공지에 애초에 없었으면 에러
            for (Long removeId : removeIds) {
                if (!existingAttachmentIds.contains(removeId)) {
                    throw new CustomExceptionHandler(ErrorCode.ATTACHMENT_NOT_FOUND);
                }
            }
        }

        // 삭제 요청한 첨부파일이 있으면 삭제
        if (!CollectionUtils.isEmpty(removeIds)) {
            attachmentRepository.deleteAllByIdIn(removeIds); // db 에서 제거
            notice.getAttachments().removeIf(att -> removeIds.contains(att.getId()));
        }

        // 새로 추가된 파일이 있으면 추가해서 저장
        MultipartFile[] newFiles = req.getNewFiles();
        if (newFiles != null && newFiles.length > 0) {
            for (MultipartFile file : newFiles) {
                if (!file.isEmpty()) {
                    Attachment savedAtt = fileStorageService.store(file);
                    notice.addAttachment(savedAtt);
                }
            }
        }

        // 변경된 notice를 저장
        Notice updatedNotice = noticeRepository.save(notice);

        // 응답 DTO로 변환
        return convertToResDTO(updatedNotice);
    }

    /**
     * 공지 삭제 조회 서비스 (delete)
     */
    @CacheEvict( // 공지가 삭제되면 캐싱해놨던 조회목록 초기화 해야함
            value = "latestNotices", // 같은 캐시 이름
            key = "'getNoticeList_0-20'", // 캐시 키 고정
            beforeInvocation = false // 메서드가 성공한 뒤에 캐시 삭제
    )
    public void deleteNotice(Long userId, Long noticeId) {
        // 작성자 예외처리1 (회원여부)
        Member member = memberRepository.findById(userId)
                .orElseThrow(()-> new CustomExceptionHandler(ErrorCode.MEMBER_NOT_FOUND));

        // 공지 조회 예외처리
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(()-> new CustomExceptionHandler(ErrorCode.NOTICE_NOT_FOUND));

        // 작성자 예외처리2 (작성자만 삭제 가능)
        if (!userId.equals(notice.getMember().getId())) {
            throw new CustomExceptionHandler(ErrorCode.ACCESS_DENIED);
        }

        // 삭제
//        attachmentRepository.deleteAllByNoticeId(noticeId);
        noticeRepository.delete(notice);
    }
}
