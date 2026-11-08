import React, { useEffect } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { Button, Result, Spin } from 'antd';
import { useAuth } from '@/hooks/useAuth';

interface AdminRouteProps {
  children: React.ReactNode;
}

/**
 * 运营管理路由守卫：仅 ADMIN 可访问
 */
const AdminRoute: React.FC<AdminRouteProps> = ({ children }) => {
  const navigate = useNavigate();
  const { user, isAuthenticated, loading, fetchUserInfo } = useAuth();

  useEffect(() => {
    if (isAuthenticated && !user) {
      fetchUserInfo();
    }
  }, [isAuthenticated, user, fetchUserInfo]);

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (loading && !user) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 320 }}>
        <Spin size="large" />
      </div>
    );
  }

  if (!user || user.role !== 'ADMIN') {
    return (
      <Result
        status="403"
        title="403"
        subTitle="当前账号没有运营管理权限"
        extra={
          <Button type="primary" onClick={() => navigate('/')}>
            返回首页
          </Button>
        }
      />
    );
  }

  return <>{children}</>;
};

export default AdminRoute;
