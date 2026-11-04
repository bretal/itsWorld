import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import ProtectedRoute from '@/components/ProtectedRoute';
import { useAuth } from '@/hooks/useAuth';
import './App.css';

// Auth Pages
import LoginPage from '@/pages/auth/Login';
import RegisterPage from '@/pages/auth/Register';

// Layouts
import MainLayout from '@/layouts/MainLayout';

// Pages
import HomePage from '@/pages/home/HomePage';
import UserProfilePage from '@/pages/profile/UserProfilePage';
import ChangePasswordPage from '@/pages/profile/ChangePasswordPage';

// Pet Pages
import PetListPage from '@/pages/pet/PetListPage';
import PetCreatePage from '@/pages/pet/PetCreatePage';
import PetDetailPage from '@/pages/pet/PetDetailPage';

// Diary Pages
import DiaryTimelinePage from '@/pages/diary/DiaryTimelinePage';
import GreetingFlowPage from '@/pages/diary/GreetingFlowPage';

// Community Pages
import CommunityPage from '@/pages/community/CommunityPage';

const App: React.FC = () => {
  const { isAuthenticated, fetchUserInfo } = useAuth();

  // 初始化：如果已认证，获取用户信息
  useEffect(() => {
    if (isAuthenticated) {
      fetchUserInfo();
    }
  }, [isAuthenticated, fetchUserInfo]);

  return (
    <ConfigProvider locale={zhCN}>
      <Router>
        <Routes>
          {/* 公开路由 */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          {/* 受保护的路由 */}
          <Route
            element={
              <ProtectedRoute>
                <MainLayout />
              </ProtectedRoute>
            }
          >
          <Route path="/" element={<HomePage />} />
          <Route path="/pet" element={<PetListPage />} />
          <Route path="/pet/create" element={<PetCreatePage />} />
          <Route path="/pet/:petId" element={<PetDetailPage />} />
          <Route path="/pet/:petId/edit" element={<PetDetailPage />} />
          <Route path="/diary" element={<DiaryTimelinePage />} />
          <Route path="/diary/greeting" element={<GreetingFlowPage />} />
          <Route path="/community" element={<CommunityPage />} />
          <Route path="/profile" element={<UserProfilePage />} />
          <Route path="/settings" element={<ChangePasswordPage />} />
          </Route>

          {/* 未匹配路由 */}
          <Route
            path="*"
            element={<div style={{ padding: 60, textAlign: 'center' }}>页面不存在</div>}
          />
        </Routes>
      </Router>
    </ConfigProvider>
  );
};

export default App;
