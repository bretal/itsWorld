import React, { useEffect, useState } from 'react';
import { Button, Card, Col, Row, Spin, Statistic, Table, Tag, message } from 'antd';
import {
  CrownOutlined,
  TeamOutlined,
  WalletOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import type { ColumnsType } from 'antd/es/table';
import * as adminOverviewService from '@/services/adminOverviewService';
import * as adminWalletService from '@/services/adminWalletService';
import { formatDateTime, formatMoney } from '@/utils/format';
import type { AdminOverview } from '@/types/admin';
import type { RechargeOrder } from '@/types/wallet';

const AdminDashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [overview, setOverview] = useState<AdminOverview | null>(null);
  const [pendingOrders, setPendingOrders] = useState<RechargeOrder[]>([]);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const [overviewData, pendingData] = await Promise.all([
          adminOverviewService.getOverview(),
          adminWalletService.getOfflineRecharges(1, 5, 'CREATED'),
        ]);
        setOverview(overviewData);
        setPendingOrders(pendingData.records || []);
      } catch (error: any) {
        message.error(error.message || '加载工作台数据失败');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const pendingColumns: ColumnsType<RechargeOrder> = [
    {
      title: '充值单号',
      dataIndex: 'rechargeNo',
      key: 'rechargeNo',
      render: (value: string) => <span style={{ fontSize: 12 }}>{value}</span>,
    },
    {
      title: '用户',
      dataIndex: 'username',
      key: 'username',
      render: (value: string | undefined, record) => value || `用户#${record.userId}`,
    },
    {
      title: '本金/赠送',
      key: 'amount',
      render: (_, record) =>
        `¥${formatMoney(record.principalAmount)} / ¥${formatMoney(record.bonusAmount)}`,
    },
    {
      title: '门店',
      dataIndex: 'storeName',
      key: 'storeName',
      render: (value?: string) => value || '-',
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (value: string) => formatDateTime(value),
    },
    {
      title: '状态',
      key: 'status',
      render: () => <Tag color="warning">待审批</Tag>,
    },
  ];

  return (
    <Spin spinning={loading}>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic title="用户总数" value={overview?.totalUsers || 0} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic title="启用用户" value={overview?.activeUsers || 0} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic title="禁用用户" value={overview?.disabledUsers || 0} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic title="管理员" value={overview?.adminUsers || 0} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic title="会员账户" value={overview?.memberCount || 0} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="钱包可用总额（元）"
              value={formatMoney(overview?.totalAvailableBalance)}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic title="本金余额（元）" value={formatMoney(overview?.totalPrincipalBalance)} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic title="赠送余额（元）" value={formatMoney(overview?.totalBonusBalance)} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic title="今日充值（元）" value={formatMoney(overview?.todayRechargeAmount)} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic title="待审批线下充值" value={overview?.pendingOfflineRecharges || 0} />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={8}>
          <Card hoverable onClick={() => navigate('/admin/users')} style={{ cursor: 'pointer' }}>
            <TeamOutlined style={{ fontSize: 28, color: '#1677ff' }} />
            <div style={{ marginTop: 8, fontWeight: 500 }}>用户管理</div>
            <div style={{ color: '#999' }}>查看全部用户余额、会员与身份状态</div>
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card hoverable onClick={() => navigate('/admin/member')} style={{ cursor: 'pointer' }}>
            <CrownOutlined style={{ fontSize: 28, color: '#faad14' }} />
            <div style={{ marginTop: 8, fontWeight: 500 }}>会员配置</div>
            <div style={{ color: '#999' }}>等级、权益、任务、优惠券与礼包</div>
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card hoverable onClick={() => navigate('/admin/wallet')} style={{ cursor: 'pointer' }}>
            <WalletOutlined style={{ fontSize: 28, color: '#52c41a' }} />
            <div style={{ marginTop: 8, fontWeight: 500 }}>钱包管理</div>
            <div style={{ color: '#999' }}>充值方案、线下审批、调账与资金流水</div>
          </Card>
        </Col>
      </Row>

      <Card
        title="待审批线下充值"
        style={{ marginTop: 16 }}
        extra={
          <Button type="link" onClick={() => navigate('/admin/wallet')}>
            前往处理
          </Button>
        }
      >
        <Table
          rowKey="id"
          columns={pendingColumns}
          dataSource={pendingOrders}
          pagination={false}
          locale={{ emptyText: '暂无待审批线下充值' }}
          scroll={{ x: 800 }}
        />
      </Card>
    </Spin>
  );
};

export default AdminDashboardPage;
