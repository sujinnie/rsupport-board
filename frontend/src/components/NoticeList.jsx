import React from 'react';
import PropTypes from 'prop-types';
import {useNavigate} from 'react-router-dom';
import {
    Table, TableBody, TableCell, TableContainer,
    TableHead, TableRow, Paper, Button
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';

function NoticeList({ noticeList }) {
    const navigate = useNavigate();

    return (
        <TableContainer component={Paper}>
            <Button
                variant="contained"
                color="primary"
                startIcon={<AddIcon />}
                onClick={() => navigate('/notices/new')}
                style={{ margin: 16 }}
            >
                공지 등록
            </Button>

            <Table>
                <TableHead>
                    <TableRow>
                        <TableCell>번호</TableCell>
                        <TableCell>제목</TableCell>
                        <TableCell>첨부파일</TableCell>
                        <TableCell>등록일시</TableCell>
                        <TableCell>조회수</TableCell>
                        <TableCell>작성자</TableCell>
                    </TableRow>
                </TableHead>

                <TableBody>
                    {noticeList.map((notice) => (
                        <TableRow
                            key={notice.id}
                            hover
                            onClick={() => navigate(`/notices/${notice.id}`)}
                        >
                            <TableCell>{notice.id}</TableCell>
                            <TableCell>{notice.title}</TableCell>
                            <TableCell>
                                {notice.attachments && notice.attachments.length > 0
                                    ? '📎' : ''}
                            </TableCell>
                            <TableCell>
                                {new Date(notice.createdAt).toLocaleString()}
                            </TableCell>
                            <TableCell>{notice.viewCount}</TableCell>
                            <TableCell>{notice.author.name}</TableCell>
                        </TableRow>
                    ))}
                    {noticeList.length === 0 && (
                        <TableRow>
                            <TableCell align="center">
                                등록된 공지가 없습니다.
                            </TableCell>
                        </TableRow>
                    )}
                </TableBody>
            </Table>
        </TableContainer>
    );
}

NoticeList.propTypes = {
    noticeList: PropTypes.arrayOf(
        PropTypes.shape({
            id: PropTypes.number.isRequired,
            title: PropTypes.string.isRequired,
            attachments: PropTypes.array,
            createdAt: PropTypes.string.isRequired,
            viewCount: PropTypes.number.isRequired,
            author: PropTypes.shape({
                id: PropTypes.number.isRequired,
                name: PropTypes.string.isRequired,
            }),
        })
    ),
};

export default NoticeList;
