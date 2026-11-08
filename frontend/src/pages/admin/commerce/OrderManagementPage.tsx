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
import type { CommerceOrderDetail, ShipmentView } from '@/types/commerce';

const PAGE_SIZE = 10;

const ORDER_STATUS: Record<string, { text: string; color: string }> = {
  WAIT_PAY: { text: '待支付', color: 'warning' },
  PAID: { text: '已支付', color: 'success' },
  PROCESSING: { text: '处理中', color: 'processing' },
  SHIPPED: { text: '已发货', color: 'cyan' },
  COMPLETED: { text: '已完成', color: 'success' },
  CANCELLED: { text: '已取消', color: 'default' },
  CLOSED: { text: '已关闭', color: 'default' },
};

const PAYMENT_STATUS: Record<string, string> = {
  UNPAID: '未支付',
  PAID: '已支付',
  PARTIAL_REFUND: '部分退款',
  REFUNDED: '已退款',
};

const OrderManagementPage: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<PageData<CommerceOrderDetail> | null>(null);
  const [page, setPage] = useState(1);
  const [status, setStatus] = useState<string | undefined>(undefined);
  const [keyword, setKeyword] = useState('');
  const [detailOpen, setDetailOpen] = useState(false);
  const [detail, setDetail] = useState<CommerceOrderDetail | null>(null);
  const [closeOpen, setCloseOpen] = useState(false);
  const [closeOrderNo, setCloseOrderNo] = useState<string | null>(null);
  const [closing, setClosing] = useState(false);
  const [shipOpen, setShipOpen] = useState(false);
  const [shipForm] = Form.useForm();
  const [shipOrderNo, setShipOrderNo] = useState<string | null>(null);
  const [shipping, setShipping] = useState(false);
  const [shipmentOpen, setShipmentOpen] = useState(false);
  const [shipment, setShipment] = useState<ShipmentView | null>(null);
  const [shipmentLoading, setShipmentLoading] = useState(false);

  const fetchOrders = async (
    current = 1,
    searchKeyword = keyword,
    searchStatus = status
  ) => {
    try {
      setLoading(true);
      const result = await commerceService.getOrders(
        current,
        PAGE_SIZE,
        searchStatus,
        searchKeyword
      );
      setData(result);
      setPage(current);
    } catch (error: any) {
      message.error(error.message || '加载订单失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders(1, '', undefined);
  }, []);

  const openDetail = async (orderNo: string) => {
    try {
      setDetailOpen(true);
      setDetail(await commerceService.getOrder(orderNo));
    } catch (error: any) {
      message.error(error.message || '加载订单详情失败');
    }
  };

  const openClose = (orderNo: string) => {
    setCloseOrderNo(orderNo);
    setCloseOpen(true);
    form.resetFields();
  };

  const handleClose = async () => {
    if (!closeOrderNo) return;
    try {
      const values = await form.validateFields();
      setClosing(true);
      await commerceService.closeOrder(closeOrderNo, values.reason);
      message.success('订单已关闭');
      setCloseOpen(false);
      await fetchOrders(page);
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error.message || '关闭失败');
    } finally {
      setClosing(false);
    }
  };

  const addressText = (snapshot?: string) => {
    if (!snapshot) return '-';
    try {
      const address = JSON.parse(snapshot);
      return [
        address.recipientName,
        address.mobile,
        address.province,
        address.city,
        address.district,
        address.addressDetail,
      ]
        .filter(Boolean)
        .join(' ');
    } catch {
      return snapshot;
    }
  };

  const openShip = (orderNo: string) => {
    setShipOrderNo(orderNo);
    setShipOpen(true);
    shipForm.resetFields();
  };

  const handleShip = async () => {
    if (!shipOrderNo) return;
    try {
      const values = await shipForm.validateFields();
      setShipping(true);
      await commerceService.shipOrder(shipOrderNo, values);
      message.success('订单已发货');
      setShipOpen(false);
      await fetchOrders(page);
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error.message || '发货失败');
    } finally {
      setShipping(false);
    }
  };

  const openShipment = async (orderNo: string) => {
    try {
      setShipmentOpen(true);
      setShipmentLoading(true);
      setShipment(await commerceService.getShipmentByOrder(orderNo));
    } catch (error: any) {
      message.error(error.message || '加载物流失败');
    } finally {
      setShipmentLoading(false);
    }
  };

  const columns: ColumnsType<CommerceOrderDetail> = [
    {
      title: '订单号',
      dataIndex: 'orderNo',
      key: 'orderNo',
      width: 220,
      render: (value: string) => <span style={{ fontSize: 12 }}>{value}</span>,
    },
    { title: '用户ID', dataIndex: 'userId', key: 'userId', width: 90 },
    {
      title: '类型',
      dataIndex: 'orderType',
      key: 'orderType',
      width: 90,
      render: (value: string) =>
        ({ PRODUCT: '商品', GROOMING: '美容预约', PACKAGE: '服务套餐' }[value] || value),
    },
    {
      title: '订单状态',
      dataIndex: 'orderStatus',
      key: 'orderStatus',
      width: 100,
      render: (value: string) => {
        const item = ORDER_STATUS[value] || { text: value, color: 'default' };
        return <Tag color={item.color}>{item.text}</Tag>;
      },
    },
    {
      title: '支付状态',
      dataIndex: 'paymentStatus',
      key: 'paymentStatus',
      width: 100,
      render: (value: string) => PAYMENT_STATUS[value] || value,
    },
    {
      title: '应付/实付（元）',
      key: 'amount',
      width: 150,
      render: (_, record) =>
        `${formatMoney(record.payableAmount)} / ${formatMoney(record.paidAmount)}`,
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
      width: 190,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => openDetail(record.orderNo)}>
            详情
          </Button>
          {record.orderStatus === 'WAIT_PAY' && (
            <Button type="link" size="small" danger onClick={() => openClose(record.orderNo)}>
              关闭
            </Button>
          )}
          {record.orderType === 'PRODUCT'
            && ['PAID', 'PROCESSING'].includes(record.orderStatus) && (
              <Button type="link" size="small" onClick={() => openShip(record.orderNo)}>
                发货
              </Button>
            )}
          {record.orderType === 'PRODUCT'
            && ['SHIPPED', 'COMPLETED'].includes(record.orderStatus) && (
              <Button type="link" size="small" onClick={() => openShipment(record.orderNo)}>
                物流
              </Button>
            )}
        </Space>
      ),
    },
  ];

  const itemColumns: ColumnsType<CommerceOrderDetail['items'][number]> = [
    { title: '商品', dataIndex: 'titleSnapshot', key: 'title' },
    { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80 },
    {
      title: '单价（元）',
      dataIndex: 'unitPrice',
      key: 'unitPrice',
      width: 100,
      render: (value: number) => formatMoney(value),
    },
    {
      title: '优惠（元）',
      dataIndex: 'discountAllocated',
      key: 'discountAllocated',
      width: 100,
      render: (value: number) => formatMoney(value),
    },
    {
      title: '应付（元）',
      dataIndex: 'payableTotal',
      key: 'payableTotal',
      width: 100,
      render: (value: number) => formatMoney(value),
    },
  ];

  return (
    <Card title="订单管理">
      <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between' }}>
        <Space>
          <Select
            allowClear
            placeholder="全部状态"
            style={{ width: 150 }}
            value={status}
            onChange={(value) => {
              setStatus(value);
              fetchOrders(1, keyword, value);
            }}
            options={Object.entries(ORDER_STATUS).map(([value, item]) => ({
              value,
              label: item.text,
            }))}
          />
          <Input.Search
            allowClear
            placeholder="订单号"
            style={{ width: 240 }}
            onSearch={(value) => {
              setKeyword(value);
              fetchOrders(1, value, status);
            }}
          />
        </Space>
      </div>
      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={data?.records || []}
        scroll={{ x: 1100 }}
        pagination={{
          current: page,
          pageSize: PAGE_SIZE,
          total: data?.total || 0,
          showSizeChanger: false,
          onChange: (next) => fetchOrders(next),
        }}
      />

      <Drawer
        title={`订单详情：${detail?.orderNo || ''}`}
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        width={860}
        destroyOnHidden
      >
        {detail && (
          <>
            <Descriptions column={2} bordered size="small">
              <Descriptions.Item label="订单号">{detail.orderNo}</Descriptions.Item>
              <Descriptions.Item label="用户ID">{detail.userId}</Descriptions.Item>
              <Descriptions.Item label="订单状态">
                {ORDER_STATUS[detail.orderStatus]?.text || detail.orderStatus}
              </Descriptions.Item>
              <Descriptions.Item label="支付状态">
                {PAYMENT_STATUS[detail.paymentStatus] || detail.paymentStatus}
              </Descriptions.Item>
              <Descriptions.Item label="原价">
                ¥{formatMoney(detail.originalAmount)}
              </Descriptions.Item>
              <Descriptions.Item label="优惠">
                ¥{formatMoney(detail.discountAmount)}
              </Descriptions.Item>
              <Descriptions.Item label="应付">
                ¥{formatMoney(detail.payableAmount)}
              </Descriptions.Item>
              <Descriptions.Item label="实付">
                ¥{formatMoney(detail.paidAmount)}
              </Descriptions.Item>
              <Descriptions.Item label="收货地址" span={2}>
                {addressText(detail.addressSnapshot)}
              </Descriptions.Item>
              <Descriptions.Item label="创建时间">
                {formatDateTime(detail.createdAt)}
              </Descriptions.Item>
              <Descriptions.Item label="支付时间">
                {detail.paidTime ? formatDateTime(detail.paidTime) : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="备注" span={2}>
                {detail.remark || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="支付渠道" span={2}>
                {detail.latestPayment
                  ? `${detail.latestPayment.payChannel} / ${detail.latestPayment.status} / ${detail.latestPayment.externalTxNo || '-'}`
                  : '未支付'}
              </Descriptions.Item>
            </Descriptions>
            <div style={{ marginTop: 20, marginBottom: 8, fontWeight: 600 }}>商品明细</div>
            <Table
              rowKey="id"
              columns={itemColumns}
              dataSource={detail.items}
              pagination={false}
              size="small"
            />
            {detail.discounts?.length > 0 && (
              <>
                <div style={{ marginTop: 20, marginBottom: 8, fontWeight: 600 }}>优惠明细</div>
                <Table
                  rowKey="id"
                  columns={[
                    { title: '类型', dataIndex: 'discountType', key: 'type' },
                    {
                      title: '金额（元）',
                      dataIndex: 'discountAmount',
                      key: 'amount',
                      render: (value: number) => formatMoney(value),
                    },
                    { title: '规则快照', dataIndex: 'ruleSnapshot', key: 'rule' },
                  ]}
                  dataSource={detail.discounts}
                  pagination={false}
                  size="small"
                />
              </>
            )}
          </>
        )}
      </Drawer>

      <Modal
        title={`关闭订单：${closeOrderNo || ''}`}
        open={closeOpen}
        onCancel={() => setCloseOpen(false)}
        onOk={handleClose}
        confirmLoading={closing}
        okText="确认关闭"
        cancelText="取消"
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <Form.Item label="关闭原因" name="reason" rules={[{ required: true, message: '请填写关闭原因' }]}>
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={`订单发货：${shipOrderNo || ''}`}
        open={shipOpen}
        onCancel={() => setShipOpen(false)}
        onOk={handleShip}
        confirmLoading={shipping}
        okText="确认发货"
        cancelText="取消"
        destroyOnHidden
      >
        <Form form={shipForm} layout="vertical">
          <Form.Item
            label="物流公司"
            name="deliveryCompany"
            rules={[{ required: true, message: '请填写物流公司' }]}
          >
            <Input placeholder="如 顺丰速运" />
          </Form.Item>
          <Form.Item
            label="物流单号"
            name="trackingNo"
            rules={[{ required: true, message: '请填写物流单号' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item label="备注" name="remark">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={`物流信息：${shipment?.orderNo || ''}`}
        open={shipmentOpen}
        onCancel={() => setShipmentOpen(false)}
        footer={null}
        width={680}
        destroyOnHidden
      >
        {shipmentLoading ? (
          <div>加载中...</div>
        ) : shipment ? (
          <>
            <Descriptions column={2} bordered size="small">
              <Descriptions.Item label="发货单号">{shipment.shipmentNo}</Descriptions.Item>
              <Descriptions.Item label="状态">
                {shipment.status === 'DELIVERED' ? '已签收' : '已发货'}
              </Descriptions.Item>
              <Descriptions.Item label="物流公司">{shipment.deliveryCompany}</Descriptions.Item>
              <Descriptions.Item label="物流单号">{shipment.trackingNo}</Descriptions.Item>
              <Descriptions.Item label="发货时间">
                {shipment.shippedTime ? formatDateTime(shipment.shippedTime) : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="签收时间">
                {shipment.receivedTime ? formatDateTime(shipment.receivedTime) : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="备注" span={2}>
                {shipment.remark || '-'}
              </Descriptions.Item>
            </Descriptions>
            <Table
              rowKey="orderItemId"
              style={{ marginTop: 16 }}
              size="small"
              pagination={false}
              dataSource={shipment.items}
              columns={[
                { title: '商品', dataIndex: 'titleSnapshot', key: 'title' },
                { title: '数量', dataIndex: 'qty', key: 'qty', width: 80 },
              ]}
            />
          </>
        ) : null}
      </Modal>
    </Card>
  );
};

export default OrderManagementPage;
