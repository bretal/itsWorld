import React, { useEffect, useState } from 'react';
import {
  Button,
  DatePicker,
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
import dayjs from 'dayjs';
import type { ColumnsType } from 'antd/es/table';
import * as adminMemberService from '@/services/adminMemberService';
import { formatDateTime } from '@/utils/format';
import type {
  CouponTemplate,
  MemberGiftEntity,
  MemberGiftRequest,
  MemberLevel,
} from '@/types/member';

const GiftTab: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [gifts, setGifts] = useState<MemberGiftEntity[]>([]);
  const [levels, setLevels] = useState<MemberLevel[]>([]);
  const [templates, setTemplates] = useState<CouponTemplate[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<MemberGiftEntity | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [giftData, levelData, templateData] = await Promise.all([
        adminMemberService.getGifts(),
        adminMemberService.getLevels(),
        adminMemberService.getCouponTemplates(),
      ]);
      setGifts(giftData || []);
      setLevels(levelData || []);
      setTemplates(templateData || []);
    } catch (error: any) {
      message.error(error.message || '加载礼包失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const levelNameMap = new Map<number, string>();
  levels.forEach((level) => levelNameMap.set(level.id, level.levelName));
  const templateNameMap = new Map<number, string>();
  templates.forEach((template) => templateNameMap.set(template.id, template.templateName));

  const openCreate = () => {
    setEditing(null);
    setModalOpen(true);
    form.resetFields();
    form.setFieldsValue({
      pointsReward: 0,
      status: 1,
      sortOrder: 0,
    });
  };

  const openEdit = (gift: MemberGiftEntity) => {
    setEditing(gift);
    setModalOpen(true);
    form.setFieldsValue({
      giftName: gift.giftName,
      description: gift.description,
      requiredLevelId: gift.requiredLevelId,
      pointsReward: gift.pointsReward,
      couponTemplateId: gift.couponTemplateId,
      startAt: gift.startAt ? dayjs(gift.startAt) : undefined,
      endAt: gift.endAt ? dayjs(gift.endAt) : undefined,
      status: gift.status,
      sortOrder: gift.sortOrder ?? 0,
    });
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      const payload: MemberGiftRequest = {
        giftName: values.giftName,
        description: values.description,
        requiredLevelId: values.requiredLevelId,
        pointsReward: values.pointsReward,
        couponTemplateId: values.couponTemplateId,
        startAt: values.startAt ? values.startAt.format('YYYY-MM-DDTHH:mm:ss') : undefined,
        endAt: values.endAt ? values.endAt.format('YYYY-MM-DDTHH:mm:ss') : undefined,
        status: values.status,
        sortOrder: values.sortOrder,
      };
      if (editing) {
        await adminMemberService.updateGift(editing.id, payload);
        message.success('礼包已更新');
      } else {
        await adminMemberService.createGift(payload);
        message.success('礼包已创建');
      }
      setModalOpen(false);
      await fetchData();
    } catch (error: any) {
      if (error?.errorFields) {
        return;
      }
      message.error(error.message || '保存失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDisable = (gift: MemberGiftEntity) => {
    Modal.confirm({
      title: '下架礼包',
      content: `确定下架「${gift.giftName}」吗？已领取记录不受影响。`,
      okText: '确定',
      cancelText: '取消',
      okType: 'danger',
      onOk: async () => {
        try {
          await adminMemberService.disableGift(gift.id);
          message.success('礼包已下架');
          await fetchData();
        } catch (error: any) {
          message.error(error.message || '下架失败');
        }
      },
    });
  };

  const columns: ColumnsType<MemberGiftEntity> = [
    {
      title: '礼包',
      dataIndex: 'giftName',
      key: 'giftName',
      width: 180,
      render: (value: string, record) => (
        <Space direction="vertical" size={0}>
          <span style={{ fontWeight: 500 }}>{value}</span>
          <span style={{ color: '#999', fontSize: 12 }}>{record.description || '-'}</span>
        </Space>
      ),
    },
    {
      title: '所需等级',
      dataIndex: 'requiredLevelId',
      key: 'requiredLevelId',
      width: 120,
      render: (value?: number) =>
        value ? levelNameMap.get(value) || `等级#${value}` : '不限',
    },
    {
      title: '积分奖励',
      dataIndex: 'pointsReward',
      key: 'pointsReward',
      width: 100,
    },
    {
      title: '附带优惠券',
      dataIndex: 'couponTemplateId',
      key: 'couponTemplateId',
      width: 150,
      render: (value?: number) =>
        value ? templateNameMap.get(value) || `券#${value}` : '-',
    },
    {
      title: '有效期',
      key: 'validity',
      width: 260,
      render: (_, record) => (
        <span style={{ fontSize: 12, color: '#666' }}>
          {record.startAt ? formatDateTime(record.startAt) : '不限'} 至{' '}
          {record.endAt ? formatDateTime(record.endAt) : '不限'}
        </span>
      ),
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
          新增礼包
        </Button>
      </div>
      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={gifts}
        pagination={false}
        scroll={{ x: 1050 }}
      />

      <Modal
        title={editing ? `编辑礼包：${editing.giftName}` : '新增礼包'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        okText="保存"
        cancelText="取消"
        width={680}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item
              label="礼包名称"
              name="giftName"
              rules={[{ required: true, message: '请输入礼包名称' }]}
              style={{ width: 240 }}
            >
              <Input placeholder="如 新人礼包" />
            </Form.Item>
            <Form.Item label="所需等级" name="requiredLevelId" style={{ width: 180 }}>
              <Select
                allowClear
                placeholder="不限等级"
                options={levels
                  .filter((level) => level.status === 1)
                  .map((level) => ({
                    label: level.levelName,
                    value: level.id,
                  }))}
              />
            </Form.Item>
          </Space>
          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item label="积分奖励" name="pointsReward" style={{ width: 150 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="附带优惠券" name="couponTemplateId" style={{ width: 220 }}>
              <Select
                allowClear
                placeholder="不附带"
                options={templates
                  .filter((template) => template.status === 1)
                  .map((template) => ({
                    label: template.templateName,
                    value: template.id,
                  }))}
              />
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
            <Form.Item label="开始时间" name="startAt" style={{ width: 220 }}>
              <DatePicker showTime style={{ width: '100%' }} placeholder="留空表示不限" />
            </Form.Item>
            <Form.Item label="结束时间" name="endAt" style={{ width: 220 }}>
              <DatePicker showTime style={{ width: '100%' }} placeholder="留空表示不限" />
            </Form.Item>
            <Form.Item label="排序" name="sortOrder" style={{ width: 100 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
          </Space>
          <Form.Item label="礼包说明" name="description">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default GiftTab;
