import React, { useEffect, useState } from 'react';
import {
  Button,
  Card,
  Form,
  Input,
  Modal,
  Rate,
  Select,
  Space,
  Table,
  Tag,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import * as commerceService from '@/services/adminCommerceService';
import { formatDateTime } from '@/utils/format';
import type { PageData } from '@/types/common';
import type { ReviewView } from '@/types/commerce';

const PAGE_SIZE = 10;

const STATUS_MAP: Record<string, { text: string; color: string }> = {
  PENDING: { text: '待审核', color: 'warning' },
  APPROVED: { text: '已通过', color: 'success' },
  HIDDEN: { text: '已隐藏', color: 'default' },
};

const ReviewManagementPage: React.FC = () => {
  const [replyForm] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<PageData<ReviewView> | null>(null);
  const [page, setPage] = useState(1);
  const [status, setStatus] = useState<string | undefined>(undefined);
  const [replyOpen, setReplyOpen] = useState(false);
  const [replyTarget, setReplyTarget] = useState<ReviewView | null>(null);
  const [replying, setReplying] = useState(false);

  const fetchReviews = async (current = 1, searchStatus = status) => {
    try {
      setLoading(true);
      const result = await commerceService.getReviews(current, PAGE_SIZE, searchStatus);
      setData(result);
      setPage(current);
    } catch (error: any) {
      message.error(error.message || '加载评价失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReviews(1, undefined);
  }, []);

  const runAction = async (action: () => Promise<unknown>, successText: string) => {
    try {
      await action();
      message.success(successText);
      await fetchReviews(page);
    } catch (error: any) {
      message.error(error.message || '操作失败');
    }
  };

  const openReply = (record: ReviewView) => {
    setReplyTarget(record);
    setReplyOpen(true);
    replyForm.setFieldsValue({ reply: record.reply || '' });
  };

  const submitReply = async () => {
    if (!replyTarget) return;
    try {
      const values = await replyForm.validateFields();
      setReplying(true);
      await commerceService.replyReview(replyTarget.id, values.reply);
      message.success('回复已提交');
      setReplyOpen(false);
      await fetchReviews(page);
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error.message || '回复失败');
    } finally {
      setReplying(false);
    }
  };

  const columns: ColumnsType<ReviewView> = [
    {
      title: '订单号',
      dataIndex: 'orderNo',
      key: 'orderNo',
      width: 200,
      render: (value?: string) => (value ? <span style={{ fontSize: 12 }}>{value}</span> : '-'),
    },
    { title: '用户', dataIndex: 'username', key: 'username', width: 110 },
    {
      title: '评分',
      dataIndex: 'rating',
      key: 'rating',
      width: 150,
      render: (value: number) => <Rate disabled value={value} style={{ fontSize: 14 }} />,
    },
    {
      title: '评价内容',
      dataIndex: 'content',
      key: 'content',
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (value: string) => {
        const item = STATUS_MAP[value] || { text: value, color: 'default' };
        return <Tag color={item.color}>{item.text}</Tag>;
      },
    },
    {
      title: '评价时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (value: string) => formatDateTime(value),
    },
    {
      title: '操作',
      key: 'action',
      width: 230,
      render: (_, record) => (
        <Space size="small">
          {record.status === 'PENDING' && (
            <Button
              type="link"
              size="small"
              onClick={() =>
                runAction(() => commerceService.approveReview(record.id), '评价已通过')
              }
            >
              通过
            </Button>
          )}
          {record.status !== 'HIDDEN' && (
            <Button
              type="link"
              size="small"
              danger
              onClick={() =>
                runAction(() => commerceService.hideReview(record.id), '评价已隐藏')
              }
            >
              隐藏
            </Button>
          )}
          <Button type="link" size="small" onClick={() => openReply(record)}>
            回复
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <Card title="评价管理">
      <div style={{ marginBottom: 12 }}>
        <Select
          allowClear
          placeholder="全部状态"
          style={{ width: 150 }}
          value={status}
          onChange={(value) => {
            setStatus(value);
            fetchReviews(1, value);
          }}
          options={Object.entries(STATUS_MAP).map(([value, item]) => ({
            value,
            label: item.text,
          }))}
        />
      </div>
      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={data?.records || []}
        scroll={{ x: 1050 }}
        expandable={{
          expandedRowRender: (record) => (
            <div>
              <div style={{ color: '#5f6673' }}>{record.content || '（无文字评价）'}</div>
              {record.images?.length > 0 && (
                <Space style={{ marginTop: 8 }} wrap>
                  {record.images.map((image) => (
                    <img
                      key={image}
                      src={image}
                      alt=""
                      style={{ width: 72, height: 72, objectFit: 'cover', borderRadius: 4 }}
                    />
                  ))}
                </Space>
              )}
              {record.reply && (
                <div style={{ marginTop: 8, color: '#2f80ed' }}>
                  商家回复：{record.reply}
                </div>
              )}
            </div>
          ),
        }}
        pagination={{
          current: page,
          pageSize: PAGE_SIZE,
          total: data?.total || 0,
          showSizeChanger: false,
          onChange: (next) => fetchReviews(next),
        }}
      />

      <Modal
        title={`回复评价：${replyTarget?.orderNo || ''}`}
        open={replyOpen}
        onCancel={() => setReplyOpen(false)}
        onOk={submitReply}
        confirmLoading={replying}
        okText="提交回复"
        cancelText="取消"
        destroyOnHidden
      >
        <Form form={replyForm} layout="vertical">
          <Form.Item
            label="回复内容"
            name="reply"
            rules={[{ required: true, message: '请输入回复内容' }]}
          >
            <Input.TextArea rows={4} maxLength={500} showCount />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

export default ReviewManagementPage;
