import React, { useEffect, useState } from 'react';
import { Select, Table, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import * as adminWalletService from '@/services/adminWalletService';
import { formatDateTime, formatMoney, formatMoneyDelta } from '@/utils/format';
import type { PageData } from '@/types/common';
import type { WalletTransaction } from '@/types/wallet';

const PAGE_SIZE = 10;

const TX_TYPE_LABELS: Record<string, string> = {
  RECHARGE: '充值',
  ADJUSTMENT: '调账',
  PAYMENT: '消费',
  REFUND: '退款',
};

const TransactionTab: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<PageData<WalletTransaction> | null>(null);
  const [page, setPage] = useState(1);
  const [txType, setTxType] = useState<string | undefined>(undefined);

  const fetchData = async (current = 1, type = txType) => {
    try {
      setLoading(true);
      const result = await adminWalletService.getTransactions(current, PAGE_SIZE, type);
      setData(result);
      setPage(current);
    } catch (error: any) {
      message.error(error.message || '加载资金流水失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData(1, undefined);
  }, []);

  const columns: ColumnsType<WalletTransaction> = [
    {
      title: '时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: (value: string) => formatDateTime(value),
    },
    {
      title: '用户',
      dataIndex: 'username',
      key: 'username',
      width: 120,
      render: (value: string | undefined, record) => value || `用户#${record.userId}`,
    },
    {
      title: '类型',
      dataIndex: 'txType',
      key: 'txType',
      width: 90,
      render: (value: string) => TX_TYPE_LABELS[value] || value,
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
      title: '变动后本金',
      dataIndex: 'principalAfter',
      key: 'principalAfter',
      width: 120,
      render: (value: number) => formatMoney(value),
    },
    {
      title: '变动后赠送',
      dataIndex: 'bonusAfter',
      key: 'bonusAfter',
      width: 120,
      render: (value: number) => formatMoney(value),
    },
    {
      title: '业务单号',
      dataIndex: 'businessNo',
      key: 'businessNo',
      width: 220,
      render: (value?: string) => <span style={{ fontSize: 12 }}>{value || '-'}</span>,
    },
    {
      title: '说明',
      dataIndex: 'remark',
      key: 'remark',
      render: (value?: string) => value || '-',
    },
  ];

  return (
    <>
      <div style={{ marginBottom: 12 }}>
        <Select
          allowClear
          placeholder="全部类型"
          style={{ width: 160 }}
          value={txType}
          onChange={(value) => {
            setTxType(value);
            fetchData(1, value);
          }}
          options={Object.entries(TX_TYPE_LABELS).map(([value, label]) => ({ value, label }))}
        />
      </div>
      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={data?.records || []}
        scroll={{ x: 1250 }}
        pagination={{
          current: page,
          pageSize: PAGE_SIZE,
          total: data?.total || 0,
          showSizeChanger: false,
          onChange: (nextPage) => fetchData(nextPage),
        }}
      />
    </>
  );
};

export default TransactionTab;
