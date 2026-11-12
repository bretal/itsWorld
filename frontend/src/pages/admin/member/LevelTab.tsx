import ImageUpload from '@/components/ImageUpload';
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
import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import * as adminMemberService from '@/services/adminMemberService';
import { formatDiscount } from '@/utils/format';
import type { MemberLevel, MemberLevelRequest } from '@/types/member';

const BENEFIT_TYPES = [
  { label: '折扣', value: 'DISCOUNT' },
  { label: '礼包', value: 'GIFT' },
  { label: '服务', value: 'SERVICE' },
  { label: '其他', value: 'OTHER' },
];

const LevelTab: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [levels, setLevels] = useState<MemberLevel[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<MemberLevel | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const fetchLevels = async () => {
    try {
      setLoading(true);
      const data = await adminMemberService.getLevels();
      setLevels(data || []);
    } catch (error: any) {
      message.error(error.message || '加载等级失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLevels();
  }, []);

  const openCreate = () => {
    setEditing(null);
    setModalOpen(true);
    form.resetFields();
    form.setFieldsValue({
      status: 1,
      sortOrder: 0,
      discountRate: 1,
      benefits: [],
    });
  };

  const openEdit = (level: MemberLevel) => {
    setEditing(level);
    setModalOpen(true);
    form.setFieldsValue({
      levelCode: level.levelCode,
      levelName: level.levelName,
      growthThreshold: level.growthThreshold,
      iconUrl: level.iconUrl,
      discountRate: level.discountRate ?? 1,
      description: level.description,
      status: level.status ?? 1,
      sortOrder: level.sortOrder ?? 0,
      benefits: (level.benefits || []).map((benefit) => ({
        benefitName: benefit.benefitName,
        benefitType: benefit.benefitType,
        benefitValue: benefit.benefitValue,
        description: benefit.description,
      })),
    });
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      const payload: MemberLevelRequest = {
        levelCode: values.levelCode,
        levelName: values.levelName,
        growthThreshold: values.growthThreshold,
        iconUrl: values.iconUrl,
        discountRate: values.discountRate,
        description: values.description,
        status: values.status,
        sortOrder: values.sortOrder,
        benefits: (values.benefits || []).filter((item: any) => item?.benefitName),
      };
      if (editing) {
        await adminMemberService.updateLevel(editing.id, payload);
        message.success('等级已更新');
      } else {
        await adminMemberService.createLevel(payload);
        message.success('等级已创建');
      }
      setModalOpen(false);
      await fetchLevels();
    } catch (error: any) {
      if (error?.errorFields) {
        return;
      }
      message.error(error.message || '保存失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDisable = (level: MemberLevel) => {
    Modal.confirm({
      title: '下架等级',
      content: `确定下架「${level.levelName}」吗？已在该等级的用户不会被降级。`,
      okText: '确定',
      cancelText: '取消',
      okType: 'danger',
      onOk: async () => {
        try {
          await adminMemberService.disableLevel(level.id);
          message.success('等级已下架');
          await fetchLevels();
        } catch (error: any) {
          message.error(error.message || '下架失败');
        }
      },
    });
  };

  const columns: ColumnsType<MemberLevel> = [
    {
      title: '等级',
      dataIndex: 'levelName',
      key: 'levelName',
      width: 140,
      render: (value: string, record) => (
        <Space>
          <span style={{ fontWeight: 500 }}>{value}</span>
          {record.current && <Tag color="gold">当前</Tag>}
        </Space>
      ),
    },
    {
      title: '编码',
      dataIndex: 'levelCode',
      key: 'levelCode',
      width: 110,
    },
    {
      title: '成长值门槛',
      dataIndex: 'growthThreshold',
      key: 'growthThreshold',
      width: 110,
    },
    {
      title: '折扣',
      dataIndex: 'discountRate',
      key: 'discountRate',
      width: 90,
      render: (value?: number) => formatDiscount(value),
    },
    {
      title: '权益',
      key: 'benefits',
      render: (_, record) => (
        <Space wrap>
          {(record.benefits || []).map((benefit) => (
            <Tag key={benefit.id} color="blue">
              {benefit.benefitName}
            </Tag>
          ))}
        </Space>
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
          新增等级
        </Button>
      </div>
      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={levels}
        pagination={false}
        scroll={{ x: 1000 }}
      />

      <Modal
        title={editing ? `编辑等级：${editing.levelName}` : '新增等级'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        confirmLoading={submitting}
        okText="保存"
        cancelText="取消"
        width={820}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item
              label="等级编码"
              name="levelCode"
              rules={[{ required: true, message: '请输入等级编码' }]}
              style={{ width: 200 }}
            >
              <Input placeholder="如 SILVER" />
            </Form.Item>
            <Form.Item
              label="等级名称"
              name="levelName"
              rules={[{ required: true, message: '请输入等级名称' }]}
              style={{ width: 200 }}
            >
              <Input placeholder="如 银卡会员" />
            </Form.Item>
            <Form.Item
              label="成长值门槛"
              name="growthThreshold"
              rules={[{ required: true, message: '请输入成长值门槛' }]}
              style={{ width: 150 }}
            >
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
          </Space>
          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item label="会员折扣率（0-1）" name="discountRate" style={{ width: 200 }}>
              <InputNumber min={0} max={1} step={0.01} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="排序" name="sortOrder" style={{ width: 120 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="状态" name="status" style={{ width: 120 }}>
              <Select
                options={[
                  { label: '启用', value: 1 },
                  { label: '停用', value: 0 },
                ]}
              />
            </Form.Item>
            <Form.Item label="图标" name="iconUrl" style={{ width: 240 }}>
            <ImageUpload />
            </Form.Item>
          </Space>
          <Form.Item label="等级说明" name="description">
            <Input.TextArea rows={2} />
          </Form.Item>

          <div style={{ fontWeight: 500, marginBottom: 8 }}>等级权益</div>
          <Form.List name="benefits">
            {(fields, { add, remove }) => (
              <>
                {fields.map(({ key, name, ...restField }) => (
                  <Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                    <Form.Item
                      {...restField}
                      name={[name, 'benefitName']}
                      rules={[{ required: true, message: '请输入权益名称' }]}
                      style={{ marginBottom: 0 }}
                    >
                      <Input placeholder="权益名称" />
                    </Form.Item>
                    <Form.Item {...restField} name={[name, 'benefitType']} style={{ marginBottom: 0 }}>
                      <Select placeholder="类型" options={BENEFIT_TYPES} style={{ width: 110 }} />
                    </Form.Item>
                    <Form.Item {...restField} name={[name, 'benefitValue']} style={{ marginBottom: 0 }}>
                      <Input placeholder="权益值" style={{ width: 110 }} />
                    </Form.Item>
                    <Form.Item {...restField} name={[name, 'description']} style={{ marginBottom: 0 }}>
                      <Input placeholder="说明" style={{ width: 220 }} />
                    </Form.Item>
                    <MinusCircleOutlined onClick={() => remove(name)} />
                  </Space>
                ))}
                <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>
                  添加权益
                </Button>
              </>
            )}
          </Form.List>
        </Form>
      </Modal>
    </>
  );
};

export default LevelTab;
