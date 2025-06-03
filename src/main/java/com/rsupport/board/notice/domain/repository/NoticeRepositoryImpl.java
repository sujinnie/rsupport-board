package com.rsupport.board.notice.domain.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.rsupport.board.common.utils.QueryDslSortUtil;
import com.rsupport.board.member.domain.entity.Member;
import com.rsupport.board.member.domain.entity.QMember;
import com.rsupport.board.notice.api.dto.NoticeListItemDTO;
import com.rsupport.board.notice.api.dto.NoticeListReqDTO;
import com.rsupport.board.notice.api.dto.QNoticeListItemDTO;
import com.rsupport.board.notice.domain.entity.Notice;
import com.rsupport.board.notice.domain.entity.QAttachment;
import com.rsupport.board.notice.domain.entity.QNotice;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class NoticeRepositoryImpl implements NoticeRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    private final EntityManager em;

    public NoticeRepositoryImpl(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }

    private final QNotice notice = QNotice.notice;
    private final QAttachment attachment = QAttachment.attachment;
    private final QMember member = QMember.member;

    /**
     * 공지 목록 조회 쿼리 (검색+페이지네이션)
     * - 제목+내용(titleOnly=false)인 경우에는
     * - 제목에서 키워드 먼저 찾고
     * - 제목에서 찾아진 결과들을 제외한 범위에서 내용 검색 하도록 분기
     */
    @Override
    public Page<NoticeListItemDTO> findAllBySearchCondition(NoticeListReqDTO req, Pageable pageable) {
        String keyword = (req.getKeyword() != null) ? req.getKeyword().trim() : null;
        Boolean titleOnly = req.getTitleOnly();

        // 1. 키워드가 없거나 titleOnly=true 인경우
        if(keyword == null || keyword.isEmpty() || Boolean.TRUE.equals(titleOnly)) {
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
                    .orderBy(buildOrderCondition(pageable))
                    .fetch();

            Long _total = queryFactory
                    .select(notice.count())
                    .from(notice)
                    .where(searchConds)
                    .fetchOne();

            long total = (_total != null) ? _total : 0L;

            return new PageImpl<>(noticeList, pageable, total);
        }

        // 2. 재목+내용(titleOnly = false) 인 경우 (제목 먼저 조회하고 나머지에서 내용 조회)
        // 2-1. 제목에 키워드 포함 + 범위조회
        BooleanExpression rangeCondition = buildRangeCondition(req);
        BooleanExpression titleCondition = notice.title.containsIgnoreCase(keyword);
        if (rangeCondition != null) {
            titleCondition = titleCondition.and(rangeCondition);
        }

        List<NoticeListItemDTO> titleMatchedList = queryFactory
                .select(new QNoticeListItemDTO(
                        notice.id,
                        notice.title,
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
                .where(titleCondition)
                .orderBy(buildOrderCondition(pageable))
                .fetch();

        long titleCount = titleMatchedList.size();

        // 2-2. 제목에 있던 공지 제외 + 내용에 키워드 포함 + 범위조건
        BooleanExpression contentOnlyCond = notice.content.containsIgnoreCase(keyword)
                .and(notice.title.containsIgnoreCase(keyword).not());
        if (rangeCondition != null) {
            contentOnlyCond = contentOnlyCond.and(rangeCondition);
        }

        List<NoticeListItemDTO> contentMatchedList = queryFactory
                .select(new QNoticeListItemDTO(
                        notice.id,
                        notice.title,
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
                .where(contentOnlyCond)
                .orderBy(buildOrderCondition(pageable))
                .fetch();

        long contentCount = contentMatchedList.size();

        // 2-3) 두 리스트(titleMatchedList, contentMatchedList) 합치기
        List<NoticeListItemDTO> combined = new ArrayList<>(titleMatchedList.size() + contentMatchedList.size());
        combined.addAll(titleMatchedList);
        combined.addAll(contentMatchedList);

        // 2-4) 자바 레벨에서 “createdAt DESC” 다시 정렬
        combined.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        // 2-5) page, size에 맞춰 자바 레벨 페이지네이션 (서브리스트)
        int offset = (int) pageable.getOffset();
        int pageSize = pageable.getPageSize();
        int endIndex = Math.min(offset + pageSize, combined.size());

        List<NoticeListItemDTO> pageContent;
        if (offset >= combined.size()) {
            pageContent = List.of();
        } else {
            pageContent = combined.subList(offset, endIndex);
        }

        // 2-6) 전체 건수 = titleCount + contentCount
        long total = titleCount + contentCount;

        return new PageImpl<>(pageContent, pageable, total);
    }

    /**
     * 공지목록 조회 시 요청된 검색범위 조건절 만들기
     */
    private BooleanExpression buildRangeCondition(NoticeListReqDTO req) {
        BooleanExpression result = null;

        // 등록일 검색 (fromDate ~ toDate)
        if (req.getFromDate() != null) {
            LocalDateTime fromDateTime = req.getFromDate().atStartOfDay(); // 00:00
            BooleanExpression fromDate = notice.startAt.goe(fromDateTime);
            result = (result == null ? fromDate : result.and(fromDate));
        }
        if (req.getToDate() != null) {
            LocalDateTime toDateTime = req.getToDate().atTime(LocalTime.MAX); // 23:59
            BooleanExpression toDate = notice.endAt.loe(toDateTime);
            result = (result == null ? toDate : result.and(toDate));
        }

        return result;
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
            BooleanExpression fromDate = notice.startAt.goe(fromDateTime);
            result = (result == null ? fromDate : result.and(fromDate));
        }
        if (req.getToDate() != null) {
            LocalDateTime toDateTime = req.getToDate().atTime(LocalTime.MAX); // 23:59
            BooleanExpression toDate = notice.endAt.loe(toDateTime);
            result = (result == null ? toDate : result.and(toDate));
        }

        return result;
    }

    /**
     * 공지 목록 조회 시 정렬 조건 만들기
     */
    private OrderSpecifier<?>[] buildOrderCondition(Pageable pageable) {
        // Sort 가 없을 때는 createAt, DESC 를 default 로 반환
        if (pageable.getSort() == null || pageable.getSort().isEmpty()) {
            return new OrderSpecifier<?>[]{ notice.createdAt.desc() };
        }

        return QueryDslSortUtil.getOrderConds(pageable.getSort(), Notice.class);
    }

    /**
     * 공지 상세 조회 쿼리 (멤버, 첨부파일 패치조인해서 가져오기)
     */
    @Override
    public Optional<Notice> findWithMemberAndAttachmentsById(Long noticeId) {
        Notice fetchedNotice = queryFactory.selectFrom(notice)
                .join(notice.member, member).fetchJoin()
                .leftJoin(notice.attachments, attachment).fetchJoin()
                .where(notice.id.eq(noticeId))
                .fetchOne();

        return Optional.ofNullable(fetchedNotice);
    }

    /**
     * noticeId → delta 맵을 돌며, 각 공지의 view_count를 누적 반영
     */
    @Override
    public void batchIncreaseViewCount(Map<Long, Long> viewIncrementCnt) {
        StringBuilder sb = new StringBuilder("UPDATE notice SET view_count = CASE ");
        viewIncrementCnt.forEach((id, delta) -> {
            sb.append("WHEN id = ").append(id)
              .append(" THEN view_count + ").append(delta).append(" ");
        });
        sb.append("ELSE view_count END WHERE id IN (");

        // where id in (~~): id 리스트 만들기
        sb.append(String.join(",", viewIncrementCnt.keySet().stream()
            .map(String::valueOf)
            .toList()));
        sb.append(")");

        Query caseQuery = em.createNativeQuery(sb.toString());
        caseQuery.executeUpdate();
    }
}
