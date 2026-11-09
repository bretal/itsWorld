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
import AdminDashboardPage from '@/pages/admin/AdminDashboardPage';
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

// Member Pages
import MemberCenterPage from '@/pages/member/MemberCenterPage';
import MemberLevelPage from '@/pages/member/MemberLevelPage';
import MemberPointsPage from '@/pages/member/MemberPointsPage';
import MemberWalletPage from '@/pages/member/MemberWalletPage';
import MemberCouponsPage from '@/pages/member/MemberCouponsPage';

import DataManagementPage from '@/pages/admin/data/DataManagementPage';
import ReportManagementPage from '@/pages/admin/data/ReportManagementPage';

// Admin Pages
import AdminRoute from '@/components/AdminRoute';
import UserManagementPage from '@/pages/admin/users/UserManagementPage';
import MemberAdminPage from '@/pages/admin/member/MemberAdminPage';
import WalletAdminPage from '@/pages/admin/wallet/WalletAdminPage';
import StoreManagementPage from '@/pages/admin/commerce/StoreManagementPage';
import GroomingManagementPage from '@/pages/admin/commerce/GroomingManagementPage';
import ProductManagementPage from '@/pages/admin/commerce/ProductManagementPage';
import InventoryManagementPage from '@/pages/admin/commerce/InventoryManagementPage';
import OrderManagementPage from '@/pages/admin/commerce/OrderManagementPage';
import BookingManagementPage from '@/pages/admin/commerce/BookingManagementPage';
import PackageManagementPage from '@/pages/admin/commerce/PackageManagementPage';
import AfterSaleManagementPage from '@/pages/admin/commerce/AfterSaleManagementPage';
import ReviewManagementPage from '@/pages/admin/commerce/ReviewManagementPage';

const theme = {
  token: {
    colorPrimary: '#1677ff',
    borderRadius: 8,
    colorBgLayout: '#f5f7fa',
  },
  components: {
    Layout: {
      headerBg: '#001529',
      siderBg: '#001529',
    },
    Card: {
      borderRadiusLG: 12,
    },
  },
};

const App: React.FC = () => {
  const { isAuthenticated, fetchUserInfo } = useAuth();

  // 初始化：如果已认证，获取用户信息
  useEffect(() => {
    if (isAuthenticated) {
      fetchUserInfo();
    }
  }, [isAuthenticated, fetchUserInfo]);

  return (
    <ConfigProvider locale={zhCN} theme={theme}>
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
          <Route path="/" element={<AdminDashboardPage />} />
          <Route path="/pet" element={<PetListPage />} />
          <Route path="/pet/create" element={<PetCreatePage />} />
          <Route path="/pet/:petId" element={<PetDetailPage />} />
          <Route path="/pet/:petId/edit" element={<PetDetailPage />} />
          <Route path="/diary" element={<DiaryTimelinePage />} />
          <Route path="/diary/greeting" element={<GreetingFlowPage />} />
          <Route path="/community" element={<CommunityPage />} />
          <Route path="/profile" element={<UserProfilePage />} />
          <Route path="/settings" element={<ChangePasswordPage />} />

          {/* 会员中心 */}
          <Route path="/member" element={<MemberCenterPage />} />
          <Route path="/member/levels" element={<MemberLevelPage />} />
          <Route path="/member/points" element={<MemberPointsPage />} />
          <Route path="/member/wallet" element={<MemberWalletPage />} />
          <Route path="/member/coupons" element={<MemberCouponsPage />} />

          <Route path="/admin/pets" element={<AdminRoute><DataManagementPage section="pets" /></AdminRoute>} />
          <Route path="/admin/diaries" element={<AdminRoute><DataManagementPage section="diaries" /></AdminRoute>} />
          <Route path="/admin/community" element={<AdminRoute><DataManagementPage section="community" /></AdminRoute>} />
          <Route path="/admin/health" element={<AdminRoute><DataManagementPage section="health" /></AdminRoute>} />
          <Route path="/admin/reports" element={<AdminRoute><ReportManagementPage /></AdminRoute>} />
          {/* 运营管理（仅管理员） */}
          <Route
            path="/admin/users"
            element={
              <AdminRoute>
                <UserManagementPage />
              </AdminRoute>
            }
          />
          <Route
            path="/admin/commerce/stores"
            element={
              <AdminRoute>
                <StoreManagementPage />
              </AdminRoute>
            }
          />
          <Route
            path="/admin/commerce/grooming"
            element={
              <AdminRoute>
                <GroomingManagementPage />
              </AdminRoute>
            }
          />
          <Route
            path="/admin/commerce/shop"
            element={
              <AdminRoute>
                <ProductManagementPage />
              </AdminRoute>
            }
          />
          <Route
            path="/admin/commerce/inventory"
            element={
              <AdminRoute>
                <InventoryManagementPage />
              </AdminRoute>
            }
          />
          <Route
            path="/admin/commerce/orders"
            element={
              <AdminRoute>
                <OrderManagementPage />
              </AdminRoute>
            }
          />
          <Route
            path="/admin/commerce/bookings"
            element={
              <AdminRoute>
                <BookingManagementPage />
              </AdminRoute>
            }
          />
          <Route
            path="/admin/commerce/packages"
            element={
              <AdminRoute>
                <PackageManagementPage />
              </AdminRoute>
            }
          />
          <Route
            path="/admin/commerce/after-sales"
            element={
              <AdminRoute>
                <AfterSaleManagementPage />
              </AdminRoute>
            }
          />
          <Route
            path="/admin/commerce/reviews"
            element={
              <AdminRoute>
                <ReviewManagementPage />
              </AdminRoute>
            }
          />
          <Route
            path="/admin/member"
            element={
              <AdminRoute>
                <MemberAdminPage />
              </AdminRoute>
            }
          />
          <Route
            path="/admin/wallet"
            element={
              <AdminRoute>
                <WalletAdminPage />
              </AdminRoute>
            }
          />
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
