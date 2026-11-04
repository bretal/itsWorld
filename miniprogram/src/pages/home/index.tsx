import React, { useEffect } from 'react';
import Taro from '@tarojs/taro';
import { View, Text, Button, Image } from '@tarojs/components';
import { useAuth, useApp } from '@/hooks';
import './index.scss';

const Home: React.FC = () => {
  const { user, isAuthenticated } = useAuth();
  const { currentPet, fetchPets } = useApp();

  useEffect(() => {
    if (!isAuthenticated) {
      Taro.redirectTo({ url: '/pages/auth/index' });
      return;
    }
    fetchPets();
  }, [isAuthenticated]);

  return (
    <View className="home-container">
      <View className="home-header">
        <Text className="greeting">
          {user?.nickname ? `欢迎，${user.nickname}` : '欢迎回来'}
        </Text>
      </View>

      {currentPet && (
        <View className="pet-card">
          <View className="pet-avatar">
            <Text className="avatar-emoji">🐕</Text>
          </View>
          <View className="pet-info">
            <Text className="pet-name">{currentPet.name}</Text>
            <Text className="pet-desc">
              {currentPet.breed} · {currentPet.gender}
            </Text>
          </View>
        </View>
      )}

      <View className="menu-grid">
        <View
          className="menu-item"
          onClick={() => Taro.navigateTo({ url: '/pages/diary/greeting' })}
        >
          <Text className="menu-icon">📝</Text>
          <Text className="menu-text">记录问候</Text>
        </View>

        <View
          className="menu-item"
          onClick={() => Taro.navigateTo({ url: '/pages/health/index' })}
        >
          <Text className="menu-icon">❤️</Text>
          <Text className="menu-text">健康管理</Text>
        </View>

        <View
          className="menu-item"
          onClick={() => Taro.navigateTo({ url: '/pages/community/index' })}
        >
          <Text className="menu-icon">👥</Text>
          <Text className="menu-text">宠友圈</Text>
        </View>

        <View
          className="menu-item"
          onClick={() => Taro.navigateTo({ url: '/pages/ai/index' })}
        >
          <Text className="menu-icon">🤖</Text>
          <Text className="menu-text">AI 识别</Text>
        </View>
      </View>
    </View>
  );
};

export default Home;
