import React, { useState, useEffect } from 'react';
import { Layout, Menu, Dropdown, Avatar, Button, Spin, message } from 'antd';
import { useNavigate, Outlet } from 'react-router-dom';
import {
  HomeOutlined,
  FileOutlined,
  TeamOutlined,
  UserOutlined,
  LogoutOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import { useAuth } from '@/hooks/useAuth';
import { useApp } from '@/hooks/useApp';
import * as petService from '@/services/petService';
import styles from './MainLayout.module.css';

const { Header, Sider, Content } = Layout;

interface MenuItem {
  key: string;
  icon: React.ReactNode;
  label: string;
  path: string;
}

const MainLayout: React.FC = () => {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const { currentPetId, pets, setPets, setCurrentPetId, setCurrentPet } = useApp();
  const [collapsed, setCollapsed] = useState(false);
  const [loading, setLoading] = useState(true);

  // 初始化：加载宠物列表
  useEffect(() => {
    const fetchPets = async () => {
      try {
        setLoading(true);
        const data = await petService.listPets();
        const petList = data || [];
        setPets(petList);
        if (petList.length > 0) {
          const selected = petList.find((pet) => pet.id === currentPetId) || petList[0];
          setCurrentPetId(selected.id);
          setCurrentPet(selected);
        } else {
          setCurrentPetId(null);
          setCurrentPet(null);
        }
      } catch (error: any) {
        message.error(error.message || '加载宠物列表失败');
      } finally {
        setLoading(false);
      }
    };

    fetchPets();
  }, []);

  const menuItems: MenuItem[] = [
    {
      key: 'home',
      icon: <HomeOutlined />,
      label: '首页',
      path: '/',
    },
    {
      key: 'pet',
      icon: <UserOutlined />,
      label: '宠物档案',
      path: '/pet',
    },
    {
      key: 'diary',
      icon: <FileOutlined />,
      label: '日记',
      path: '/diary',
    },
    {
      key: 'community',
      icon: <TeamOutlined />,
      label: '宠友圈',
      path: '/community',
    },
  ];

  const userMenu = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人中心',
      onClick: () => navigate('/profile'),
    },
    {
      key: 'settings',
      icon: <SettingOutlined />,
      label: '设置',
      onClick: () => navigate('/settings'),
    },
    {
      type: 'divider' as const,
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '登出',
      onClick: () => {
        logout();
        setPets([]);
        setCurrentPetId(null);
        setCurrentPet(null);
        message.success('已登出');
        navigate('/login');
      },
    },
  ];

  const handleMenuClick = (key: string) => {
    const item = menuItems.find((m) => m.key === key);
    if (item) {
      navigate(item.path);
    }
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider trigger={null} collapsible collapsed={collapsed} width={200}>
        <div className={styles.logo}>
          <span>它的世界</span>
        </div>
        <Menu
          theme="dark"
          mode="inline"
          items={menuItems.map((item) => ({
            key: item.key,
            icon: item.icon,
            label: item.label,
          }))}
          onClick={(e) => handleMenuClick(e.key)}
        />
      </Sider>

      <Layout>
        <Header className={styles.header}>
          <Button
            type="text"
            icon={collapsed ? '☰' : '✕'}
            onClick={() => setCollapsed(!collapsed)}
            style={{ color: '#fff', fontSize: 18 }}
          />

          <div className={styles.headerRight}>
            <Spin spinning={loading} size="small">
              {pets.length > 0 && (
                <Dropdown
                  menu={{
                    items: pets.map((pet) => ({
                      key: pet.id.toString(),
                      label: pet.petName,
                      onClick: () => {
                        setCurrentPetId(pet.id);
                        setCurrentPet(pet);
                      },
                    })),
                  }}
                >
                  <Button type="text" style={{ color: '#fff' }}>
                    {pets.find((p) => p.id === currentPetId)?.petName || '选择宠物'} ▼
                  </Button>
                </Dropdown>
              )}
            </Spin>

            <Dropdown menu={{ items: userMenu }}>
              <Avatar
                icon={<UserOutlined />}
                src={user?.avatarUrl}
                style={{ cursor: 'pointer', marginLeft: 16 }}
              />
            </Dropdown>
          </div>
        </Header>

        <Content className={styles.content}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
};

export default MainLayout;
