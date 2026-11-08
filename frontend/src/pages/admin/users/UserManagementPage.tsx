import React, { useEffect, useState } from 'react';
import {
  Button,
  Card,
  Descriptions,
  Drawer,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  message,
} from 'antd';
import {
  CheckCircleOutlined,
  EyeOutlined,
  GiftOutlined,
  StopOutlined,
  WalletOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import * as adminMemberService from '@/services/adminMemberService';
import * as adminWalletService from '@/services/adminWalletService';
import {
  formatDateTime,
  formatDiscount,
  formatMoney,
  formatMoneyDelta,
  yuanToCents,
} from '@/utils/format';
import type { PageData } from '@/types/common';
import type {
  AdminMemberAccount,
  Coupon,
  CouponTemplate,
  MemberGrowthLog,
  MemberPointsLog,
} from '@/types/member';
import type { RechargeOrder, WalletTransaction } from '@/types/wallet';

const PAGE_SIZE = 10;

const SOURCE_LABELS: Record<string, string> = {
  TASK: '任务奖励',
  GIFT: '礼包奖励',
  MANUAL: '人工调整',
};

const TX_TYPE_LABELS: Record<string, string> = {
  RECHARGE: '充值',
  ADJUSTMENT: '调账',
  PAYMENT: '消费',
  REFUND: '退款',
};

const COUPON_STATUS_LABELS: Record<string, { text: string; color: string }> = {
  AVAILABLE: { text: '可用', color: 'success' },
  LOCKED: { text: '锁定中', color: 'warning' },
  USED: { text: '已使用', color: 'default' },
  EXPIRED: { text: '已过期', color: 'error' },
};

const rechargeStatusTag = (order: RechargeOrder) => {
  if (order.creditStatus === 'CREDITED') {
    return <Tag color="success">已到账</Tag>;
  }
  if (order.paymentStatus === 'CLOSED') {
    return <Tag>已关闭</Tag>;
  }
  if (order.paymentStatus === 'PAID') {
    return <Tag color="processing">待入账</Tag>;
  }
  return <Tag color="warning">待支付</Tag>;
};

const couponValue = (coupon: Coupon) =>
  coupon.couponType === 'CASH'
    ? `¥${formatMoney(coupon.amountCents)}`
    : formatDiscount(coupon.discountRate);

const UserManagementPage: React.FC = () => {
  const [adjustForm] = Form.useForm();
  const [issueForm] = Form.useForm();

  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<PageData<AdminMemberAccount> | null>(null);
  const [page, setPage] = useState(1);
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState<number | undefined>(undefined);
  const [roleFilter, setRoleFilter] = useState<string | undefined>(undefined);

  const [detailOpen, setDetailOpen] = useState(false);
  const [detailUser, setDetailUser] = useState<AdminMemberAccount | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [growthData, setGrowthData] = useState<PageData<MemberGrowthLog> | null>(null);
  const [pointsData, setPointsData] = useState<PageData<MemberPointsLog> | null>(null);
  const [txData, setTxData] = useState<PageData<WalletTransaction> | null>(null);
  const [rechargeData, setRechargeData] = useState<PageData<RechargeOrder> | null>(null);
  const [coupons, setCoupons] = useState<Coupon[]>([]);
  const [growthPage, setGrowthPage] = useState(1);
  const [pointsPage, setPointsPage] = useState(1);
  const [txPage, setTxPage] = useState(1);
  const [rechargePage, setRechargePage] = useState(1);

  const [adjustOpen, setAdjustOpen] = useState(false);
  const [adjustUser, setAdjustUser] = useState<AdminMemberAccount | null>(null);
  const [adjustSubmitting, setAdjustSubmitting] = useState(false);

  const [issueOpen, setIssueOpen] = useState(false);
  const [issueUser, setIssueUser] = useState<AdminMemberAccount | null>(null);
  const [templates, setTemplates] = useState<CouponTemplate[]>([]);
  const [issueSubmitting, setIssueSubmitting] = useState(false);

  const fetchAccounts = async (
    current = 1,
    searchKeyword = keyword,
    status = statusFilter,
    role = roleFilter
  ) => {
    try {
      setLoading(true);
      const result = await adminMemberService.getAccounts(
        current,
        PAGE_SIZE,
        searchKeyword,
        status,
        role
      );
      setData(result);
      setPage(current);
    } catch (error: any) {
      message.error(error.message || '加载用户列表失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAccounts(1);
  }, [statusFilter, roleFilter]);

  const handleSearch = (value: string) => {
    setKeyword(value);
    setPage(1);
    fetchAccounts(1, value, statusFilter, roleFilter);
  };

  const refresh = () => fetchAccounts(page);

  const handleToggleStatus = (user: AdminMemberAccount) => {
    const nextStatus = user.status === 1 ? 0 : 1;
    Modal.confirm({
      title: nextStatus === 0 ? '禁用用户' : '启用用户',
      content:
        nextStatus === 0
          ? `确定禁用「${user.nickName || user.username}」吗？禁用后该用户无法登录小程序/接口。`
          : `确定恢复「${user.nickName || user.username}」的账号吗？`,
      okText: '确定',
      cancelText: '取消',
      okType: nextStatus === 0 ? 'danger' : 'primary',
      onOk: async () => {
        try {
          await adminMemberService.updateUserStatus(user.userId, { status: nextStatus });
          message.success(nextStatus === 0 ? '用户已禁用' : '用户已启用');
          await refresh();
        } catch (error: any) {
          message.error(error.message || '状态更新失败');
        }
      },
    });
  };

  const openDetail = async (user: AdminMemberAccount) => {
    setDetailUser(user);
    setDetailOpen(true);
    setGrowthPage(1);
    setPointsPage(1);
    setTxPage(1);
    setRechargePage(1);
    try {
      setDetailLoading(true);
      const [growth, points, txs, recharges, couponList] = await Promise.all([
        adminMemberService.getUserGrowthLogs(user.userId, 1, PAGE_SIZE),
        adminMemberService.getUserPointsLogs(user.userId, 1, PAGE_SIZE),
        adminMemberService.getUserTransactions(user.userId, 1, PAGE_SIZE),
        adminMemberService.getUserRecharges(user.userId, 1, PAGE_SIZE),
        adminMemberService.getUserCoupons(user.userId),
      ]);
      setGrowthData(growth);
      setPointsData(points);
      setTxData(txs);
      setRechargeData(recharges);
      setCoupons(couponList || []);
    } catch (error: any) {
      message.error(error.message || '加载用户详情失败');
    } finally {
      setDetailLoading(false);
    }
  };

  const fetchGrowth = async (current: number) => {
    if (!detailUser) {
      return;
    }
    try {
      setDetailLoading(true);
      const result = await adminMemberService.getUserGrowthLogs(detailUser.userId, current, PAGE_SIZE);
      setGrowthData(result);
      setGrowthPage(current);
    } catch (error: any) {
      message.error(error.message || '加载成长值流水失败');
    } finally {
      setDetailLoading(false);
    }
  };

  const fetchPoints = async (current: number) => {
    if (!detailUser) {
      return;
    }
    try {
      setDetailLoading(true);
      const result = await adminMemberService.getUserPointsLogs(detailUser.userId, current, PAGE_SIZE);
      setPointsData(result);
      setPointsPage(current);
    } catch (error: any) {
      message.error(error.message || '加载积分流水失败');
    } finally {
      setDetailLoading(false);
    }
  };

  const fetchTransactions = async (current: number) => {
    if (!detailUser) {
      return;
    }
    try {
      setDetailLoading(true);
      const result = await adminMemberService.getUserTransactions(detailUser.userId, current, PAGE_SIZE);
      setTxData(result);
      setTxPage(current);
    } catch (error: any) {
      message.error(error.message || '加载钱包流水失败');
    } finally {
      setDetailLoading(false);
    }
  };

  const fetchRecharges = async (current: number) => {
    if (!detailUser) {
      return;
    }
    try {
      setDetailLoading(true);
      const result = await adminMemberService.getUserRecharges(detailUser.userId, current, PAGE_SIZE);
      setRechargeData(result);
      setRechargePage(current);
    } catch (error: any) {
      message.error(error.message || '加载充值单失败');
    } finally {
      setDetailLoading(false);
    }
  };

  const openAdjust = (user: AdminMemberAccount) => {
    setAdjustUser(user);
    setAdjustOpen(true);
    adjustForm.resetFields();
    adjustForm.setFieldsValue({ principalDeltaYuan: 0, bonusDeltaYuan: 0 });
  };

  const handleAdjust = async () => {
    if (!adjustUser) {
      return;
    }
    try {
      const values = await adjustForm.validateFields();
      const principalDelta = yuanToCents(values.principalDeltaYuan || 0);
      const bonusDelta = yuanToCents(values.bonusDeltaYuan || 0);
      if (principalDelta === 0 && bonusDelta === 0) {
        message.warning('本金/赠送变动不能同时为 0');
        return;
      }
      setAdjustSubmitting(true);
      await adminWalletService.adjustBalance({
        userId: adjustUser.userId,
        principalDelta,
        bonusDelta,
        reason: values.reason,
      });
      message.success('调账成功');
      setAdjustOpen(false);
      await refresh();
    } catch (error: any) {
      if (error?.errorFields) {
        return;
      }
      message.error(error.message || '调账失败');
    } finally {
      setAdjustSubmitting(false);
    }
  };

  const openIssue = async (user: AdminMemberAccount) => {
    setIssueUser(user);
    setIssueOpen(true);
    issueForm.resetFields();
    issueForm.setFieldsValue({ quantity: 1 });
    try {
      const list = await adminMemberService.getCouponTemplates();
      setTemplates((list || []).filter((item) => item.status === 1));
    } catch (error: any) {
      message.error(error.message || '加载优惠券模板失败');
    }
  };

  const handleIssue = async () => {
    if (!issueUser) {
      return;
    }
    try {
      const values = await issueForm.validateFields();
      setIssueSubmitting(true);
      await adminMemberService.issueCoupons({
        userId: issueUser.userId,
        templateId: values.templateId,
        quantity: values.quantity,
      });
      message.success('发券成功');
      setIssueOpen(false);
    } catch (error: any) {
      if (error?.errorFields) {
        return;
      }
      message.error(error.message || '发券失败');
    } finally {
      setIssueSubmitting(false);
    }
  };

  const columns: ColumnsType<AdminMemberAccount> = [
    {
      title: '用户',
      key: 'user',
      fixed: 'left',
      width: 160,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <span style={{ fontWeight: 500 }}>{record.nickName || record.username}</span>
          <span style={{ color: '#999', fontSize: 12 }}>@{record.username}</span>
        </Space>
      ),
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      key: 'phone',
      width: 130,
      render: (value?: string) => value || '-',
    },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      width: 90,
      render: (value?: string) =>
        value === 'ADMIN' ? <Tag color="gold">管理员</Tag> : <Tag>用户</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 90,
      render: (value?: number) =>
        value === 0 ? <Tag color="error">已禁用</Tag> : <Tag color="success">启用</Tag>,
    },
    {
      title: '会员等级',
      dataIndex: 'levelName',
      key: 'levelName',
      width: 110,
      render: (value?: string) => value || <span style={{ color: '#999' }}>未开通</span>,
    },
    {
      title: '成长/积分',
      key: 'memberValue',
      width: 120,
      render: (_, record) => `${record.growthValue} / ${record.pointsBalance}`,
    },
    {
      title: '本金（元）',
      dataIndex: 'principalBalance',
      key: 'principalBalance',
      width: 110,
      render: (value: number) => formatMoney(value),
    },
    {
      title: '赠送（元）',
      dataIndex: 'bonusBalance',
      key: 'bonusBalance',
      width: 110,
      render: (value: number) => formatMoney(value),
    },
    {
      title: '可用（元）',
      dataIndex: 'availableBalance',
      key: 'availableBalance',
      width: 110,
      render: (value: number) => formatMoney(value),
    },
    {
      title: '注册时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (value: string) => formatDateTime(value),
    },
    {
      title: '最后登录',
      dataIndex: 'lastLoginAt',
      key: 'lastLoginAt',
      width: 160,
      render: (value?: string) => formatDateTime(value),
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 260,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => openDetail(record)}>
            详情
          </Button>
          <Button
            type="link"
            size="small"
            icon={<WalletOutlined />}
            onClick={() => openAdjust(record)}
          >
            调账
          </Button>
          <Button
            type="link"
            size="small"
            icon={<GiftOutlined />}
            onClick={() => openIssue(record)}
          >
            发券
          </Button>
          <Button
            type="link"
            size="small"
            danger={record.status === 1}
            icon={record.status === 1 ? <StopOutlined /> : <CheckCircleOutlined />}
            onClick={() => handleToggleStatus(record)}
          >
            {record.status === 1 ? '禁用' : '启用'}
          </Button>
        </Space>
      ),
    },
  ];

  const growthColumns: ColumnsType<MemberGrowthLog> = [
    {
      title: '时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (value: string) => formatDateTime(value),
    },
    {
      title: '变动',
      dataIndex: 'changeValue',
      key: 'changeValue',
      width: 90,
      render: (value: number) => (
        <span style={{ color: value >= 0 ? '#52c41a' : '#f5222d' }}>
          {value > 0 ? `+${value}` : value}
        </span>
      ),
    },
    {
      title: '变动后',
      dataIndex: 'balanceAfter',
      key: 'balanceAfter',
      width: 90,
    },
    {
      title: '来源',
      dataIndex: 'sourceType',
      key: 'sourceType',
      width: 110,
      render: (value: string) => SOURCE_LABELS[value] || value,
    },
    {
      title: '说明',
      dataIndex: 'remark',
      key: 'remark',
      render: (value?: string) => value || '-',
    },
  ];

  const pointsColumns: ColumnsType<MemberPointsLog> = growthColumns as ColumnsType<MemberPointsLog>;

  const txColumns: ColumnsType<WalletTransaction> = [
    {
      title: '时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (value: string) => formatDateTime(value),
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
      title: '变动后本金/赠送',
      key: 'after',
      width: 160,
      render: (_, record) =>
        `¥${formatMoney(record.principalAfter)} / ¥${formatMoney(record.bonusAfter)}`,
    },
    {
      title: '说明',
      dataIndex: 'remark',
      key: 'remark',
      render: (value?: string) => value || '-',
    },
  ];

  const rechargeColumns: ColumnsType<RechargeOrder> = [
    {
      title: '充值单号',
      dataIndex: 'rechargeNo',
      key: 'rechargeNo',
      width: 210,
      render: (value: string) => <span style={{ fontSize: 12 }}>{value}</span>,
    },
    {
      title: '方案',
      dataIndex: 'planName',
      key: 'planName',
      width: 120,
      render: (value?: string) => value || '-',
    },
    {
      title: '本金/赠送',
      key: 'amount',
      width: 140,
      render: (_, record) =>
        `¥${formatMoney(record.principalAmount)} / ¥${formatMoney(record.bonusAmount)}`,
    },
    {
      title: '渠道',
      dataIndex: 'channel',
      key: 'channel',
      width: 80,
      render: (value: string) => (value === 'OFFLINE' ? '线下' : '线上'),
    },
    {
      title: '状态',
      key: 'status',
      width: 100,
      render: (_, record) => rechargeStatusTag(record),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (value: string) => formatDateTime(value),
    },
  ];

  const couponColumns: ColumnsType<Coupon> = [
    {
      title: '优惠券',
      dataIndex: 'templateName',
      key: 'templateName',
      width: 160,
      render: (value: string, record) => (
        <Space direction="vertical" size={0}>
          <span style={{ fontWeight: 500 }}>{value}</span>
          <span style={{ color: '#fa541c' }}>{couponValue(record)}</span>
        </Space>
      ),
    },
    {
      title: '使用条件',
      key: 'condition',
      width: 140,
      render: (_, record) =>
        record.minAmountCents && record.minAmountCents > 0
          ? `满 ¥${formatMoney(record.minAmountCents)}`
          : '无门槛',
    },
    {
      title: '有效期',
      key: 'validity',
      width: 220,
      render: (_, record) =>
        `${formatDateTime(record.validFrom)} 至 ${record.validTo ? formatDateTime(record.validTo) : '长期'}`,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (value: string) => {
        const status = COUPON_STATUS_LABELS[value] || { text: value, color: 'default' };
        return <Tag color={status.color}>{status.text}</Tag>;
      },
    },
  ];

  return (
    <>
      <Card title="用户管理">
        <div
          style={{
            marginBottom: 12,
            display: 'flex',
            gap: 12,
            flexWrap: 'wrap',
            justifyContent: 'space-between',
          }}
        >
          <Space wrap>
            <Input.Search
              allowClear
              placeholder="用户名 / 昵称 / 手机号"
              style={{ width: 260 }}
              onSearch={handleSearch}
            />
            <Select
              allowClear
              placeholder="全部状态"
              style={{ width: 130 }}
              value={statusFilter}
              onChange={(value) => {
                setStatusFilter(value);
                setPage(1);
              }}
              options={[
                { label: '启用', value: 1 },
                { label: '禁用', value: 0 },
              ]}
            />
            <Select
              allowClear
              placeholder="全部角色"
              style={{ width: 130 }}
              value={roleFilter}
              onChange={(value) => {
                setRoleFilter(value);
                setPage(1);
              }}
              options={[
                { label: '普通用户', value: 'USER' },
                { label: '管理员', value: 'ADMIN' },
              ]}
            />
          </Space>
        </div>

        <Table
          rowKey="userId"
          loading={loading}
          columns={columns}
          dataSource={data?.records || []}
          scroll={{ x: 1650 }}
          pagination={{
            current: page,
            pageSize: PAGE_SIZE,
            total: data?.total || 0,
            showSizeChanger: false,
            onChange: (nextPage) => fetchAccounts(nextPage),
          }}
        />
      </Card>

      <Drawer
        title={`用户详情：${detailUser?.nickName || detailUser?.username || ''}`}
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        width={960}
        destroyOnHidden
      >
        {detailUser && (
          <Tabs
            items={[
              {
                key: 'overview',
                label: '概览',
                children: (
                  <Descriptions column={2} bordered size="small">
                    <Descriptions.Item label="用户名">{detailUser.username}</Descriptions.Item>
                    <Descriptions.Item label="昵称">{detailUser.nickName || '-'}</Descriptions.Item>
                    <Descriptions.Item label="手机号">{detailUser.phone || '-'}</Descriptions.Item>
                    <Descriptions.Item label="邮箱">{detailUser.email || '-'}</Descriptions.Item>
                    <Descriptions.Item label="角色">
                      {detailUser.role === 'ADMIN' ? '管理员' : '普通用户'}
                    </Descriptions.Item>
                    <Descriptions.Item label="状态">
                      {detailUser.status === 0 ? (
                        <Tag color="error">已禁用</Tag>
                      ) : (
                        <Tag color="success">启用</Tag>
                      )}
                    </Descriptions.Item>
                    <Descriptions.Item label="会员等级">
                      {detailUser.levelName || '未开通'}
                    </Descriptions.Item>
                    <Descriptions.Item label="成长值/积分">
                      {detailUser.growthValue} / {detailUser.pointsBalance}
                    </Descriptions.Item>
                    <Descriptions.Item label="本金余额">
                      ¥{formatMoney(detailUser.principalBalance)}
                    </Descriptions.Item>
                    <Descriptions.Item label="赠送余额">
                      ¥{formatMoney(detailUser.bonusBalance)}
                    </Descriptions.Item>
                    <Descriptions.Item label="可用余额">
                      ¥{formatMoney(detailUser.availableBalance)}
                    </Descriptions.Item>
                    <Descriptions.Item label="注册时间">
                      {formatDateTime(detailUser.createdAt)}
                    </Descriptions.Item>
                    <Descriptions.Item label="最后登录">
                      {formatDateTime(detailUser.lastLoginAt)}
                    </Descriptions.Item>
                  </Descriptions>
                ),
              },
              {
                key: 'growth',
                label: '成长值流水',
                children: (
                  <Table
                    rowKey="id"
                    loading={detailLoading}
                    columns={growthColumns}
                    dataSource={growthData?.records || []}
                    locale={{ emptyText: '暂无成长值流水' }}
                    pagination={{
                      current: growthPage,
                      pageSize: PAGE_SIZE,
                      total: growthData?.total || 0,
                      showSizeChanger: false,
                      onChange: fetchGrowth,
                    }}
                  />
                ),
              },
              {
                key: 'points',
                label: '积分流水',
                children: (
                  <Table
                    rowKey="id"
                    loading={detailLoading}
                    columns={pointsColumns}
                    dataSource={pointsData?.records || []}
                    locale={{ emptyText: '暂无积分流水' }}
                    pagination={{
                      current: pointsPage,
                      pageSize: PAGE_SIZE,
                      total: pointsData?.total || 0,
                      showSizeChanger: false,
                      onChange: fetchPoints,
                    }}
                  />
                ),
              },
              {
                key: 'transactions',
                label: '钱包流水',
                children: (
                  <Table
                    rowKey="id"
                    loading={detailLoading}
                    columns={txColumns}
                    dataSource={txData?.records || []}
                    locale={{ emptyText: '暂无钱包流水' }}
                    pagination={{
                      current: txPage,
                      pageSize: PAGE_SIZE,
                      total: txData?.total || 0,
                      showSizeChanger: false,
                      onChange: fetchTransactions,
                    }}
                  />
                ),
              },
              {
                key: 'recharges',
                label: '充值单',
                children: (
                  <Table
                    rowKey="id"
                    loading={detailLoading}
                    columns={rechargeColumns}
                    dataSource={rechargeData?.records || []}
                    locale={{ emptyText: '暂无充值单' }}
                    pagination={{
                      current: rechargePage,
                      pageSize: PAGE_SIZE,
                      total: rechargeData?.total || 0,
                      showSizeChanger: false,
                      onChange: fetchRecharges,
                    }}
                  />
                ),
              },
              {
                key: 'coupons',
                label: '优惠券',
                children: (
                  <Table
                    rowKey="id"
                    loading={detailLoading}
                    columns={couponColumns}
                    dataSource={coupons}
                    locale={{ emptyText: '暂无优惠券' }}
                    pagination={{ pageSize: 10, showSizeChanger: false }}
                  />
                ),
              },
            ]}
          />
        )}
      </Drawer>

      <Modal
        title={`调整余额：${adjustUser?.nickName || adjustUser?.username || ''}`}
        open={adjustOpen}
        onCancel={() => setAdjustOpen(false)}
        onOk={handleAdjust}
        confirmLoading={adjustSubmitting}
        okText="确认调账"
        cancelText="取消"
        destroyOnHidden
      >
        {adjustUser && (
          <div style={{ marginBottom: 12, color: '#666' }}>
            当前余额：本金 ¥{formatMoney(adjustUser.principalBalance)} / 赠送 ¥
            {formatMoney(adjustUser.bonusBalance)}
          </div>
        )}
        <Form form={adjustForm} layout="vertical">
          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item label="本金变动（元，可负）" name="principalDeltaYuan" style={{ width: 200 }}>
              <InputNumber step={0.01} style={{ width: '100%' }} placeholder="正数调增，负数调减" />
            </Form.Item>
            <Form.Item label="赠送变动（元，可负）" name="bonusDeltaYuan" style={{ width: 200 }}>
              <InputNumber step={0.01} style={{ width: '100%' }} placeholder="正数调增，负数调减" />
            </Form.Item>
          </Space>
          <Form.Item
            label="调账原因"
            name="reason"
            rules={[{ required: true, message: '请填写调账原因' }]}
          >
            <Input.TextArea rows={3} placeholder="如：线下补偿、纠错、活动赠送" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={`发券给 ${issueUser?.nickName || issueUser?.username || ''}`}
        open={issueOpen}
        onCancel={() => setIssueOpen(false)}
        onOk={handleIssue}
        confirmLoading={issueSubmitting}
        okText="确认发放"
        cancelText="取消"
        destroyOnHidden
      >
        <Form form={issueForm} layout="vertical">
          <Form.Item
            label="优惠券模板"
            name="templateId"
            rules={[{ required: true, message: '请选择优惠券模板' }]}
          >
            <Select
              placeholder="请选择启用中的模板"
              options={templates.map((template) => ({
                label: `${template.templateName}（剩余 ${template.totalCount - template.issuedCount} 张）`,
                value: template.id,
              }))}
            />
          </Form.Item>
          <Form.Item
            label="发放数量"
            name="quantity"
            rules={[{ required: true, message: '请输入发放数量' }]}
          >
            <InputNumber min={1} max={100} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default UserManagementPage;
