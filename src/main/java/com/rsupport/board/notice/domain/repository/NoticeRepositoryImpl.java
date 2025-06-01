package com.rsupport.board.notice.domain.repository;

import com.rsupport.board.notice.api.dto.NoticeListItemDTO;
import com.rsupport.board.notice.api.dto.NoticeListReqDTO;
import com.rsupport.board.notice.api.dto.QNoticeListItemDTO;
import com.rsupport.board.notice.domain.entity.QAttachment;
import com.rsupport.board.notice.domain.entity.QNotice;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Repository
public class NoticeRepositoryImpl implements NoticeRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    @Autowired
    public NoticeRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    private final QNotice notice = QNotice.notice;
    private final QAttachment attachment = QAttachment.attachment;

    /**
     * 공지 목록 조회 쿼리 (검색+페이지네이션)
     */
    @Override
    public Page<NoticeListItemDTO> findAllBySearchCondition(NoticeListReqDTO req, Pageable pageable) {
        // where 절에 사용될 조건
        BooleanExpression searchConds = buildSearchCondition(req);

        // 조회 쿼리
        List<NoticeListItemDTO> noticeList = queryFactory
                .select(new QNoticeListItemDTO(
                        notice.id,
                        notice.title,
                        // hasAttachment : attachment크기체크
                        Expressions.cases()
                                .when(attachment.id.isNotNull())
                                .then(true)
                                .otherwise(false),
                        notice.createdAt,
                        notice.startAt,
                        notice.endAt,
                        notice.viewCount,
                        notice.member.id,
                        notice.member.name
                ))
                .distinct()
                .from(notice)
                .leftJoin(notice.attachments, attachment)
                .where(searchConds)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(notice.createdAt.desc())
                .fetch();

        Long _total = queryFactory
                .select(notice.count())
                .from(notice)
                .where(searchConds)
                .fetchOne();

        long total = (_total != null) ? _total : 0L;

        return new PageImpl<>(noticeList, pageable, total);
    }

    /**
     * 공지목록 조회 시 요청된 조건절 만들기
     */
    private BooleanExpression buildSearchCondition(NoticeListReqDTO req) {
        BooleanExpression result = null;

        // 키워드 처리
        if (req.getKeyword() != null && !req.getKeyword().trim().isEmpty()) {
            String keyword = req.getKeyword().trim();

            // titleOnly == true -> 제목만 검색 (대소문자 무시)
            if (Boolean.TRUE.equals(req.getTitleOnly())) {
                result = notice.title.containsIgnoreCase(keyword);
            }
            else { // false , null -> 제목+내용 검색 (대소문자 무시)
                result = notice.title.containsIgnoreCase(keyword)
                        .or(notice.content.containsIgnoreCase(keyword));
            }
        }

        // 등록일 검색 (fromDate ~ toDate)
        if (req.getFromDate() != null) {
            LocalDateTime fromDateTime = req.getFromDate().atStartOfDay(); // 00:00
            BooleanExpression fromDate = notice.createdAt.goe(fromDateTime);
            result = (result == null ? fromDate : result.and(fromDate));
        }
        if (req.getToDate() != null) {
            LocalDateTime toDateTime = req.getToDate().atTime(LocalTime.MAX); // 23:59
            BooleanExpression toDate = notice.createdAt.loe(toDateTime);
            result = (result == null ? toDate : result.and(toDate));
        }

        return result;
    }
}
