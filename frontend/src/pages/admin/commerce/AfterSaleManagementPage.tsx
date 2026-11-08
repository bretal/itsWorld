import React, { useEffect, useState } from 'react';
import {
  Button,
  Card,
  Descriptions,
  Drawer,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  message,
} from 'antd';
import { EyeOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import * as commerceService from '@/services/adminCommerceService';
import { formatDateTime, formatMoney } from '@/utils/format';
import type { PageData } from '@/types/common';
import type { AfterSaleView } from '@/types/commerce';

const PAGE_SIZE = 10;

const STATUS_MAP: Record<string, { text: string; color: string }> = {
  APPLIED: { text: '待审核', color: 'warning' },
  REVIEWING: { text: '审核中', color: 'processing' },
  APPROVED: { text: '已通过', color: 'cyan' },
  REJECTED: { text: '已拒绝', color: 'default' },
  REFUNDING: { text: '退款中', color: 'processing' },
  REFUNDED: { text: '已退款', color: 'success' },
  REFUND_FAILED: { text: '退款失败', color: 'error' },
};

const TYPE_MAP: Record<string, string> = {
  REFUND_ONLY: '仅退款',
  RETURN_REFUND: '退货退款',
};

const AfterSaleManagementPage: React.FC = () => {
  const [reviewForm] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<PageData<AfterSaleView> | null>(null);
  const [page, setPage] = useState(1);
  const [status, setStatus] = useState<string | undefined>(undefined);
  const [keyword, setKeyword] = useState('');

  const [detailOpen, setDetailOpen] = useState(false);
  const [detail, setDetail] = useState<AfterSaleView | null>(null);

  const [reviewOpen, setReviewOpen] = useState(false);
  const [reviewing, setReviewing] = useState(false);
  const [reviewTarget, setReviewTarget] = useState<AfterSaleView | null>(null);
  const [reviewApproved, setReviewApproved] = useState(true);

  const fetchAfterSales = async (
    current = 1,
    searchStatus = status,
    searchKeyword = keyword
  ) => {
    try {
      setLoading(true);
      const result = await commerceService.getAfterSales(
        current,
        PAGE_SIZE,
        searchStatus,
        searchKeyword
      );
      setData(result);
      setPage(current);
    } catch (error: any) {
      message.error(error.message || '加载售后失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAfterSales(1, undefined, '');
  }, []);

  const openDetail = async (afterSaleNo: string) => {
    try {
      setDetailOpen(true);
      setDetail(await commerceService.getAfterSale(afterSaleNo));
    } catch (error: any) {
      message.error(error.message || '加载售后详情失败');
    }
  };

  const openReview = (record: AfterSaleView, approved: boolean) => {
    setReviewTarget(record);
    setReviewApproved(approved);
    setReviewOpen(true);
    reviewForm.resetFields();
  };

  const submitReview = async () => {
    if (!reviewTarget) return;
    try {
      const values = await reviewForm.validateFields();
      setReviewing(true);
      await commerceService.reviewAfterSale(
        reviewTarget.afterSaleNo,
        reviewApproved,
        values.remark
      );
      message.success(reviewApproved ? '售后已通过，请执行退款' : '售后已拒绝');
      setReviewOpen(false);
      await fetchAfterSales(page);
      if (detail?.afterSaleNo === reviewTarget.afterSaleNo) {
        setDetail(await commerceService.getAfterSale(reviewTarget.afterSaleNo));
      }
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error.message || '审核失败');
    } finally {
      setReviewing(false);
    }
  };

  const executeRefund = (record: AfterSaleView) => {
    Modal.confirm({
      title: `执行退款：${record.afterSaleNo}`,
      content: `退款金额 ¥${formatMoney(record.amount)}，确认执行吗？`,
      okText: '确认退款',
      onOk: async () => {
        try {
          await commerceService.executeAfterSaleRefund(record.afterSaleNo);
          message.success('退款已完成');
          await fetchAfterSales(page);
          if (detail?.afterSaleNo === record.afterSaleNo) {
            setDetail(await commerceService.getAfterSale(record.afterSaleNo));
          }
        } catch (error: any) {
          message.error(error.message || '退款失败');
        }
      },
    });
  };

  const columns: ColumnsType<AfterSaleView> = [
    {
      title: '售后单号',
      dataIndex: 'afterSaleNo',
      key: 'afterSaleNo',
      width: 200,
      render: (value: string) => <span style={{ fontSize: 12 }}>{value}</span>,
    },
    {
      title: '订单号',
      dataIndex: 'orderNo',
      key: 'orderNo',
      width: 200,
      render: (value?: string) => (value ? <span style={{ fontSize: 12 }}>{value}</span> : '-'),
    },
    { title: '用户', dataIndex: 'username', key: 'username', width: 110 },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      width: 100,
      render: (value: string) => TYPE_MAP[value] || value,
    },
    {
      title: '退款金额（元）',
      dataIndex: 'amount',
      key: 'amount',
      width: 130,
      render: (value: number) => formatMoney(value),
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
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (value: string) => formatDateTime(value),
    },
    {
      title: '操作',
      key: 'action',
      width: 240,
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => openDetail(record.afterSaleNo)}
          >
            详情
          </Button>
          {['APPLIED', 'REVIEWING'].includes(record.status) && (
            <>
              <Button type="link" size="small" onClick={() => openReview(record, true)}>
                通过
              </Button>
              <Button type="link" size="small" danger onClick={() => openReview(record, false)}>
                拒绝
              </Button>
            </>
          )}
          {['APPROVED', 'REFUND_FAILED'].includes(record.status) && (
            <Button type="link" size="small" onClick={() => executeRefund(record)}>
              执行退款
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <Card title="售后管理">
      <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between' }}>
        <Space>
          <Select
            allowClear
            placeholder="全部状态"
            style={{ width: 150 }}
            value={status}
            onChange={(value) => {
              setStatus(value);
              fetchAfterSales(1, value, keyword);
            }}
            options={Object.entries(STATUS_MAP).map(([value, item]) => ({
              value,
              label: item.text,
            }))}
          />
          <Input.Search
            allowClear
            placeholder="售后单号"
            style={{ width: 220 }}
            onSearch={(value) => {
              setKeyword(value);
              fetchAfterSales(1, status, value);
            }}
          />
        </Space>
      </div>
      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={data?.records || []}
        scroll={{ x: 1200 }}
        pagination={{
          current: page,
          pageSize: PAGE_SIZE,
          total: data?.total || 0,
          showSizeChanger: false,
          onChange: (next) => fetchAfterSales(next),
        }}
      />

      <Drawer
        title={`售后详情：${detail?.afterSaleNo || ''}`}
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        width={820}
        destroyOnHidden
      >
        {detail && (
          <>
            <Descriptions column={2} bordered size="small">
              <Descriptions.Item label="售后单号">{detail.afterSaleNo}</Descriptions.Item>
              <Descriptions.Item label="订单号">{detail.orderNo || '-'}</Descriptions.Item>
              <Descriptions.Item label="用户">{detail.username || detail.userId}</Descriptions.Item>
              <Descriptions.Item label="类型">
                {TYPE_MAP[detail.type] || detail.type}
              </Descriptions.Item>
              <Descriptions.Item label="退款金额">
                ¥{formatMoney(detail.amount)}
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                {STATUS_MAP[detail.status]?.text || detail.status}
              </Descriptions.Item>
              <Descriptions.Item label="申请原因" span={2}>
                {detail.reason}
              </Descriptions.Item>
              <Descriptions.Item label="问题描述" span={2}>
                {detail.description || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="审核备注" span={2}>
                {detail.reviewRemark || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="申请时间">
                {formatDateTime(detail.createdAt)}
              </Descriptions.Item>
              <Descriptions.Item label="退款时间">
                {detail.refundedAt ? formatDateTime(detail.refundedAt) : '-'}
              </Descriptions.Item>
            </Descriptions>
            <div style={{ marginTop: 20, marginBottom: 8, fontWeight: 600 }}>售后明细</div>
            <Table
              rowKey="id"
              size="small"
              pagination={false}
              dataSource={detail.items}
              columns={[
                { title: '商品', dataIndex: 'titleSnapshot', key: 'title' },
                { title: '数量', dataIndex: 'qty', key: 'qty', width: 80 },
                {
                  title: '退款金额（元）',
                  dataIndex: 'amount',
                  key: 'amount',
                  width: 120,
                  render: (value: number) => formatMoney(value),
                },
              ]}
            />
            {detail.refund && (
              <>
                <div style={{ marginTop: 20, marginBottom: 8, fontWeight: 600 }}>退款记录</div>
                <Descriptions column={2} bordered size="small">
                  <Descriptions.Item label="退款单号">{detail.refund.refundNo}</Descriptions.Item>
                  <Descriptions.Item label="渠道">{detail.refund.channel}</Descriptions.Item>
                  <Descriptions.Item label="状态">{detail.refund.status}</Descriptions.Item>
                  <Descriptions.Item label="外部单号">
                    {detail.refund.externalRefundNo || '-'}
                  </Descriptions.Item>
                </Descriptions>
              </>
            )}
            <div style={{ marginTop: 20, marginBottom: 8, fontWeight: 600 }}>状态日志</div>
            <Table
              rowKey="id"
              size="small"
              pagination={false}
              dataSource={detail.logs || []}
              columns={[
                { title: '动作', dataIndex: 'action', key: 'action', width: 120 },
                {
                  title: '状态',
                  key: 'status',
                  width: 180,
                  render: (_, record) => `${record.fromStatus || '-'} → ${record.toStatus}`,
                },
                { title: '操作方', dataIndex: 'operatorType', key: 'operatorType', width: 90 },
                { title: '备注', dataIndex: 'remark', key: 'remark' },
                {
                  title: '时间',
                  dataIndex: 'createdAt',
                  key: 'createdAt',
                  width: 160,
                  render: (value: string) => formatDateTime(value),
                },
              ]}
            />
          </>
        )}
      </Drawer>

      <Modal
        title={reviewApproved ? '通过售后' : '拒绝售后'}
        open={reviewOpen}
        onCancel={() => setReviewOpen(false)}
        onOk={submitReview}
        confirmLoading={reviewing}
        okText={reviewApproved ? '确认通过' : '确认拒绝'}
        cancelText="取消"
        destroyOnHidden
      >
        <Form form={reviewForm} layout="vertical">
          <Form.Item label="审核备注" name="remark">
            <Input.TextArea rows={3} placeholder={reviewApproved ? '可选' : '请说明拒绝原因'} />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

export default AfterSaleManagementPage;
