package com.rsupport.board.notice.service;

import com.rsupport.board.common.exception.CustomExceptionHandler;
import com.rsupport.board.common.exception.ErrorCode;
import com.rsupport.board.member.domain.entity.Member;
import com.rsupport.board.notice.api.dto.*;
import com.rsupport.board.notice.domain.entity.Notice;
import com.rsupport.board.notice.domain.entity.Attachment;
import com.rsupport.board.member.domain.repository.MemberRepository;
import com.rsupport.board.notice.domain.repository.NoticeRepository;
import com.rsupport.board.notice.infra.FileStorageService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {
    private final MemberRepository memberRepository;
    private final NoticeRepository noticeRepository;
    private final FileStorageService fileStorageService;

    /**
     * 공지 등록 서비스 (create)
     */
    @Override
    @Transactional
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

        return new NoticeResponseDTO(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getStartAt(),
                notice.getEndAt(),
                notice.getCreatedAt(),
                notice.getUpdatedAt(),
                (long) notice.getViewCount(),
                author,
                attachments
        );
    }

    /**
     * 공지 목록 조회 서비스 (검색+페이지네이션)
     */
    @Override
    @Transactional
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
        // 작성자 예외처리 (유저만 공지 등록 가능)
        Member member = memberRepository.findById(userId)
                .orElseThrow(()-> new CustomExceptionHandler(ErrorCode.MEMBER_NOT_FOUND));

        // 공지 id 확인
        Notice notice = noticeRepository.findWithMemberAndAttachmentsById(noticeId)
                .orElseThrow(()-> new CustomExceptionHandler(ErrorCode.NOTICE_NOT_FOUND));

        // 조회수 증가
        noticeRepository.incrementViewCountOnly(noticeId);
        Notice refreshed = noticeRepository.findWithMemberAndAttachmentsById(noticeId)
                .orElseThrow(()-> new CustomExceptionHandler(ErrorCode.NOTICE_NOT_FOUND));

        // 응답 DTO로 변환
        return convertToResDTO(refreshed);
    }
}
