import React, { useEffect, useState } from 'react';
import {
  Button,
  Card,
  Drawer,
  Form,
  Input,
  InputNumber,
  Modal,
  Space,
  Table,
  Tag,
  message,
} from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import * as commerceService from '@/services/adminCommerceService';
import { formatDateTime, formatMoney } from '@/utils/format';
import type { PageData } from '@/types/common';
import type { InventoryLogView, InventoryView } from '@/types/commerce';

const PAGE_SIZE = 10;

const InventoryManagementPage: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<PageData<InventoryView> | null>(null);
  const [page, setPage] = useState(1);
  const [keyword, setKeyword] = useState('');
  const [adjustOpen, setAdjustOpen] = useState(false);
  const [adjustSku, setAdjustSku] = useState<InventoryView | null>(null);
  const [adjusting, setAdjusting] = useState(false);
  const [logsOpen, setLogsOpen] = useState(false);
  const [logs, setLogs] = useState<PageData<InventoryLogView> | null>(null);
  const [logsPage, setLogsPage] = useState(1);
  const [logsSkuId, setLogsSkuId] = useState<number | undefined>(undefined);

  const fetchData = async (current = 1, searchKeyword = keyword) => {
    try {
      setLoading(true);
      const result = await commerceService.getInventory(current, PAGE_SIZE, searchKeyword);
      setData(result);
      setPage(current);
    } catch (error: any) {
      message.error(error.message || '加载库存失败');
    } finally {
      setLoading(false);
    }
  };

  const fetchLogs = async (current = 1, skuId = logsSkuId) => {
    try {
      const result = await commerceService.getInventoryLogs(current, PAGE_SIZE, skuId);
      setLogs(result);
      setLogsPage(current);
    } catch (error: any) {
      message.error(error.message || '加载库存流水失败');
    }
  };

  useEffect(() => {
    fetchData(1, '');
  }, []);

  const openAdjust = (record: InventoryView) => {
    setAdjustSku(record);
    setAdjustOpen(true);
    form.resetFields();
  };

  const handleAdjust = async () => {
    if (!adjustSku) return;
    try {
      const values = await form.validateFields();
      setAdjusting(true);
      await commerceService.adjustInventory({
        skuId: adjustSku.skuId,
        changeQty: values.changeQty,
        reason: values.reason,
      });
      message.success('库存已调整');
      setAdjustOpen(false);
      await fetchData(page);
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error.message || '调整失败');
    } finally {
      setAdjusting(false);
    }
  };

  const openLogs = async (record: InventoryView) => {
    setLogsSkuId(record.skuId);
    setLogsPage(1);
    setLogsOpen(true);
    await fetchLogs(1, record.skuId);
  };

  const columns: ColumnsType<InventoryView> = [
    {
      title: '商品/SKU',
      key: 'product',
      render: (_, record) => (
        <Space>
          {record.productCoverUrl ? (
            <img
              src={record.productCoverUrl}
              alt=""
              style={{ width: 40, height: 40, objectFit: 'cover', borderRadius: 4 }}
            />
          ) : null}
          <Space direction="vertical" size={0}>
            <span style={{ fontWeight: 500 }}>{record.productName}</span>
            <span style={{ color: '#999', fontSize: 12 }}>{record.skuCode}</span>
          </Space>
        </Space>
      ),
    },
    { title: '规格', dataIndex: 'specJson', key: 'specJson', render: (value?: string) => value || '-' },
    {
      title: '售价(元)',
      dataIndex: 'salePrice',
      key: 'salePrice',
      width: 110,
      render: (value: number) => formatMoney(value),
    },
    { title: '可售', dataIndex: 'availableQty', key: 'availableQty', width: 90 },
    { title: '锁定', dataIndex: 'lockedQty', key: 'lockedQty', width: 90 },
    { title: '已售', dataIndex: 'soldQty', key: 'soldQty', width: 90 },
    {
      title: 'SKU状态',
      dataIndex: 'skuStatus',
      key: 'skuStatus',
      width: 100,
      render: (value: string) => (value === 'ENABLED' ? <Tag color="success">启用</Tag> : <Tag>停用</Tag>),
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" icon={<PlusOutlined />} onClick={() => openAdjust(record)}>
            调整库存
          </Button>
          <Button type="link" size="small" onClick={() => openLogs(record)}>
            流水
          </Button>
        </Space>
      ),
    },
  ];

  const logColumns: ColumnsType<InventoryLogView> = [
    {
      title: '时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (value: string) => formatDateTime(value),
    },
    {
      title: '变动',
      dataIndex: 'changeAvailable',
      key: 'changeAvailable',
      width: 90,
      render: (value: number) => (
        <span style={{ color: value >= 0 ? '#52c41a' : '#f5222d' }}>
          {value > 0 ? `+${value}` : value}
        </span>
      ),
    },
    {
      title: '变动前/后',
      key: 'beforeAfter',
      width: 130,
      render: (_, record) => `${record.beforeAvailable} → ${record.afterAvailable}`,
    },
    { title: '类型', dataIndex: 'bizType', key: 'bizType', width: 130 },
    { title: '说明', dataIndex: 'remark', key: 'remark' },
  ];

  return (
    <Card title="库存管理">
      <div style={{ marginBottom: 12, textAlign: 'right' }}>
        <Input.Search
          allowClear
          placeholder="商品名称/SKU编码"
          style={{ width: 260 }}
          onSearch={(value) => {
            setKeyword(value);
            fetchData(1, value);
          }}
        />
      </div>
      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={data?.records || []}
        scroll={{ x: 1000 }}
        pagination={{
          current: page,
          pageSize: PAGE_SIZE,
          total: data?.total || 0,
          showSizeChanger: false,
          onChange: (next) => fetchData(next),
        }}
      />

      <Modal
        title={`调整库存：${adjustSku?.productName || ''}`}
        open={adjustOpen}
        onCancel={() => setAdjustOpen(false)}
        onOk={handleAdjust}
        confirmLoading={adjusting}
        okText="确认调整"
        cancelText="取消"
        destroyOnHidden
      >
        {adjustSku && (
          <div style={{ marginBottom: 12, color: '#666' }}>
            当前可售库存：{adjustSku.availableQty}，SKU：{adjustSku.skuCode}
          </div>
        )}
        <Form form={form} layout="vertical">
          <Form.Item
            label="调整数量（正数增加，负数减少）"
            name="changeQty"
            rules={[{ required: true, message: '请输入调整数量' }]}
          >
            <InputNumber style={{ width: '100%' }} placeholder="如 50 或 -5" />
          </Form.Item>
          <Form.Item
            label="调整原因"
            name="reason"
            rules={[{ required: true, message: '请填写调整原因' }]}
          >
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title="库存流水"
        open={logsOpen}
        onClose={() => setLogsOpen(false)}
        width={820}
        destroyOnHidden
      >
        <Table
          rowKey="id"
          columns={logColumns}
          dataSource={logs?.records || []}
          pagination={{
            current: logsPage,
            pageSize: PAGE_SIZE,
            total: logs?.total || 0,
            showSizeChanger: false,
            onChange: (next) => fetchLogs(next),
          }}
        />
      </Drawer>
    </Card>
  );
};

export default InventoryManagementPage;
