import React, {useEffect, useState} from 'react';
import {useNavigate, useParams, useSearchParams} from 'react-router-dom';
import {fetchNoticeById, createNotice, updateNotice, fetchNoticeList} from '../api/NoticeApi';
import NoticeForm from '../components/NoticeForm';

function NoticeFormPage() {
    const { noticeId } = useParams(); // 있으면 수정, 없으면 생성
    const navigate = useNavigate();

    const isEdit = Boolean(noticeId);
    const [initialValues, setInitialValues] = useState({
        title: '',
        content: '',
        startAt: '',
        endAt: '',
        files: [],
    });
    const [loading, setLoading] = useState(isEdit);

    const getNotice = async () => {
        try {
            const res = await fetchNoticeById(noticeId);

            setInitialValues({
                title: res.title || '',
                content: res.content || '',
                startAt: res.startAt ? res.startAt.substring(0, 16) : '',
                endAt: res.endAt ? res.endAt.substring(0, 16) : '',
                files: [] // todo: 기존 첨부파일 보여주기 ..
            });
        } catch (e) {
            console.error('공지 수정용 상세 조회 실패', e);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (isEdit) { // 편집모드일때만 가져옴
            getNotice();
        }
    }, [noticeId, isEdit]);

    const handleSubmit = async (values) => {
        // req 생성 (formData 형태)
        const formData = new FormData();
        formData.append('userId', 1);
        formData.append('title', values.title);
        formData.append('content', values.content);
        formData.append('startAt', values.startAt);
        formData.append('endAt', values.endAt);

        if (values.files != null && values.files.length > 0) {
            Array.from(values.files).forEach((file) => {
                formData.append('files', file);
            });
        }

        try {
            if (isEdit) {
                await updateNotice(noticeId, formData);
            } else {
                await createNotice(formData);
            }
            navigate(`/notices`);
        } catch (err) {
            console.error('공지 등록/수정 실패', err);
        }
    };

    if (loading) return <div>로딩중. . . . . .</div>;

    return (
        <div style={{ padding: 24 }}>
            <NoticeForm
                initialValues={initialValues}
                onSubmit={handleSubmit}
                isEdit={isEdit}
            />
        </div>
    );
}

export default NoticeFormPage;
