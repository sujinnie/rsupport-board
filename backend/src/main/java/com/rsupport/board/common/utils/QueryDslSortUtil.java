package com.rsupport.board.common.utils;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.PathBuilder;
import org.springframework.data.domain.Sort;

/**
 * Pageable 에 넘어온 Sort 객체를 쿼리dsl 에서 사용할 수 있게 변환
 */
public class QueryDslSortUtil {
    public static <T> OrderSpecifier<?>[] getOrderConds(Sort sort, Class<T> entityClass) {
        // PathBuilder는 Querydsl Q타입 없이 문자열 컬럼명을 통해 동적 정렬을 가능하게 해 줍니다.
        PathBuilder<T> entityPath = new PathBuilder<>(entityClass, entityClass.getSimpleName().toLowerCase());

        return sort.stream()
                .map(order -> {
                    String property = order.getProperty();
                    Order direction = order.isAscending() ? Order.ASC : Order.DESC;
                    return new OrderSpecifier<>(direction, entityPath.get(property, Comparable.class));
                })
                .toArray(OrderSpecifier<?>[]::new);
    }
}
