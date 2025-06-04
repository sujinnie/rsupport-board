import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';

import NoticeListPage from './pages/NoticeListPage';
import NoticeDetailPage from './pages/NoticeDetailPage';
import NoticeFormPage from './pages/NoticeFormPage';

function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<NoticeListPage />} />
                <Route path="/notices/:noticeId" element={<NoticeDetailPage />} />
                <Route path="/notices/new" element={<NoticeFormPage />} />
                <Route path="/notices/:noticeId/edit" element={<NoticeFormPage />} />
            </Routes>
        </BrowserRouter>
    );
}

export default App;
