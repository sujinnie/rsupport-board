import axios from 'axios';

/**
 * 테스트 용 userId = 1 로 고정되어있습니다.
 */


// 1) 전체 공지 목록 조회
export const fetchNoticeList = async (
    searchConditions = { keyword: '', titleOnly: false, fromDate: null, toDate: null },
    pageOptions = {page: 0, size: 20},
    sortOption = 'createdAt,desc', // 디폴트: 등록일시 내림차순
) => {
    const response = await axios.get('/v1/notices', {
        params: {
            keyword: searchConditions.keyword,
            titleOnly: searchConditions.titleOnly,
            fromDate: searchConditions.fromDate,
            toDate: searchConditions.toDate,
            page: pageOptions.page,
            size: pageOptions.size,
            sort: sortOption, // 예: 'createdAt,desc' 또는 'viewCount,desc'
            // userId: 1,
        },
    });
    return response.data.data;
};

// 2) 공지 상세 조회
export const fetchNoticeById = async (noticeId) => {
    const response = await axios.get(`/v1/notices/${noticeId}`,{
        params: {'userId': 1}
    });
    return response.data.data;
};

// 3) 공지 등록
export const createNotice = async (formData) => {
    const response = await axios.post('/v1/notices', formData, {
        headers: {'Content-Type': 'multipart/form-data'}
    });
    return response.data.data;
};

// 4) 공지 수정
export const updateNotice = async (noticeId, formData) => {
    const response = await axios.patch(`/v1/notices/${noticeId}`, formData, {
        headers: {'Content-Type': 'multipart/form-data'},
        params: {'userId': 1}
    });
    return response.data.data;
};

// 5) 공지 삭제
export const deleteNotice = async (noticeId) => {
    const response = await axios.delete(`/v1/notices/${noticeId}`, {
        params: {'userId': 1}
    });
    return response.data;  // 204 No Content
};
