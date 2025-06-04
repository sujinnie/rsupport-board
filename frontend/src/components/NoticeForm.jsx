import React, {useState} from 'react';
import {
    TextField, Button, Stack
} from '@mui/material';

function NoticeForm({ initialValues, onSubmit, isEdit }) {
    // 초기값을 props로 받아옴
    const [values, setValues] = useState({
        title: initialValues.title,
        content: initialValues.content,
        startAt: initialValues.startAt,
        endAt: initialValues.endAt,
        files: initialValues.files
    });

    const handleChange = (e) => {
        const { name, value } = e.target; // name, value 속성 가진 필드를 타겟으로..
        setValues((prev) => ({
            ...prev,
            [name]: value
        }));
    };

    const handleFileChange = (e) => {
        setValues((prev) => ({
            ...prev,
            files: e.target.files
        }));
    };

    const handleSubmitForm = (e) => {
        e.preventDefault(); // 페이지 리로드 방지
        onSubmit(values);
    };

    return (
        <form onSubmit={handleSubmitForm}>
            <Stack spacing={2} sx={{ maxWidth: 600 }}>
                <TextField
                    label="제목"
                    name="title"
                    value={values.title}
                    onChange={handleChange}
                    required
                />
                <TextField
                    label="내용"
                    name="content"
                    multiline
                    minRows={4}
                    value={values.content}
                    onChange={handleChange}
                    required
                />
                <TextField
                    label="공지 시작일시"
                    type="datetime-local"
                    name="startAt"
                    value={values.startAt}
                    onChange={handleChange}
                    InputLabelProps={{shrink: true}}
                    required
                />
                <TextField
                    label="공지 종료일시"
                    type="datetime-local"
                    name="endAt"
                    value={values.endAt}
                    onChange={handleChange}
                    InputLabelProps={{shrink: true}}
                    required
                />
                <Button variant="contained" component="label">
                    첨부파일 선택
                    <input
                        type="file"
                        name="files"
                        multiple
                        hidden
                        onChange={handleFileChange}
                    />
                </Button>
                {values.files != null && values.files.length > 0 && (
                    <div>
                        선택된 파일:
                        {Array.from(values.files).map((file) => (
                            <div key={file.name}>{file.name}</div>
                        ))}
                    </div>
                )}
                <Button type="submit" variant="contained" color="primary">
                    {isEdit ? '수정 완료' : '등록하기'}
                </Button>
            </Stack>
        </form>
    );
}

export default NoticeForm;
