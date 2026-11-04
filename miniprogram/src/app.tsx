import React from 'react';
import { useAuth } from '@/hooks/useAuth';
import './app.scss';

const App: React.FC = () => {
  const { isAuthenticated } = useAuth();

  return (
    <div>
      {/* Taro 会自动处理路由和页面切换 */}
    </div>
  );
};

export default App;
