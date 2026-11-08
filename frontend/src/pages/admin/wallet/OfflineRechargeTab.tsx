import React, { useEffect, useState } from 'react';
import {
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  message,
} from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import * as adminWalletService from '@/services/adminWalletService';
import { formatDateTime, formatMoney } from '@/utils/format';
import type { PageData } from '@/types/common';
import type { RechargeOrder, RechargePlan } from '@/types/wallet';

const PAGE_SIZE = 10;

const statusTag = (order: RechargeOrder) => {
  if (order.creditStatus === 'CREDITED') {
    return <Tag color="success">已到账</Tag>;
  }
  if (order.paymentStatus === 'CLOSED') {
    return <Tag>已关闭</Tag>;
  }
  if (order.paymentStatus === 'PAID') {
    return <Tag color="processing">待入账</Tag>;
  }
  return <Tag color="warning">待审批</Tag>;
};

const OfflineRechargeTab: React.FC = () => {
  const [createForm] = Form.useForm();
  const [actionForm] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<PageData<RechargeOrder> | null>(null);
  const [page, setPage] = useState(1);
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);
  const [plans, setPlans] = useState<RechargePlan[]>([]);
  const [createOpen, setCreateOpen] = useState(false);
  const [creating, setCreating] = useState(false);
  const [actionOpen, setActionOpen] = useState(false);
  const [actionType, setActionType] = useState<'approve' | 'reject'>('approve');
  const [actionOrder, setActionOrder] = useState<RechargeOrder | null>(null);
  const [actionSubmitting, setActionSubmitting] = useState(false);

  const fetchOrders = async (current = 1, status = statusFilter) => {
    try {
      setLoading(true);
      const result = await adminWalletService.getOfflineRecharges(current, PAGE_SIZE, status);
      setData(result);
      setPage(current);
    } catch (error: any) {
      message.error(error.message || '加载线下充值失败');
    } finally {
      setLoading(false);
    }
  };

  const fetchPlans = async () => {
    try {
      const list = await adminWalletService.getPlans();
      setPlans((list || []).filter((plan) => plan.status === 1));
    } catch (error: any) {
      message.error(error.message || '加载充值方案失败');
    }
  };

  useEffect(() => {
    fetchOrders(1, undefined);
    fetchPlans();
  }, []);

  const openCreate = () => {
    createForm.resetFields();
    setCreateOpen(true);
  };

  const handleCreate = async () => {
    try {
      const values = await createForm.validateFields();
      setCreating(true);
      await adminWalletService.createOfflineRecharge(values);
      message.success('线下充值申请已创建');
      setCreateOpen(false);
      await fetchOrders(1);
    } catch (error: any) {
      if (error?.errorFields) {
        return;
      }
      message.error(error.message || '创建失败');
    } finally {
      setCreating(false);
    }
  };

  const openAction = (order: RechargeOrder, type: 'approve' | 'reject') => {
    setActionOrder(order);
    setActionType(type);
    setActionOpen(true);
    actionForm.resetFields();
  };

  const handleAction = async () => {
    if (!actionOrder) {
      return;
    }
    try {
      const values = await actionForm.validateFields();
      setActionSubmitting(true);
      if (actionType === 'approve') {
        await adminWalletService.approveOfflineRecharge(actionOrder.rechargeNo, {
          externalTxNo: values.externalTxNo,
        });
        message.success('审批通过，已到账');
      } else {
        await adminWalletService.rejectOfflineRecharge(actionOrder.rechargeNo, {
          rejectReason: values.rejectReason,
        });
        message.success('已驳回');
      }
      setActionOpen(false);
      await fetchOrders(page);
    } catch (error: any) {
      if (error?.errorFields) {
        return;
      }
      message.error(error.message || '操作失败');
    } finally {
      setActionSubmitting(false);
    }
  };

  const columns: ColumnsType<RechargeOrder> = [
    {
      title: '充值单号',
      dataIndex: 'rechargeNo',
      key: 'rechargeNo',
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
      title: '方案',
      dataIndex: 'planName',
      key: 'planName',
      width: 130,
      render: (value?: string) => value || '-',
    },
    {
      title: '本金（元）',
      dataIndex: 'principalAmount',
      key: 'principalAmount',
      width: 110,
      render: (value: number) => formatMoney(value),
    },
    {
      title: '赠送（元）',
      dataIndex: 'bonusAmount',
      key: 'bonusAmount',
      width: 110,
      render: (value: number) => formatMoney(value),
    },
    {
      title: '门店',
      dataIndex: 'storeName',
      key: 'storeName',
      width: 140,
      render: (value?: string) => value || '-',
    },
    {
      title: '状态',
      key: 'status',
      width: 110,
      render: (_, record) => statusTag(record),
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
      fixed: 'right',
      width: 150,
      render: (_, record) => {
        if (record.paymentStatus !== 'CREATED' || record.creditStatus === 'CREDITED') {
          return <span style={{ color: '#999' }}>-</span>;
        }
        return (
          <Space size="small">
            <Button type="link" size="small" onClick={() => openAction(record, 'approve')}>
              审批
            </Button>
            <Button
              type="link"
              size="small"
              danger
              onClick={() => openAction(record, 'reject')}
            >
              驳回
            </Button>
          </Space>
        );
      },
    },
  ];

  return (
    <>
      <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between' }}>
        <Select
          allowClear
          placeholder="全部状态"
          style={{ width: 160 }}
          value={statusFilter}
          onChange={(value) => {
            setStatusFilter(value);
            fetchOrders(1, value);
          }}
          options={[
            { label: '待审批', value: 'CREATED' },
            { label: '待入账', value: 'PAID' },
            { label: '已关闭', value: 'CLOSED' },
          ]}
        />
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          创建线下充值申请
        </Button>
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
          onChange: (nextPage) => fetchOrders(nextPage),
        }}
      />

      <Modal
        title="创建线下充值申请"
        open={createOpen}
        onCancel={() => setCreateOpen(false)}
        onOk={handleCreate}
        confirmLoading={creating}
        okText="创建"
        cancelText="取消"
        destroyOnHidden
      >
        <Form form={createForm} layout="vertical">
          <Form.Item
            label="用户ID"
            name="userId"
            rules={[{ required: true, message: '请输入用户ID' }]}
            tooltip="可在「会员账户」页签查询用户ID"
          >
            <InputNumber min={1} style={{ width: '100%' }} placeholder="如 7" />
          </Form.Item>
          <Form.Item
            label="充值方案"
            name="planId"
            rules={[{ required: true, message: '请选择充值方案' }]}
          >
            <Select
              placeholder="请选择启用中的方案"
              options={plans.map((plan) => ({
                label: `${plan.planName}（本金¥${formatMoney(plan.principalAmount)} 赠¥${formatMoney(plan.bonusAmount)}）`,
                value: plan.id,
              }))}
            />
          </Form.Item>
          <Form.Item
            label="门店名称"
            name="storeName"
            rules={[{ required: true, message: '请输入门店名称' }]}
          >
            <Input placeholder="如 它的世界·XX店" />
          </Form.Item>
          <Form.Item label="备注" name="remark">
            <Input.TextArea rows={2} placeholder="收款方式、凭证说明等" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={actionType === 'approve' ? '审批线下充值' : '驳回线下充值'}
        open={actionOpen}
        onCancel={() => setActionOpen(false)}
        onOk={handleAction}
        confirmLoading={actionSubmitting}
        okText={actionType === 'approve' ? '确认收款并到账' : '确认驳回'}
        cancelText="取消"
        destroyOnHidden
      >
        {actionOrder && (
          <div style={{ marginBottom: 12, color: '#666' }}>
            充值单：{actionOrder.rechargeNo} · 用户：{actionOrder.username || actionOrder.userId} ·
            本金 ¥{formatMoney(actionOrder.principalAmount)} · 赠送 ¥
            {formatMoney(actionOrder.bonusAmount)}
          </div>
        )}
        <Form form={actionForm} layout="vertical">
          {actionType === 'approve' ? (
            <Form.Item label="线下收款凭证号" name="externalTxNo">
              <Input placeholder="可选，如收据号/流水号" />
            </Form.Item>
          ) : (
            <Form.Item label="驳回原因" name="rejectReason">
              <Input.TextArea rows={3} placeholder="可选，默认“管理员驳回”" />
            </Form.Item>
          )}
        </Form>
      </Modal>
    </>
  );
};

export default OfflineRechargeTab;
