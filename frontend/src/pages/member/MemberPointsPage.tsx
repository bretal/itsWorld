import React, { useEffect, useState } from 'react';
import { Card, Table, Tabs, message } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import type { ColumnsType } from 'antd/es/table';
import * as memberService from '@/services/memberService';
import { formatDateTime } from '@/utils/format';
import type { PageData } from '@/types/common';
import type { MemberGrowthLog, MemberPointsLog } from '@/types/member';

const PAGE_SIZE = 10;

const SOURCE_LABELS: Record<string, string> = {
  TASK: '任务奖励',
  GIFT: '礼包奖励',
  MANUAL: '人工调整',
};

const MemberPointsPage: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [pointsData, setPointsData] = useState<PageData<MemberPointsLog> | null>(null);
  const [growthData, setGrowthData] = useState<PageData<MemberGrowthLog> | null>(null);
  const [pointsPage, setPointsPage] = useState(1);
  const [growthPage, setGrowthPage] = useState(1);

  const fetchPoints = async (page: number) => {
    try {
      setLoading(true);
      const data = await memberService.getPointsLogs(page, PAGE_SIZE);
      setPointsData(data);
      setPointsPage(page);
    } catch (error: any) {
      message.error(error.message || '加载积分流水失败');
    } finally {
      setLoading(false);
    }
  };

  const fetchGrowth = async (page: number) => {
    try {
      setLoading(true);
      const data = await memberService.getGrowthLogs(page, PAGE_SIZE);
      setGrowthData(data);
      setGrowthPage(page);
    } catch (error: any) {
      message.error(error.message || '加载成长值流水失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPoints(1);
    fetchGrowth(1);
  }, []);

  const changeRender = (value: number) => (
    <span style={{ color: value >= 0 ? '#52c41a' : '#f5222d', fontWeight: 500 }}>
      {value > 0 ? `+${value}` : value}
    </span>
  );

  const baseColumns: ColumnsType<any> = [
    {
      title: '时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: (value: string) => formatDateTime(value),
    },
    {
      title: '变动',
      dataIndex: 'changeValue',
      key: 'changeValue',
      width: 100,
      render: (value: number) => changeRender(value),
    },
    {
      title: '变动后余额',
      dataIndex: 'balanceAfter',
      key: 'balanceAfter',
      width: 120,
    },
    {
      title: '来源',
      dataIndex: 'sourceType',
      key: 'sourceType',
      width: 120,
      render: (value: string) => SOURCE_LABELS[value] || value,
    },
    {
      title: '说明',
      dataIndex: 'remark',
      key: 'remark',
      render: (value?: string) => value || '-',
    },
  ];

  const renderTable = <T extends { id: number }>(
    data: PageData<T> | null,
    current: number,
    onChange: (page: number) => void,
    emptyText: string
  ) => (
    <Table
      rowKey="id"
      loading={loading}
      columns={baseColumns}
      dataSource={data?.records || []}
      locale={{ emptyText }}
      pagination={{
        current,
        pageSize: PAGE_SIZE,
        total: data?.total || 0,
        showSizeChanger: false,
        onChange,
      }}
    />
  );

  return (
    <Card
      title={
        <span>
          <ArrowLeftOutlined
            onClick={() => navigate('/member')}
            style={{ marginRight: 8, cursor: 'pointer' }}
          />
          积分与成长
        </span>
      }
    >
      <Tabs
        items={[
          {
            key: 'points',
            label: '积分流水',
            children: renderTable(pointsData, pointsPage, fetchPoints, '暂无积分流水'),
          },
          {
            key: 'growth',
            label: '成长值流水',
            children: renderTable(growthData, growthPage, fetchGrowth, '暂无成长值流水'),
          },
        ]}
      />
    </Card>
  );
};

export default MemberPointsPage;
