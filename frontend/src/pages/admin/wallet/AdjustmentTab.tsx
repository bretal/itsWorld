import React, { useEffect, useState } from 'react';
import { Table, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import * as adminWalletService from '@/services/adminWalletService';
import { formatDateTime, formatMoney, formatMoneyDelta } from '@/utils/format';
import type { PageData } from '@/types/common';
import type { WalletAdjustment } from '@/types/wallet';

const PAGE_SIZE = 10;

const AdjustmentTab: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<PageData<WalletAdjustment> | null>(null);
  const [page, setPage] = useState(1);

  const fetchData = async (current = 1) => {
    try {
      setLoading(true);
      const result = await adminWalletService.getAdjustments(current, PAGE_SIZE);
      setData(result);
      setPage(current);
    } catch (error: any) {
      message.error(error.message || '加载调账记录失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData(1);
  }, []);

  const columns: ColumnsType<WalletAdjustment> = [
    {
      title: '调账单号',
      dataIndex: 'adjustmentNo',
      key: 'adjustmentNo',
      width: 220,
      render: (value: string) => <span style={{ fontSize: 12 }}>{value}</span>,
    },
    {
      title: '用户',
      dataIndex: 'username',
      key: 'username',
      width: 120,
      render: (value: string | undefined, record) => value || `用户#${record.userId}`,
    },
    {
      title: '本金变动（元）',
      dataIndex: 'principalDelta',
      key: 'principalDelta',
      width: 130,
      render: (value: number) => (
        <span style={{ color: value >= 0 ? '#52c41a' : '#f5222d' }}>
          {formatMoneyDelta(value)}
        </span>
      ),
    },
    {
      title: '赠送变动（元）',
      dataIndex: 'bonusDelta',
      key: 'bonusDelta',
      width: 130,
      render: (value: number) => (
        <span style={{ color: value >= 0 ? '#52c41a' : '#f5222d' }}>
          {formatMoneyDelta(value)}
        </span>
      ),
    },
    {
      title: '变动后本金/赠送',
      key: 'after',
      width: 170,
      render: (_, record) =>
        `¥${formatMoney(record.principalAfter)} / ¥${formatMoney(record.bonusAfter)}`,
    },
    {
      title: '原因',
      dataIndex: 'reason',
      key: 'reason',
      render: (value: string) => value || '-',
    },
    {
      title: '操作人',
      dataIndex: 'operatorName',
      key: 'operatorName',
      width: 120,
      render: (value: string | undefined, record) => value || `管理员#${record.operatorId}`,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 90,
      render: (value: string) =>
        value === 'SUCCESS' ? <Tag color="success">成功</Tag> : <Tag color="error">{value}</Tag>,
    },
    {
      title: '时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (value: string) => formatDateTime(value),
    },
  ];

  return (
    <Table
      rowKey="id"
      loading={loading}
      columns={columns}
      dataSource={data?.records || []}
      scroll={{ x: 1250 }}
      locale={{ emptyText: '暂无调账记录' }}
      pagination={{
        current: page,
        pageSize: PAGE_SIZE,
        total: data?.total || 0,
        showSizeChanger: false,
        onChange: fetchData,
      }}
    />
  );
};

export default AdjustmentTab;
