import React from 'react';
import { Card, Tabs } from 'antd';
import RechargePlanTab from './RechargePlanTab';
import OfflineRechargeTab from './OfflineRechargeTab';
import AdjustmentTab from './AdjustmentTab';
import TransactionTab from './TransactionTab';

const WalletAdminPage: React.FC = () => {
  return (
    <Card title="钱包管理">
      <Tabs
        items={[
          { key: 'plans', label: '充值方案', children: <RechargePlanTab /> },
          { key: 'offline', label: '线下充值审批', children: <OfflineRechargeTab /> },
          { key: 'adjustments', label: '余额调账', children: <AdjustmentTab /> },
          { key: 'transactions', label: '资金流水', children: <TransactionTab /> },
        ]}
      />
    </Card>
  );
};

export default WalletAdminPage;
