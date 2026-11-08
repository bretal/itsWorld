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
import { centsToYuan, formatMoney, yuanToCents } from '@/utils/format';
import type { RechargePlan, RechargePlanRequest } from '@/types/wallet';

const RechargePlanTab: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [plans, setPlans] = useState<RechargePlan[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<RechargePlan | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const fetchPlans = async () => {
    try {
      setLoading(true);
      const data = await adminWalletService.getPlans();
      setPlans(data || []);
    } catch (error: any) {
      message.error(error.message || '加载充值方案失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPlans();
  }, []);

  const openCreate = () => {
    setEditing(null);
    setModalOpen(true);
    form.resetFields();
    form.setFieldsValue({
      principalYuan: 100,
      bonusYuan: 10,
      bonusValidDays: 30,
      status: 1,
      sortOrder: 0,
    });
  };

  const openEdit = (plan: RechargePlan) => {
    setEditing(plan);
    setModalOpen(true);
    form.setFieldsValue({
      planName: plan.planName,
      principalYuan: centsToYuan(plan.principalAmount),
      bonusYuan: centsToYuan(plan.bonusAmount),
      bonusValidDays: plan.bonusValidDays,
      description: plan.description,
      status: plan.status,
      sortOrder: plan.sortOrder ?? 0,
    });
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      const payload: RechargePlanRequest = {
        planName: values.planName,
        principalAmount: yuanToCents(values.principalYuan),
        bonusAmount: yuanToCents(values.bonusYuan || 0),
        bonusValidDays: values.bonusValidDays ?? 0,
        description: values.description,
        status: values.status,
        sortOrder: values.sortOrder,
      };
      if (editing) {
        await adminWalletService.updatePlan(editing.id, payload);
        message.success('充值方案已更新');
      } else {
        await adminWalletService.createPlan(payload);
        message.success('充值方案已创建');
      }
      setModalOpen(false);
      await fetchPlans();
    } catch (error: any) {
      if (error?.errorFields) {
        return;
      }
      message.error(error.message || '保存失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDisable = (plan: RechargePlan) => {
    Modal.confirm({
      title: '下架充值方案',
      content: `确定下架「${plan.planName}」吗？历史充值单仍按原方案快照结算。`,
      okText: '确定',
      cancelText: '取消',
      okType: 'danger',
      onOk: async () => {
        try {
          await adminWalletService.disablePlan(plan.id);
          message.success('充值方案已下架');
          await fetchPlans();
        } catch (error: any) {
          message.error(error.message || '下架失败');
        }
      },
    });
  };

  const columns: ColumnsType<RechargePlan> = [
    {
      title: '方案名称',
      dataIndex: 'planName',
      key: 'planName',
      width: 180,
      render: (value: string, record) => (
        <Space direction="vertical" size={0}>
          <span style={{ fontWeight: 500 }}>{value}</span>
          <span style={{ color: '#999', fontSize: 12 }}>{record.description || '-'}</span>
        </Space>
      ),
    },
    {
      title: '本金（元）',
      dataIndex: 'principalAmount',
      key: 'principalAmount',
      width: 120,
      render: (value: number) => formatMoney(value),
    },
    {
      title: '赠送（元）',
      dataIndex: 'bonusAmount',
      key: 'bonusAmount',
      width: 120,
      render: (value: number) => formatMoney(value),
    },
    {
      title: '赠送有效期',
      dataIndex: 'bonusValidDays',
      key: 'bonusValidDays',
      width: 120,
      render: (value: number) => (value > 0 ? `${value}天` : '长期'),
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      key: 'sortOrder',
      width: 80,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 90,
      render: (value: number) =>
        value === 1 ? <Tag color="success">启用</Tag> : <Tag>停用</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      width: 140,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => openEdit(record)}>
            编辑
          </Button>
          {record.status === 1 && (
            <Button type="link" size="small" danger onClick={() => handleDisable(record)}>
              下架
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <>
      <div style={{ marginBottom: 12, textAlign: 'right' }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          新增充值方案
        </Button>
      </div>
      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={plans}
        pagination={false}
        scroll={{ x: 900 }}
      />

      <Modal
        title={editing ? `编辑充值方案：${editing.planName}` : '新增充值方案'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        okText="保存"
        cancelText="取消"
        width={620}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item
              label="方案名称"
              name="planName"
              rules={[{ required: true, message: '请输入方案名称' }]}
              style={{ width: 260 }}
            >
              <Input placeholder="如 充500赠80" />
            </Form.Item>
            <Form.Item label="状态" name="status" style={{ width: 120 }}>
              <Select
                options={[
                  { label: '启用', value: 1 },
                  { label: '停用', value: 0 },
                ]}
              />
            </Form.Item>
          </Space>
          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item
              label="本金（元）"
              name="principalYuan"
              rules={[{ required: true, message: '请输入本金金额' }]}
              style={{ width: 160 }}
            >
              <InputNumber min={0.01} step={0.01} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="赠送（元）" name="bonusYuan" style={{ width: 160 }}>
              <InputNumber min={0} step={0.01} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="赠送有效天数" name="bonusValidDays" style={{ width: 160 }}>
              <InputNumber min={0} style={{ width: '100%' }} placeholder="0表示长期" />
            </Form.Item>
            <Form.Item label="排序" name="sortOrder" style={{ width: 100 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
          </Space>
          <Form.Item label="方案说明" name="description">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default RechargePlanTab;
