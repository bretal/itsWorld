import React, { useState } from 'react';
import Taro from '@tarojs/taro';
import { View, Text, Input, Button, Form } from '@tarojs/components';
import { useAuth } from '@/hooks';
import './index.scss';

const Login: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();

  const handleLogin = async () => {
    if (!email || !password) {
      Taro.showToast({
        title: '请输入邮箱和密码',
        icon: 'error',
      });
      return;
    }

    setLoading(true);
    try {
      await login({ email, password });
      Taro.showToast({
        title: '登录成功',
        icon: 'success',
      });
      // 重定向到首页
      Taro.redirectTo({ url: '/pages/home/index' });
    } catch (error: any) {
      Taro.showToast({
        title: error.message || '登录失败',
        icon: 'error',
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <View className="login-container">
      <View className="login-header">
        <Text className="logo">🐾</Text>
        <Text className="title">它的世界</Text>
        <Text className="subtitle">和TA一起成长</Text>
      </View>

      <Form className="login-form">
        <View className="form-group">
          <Input
            type="email"
            placeholder="邮箱"
            value={email}
            onInput={(e) => setEmail(e.detail.value)}
            className="form-input"
          />
        </View>

        <View className="form-group">
          <Input
            type="password"
            placeholder="密码"
            value={password}
            onInput={(e) => setPassword(e.detail.value)}
            className="form-input"
          />
        </View>

        <Button
          className="login-btn"
          loading={loading}
          onClick={handleLogin}
        >
          {loading ? '登录中...' : '登录'}
        </Button>
      </Form>

      <View className="login-footer">
        <Text className="footer-text">没有账号？</Text>
        <Button
          className="link-btn"
          onClick={() => Taro.navigateTo({ url: '/pages/auth/register' })}
        >
          立即注册
        </Button>
      </View>
    </View>
  );
};

export default Login;
