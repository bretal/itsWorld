import React, { useEffect, useState } from 'react';
import {
  Button,
  Card,
  DatePicker,
  Descriptions,
  Drawer,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  TimePicker,
  message,
} from 'antd';
import { EyeOutlined, PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import * as commerceService from '@/services/adminCommerceService';
import { formatDateTime } from '@/utils/format';
import type { PageData } from '@/types/common';
import type { BookingSlot, BookingView, CommerceStore } from '@/types/commerce';

const PAGE_SIZE = 10;

const BOOKING_STATUS: Record<string, { text: string; color: string }> = {
  PENDING_PAYMENT: { text: '待支付', color: 'warning' },
  CONFIRMED: { text: '已确认', color: 'processing' },
  CHECKED_IN: { text: '已签到', color: 'cyan' },
  IN_SERVICE: { text: '服务中', color: 'blue' },
  COMPLETED: { text: '已完成', color: 'success' },
  CANCELLED: { text: '已取消', color: 'default' },
};

const SLOT_STATUS: Record<string, { text: string; color: string }> = {
  OPEN: { text: '开放', color: 'success' },
  CLOSED: { text: '关闭', color: 'default' },
};

const BookingManagementPage: React.FC = () => {
  const [slotForm] = Form.useForm();
  const [generateForm] = Form.useForm();
  const [rescheduleForm] = Form.useForm();

  const [activeTab, setActiveTab] = useState('bookings');
  const [stores, setStores] = useState<CommerceStore[]>([]);

  const [bookingData, setBookingData] = useState<PageData<BookingView> | null>(null);
  const [bookingLoading, setBookingLoading] = useState(false);
  const [bookingPage, setBookingPage] = useState(1);
  const [bookingStatus, setBookingStatus] = useState<string | undefined>(undefined);
  const [bookingDate, setBookingDate] = useState<string | undefined>(undefined);
  const [bookingStoreId, setBookingStoreId] = useState<number | undefined>(undefined);
  const [bookingKeyword, setBookingKeyword] = useState('');

  const [detailOpen, setDetailOpen] = useState(false);
  const [detail, setDetail] = useState<BookingView | null>(null);

  const [rescheduleOpen, setRescheduleOpen] = useState(false);
  const [rescheduleBooking, setRescheduleBooking] = useState<BookingView | null>(null);
  const [rescheduleDate, setRescheduleDate] = useState<string>(dayjs().format('YYYY-MM-DD'));
  const [rescheduleSlots, setRescheduleSlots] = useState<BookingSlot[]>([]);

  const [slotData, setSlotData] = useState<PageData<BookingSlot> | null>(null);
  const [slotLoading, setSlotLoading] = useState(false);
  const [slotPage, setSlotPage] = useState(1);
  const [slotStoreId, setSlotStoreId] = useState<number | undefined>(undefined);
  const [slotDate, setSlotDate] = useState<string | undefined>(undefined);
  const [slotStatus, setSlotStatus] = useState<string | undefined>(undefined);

  const [slotModalOpen, setSlotModalOpen] = useState(false);
  const [editingSlot, setEditingSlot] = useState<BookingSlot | null>(null);
  const [generateOpen, setGenerateOpen] = useState(false);

  const storeNameMap = new Map<number, string>();
  stores.forEach((store) => storeNameMap.set(store.id, store.name));

  const fetchStores = async () => {
    try {
      const result = await commerceService.getStores(1, 100);
      setStores(result.records || []);
    } catch {
      // 下拉数据失败不阻塞
    }
  };

  const fetchBookings = async (
    current = 1,
    status = bookingStatus,
    date = bookingDate,
    storeId = bookingStoreId,
    keyword = bookingKeyword
  ) => {
    try {
      setBookingLoading(true);
      const result = await commerceService.getBookings(
        current,
        PAGE_SIZE,
        status,
        date,
        storeId,
        keyword
      );
      setBookingData(result);
      setBookingPage(current);
    } catch (error: any) {
      message.error(error.message || '加载预约失败');
    } finally {
      setBookingLoading(false);
    }
  };

  const fetchSlots = async (
    current = 1,
    storeId = slotStoreId,
    date = slotDate,
    status = slotStatus
  ) => {
    try {
      setSlotLoading(true);
      const result = await commerceService.getBookingSlots(
        current,
        PAGE_SIZE,
        storeId,
        date,
        status
      );
      setSlotData(result);
      setSlotPage(current);
    } catch (error: any) {
      message.error(error.message || '加载时段失败');
    } finally {
      setSlotLoading(false);
    }
  };

  useEffect(() => {
    fetchStores();
    fetchBookings(1, undefined, undefined, undefined, '');
    fetchSlots(1, undefined, undefined, undefined);
  }, []);

  const openDetail = async (bookingNo: string) => {
    try {
      setDetailOpen(true);
      setDetail(await commerceService.getBooking(bookingNo));
    } catch (error: any) {
      message.error(error.message || '加载预约详情失败');
    }
  };

  const runAction = async (
    action: () => Promise<void>,
    successText: string,
    refresh: () => Promise<void>
  ) => {
    try {
      await action();
      message.success(successText);
      await refresh();
    } catch (error: any) {
      message.error(error.message || '操作失败');
    }
  };

  const confirmCancel = (record: BookingView) => {
    Modal.confirm({
      title: `取消预约：${record.bookingNo}`,
      content: '已支付预约取消后，退款需在售后流程处理。',
      okText: '确认取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await commerceService.cancelBooking(record.bookingNo, '后台取消');
          message.success('预约已取消');
          await fetchBookings(bookingPage);
        } catch (error: any) {
          message.error(error.message || '取消失败');
        }
      },
    });
  };

  const openReschedule = async (record: BookingView) => {
    setRescheduleBooking(record);
    setRescheduleOpen(true);
    setRescheduleDate(dayjs().format('YYYY-MM-DD'));
    rescheduleForm.resetFields();
    await loadRescheduleSlots(record.storeId, dayjs().format('YYYY-MM-DD'));
  };

  const loadRescheduleSlots = async (storeId: number, date: string) => {
    try {
      const result = await commerceService.getBookingSlots(1, 200, storeId, date, 'OPEN');
      setRescheduleSlots(result.records || []);
    } catch (error: any) {
      message.error(error.message || '加载可约时段失败');
    }
  };

  const submitReschedule = async () => {
    if (!rescheduleBooking) return;
    try {
      const values = await rescheduleForm.validateFields();
      await commerceService.rescheduleBooking(rescheduleBooking.bookingNo, values.slotId);
      message.success('预约已改期');
      setRescheduleOpen(false);
      await fetchBookings(bookingPage);
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error.message || '改期失败');
    }
  };

  const bookingColumns: ColumnsType<BookingView> = [
    {
      title: '预约号',
      dataIndex: 'bookingNo',
      key: 'bookingNo',
      width: 190,
      render: (value: string) => <span style={{ fontSize: 12 }}>{value}</span>,
    },
    { title: '宠物', dataIndex: 'petName', key: 'petName', width: 100 },
    { title: '服务', dataIndex: 'serviceName', key: 'serviceName', width: 140 },
    {
      title: '门店',
      dataIndex: 'storeId',
      key: 'storeId',
      width: 140,
      render: (value: number) => storeNameMap.get(value) || `门店#${value}`,
    },
    {
      title: '时段',
      key: 'slot',
      width: 180,
      render: (_, record) =>
        record.serviceDate
          ? `${record.serviceDate} ${String(record.startTime || '').slice(0, 5)}-${String(
              record.endTime || ''
            ).slice(0, 5)}`
          : '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (value: string) => {
        const item = BOOKING_STATUS[value] || { text: value, color: 'default' };
        return <Tag color={item.color}>{item.text}</Tag>;
      },
    },
    {
      title: '订单号',
      dataIndex: 'orderNo',
      key: 'orderNo',
      width: 190,
      render: (value?: string) => (value ? <span style={{ fontSize: 12 }}>{value}</span> : '套餐次数'),
    },
    {
      title: '操作',
      key: 'action',
      width: 300,
      render: (_, record) => (
        <Space size="small" wrap>
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => openDetail(record.bookingNo)}>
            详情
          </Button>
          {record.status === 'CONFIRMED' && (
            <>
              <Button
                type="link"
                size="small"
                onClick={() =>
                  runAction(
                    () => commerceService.checkinBooking(record.bookingNo),
                    '已签到',
                    () => fetchBookings(bookingPage)
                  )
                }
              >
                签到
              </Button>
              <Button type="link" size="small" onClick={() => openReschedule(record)}>
                改期
              </Button>
            </>
          )}
          {record.status === 'CHECKED_IN' && (
            <Button
              type="link"
              size="small"
              onClick={() =>
                runAction(
                  () => commerceService.startBooking(record.bookingNo),
                  '已开始服务',
                  () => fetchBookings(bookingPage)
                )
              }
            >
              开始服务
            </Button>
          )}
          {record.status === 'IN_SERVICE' && (
            <Button
              type="link"
              size="small"
              onClick={() =>
                runAction(
                  () => commerceService.completeBooking(record.bookingNo),
                  '服务已完成',
                  () => fetchBookings(bookingPage)
                )
              }
            >
              完成
            </Button>
          )}
          {['PENDING_PAYMENT', 'CONFIRMED', 'CHECKED_IN'].includes(record.status) && (
            <Button type="link" size="small" danger onClick={() => confirmCancel(record)}>
              取消
            </Button>
          )}
        </Space>
      ),
    },
  ];

  const slotColumns: ColumnsType<BookingSlot> = [
    {
      title: '门店',
      dataIndex: 'storeId',
      key: 'storeId',
      render: (value: number) => storeNameMap.get(value) || `门店#${value}`,
    },
    { title: '日期', dataIndex: 'serviceDate', key: 'serviceDate', width: 120 },
    {
      title: '时段',
      key: 'time',
      width: 140,
      render: (_, record) =>
        `${String(record.startTime || '').slice(0, 5)}-${String(record.endTime || '').slice(0, 5)}`,
    },
    {
      title: '容量（总/已约/锁定）',
      key: 'capacity',
      width: 170,
      render: (_, record) =>
        `${record.totalCapacity} / ${record.reservedCapacity} / ${record.lockedCapacity}`,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 90,
      render: (value: string) => {
        const item = SLOT_STATUS[value] || { text: value, color: 'default' };
        return <Tag color={item.color}>{item.text}</Tag>;
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => openEditSlot(record)}>
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            danger
            onClick={async () => {
              try {
                await commerceService.deleteBookingSlot(record.id);
                message.success('时段已删除');
                await fetchSlots(slotPage);
              } catch (error: any) {
                message.error(error.message || '删除失败');
              }
            }}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ];

  const openCreateSlot = () => {
    setEditingSlot(null);
    setSlotModalOpen(true);
    slotForm.resetFields();
    slotForm.setFieldsValue({
      storeId: slotStoreId,
      serviceDate: slotDate ? dayjs(slotDate) : dayjs(),
      totalCapacity: 1,
      status: 'OPEN',
    });
  };

  const openEditSlot = (record: BookingSlot) => {
    setEditingSlot(record);
    setSlotModalOpen(true);
    slotForm.setFieldsValue({
      storeId: record.storeId,
      staffId: record.staffId || undefined,
      serviceDate: dayjs(record.serviceDate),
      startTime: dayjs(String(record.startTime || '').slice(0, 5), 'HH:mm'),
      endTime: dayjs(String(record.endTime || '').slice(0, 5), 'HH:mm'),
      totalCapacity: record.totalCapacity,
      status: record.status,
    });
  };

  const submitSlot = async () => {
    try {
      const values = await slotForm.validateFields();
      const payload = {
        storeId: values.storeId,
        staffId: values.staffId,
        serviceDate: values.serviceDate.format('YYYY-MM-DD'),
        startTime: values.startTime.format('HH:mm:ss'),
        endTime: values.endTime.format('HH:mm:ss'),
        totalCapacity: values.totalCapacity,
        status: values.status,
      };
      if (editingSlot) {
        await commerceService.updateBookingSlot(editingSlot.id, payload);
        message.success('时段已更新');
      } else {
        await commerceService.createBookingSlot(payload);
        message.success('时段已创建');
      }
      setSlotModalOpen(false);
      await fetchSlots(slotPage);
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error.message || '保存失败');
    }
  };

  const submitGenerate = async () => {
    try {
      const values = await generateForm.validateFields();
      const created = await commerceService.generateBookingSlots({
        storeId: values.storeId,
        staffId: values.staffId,
        serviceDate: values.serviceDate.format('YYYY-MM-DD'),
        startTime: values.startTime.format('HH:mm:ss'),
        endTime: values.endTime.format('HH:mm:ss'),
        intervalMinutes: values.intervalMinutes,
        totalCapacity: values.totalCapacity,
      });
      message.success(`已生成 ${created.length} 个时段`);
      setGenerateOpen(false);
      await fetchSlots(1, values.storeId, values.serviceDate.format('YYYY-MM-DD'), slotStatus);
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error.message || '生成失败');
    }
  };

  return (
    <Card title="预约管理">
      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        items={[
          {
            key: 'bookings',
            label: '预约列表',
            children: (
              <>
                <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between' }}>
                  <Space wrap>
                    <Select
                      allowClear
                      placeholder="全部状态"
                      style={{ width: 140 }}
                      value={bookingStatus}
                      onChange={(value) => {
                        setBookingStatus(value);
                        fetchBookings(1, value, bookingDate, bookingStoreId, bookingKeyword);
                      }}
                      options={Object.entries(BOOKING_STATUS).map(([value, item]) => ({
                        value,
                        label: item.text,
                      }))}
                    />
                    <Select
                      allowClear
                      placeholder="全部门店"
                      style={{ width: 160 }}
                      value={bookingStoreId}
                      onChange={(value) => {
                        setBookingStoreId(value);
                        fetchBookings(1, bookingStatus, bookingDate, value, bookingKeyword);
                      }}
                      options={stores.map((store) => ({ label: store.name, value: store.id }))}
                    />
                    <DatePicker
                      placeholder="预约日期"
                      value={bookingDate ? dayjs(bookingDate) : null}
                      onChange={(value) => {
                        const date = value ? value.format('YYYY-MM-DD') : undefined;
                        setBookingDate(date);
                        fetchBookings(1, bookingStatus, date, bookingStoreId, bookingKeyword);
                      }}
                    />
                    <Input.Search
                      allowClear
                      placeholder="预约号"
                      style={{ width: 200 }}
                      onSearch={(value) => {
                        setBookingKeyword(value);
                        fetchBookings(1, bookingStatus, bookingDate, bookingStoreId, value);
                      }}
                    />
                  </Space>
                </div>
                <Table
                  rowKey="id"
                  loading={bookingLoading}
                  columns={bookingColumns}
                  dataSource={bookingData?.records || []}
                  scroll={{ x: 1350 }}
                  pagination={{
                    current: bookingPage,
                    pageSize: PAGE_SIZE,
                    total: bookingData?.total || 0,
                    showSizeChanger: false,
                    onChange: (next) => fetchBookings(next),
                  }}
                />
              </>
            ),
          },
          {
            key: 'slots',
            label: '时段管理',
            children: (
              <>
                <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between' }}>
                  <Space wrap>
                    <Select
                      allowClear
                      placeholder="全部门店"
                      style={{ width: 160 }}
                      value={slotStoreId}
                      onChange={(value) => {
                        setSlotStoreId(value);
                        fetchSlots(1, value, slotDate, slotStatus);
                      }}
                      options={stores.map((store) => ({ label: store.name, value: store.id }))}
                    />
                    <DatePicker
                      placeholder="日期"
                      value={slotDate ? dayjs(slotDate) : null}
                      onChange={(value) => {
                        const date = value ? value.format('YYYY-MM-DD') : undefined;
                        setSlotDate(date);
                        fetchSlots(1, slotStoreId, date, slotStatus);
                      }}
                    />
                    <Select
                      allowClear
                      placeholder="全部状态"
                      style={{ width: 120 }}
                      value={slotStatus}
                      onChange={(value) => {
                        setSlotStatus(value);
                        fetchSlots(1, slotStoreId, slotDate, value);
                      }}
                      options={Object.entries(SLOT_STATUS).map(([value, item]) => ({
                        value,
                        label: item.text,
                      }))}
                    />
                  </Space>
                  <Space>
                    <Button onClick={() => {
                      setGenerateOpen(true);
                      generateForm.resetFields();
                      generateForm.setFieldsValue({
                        storeId: slotStoreId,
                        serviceDate: slotDate ? dayjs(slotDate) : dayjs(),
                        intervalMinutes: 60,
                        totalCapacity: 1,
                      });
                    }}>
                      批量生成
                    </Button>
                    <Button type="primary" icon={<PlusOutlined />} onClick={openCreateSlot}>
                      新增时段
                    </Button>
                  </Space>
                </div>
                <Table
                  rowKey="id"
                  loading={slotLoading}
                  columns={slotColumns}
                  dataSource={slotData?.records || []}
                  pagination={{
                    current: slotPage,
                    pageSize: PAGE_SIZE,
                    total: slotData?.total || 0,
                    showSizeChanger: false,
                    onChange: (next) => fetchSlots(next),
                  }}
                />
              </>
            ),
          },
        ]}
      />

      <Drawer
        title={`预约详情：${detail?.bookingNo || ''}`}
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        width={820}
        destroyOnHidden
      >
        {detail && (
          <>
            <Descriptions column={2} bordered size="small">
              <Descriptions.Item label="预约号">{detail.bookingNo}</Descriptions.Item>
              <Descriptions.Item label="状态">
                {BOOKING_STATUS[detail.status]?.text || detail.status}
              </Descriptions.Item>
              <Descriptions.Item label="用户">{detail.username || detail.userId}</Descriptions.Item>
              <Descriptions.Item label="宠物">{detail.petName || detail.petId}</Descriptions.Item>
              <Descriptions.Item label="服务">{detail.serviceName || detail.serviceId}</Descriptions.Item>
              <Descriptions.Item label="门店">{detail.storeName || detail.storeId}</Descriptions.Item>
              <Descriptions.Item label="日期">{detail.serviceDate || '-'}</Descriptions.Item>
              <Descriptions.Item label="时段">
                {detail.startTime
                  ? `${String(detail.startTime).slice(0, 5)}-${String(detail.endTime).slice(0, 5)}`
                  : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="订单号">{detail.orderNo || '套餐次数'}</Descriptions.Item>
              <Descriptions.Item label="创建时间">{formatDateTime(detail.createdAt)}</Descriptions.Item>
              <Descriptions.Item label="签到时间">
                {detail.checkinTime ? formatDateTime(detail.checkinTime) : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="完成时间">
                {detail.completedTime ? formatDateTime(detail.completedTime) : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="取消原因" span={2}>
                {detail.cancelReason || '-'}
              </Descriptions.Item>
            </Descriptions>
            <div style={{ marginTop: 20, marginBottom: 8, fontWeight: 600 }}>状态日志</div>
            <Table
              rowKey="id"
              size="small"
              pagination={false}
              dataSource={detail.logs || []}
              columns={[
                { title: '动作', dataIndex: 'action', key: 'action', width: 100 },
                {
                  title: '状态',
                  key: 'status',
                  width: 160,
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
        title={editingSlot ? '编辑时段' : '新增时段'}
        open={slotModalOpen}
        onCancel={() => setSlotModalOpen(false)}
        onOk={submitSlot}
        okText="保存"
        cancelText="取消"
        destroyOnHidden
      >
        <Form form={slotForm} layout="vertical">
          <Form.Item label="门店" name="storeId" rules={[{ required: true, message: '请选择门店' }]}>
            <Select
              options={stores.map((store) => ({ label: store.name, value: store.id }))}
              placeholder="选择门店"
            />
          </Form.Item>
          <Form.Item label="日期" name="serviceDate" rules={[{ required: true, message: '请选择日期' }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item label="开始时间" name="startTime" rules={[{ required: true, message: '请选择开始时间' }]}>
              <TimePicker format="HH:mm" minuteStep={15} />
            </Form.Item>
            <Form.Item label="结束时间" name="endTime" rules={[{ required: true, message: '请选择结束时间' }]}>
              <TimePicker format="HH:mm" minuteStep={15} />
            </Form.Item>
            <Form.Item label="容量" name="totalCapacity" rules={[{ required: true, message: '请输入容量' }]}>
              <InputNumber min={1} />
            </Form.Item>
            <Form.Item label="状态" name="status">
              <Select
                style={{ width: 110 }}
                options={[
                  { label: '开放', value: 'OPEN' },
                  { label: '关闭', value: 'CLOSED' },
                ]}
              />
            </Form.Item>
          </Space>
        </Form>
      </Modal>

      <Modal
        title="批量生成时段"
        open={generateOpen}
        onCancel={() => setGenerateOpen(false)}
        onOk={submitGenerate}
        okText="生成"
        cancelText="取消"
        destroyOnHidden
      >
        <Form form={generateForm} layout="vertical">
          <Form.Item label="门店" name="storeId" rules={[{ required: true, message: '请选择门店' }]}>
            <Select options={stores.map((store) => ({ label: store.name, value: store.id }))} />
          </Form.Item>
          <Form.Item label="日期" name="serviceDate" rules={[{ required: true, message: '请选择日期' }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item label="开始时间" name="startTime" rules={[{ required: true, message: '请选择开始时间' }]}>
              <TimePicker format="HH:mm" minuteStep={15} />
            </Form.Item>
            <Form.Item label="结束时间" name="endTime" rules={[{ required: true, message: '请选择结束时间' }]}>
              <TimePicker format="HH:mm" minuteStep={15} />
            </Form.Item>
          </Space>
          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item label="间隔(分钟)" name="intervalMinutes" rules={[{ required: true, message: '请输入间隔' }]}>
              <InputNumber min={15} step={15} />
            </Form.Item>
            <Form.Item label="容量" name="totalCapacity" rules={[{ required: true, message: '请输入容量' }]}>
              <InputNumber min={1} />
            </Form.Item>
          </Space>
        </Form>
      </Modal>

      <Modal
        title={`预约改期：${rescheduleBooking?.bookingNo || ''}`}
        open={rescheduleOpen}
        onCancel={() => setRescheduleOpen(false)}
        onOk={submitReschedule}
        okText="确认改期"
        cancelText="取消"
        destroyOnHidden
      >
        <Form form={rescheduleForm} layout="vertical">
          <Form.Item label="新日期">
            <DatePicker
              value={dayjs(rescheduleDate)}
              onChange={async (value) => {
                const date = value ? value.format('YYYY-MM-DD') : dayjs().format('YYYY-MM-DD');
                setRescheduleDate(date);
                if (rescheduleBooking) {
                  await loadRescheduleSlots(rescheduleBooking.storeId, date);
                }
              }}
            />
          </Form.Item>
          <Form.Item label="新时段" name="slotId" rules={[{ required: true, message: '请选择新时段' }]}>
            <Select
              placeholder="选择可约时段"
              options={rescheduleSlots.map((slot) => ({
                label: `${String(slot.startTime || '').slice(0, 5)}-${String(slot.endTime || '').slice(
                  0,
                  5
                )}（余 ${Math.max(
                  slot.totalCapacity - slot.reservedCapacity - slot.lockedCapacity,
                  0
                )}）`,
                value: slot.id,
              }))}
            />
          </Form.Item>
          <div style={{ color: '#999', fontSize: 12 }}>
            服务：{rescheduleBooking?.serviceName || '-'}；门店：{rescheduleBooking?.storeName || '-'}
          </div>
        </Form>
      </Modal>
    </Card>
  );
};

export default BookingManagementPage;
