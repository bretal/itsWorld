import React from 'react';
import { Card, Tabs } from 'antd';
import LevelTab from './LevelTab';
import TaskTab from './TaskTab';
import CouponTab from './CouponTab';
import GiftTab from './GiftTab';
import MemberCenterAdminTab from './MemberCenterAdminTab';

const MemberAdminPage: React.FC = () => {
  return (
    <Card title="会员配置">
      <Tabs
        items={[
          { key: 'subscription', label: '付费会员方案', children: <MemberCenterAdminTab kind="subscription-plans" /> },
          { key: 'rewards', label: '成长值兑换商品', children: <MemberCenterAdminTab kind="reward-products" /> },
          { key: 'redemptions', label: '兑换核销', children: <MemberCenterAdminTab kind="redemptions" /> },
          { key: 'levels', label: '等级权益', children: <LevelTab /> },
          { key: 'tasks', label: '成长任务', children: <TaskTab /> },
          { key: 'coupons', label: '优惠券', children: <CouponTab /> },
          { key: 'gifts', label: '礼包', children: <GiftTab /> },
        ]}
      />
    </Card>
  );
};

export default MemberAdminPage;
