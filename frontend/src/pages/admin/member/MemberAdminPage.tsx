import React from 'react';
import { Card, Tabs } from 'antd';
import LevelTab from './LevelTab';
import TaskTab from './TaskTab';
import CouponTab from './CouponTab';
import GiftTab from './GiftTab';

const MemberAdminPage: React.FC = () => {
  return (
    <Card title="会员配置">
      <Tabs
        items={[
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
