package com.rsupport.board.notice.service;

import com.rsupport.board.common.exception.CustomExceptionHandler;
import com.rsupport.board.common.exception.ErrorCode;
import com.rsupport.board.member.domain.entity.Member;
import com.rsupport.board.notice.domain.entity.Notice;
import com.rsupport.board.notice.domain.entity.Attachment;
import com.rsupport.board.notice.api.dto.NoticeCreateReqDTO;
import com.rsupport.board.notice.api.dto.NoticeResponseDTO;
import com.rsupport.board.member.domain.repository.MemberRepository;
import com.rsupport.board.notice.domain.repository.NoticeRepository;
import com.rsupport.board.notice.infra.FileStorageService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
        NoticeResponseDTO.AuthorDTO author = new NoticeResponseDTO.AuthorDTO(
                notice.getMember().getId(),
                notice.getMember().getName(),
                notice.getMember().getEmail()
        );

        List<NoticeResponseDTO.AttachmentDTO> attachments = notice.getAttachments().stream()
                .map(attachment -> new NoticeResponseDTO.AttachmentDTO(
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
}
