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
import type { AdminMemberTask, MemberTaskRequest } from '@/types/member';

const PERIOD_OPTIONS = [
  { label: '每日', value: 'DAILY' },
  { label: '每周', value: 'WEEKLY' },
  { label: '一次性', value: 'ONCE' },
];

const periodLabel = (value: string) =>
  PERIOD_OPTIONS.find((option) => option.value === value)?.label || value;

const TaskTab: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [tasks, setTasks] = useState<AdminMemberTask[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AdminMemberTask | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const fetchTasks = async () => {
    try {
      setLoading(true);
      const data = await adminMemberService.getTasks();
      setTasks(data || []);
    } catch (error: any) {
      message.error(error.message || '加载任务失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTasks();
  }, []);

  const openCreate = () => {
    setEditing(null);
    setModalOpen(true);
    form.resetFields();
    form.setFieldsValue({
      periodType: 'DAILY',
      targetValue: 1,
      pointsReward: 0,
      growthReward: 0,
      status: 1,
      sortOrder: 0,
    });
  };

  const openEdit = (task: AdminMemberTask) => {
    setEditing(task);
    setModalOpen(true);
    form.setFieldsValue(task);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      const payload: MemberTaskRequest = {
        taskCode: values.taskCode,
        taskName: values.taskName,
        description: values.description,
        periodType: values.periodType,
        targetValue: values.targetValue,
        pointsReward: values.pointsReward,
        growthReward: values.growthReward,
        status: values.status,
        sortOrder: values.sortOrder,
      };
      if (editing) {
        await adminMemberService.updateTask(editing.id, payload);
        message.success('任务已更新');
      } else {
        await adminMemberService.createTask(payload);
        message.success('任务已创建');
      }
      setModalOpen(false);
      await fetchTasks();
    } catch (error: any) {
      if (error?.errorFields) {
        return;
      }
      message.error(error.message || '保存失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDisable = (task: AdminMemberTask) => {
    Modal.confirm({
      title: '下架任务',
      content: `确定下架「${task.taskName}」吗？已产生的进度记录会保留。`,
      okText: '确定',
      cancelText: '取消',
      okType: 'danger',
      onOk: async () => {
        try {
          await adminMemberService.disableTask(task.id);
          message.success('任务已下架');
          await fetchTasks();
        } catch (error: any) {
          message.error(error.message || '下架失败');
        }
      },
    });
  };

  const columns: ColumnsType<AdminMemberTask> = [
    {
      title: '任务',
      dataIndex: 'taskName',
      key: 'taskName',
      width: 160,
      render: (value: string, record) => (
        <Space direction="vertical" size={0}>
          <span style={{ fontWeight: 500 }}>{value}</span>
          <span style={{ color: '#999', fontSize: 12 }}>{record.description || '-'}</span>
        </Space>
      ),
    },
    {
      title: '编码',
      dataIndex: 'taskCode',
      key: 'taskCode',
      width: 160,
    },
    {
      title: '周期',
      dataIndex: 'periodType',
      key: 'periodType',
      width: 90,
      render: (value: string) => <Tag color="cyan">{periodLabel(value)}</Tag>,
    },
    {
      title: '目标次数',
      dataIndex: 'targetValue',
      key: 'targetValue',
      width: 100,
    },
    {
      title: '积分奖励',
      dataIndex: 'pointsReward',
      key: 'pointsReward',
      width: 100,
    },
    {
      title: '成长值奖励',
      dataIndex: 'growthReward',
      key: 'growthReward',
      width: 110,
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
          新增任务
        </Button>
      </div>
      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={tasks}
        pagination={false}
        scroll={{ x: 950 }}
      />

      <Modal
        title={editing ? `编辑任务：${editing.taskName}` : '新增任务'}
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
              label="任务编码"
              name="taskCode"
              rules={[{ required: true, message: '请输入任务编码' }]}
              style={{ width: 220 }}
              tooltip="编码与业务埋点绑定，编辑时不可修改"
            >
              <Input placeholder="如 DAILY_CHECKIN" disabled={!!editing} />
            </Form.Item>
            <Form.Item
              label="任务名称"
              name="taskName"
              rules={[{ required: true, message: '请输入任务名称' }]}
              style={{ width: 220 }}
            >
              <Input placeholder="如 每日签到" />
            </Form.Item>
          </Space>
          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item label="周期" name="periodType" style={{ width: 140 }}>
              <Select options={PERIOD_OPTIONS} />
            </Form.Item>
            <Form.Item
              label="目标次数"
              name="targetValue"
              rules={[{ required: true, message: '请输入目标次数' }]}
              style={{ width: 140 }}
            >
              <InputNumber min={1} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="积分奖励" name="pointsReward" style={{ width: 140 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="成长值奖励" name="growthReward" style={{ width: 140 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
          </Space>
          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item label="状态" name="status" style={{ width: 120 }}>
              <Select
                options={[
                  { label: '启用', value: 1 },
                  { label: '停用', value: 0 },
                ]}
              />
            </Form.Item>
            <Form.Item label="排序" name="sortOrder" style={{ width: 120 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
          </Space>
          <Form.Item label="任务说明" name="description">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default TaskTab;
