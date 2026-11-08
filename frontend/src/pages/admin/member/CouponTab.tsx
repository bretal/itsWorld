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
import * as adminMemberService from '@/services/adminMemberService';
import { centsToYuan, formatDiscount, formatMoney, yuanToCents } from '@/utils/format';
import type { CouponTemplate, CouponTemplateRequest } from '@/types/member';

const CouponTab: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [templates, setTemplates] = useState<CouponTemplate[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<CouponTemplate | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const couponType = Form.useWatch('couponType', form);

  const fetchTemplates = async () => {
    try {
      setLoading(true);
      const data = await adminMemberService.getCouponTemplates();
      setTemplates(data || []);
    } catch (error: any) {
      message.error(error.message || '加载优惠券模板失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTemplates();
  }, []);

  const openCreate = () => {
    setEditing(null);
    setModalOpen(true);
    form.resetFields();
    form.setFieldsValue({
      couponType: 'CASH',
      minAmountYuan: 0,
      totalCount: 100,
      validDays: 30,
      status: 1,
    });
  };

  const openEdit = (template: CouponTemplate) => {
    setEditing(template);
    setModalOpen(true);
    form.setFieldsValue({
      templateName: template.templateName,
      couponType: template.couponType,
      amountYuan:
        template.amountCents !== undefined ? centsToYuan(template.amountCents) : undefined,
      discountRate: template.discountRate,
      minAmountYuan: centsToYuan(template.minAmountCents || 0),
      totalCount: template.totalCount,
      validDays: template.validDays,
      description: template.description,
      status: template.status,
    });
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      const payload: CouponTemplateRequest = {
        templateName: values.templateName,
        couponType: values.couponType,
        amountCents: values.couponType === 'CASH' ? yuanToCents(values.amountYuan) : undefined,
        discountRate: values.couponType === 'DISCOUNT' ? values.discountRate : undefined,
        minAmountCents: yuanToCents(values.minAmountYuan || 0),
        totalCount: values.totalCount,
        validDays: values.validDays,
        description: values.description,
        status: values.status,
      };
      if (editing) {
        await adminMemberService.updateCouponTemplate(editing.id, payload);
        message.success('券模板已更新');
      } else {
        await adminMemberService.createCouponTemplate(payload);
        message.success('券模板已创建');
      }
      setModalOpen(false);
      await fetchTemplates();
    } catch (error: any) {
      if (error?.errorFields) {
        return;
      }
      message.error(error.message || '保存失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDisable = (template: CouponTemplate) => {
    Modal.confirm({
      title: '下架优惠券模板',
      content: `确定下架「${template.templateName}」吗？已发放的券不受影响。`,
      okText: '确定',
      cancelText: '取消',
      okType: 'danger',
      onOk: async () => {
        try {
          await adminMemberService.disableCouponTemplate(template.id);
          message.success('券模板已下架');
          await fetchTemplates();
        } catch (error: any) {
          message.error(error.message || '下架失败');
        }
      },
    });
  };

  const columns: ColumnsType<CouponTemplate> = [
    {
      title: '券名称',
      dataIndex: 'templateName',
      key: 'templateName',
      width: 160,
      render: (value: string, record) => (
        <Space direction="vertical" size={0}>
          <span style={{ fontWeight: 500 }}>{value}</span>
          <span style={{ color: '#999', fontSize: 12 }}>{record.description || '-'}</span>
        </Space>
      ),
    },
    {
      title: '类型',
      dataIndex: 'couponType',
      key: 'couponType',
      width: 90,
      render: (value: string) => (value === 'CASH' ? '现金券' : '折扣券'),
    },
    {
      title: '优惠',
      key: 'value',
      width: 110,
      render: (_, record) =>
        record.couponType === 'CASH'
          ? `¥${formatMoney(record.amountCents)}`
          : formatDiscount(record.discountRate),
    },
    {
      title: '使用门槛',
      key: 'minAmount',
      width: 130,
      render: (_, record) =>
        record.minAmountCents && record.minAmountCents > 0
          ? `满 ¥${formatMoney(record.minAmountCents)}`
          : '无门槛',
    },
    {
      title: '发行/已发',
      key: 'count',
      width: 120,
      render: (_, record) => `${record.issuedCount}/${record.totalCount}`,
    },
    {
      title: '有效天数',
      dataIndex: 'validDays',
      key: 'validDays',
      width: 100,
      render: (value?: number) => (value ? `${value}天` : '长期'),
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
          新增券模板
        </Button>
      </div>
      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={templates}
        pagination={false}
        scroll={{ x: 1000 }}
      />

      <Modal
        title={editing ? `编辑券模板：${editing.templateName}` : '新增券模板'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        okText="保存"
        cancelText="取消"
        width={640}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item
              label="券名称"
              name="templateName"
              rules={[{ required: true, message: '请输入券名称' }]}
              style={{ width: 260 }}
            >
              <Input placeholder="如 新人5元券" />
            </Form.Item>
            <Form.Item label="券类型" name="couponType" style={{ width: 140 }}>
              <Select
                options={[
                  { label: '现金券', value: 'CASH' },
                  { label: '折扣券', value: 'DISCOUNT' },
                ]}
              />
            </Form.Item>
          </Space>
          <Space style={{ display: 'flex' }} align="baseline">
            {couponType === 'DISCOUNT' ? (
              <Form.Item
                label="折扣率（0-1）"
                name="discountRate"
                rules={[{ required: true, message: '请输入折扣率' }]}
                style={{ width: 160 }}
              >
                <InputNumber min={0} max={1} step={0.01} style={{ width: '100%' }} />
              </Form.Item>
            ) : (
              <Form.Item
                label="面额（元）"
                name="amountYuan"
                rules={[{ required: true, message: '请输入券面额' }]}
                style={{ width: 160 }}
              >
                <InputNumber min={0.01} step={0.01} style={{ width: '100%' }} />
              </Form.Item>
            )}
            <Form.Item label="最低消费（元）" name="minAmountYuan" style={{ width: 160 }}>
              <InputNumber min={0} step={0.01} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item
              label="发行总量"
              name="totalCount"
              rules={[{ required: true, message: '请输入发行总量' }]}
              style={{ width: 140 }}
            >
              <InputNumber min={1} style={{ width: '100%' }} />
            </Form.Item>
          </Space>
          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item label="领取后有效天数" name="validDays" style={{ width: 180 }}>
              <InputNumber min={1} style={{ width: '100%' }} placeholder="留空表示长期" />
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
          <Form.Item label="使用说明" name="description">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default CouponTab;
