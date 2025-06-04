import React, {useEffect, useState} from 'react';
import {useParams, useNavigate} from 'react-router-dom';
import {fetchNoticeById, deleteNotice} from '../api/NoticeApi';
import NoticeDetail from '../components/NoticeDetail';
import {Button} from '@mui/material';

function NoticeDetailPage() {
    const { noticeId } = useParams();
    const navigate = useNavigate();

    const [notice, setNotice] = useState(null);
    const [loading, setLoading] = useState(true);

    const getNoticeDetail = async () => {
        let res = null;
        try{
            res = await fetchNoticeById(noticeId);
            setNotice(res);
        }
        catch (e) {
            console.error('공지 상세 조회 실패', e);
        }
        finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        getNoticeDetail();
    }, [noticeId]);

    const handleDelete = async () => {
        if (window.confirm('정말 삭제하시겠습니까?')) {
            try {
                await deleteNotice(noticeId);
                navigate('/notices');
            } catch (e) {
                console.error('공지 삭제 실패', e);
            }
        }
    };

    if (loading) return <div>로딩 중…</div>;
    if (!notice) return <div>공지 정보를 불러올 수 없습니다.</div>;

    return (
        <div style={{ padding: 24 }}>
            <NoticeDetail notice={notice} />
            <Button
                variant="contained"
                color="secondary"
                onClick={handleDelete}
                style={{ marginTop: 16, marginRight: 8 }}
            >
                삭제
            </Button>
            <Button
                variant="contained"
                color="primary"
                onClick={() => navigate(`/notices/${notice.id}/edit`)}
                style={{ marginTop: 16 }}
            >
                수정
            </Button>
        </div>
    );
}

export default NoticeDetailPage;
