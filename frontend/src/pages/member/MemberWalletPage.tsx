import React, { useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Col,
  Modal,
  Row,
  Space,
  Spin,
  Statistic,
  Table,
  Tabs,
  Tag,
  message,
} from 'antd';
import { ArrowLeftOutlined, WalletOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import type { ColumnsType } from 'antd/es/table';
import * as walletService from '@/services/walletService';
import { formatDateTime, formatMoney, formatMoneyDelta } from '@/utils/format';
import type { PageData } from '@/types/common';
import type {
  RechargeOrder,
  RechargePlan,
  WalletSummary,
  WalletTransaction,
} from '@/types/wallet';

const PAGE_SIZE = 10;

const orderStatusTag = (order: RechargeOrder) => {
  if (order.creditStatus === 'CREDITED') {
    return <Tag color="success">已到账</Tag>;
  }
  if (order.paymentStatus === 'PAID') {
    return <Tag color="processing">待入账</Tag>;
  }
  if (order.paymentStatus === 'CLOSED') {
    return <Tag>已关闭</Tag>;
  }
  if (order.paymentStatus === 'EXCEPTION') {
    return <Tag color="error">异常</Tag>;
  }
  return <Tag color="warning">待支付</Tag>;
};

const MemberWalletPage: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [wallet, setWallet] = useState<WalletSummary | null>(null);
  const [plans, setPlans] = useState<RechargePlan[]>([]);
  const [orders, setOrders] = useState<PageData<RechargeOrder> | null>(null);
  const [transactions, setTransactions] = useState<PageData<WalletTransaction> | null>(null);
  const [ordersPage, setOrdersPage] = useState(1);
  const [txPage, setTxPage] = useState(1);
  const [currentOrder, setCurrentOrder] = useState<RechargeOrder | null>(null);

  const fetchWallet = async () => {
    const data = await walletService.getWallet();
    setWallet(data);
  };

  const fetchOrders = async (page = 1) => {
    const data = await walletService.getRecharges(page, PAGE_SIZE);
    setOrders(data);
    setOrdersPage(page);
  };

  const fetchTransactions = async (page = 1) => {
    const data = await walletService.getTransactions(page, PAGE_SIZE);
    setTransactions(data);
    setTxPage(page);
  };

  const fetchAll = async () => {
    try {
      setLoading(true);
      const [walletData, planData] = await Promise.all([
        walletService.getWallet(),
        walletService.getRechargePlans(),
        fetchOrders(1),
        fetchTransactions(1),
      ]);
      setWallet(walletData);
      setPlans(planData || []);
    } catch (error: any) {
      message.error(error.message || '加载钱包信息失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAll();
  }, []);

  const handleCreateRecharge = async (plan: RechargePlan) => {
    try {
      setActionLoading(true);
      const order = await walletService.createRecharge({ planId: plan.id });
      setCurrentOrder(order);
      message.success('充值单已创建');
      await fetchOrders(1);
    } catch (error: any) {
      message.error(error.message || '创建充值单失败');
    } finally {
      setActionLoading(false);
    }
  };

  const handleMockPay = async () => {
    if (!currentOrder) {
      return;
    }
    try {
      setActionLoading(true);
      await walletService.mockPay(currentOrder.rechargeNo);
      message.success('模拟支付成功，余额已到账');
      setCurrentOrder(null);
      await Promise.all([fetchWallet(), fetchOrders(ordersPage), fetchTransactions(1)]);
    } catch (error: any) {
      message.error(error.message || '模拟支付失败');
    } finally {
      setActionLoading(false);
    }
  };

  const canMockPay = (order: RechargeOrder) =>
    !!order.mockPayEnabled
    && order.channel === 'ONLINE'
    && order.creditStatus !== 'CREDITED'
    && (order.paymentStatus === 'CREATED' || order.paymentStatus === 'PAID');

  const orderColumns: ColumnsType<RechargeOrder> = [
    {
      title: '充值单号',
      dataIndex: 'rechargeNo',
      key: 'rechargeNo',
      width: 220,
      render: (value: string) => <span style={{ fontSize: 12 }}>{value}</span>,
    },
    {
      title: '方案',
      dataIndex: 'planName',
      key: 'planName',
      render: (value?: string) => value || '-',
    },
    {
      title: '本金（元）',
      dataIndex: 'principalAmount',
      key: 'principalAmount',
      render: (value: number) => formatMoney(value),
    },
    {
      title: '赠送（元）',
      dataIndex: 'bonusAmount',
      key: 'bonusAmount',
      render: (value: number) => formatMoney(value),
    },
    {
      title: '渠道',
      dataIndex: 'channel',
      key: 'channel',
      render: (value: string) => (value === 'OFFLINE' ? '线下' : '线上'),
    },
    {
      title: '状态',
      key: 'status',
      render: (_, record) => orderStatusTag(record),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (value: string) => formatDateTime(value),
    },
    {
      title: '操作',
      key: 'action',
      width: 160,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => setCurrentOrder(record)}>
            查看
          </Button>
          {canMockPay(record) && (
            <Button
              type="link"
              size="small"
              loading={actionLoading}
              onClick={() => {
                setCurrentOrder(record);
              }}
            >
              模拟支付
            </Button>
          )}
        </Space>
      ),
    },
  ];

  const transactionColumns: ColumnsType<WalletTransaction> = [
    {
      title: '时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: (value: string) => formatDateTime(value),
    },
    {
      title: '类型',
      dataIndex: 'txType',
      key: 'txType',
      render: (value: string) => (value === 'RECHARGE' ? '充值' : value),
    },
    {
      title: '本金变动（元）',
      dataIndex: 'principalDelta',
      key: 'principalDelta',
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
      render: (value: number) => formatMoney(value),
    },
    {
      title: '变动后赠送',
      dataIndex: 'bonusAfter',
      key: 'bonusAfter',
      render: (value: number) => formatMoney(value),
    },
    {
      title: '说明',
      dataIndex: 'remark',
      key: 'remark',
      render: (value?: string) => value || '-',
    },
  ];

  return (
    <Spin spinning={loading}>
      <Card
        title={
          <span>
            <ArrowLeftOutlined
              onClick={() => navigate('/member')}
              style={{ marginRight: 8, cursor: 'pointer' }}
            />
            我的钱包
          </span>
        }
      >
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="可用余额（元）"
                value={formatMoney(wallet?.availableBalance)}
                prefix={<WalletOutlined style={{ color: '#52c41a', marginRight: 6 }} />}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic title="本金余额（元）" value={formatMoney(wallet?.principalBalance)} />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic title="赠送余额（元）" value={formatMoney(wallet?.bonusBalance)} />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="冻结金额（元）"
                value={formatMoney((wallet?.principalFrozen || 0) + (wallet?.bonusFrozen || 0))}
              />
            </Card>
          </Col>
        </Row>

        {wallet?.mockPayEnabled && (
          <Alert
            style={{ marginTop: 16 }}
            type="info"
            showIcon
            message="线上充值当前为开发环境模拟支付，点击“模拟支付”后余额立即到账；生产环境需接入微信支付。"
          />
        )}

        <Card title="充值方案" style={{ marginTop: 16 }}>
          <Row gutter={[16, 16]}>
            {plans.map((plan) => (
              <Col xs={24} sm={12} lg={8} key={plan.id}>
                <Card hoverable>
                  <div style={{ fontSize: 18, fontWeight: 600 }}>{plan.planName}</div>
                  <div style={{ marginTop: 8 }}>
                    本金 <strong>¥{formatMoney(plan.principalAmount)}</strong>
                    {'  '}赠 <strong style={{ color: '#fa541c' }}>¥{formatMoney(plan.bonusAmount)}</strong>
                  </div>
                  <div style={{ color: '#999', marginTop: 4 }}>
                    赠送余额{plan.bonusValidDays > 0 ? `${plan.bonusValidDays}天有效` : '长期有效'}
                  </div>
                  <div style={{ color: '#999', minHeight: 22 }}>{plan.description || ''}</div>
                  <Button
                    type="primary"
                    block
                    style={{ marginTop: 12 }}
                    loading={actionLoading}
                    onClick={() => handleCreateRecharge(plan)}
                  >
                    立即充值
                  </Button>
                </Card>
              </Col>
            ))}
          </Row>
        </Card>

        <Card style={{ marginTop: 16 }}>
          <Tabs
            items={[
              {
                key: 'orders',
                label: '我的充值单',
                children: (
                  <Table
                    rowKey="id"
                    columns={orderColumns}
                    dataSource={orders?.records || []}
                    locale={{ emptyText: '暂无充值记录' }}
                    scroll={{ x: 1000 }}
                    pagination={{
                      current: ordersPage,
                      pageSize: PAGE_SIZE,
                      total: orders?.total || 0,
                      showSizeChanger: false,
                      onChange: (page) => {
                        fetchOrders(page).catch((error) =>
                          message.error(error.message || '加载充值单失败')
                        );
                      },
                    }}
                  />
                ),
              },
              {
                key: 'transactions',
                label: '资金流水',
                children: (
                  <Table
                    rowKey="id"
                    columns={transactionColumns}
                    dataSource={transactions?.records || []}
                    locale={{ emptyText: '暂无资金流水' }}
                    scroll={{ x: 900 }}
                    pagination={{
                      current: txPage,
                      pageSize: PAGE_SIZE,
                      total: transactions?.total || 0,
                      showSizeChanger: false,
                      onChange: (page) => {
                        fetchTransactions(page).catch((error) =>
                          message.error(error.message || '加载资金流水失败')
                        );
                      },
                    }}
                  />
                ),
              },
            ]}
          />
        </Card>
      </Card>

      <Modal
        title="充值单"
        open={!!currentOrder}
        onCancel={() => setCurrentOrder(null)}
        footer={
          currentOrder && canMockPay(currentOrder)
            ? [
                <Button key="close" onClick={() => setCurrentOrder(null)}>
                  关闭
                </Button>,
                <Button
                  key="pay"
                  type="primary"
                  loading={actionLoading}
                  onClick={handleMockPay}
                >
                  模拟支付并到账
                </Button>,
              ]
            : [
                <Button key="close" type="primary" onClick={() => setCurrentOrder(null)}>
                  关闭
                </Button>,
              ]
        }
      >
        {currentOrder && (
          <div style={{ lineHeight: 2 }}>
            <div>充值单号：{currentOrder.rechargeNo}</div>
            <div>充值方案：{currentOrder.planName || '-'}</div>
            <div>本金：¥{formatMoney(currentOrder.principalAmount)}</div>
            <div>赠送：¥{formatMoney(currentOrder.bonusAmount)}</div>
            <div>状态：{orderStatusTag(currentOrder)}</div>
            <div>创建时间：{formatDateTime(currentOrder.createdAt)}</div>
            {currentOrder.creditStatus !== 'CREDITED' && !currentOrder.mockPayEnabled && (
              <Alert
                style={{ marginTop: 8 }}
                type="warning"
                showIcon
                message="支付渠道未接入，充值单已创建，暂未到账。"
              />
            )}
          </div>
        )}
      </Modal>
    </Spin>
  );
};

export default MemberWalletPage;
