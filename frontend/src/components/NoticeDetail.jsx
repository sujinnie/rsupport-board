import React from 'react';
import PropTypes from 'prop-types';
import {
    Typography, Divider, List, ListItem, ListItemText, Link, Paper
} from '@mui/material';

function NoticeDetail({ notice }) {
    return (
        <Paper style={{ padding: 24 }}>
            <Typography variant="h4" gutterBottom>
                {notice.title}
            </Typography>

            <Typography variant="subtitle2" color="textSecondary" gutterBottom>
                등록일시: { new Date(notice.createdAt).toLocaleString() } &nbsp;|&nbsp;
                조회수: {notice.viewCount} &nbsp;|&nbsp;
                작성자: {notice.author?.name} &nbsp;|&nbsp;
                공지시작일시: { new Date(notice.startAt).toLocaleString() } &nbsp;|&nbsp;
                공지종료일시: { new Date(notice.endAt).toLocaleString() }
            </Typography>

            <Divider style={{margin: '16px 0'}} />

            <Typography variant="body1" component="div">
                {notice.content}
            </Typography>

            {notice.attachments && notice.attachments.length > 0 && (
                <>
                    <Divider style={{ margin: '16px 0' }} />
                    <Typography variant="h6">첨부파일</Typography>
                    <List>
                        {notice.attachments.map((att) => (
                            <ListItem key={att.id}>
                                <ListItemText>
                                    <Link href={att.url} target="_blank" rel="noopener">
                                        {att.filename}
                                    </Link>
                                </ListItemText>
                            </ListItem>
                        ))}
                    </List>
                </>
            )}

            {notice.attachments.map(att => {
                const isImage = /\.(jpe?g|png|gif|bmp|webp)$/i.test(att.filename);

                return (
                    <div key={att.id} style={{ marginBottom: 16 }}>
                        {isImage ? (
                            <div style={{ marginBottom: 4 }}>
                                <img
                                    src={att.url}
                                    alt={att.filename}
                                    style={{
                                        maxWidth: '200px',
                                        maxHeight: '200px',
                                        objectFit: 'contain',
                                        border: '1px solid #ccc',
                                        borderRadius: 4,
                                    }}
                                />
                            </div>
                        ) : null}

                        {/* 파일명 + 다운로드 링크 */}
                        <div>
                            <a href={att.url} target="_blank" rel="noopener noreferrer">
                                {att.filename}
                            </a>
                        </div>
                    </div>
                );
            })}
        </Paper>
    );
}

NoticeDetail.propTypes = {
    notice: PropTypes.shape({
        id: PropTypes.number.isRequired,
        title: PropTypes.string.isRequired,
        content: PropTypes.string.isRequired,
        createdAt: PropTypes.string.isRequired,
        viewCount: PropTypes.number.isRequired,
        author: PropTypes.shape({
            id: PropTypes.number.isRequired,
            name: PropTypes.string.isRequired,
        }),
        attachments: PropTypes.arrayOf(
            PropTypes.shape({
                id: PropTypes.number.isRequired,
                filename: PropTypes.string.isRequired,
                url: PropTypes.string.isRequired,
            })
        ),
    }),
};

export default NoticeDetail;
