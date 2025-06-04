import React, {useCallback, useEffect, useState, useRef} from 'react';
import {fetchNoticeList} from '../api/NoticeApi';
import NoticeList from '../components/NoticeList';
import {
    Box, Button, FormControl, FormControlLabel, InputLabel, MenuItem, Paper, Select, Switch,
    Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TableSortLabel, TextField, Typography
} from "@mui/material";
import {useNavigate} from "react-router-dom";

function NoticeListPage() {
    const navigate = useNavigate();

    // 검색 필터
    const [keyword, setKeyword] = useState('');
    const [titleOnly, setTitleOnly] = useState(false);
    const [fromDate, setFromDate] = useState(''); // YYYY-MM-DD
    const [toDate, setToDate] = useState('');     // YYYY-MM-DD

    // 정렬
    const [sortOption, setSortOption] = useState('createdAt,desc');

    // 페이징
    const [page, setPage] = useState(0);
    const [size] = useState(20);

    // 검색 중인지
    const [isComposing, setIsComposing] = useState(false);

    const keywordInputRef = useRef(null);

    // 반환 결과
    const [noticeList, setNoticeList] = useState([]);
    const [pageInfo, setPageInfo] = useState(null);
    const [loading, setLoading] = useState(true);

    const getNoticeList = useCallback(async (searchKeyword, filterOptions, pageOptions, sortOpt) => {
        setLoading(true);
        try {
            // fetchNoticeList(searchConditions, pageOptions, sortOption) 형태라고 가정
            const res = await fetchNoticeList(searchKeyword ? {
                    keyword: searchKeyword,
                    titleOnly: filterOptions.titleOnly,
                    fromDate: filterOptions.fromDate,
                    toDate: filterOptions.toDate
                } : {
                    keyword: "",
                    titleOnly: filterOptions.titleOnly,
                    fromDate: filterOptions.fromDate,
                    toDate: filterOptions.toDate
                },
                pageOptions,
                sortOpt);

            // 백엔드 응답 구조:
            //  { status: "...", data: { noticeList: [...], pageInfo: { pageNumber, totalPages, first, last, ... } } }
            setNoticeList(res.noticeList || []);
            setPageInfo(res.pageInfo || null);
        } catch (error) {
            console.error('공지 목록 조회 실패', error);
            setNoticeList([]);
            setPageInfo(null);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        // 페이지, 정렬, 제목 필터, 날짜가 바뀔 때만 자동으로 목록 재조회
        getNoticeList(keyword, { titleOnly, fromDate: fromDate || null, toDate: toDate || null }, { page, size }, sortOption);
    }, [page, titleOnly, fromDate, toDate, sortOption, size, getNoticeList]);

    const handleCompositionStart = () => {
        setIsComposing(true);
    };

    const handleCompositionEnd = (e) => {
        setIsComposing(false);
    };

    const handleKeywordChange = (e) => {
        setKeyword(e.target.value);
        if (!isComposing) {
        }
    };

    const handleSearch = (e) => {
        e.preventDefault();
        if (isComposing) { // 한글자입력마다 포커스아웃 방지..
            return;
        }
        // 검색 버튼 누를 때마다 페이지 초기화
        setPage(0);
        getNoticeList(keyword, { titleOnly, fromDate: fromDate || null, toDate: toDate || null }, { page: 0, size }, sortOption);
    };

    const handleSortChange = (e) => {
        setSortOption(e.target.value);
        setPage(0);
    };

    const handleRequestSort = (event, property) => {
        // property: 'createdAt' 또는 'viewCount'
        const isAsc = sortOption === `${property},asc`;
        const newDir = isAsc ? 'desc' : 'asc';
        setSortOption(`${property},${newDir}`);
        setPage(0);
    };

    if (loading) {
        return <Box sx={{ p: 3 }}>로딩 중 . . . . .</Box>;
    }

    return (
        <Box sx={{ p: 3 }}>
            <Typography variant="h4" gutterBottom>
                공지사항 목록 (현재 userId = 1)
            </Typography>

            <Box
                component="form"
                onSubmit={handleSearch}
                sx={{
                    display: 'flex',
                    gap: 2,
                    alignItems: 'center',
                    flexWrap: 'wrap',
                    mb: 2,
                }}
            >
                <TextField
                    inputRef={keywordInputRef}
                    label="검색어"
                    size="small"
                    value={keyword}
                    onChange={handleKeywordChange}
                    onCompositionStart={handleCompositionStart}
                    onCompositionEnd={handleCompositionEnd}
                />

                <FormControlLabel
                    control={
                        <Switch
                            checked={titleOnly}
                            onChange={(e) => setTitleOnly(e.target.checked)}
                        />
                    }
                    label="제목만"
                />

                <TextField
                    label="시작일"
                    type="date"
                    size="small"
                    InputLabelProps={{ shrink: true }}
                    value={fromDate}
                    onChange={(e) => setFromDate(e.target.value)}
                />

                <TextField
                    label="종료일"
                    type="date"
                    size="small"
                    InputLabelProps={{ shrink: true }}
                    value={toDate}
                    onChange={(e) => setToDate(e.target.value)}
                />

                <Button variant="contained" color="primary" type="submit">
                    검색
                </Button>

                <FormControl size="small" sx={{ minWidth: 160 }}>
                    <InputLabel id="sort-select-label">정렬 기준</InputLabel>
                    <Select
                        labelId="sort-select-label"
                        label="정렬 기준"
                        value={sortOption}
                        onChange={handleSortChange}
                    >
                        <MenuItem value="createdAt,desc">등록일시 (최신순)</MenuItem>
                        <MenuItem value="createdAt,asc">등록일시 (오래된순)</MenuItem>
                        <MenuItem value="viewCount,desc">조회수 (높은순)</MenuItem>
                        <MenuItem value="viewCount,asc">조회수 (낮은순)</MenuItem>
                    </Select>
                </FormControl>

                <Button
                    variant="contained"
                    color="secondary"
                    onClick={() => navigate('/notices/new')}
                >
                    공지 등록
                </Button>
            </Box>

            {/*<NoticeList noticeList={noticeList}/>*/}

            <TableContainer component={Paper}>
                <Table size="small">
                    <TableHead>
                        <TableRow>
                            {/* 예시: 테이블 헤더 클릭 시 handleRequestSort 호출 가능 */}
                            <TableCell>
                                <TableSortLabel
                                    active={sortOption.startsWith('title')}
                                    direction={sortOption.endsWith('asc') ? 'asc' : 'desc'}
                                    onClick={(e) => handleRequestSort(e, 'title')}
                                >
                                    제목
                                </TableSortLabel>
                            </TableCell>
                            <TableCell>첨부 여부</TableCell>
                            <TableCell>
                                <TableSortLabel
                                    active={sortOption.startsWith('createdAt')}
                                    direction={sortOption.endsWith('asc') ? 'asc' : 'desc'}
                                    onClick={(e) => handleRequestSort(e, 'createdAt')}
                                >
                                    등록일시
                                </TableSortLabel>
                            </TableCell>
                            <TableCell>
                                <TableSortLabel
                                    active={sortOption.startsWith('viewCount')}
                                    direction={sortOption.endsWith('asc') ? 'asc' : 'desc'}
                                    onClick={(e) => handleRequestSort(e, 'viewCount')}
                                >
                                    조회수
                                </TableSortLabel>
                            </TableCell>
                            <TableCell>작성자</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {noticeList.map((notice) => (
                            <TableRow
                                key={notice.id}
                                hover
                                onClick={() => navigate(`/notices/${notice.id}`)}
                                sx={{ cursor: 'pointer' }}
                            >
                                <TableCell>{notice.title}</TableCell>
                                <TableCell>
                                    {notice.hasAttachment ? 'Y' : 'N'}
                                </TableCell>
                                <TableCell>
                                    {notice.createdAt
                                        ? notice.createdAt.substring(0, 19).replace('T', ' ')
                                        : '-'}
                                </TableCell>
                                <TableCell>{notice.viewCount}</TableCell>
                                <TableCell>{notice.author.name}</TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>

            {pageInfo && (
                <Box sx={{ mt: 2, display: 'flex', justifyContent: 'center', gap: 1 }}>
                    {/* 이전 페이지 버튼 */}
                    <Button
                        disabled={pageInfo.first}
                        onClick={() => setPage((prev) => Math.max(prev - 1, 0))}
                    >
                        이전
                    </Button>

                    {/* 현재 페이지 표시 */}
                    <Typography sx={{ alignSelf: 'center' }}>
                        {pageInfo.pageNumber + 1} / {pageInfo.totalPages}
                    </Typography>

                    {/* 다음 페이지 버튼 */}
                    <Button
                        disabled={pageInfo.last}
                        onClick={() => setPage((prev) => prev + 1)}
                    >
                        다음
                    </Button>
                </Box>
            )}
        </Box>
    );
}

export default NoticeListPage;
