import React, { useState, useEffect } from 'react';
import { Layout, Menu, Dropdown, Avatar, Button, message } from 'antd';
import { useLocation, useNavigate, Outlet } from 'react-router-dom';
import {
  CrownOutlined,
  DashboardOutlined,
  LogoutOutlined,
  SettingOutlined,
  ShopOutlined,
  TeamOutlined,
  UserOutlined,
  WalletOutlined,
} from '@ant-design/icons';
import { useAuth } from '@/hooks/useAuth';
import { useApp } from '@/hooks/useApp';
import * as petService from '@/services/petService';
import styles from './MainLayout.module.css';

const { Header, Sider, Content } = Layout;

interface MenuItem {
  key: string;
  icon?: React.ReactNode;
  label: string;
  path?: string;
  children?: MenuItem[];
}

const MainLayout: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuth();
  const { currentPetId, setPets, setCurrentPetId, setCurrentPet } = useApp();
  const [collapsed, setCollapsed] = useState(false);
  const [openKeys, setOpenKeys] = useState<string[]>([]);

  // 预加载宠物数据，兼容“保留但隐藏”的用户侧路由；管理后台不展示选择器。
  useEffect(() => {
    const fetchPets = async () => {
      try {
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
      } catch {
        // 管理后台不依赖宠物数据，静默失败即可
      }
    };

    fetchPets();
  }, []);

  const menuItems: MenuItem[] = [
    {
      key: 'dashboard',
      icon: <DashboardOutlined />,
      label: '工作台',
      path: '/',
    },
    {
      key: 'users',
      icon: <TeamOutlined />,
      label: '用户管理',
      path: '/admin/users',
    },
    {
      key: 'commerce',
      icon: <ShopOutlined />,
      label: '商业服务',
      children: [
        { key: 'commerce-store', label: '门店管理', path: '/admin/commerce/stores' },
        { key: 'commerce-grooming', label: '美容服务', path: '/admin/commerce/grooming' },
        { key: 'commerce-product', label: '商品管理', path: '/admin/commerce/shop' },
        { key: 'commerce-inventory', label: '库存管理', path: '/admin/commerce/inventory' },
        { key: 'commerce-order', label: '订单管理', path: '/admin/commerce/orders' },
        { key: 'commerce-booking', label: '预约管理', path: '/admin/commerce/bookings' },
        { key: 'commerce-package', label: '套餐管理', path: '/admin/commerce/packages' },
        { key: 'commerce-after-sale', label: '售后管理', path: '/admin/commerce/after-sales' },
        { key: 'commerce-review', label: '评价管理', path: '/admin/commerce/reviews' },
      ],
    },
    {
      key: 'member-config',
      icon: <CrownOutlined />,
      label: '会员配置',
      path: '/admin/member',
    },
    {
      key: 'wallet',
      icon: <WalletOutlined />,
      label: '钱包管理',
      path: '/admin/wallet',
    },
  ];

  useEffect(() => {
    if (location.pathname.startsWith('/admin/commerce')) {
      setOpenKeys((previous) =>
        previous.includes('commerce') ? previous : [...previous, 'commerce']
      );
    }
  }, [location.pathname]);

  const leafItems = menuItems.flatMap((item) => (item.children ? item.children : [item]));
  const selectedKey = leafItems
    .filter(
      (item) =>
        item.path &&
        (location.pathname === item.path || location.pathname.startsWith(`${item.path}/`))
    )
    .sort((a, b) => (b.path?.length || 0) - (a.path?.length || 0))[0]?.key;

  const userMenu = [
    {
      key: 'settings',
      icon: <SettingOutlined />,
      label: '修改密码',
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

  const findMenuItem = (items: MenuItem[], key: string): MenuItem | undefined => {
    for (const item of items) {
      if (item.key === key) {
        return item;
      }
      if (item.children) {
        const found = findMenuItem(item.children, key);
        if (found) {
          return found;
        }
      }
    }
    return undefined;
  };

  const handleMenuClick = (key: string) => {
    const item = findMenuItem(menuItems, key);
    if (item?.path) {
      navigate(item.path);
    }
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider trigger={null} collapsible collapsed={collapsed} width={200}>
        <div className={styles.logo}>
          <img src="/logo.png" alt="它的世界" className={styles.logoIcon} />
          <span className={styles.logoText}>它的世界</span>
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={selectedKey ? [selectedKey] : []}
          openKeys={openKeys}
          onOpenChange={(keys) => setOpenKeys(keys as string[])}
          items={menuItems.map((item) => ({
            key: item.key,
            icon: item.icon,
            label: item.label,
            children: item.children?.map((child) => ({
              key: child.key,
              label: child.label,
            })),
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
            <span style={{ color: 'rgba(255,255,255,0.75)', marginRight: 8 }}>
              {user?.nickName || user?.username || '管理员'}
            </span>
            <Dropdown menu={{ items: userMenu }}>
              <Avatar
                icon={<UserOutlined />}
                src={user?.avatarUrl}
                style={{ cursor: 'pointer' }}
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
