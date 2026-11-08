import React, { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { message, Spin } from 'antd';
import { useAuth } from '@/hooks/useAuth';

interface ProtectedRouteProps {
  children: React.ReactNode;
}

/**
 * 受保护的路由组件：仅管理员可进入 Web 管理后台
 * 普通用户即使持有旧 token 也会被清理并回到登录页。
 */
const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children }) => {
  const { user, isAuthenticated, loading, fetchUserInfo, logout } = useAuth();
  const [attempted, setAttempted] = useState(false);

  useEffect(() => {
    let active = true;
    if (isAuthenticated && !user) {
      fetchUserInfo().finally(() => {
        if (active) {
          setAttempted(true);
        }
      });
    } else {
      setAttempted(true);
    }
    return () => {
      active = false;
    };
  }, [isAuthenticated, user, fetchUserInfo]);

  useEffect(() => {
    if (user?.role && user.role !== 'ADMIN') {
      message.error('仅管理员可登录管理后台');
      logout();
    }
  }, [user, logout]);

  if (loading || (isAuthenticated && !user && !attempted)) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <Spin size="large" />
      </div>
    );
  }

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace />;
  }

  if (user.role !== 'ADMIN') {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
};

export default ProtectedRoute;
