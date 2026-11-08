import React, { useEffect, useState } from 'react';
import {
  Button,
  Card,
  DatePicker,
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
import { PlusOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import type { ColumnsType } from 'antd/es/table';
import * as commerceService from '@/services/adminCommerceService';
import { formatDateTime } from '@/utils/format';
import type { PageData } from '@/types/common';
import type {
  CommerceStaff,
  CommerceStaffShift,
  CommerceStore,
  CommerceStoreBusinessHour,
} from '@/types/commerce';

const PAGE_SIZE = 10;

const STORE_STATUS_OPTIONS = [
  { label: '营业中', value: 'OPEN' },
  { label: '已关闭', value: 'CLOSED' },
  { label: '暂停营业', value: 'SUSPENDED' },
];

const DAY_OPTIONS = [
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
  { label: '周日', value: 7 },
];

const StaffShiftForm: React.FC<{
  form: any;
}> = ({ form }) => (
  <Form form={form} layout="vertical">
    <Form.Item label="班次日期" name="shiftDate" rules={[{ required: true, message: '请选择日期' }]}>
      <DatePicker style={{ width: '100%' }} />
    </Form.Item>
    <Space style={{ display: 'flex' }} align="baseline">
      <Form.Item label="开始时间" name="startTime" rules={[{ required: true, message: '请选择开始时间' }]}>
        <TimePicker format="HH:mm:ss" style={{ width: 160 }} />
      </Form.Item>
      <Form.Item label="结束时间" name="endTime" rules={[{ required: true, message: '请选择结束时间' }]}>
        <TimePicker format="HH:mm:ss" style={{ width: 160 }} />
      </Form.Item>
      <Form.Item label="容量" name="capacity" rules={[{ required: true, message: '请输入容量' }]}>
        <InputNumber min={1} style={{ width: 120 }} />
      </Form.Item>
    </Space>
  </Form>
);

const StoreManagementPage: React.FC = () => {
  const [storeForm] = Form.useForm();
  const [hourForm] = Form.useForm();
  const [staffForm] = Form.useForm();
  const [shiftForm] = Form.useForm();

  const [activeTab, setActiveTab] = useState('stores');
  const [loading, setLoading] = useState(false);
  const [storeData, setStoreData] = useState<PageData<CommerceStore> | null>(null);
  const [page, setPage] = useState(1);
  const [keyword, setKeyword] = useState('');
  const [allStores, setAllStores] = useState<CommerceStore[]>([]);
  const [selectedStoreId, setSelectedStoreId] = useState<number | undefined>(undefined);
  const [selectedStaffId, setSelectedStaffId] = useState<number | undefined>(undefined);

  const [storeModalOpen, setStoreModalOpen] = useState(false);
  const [editingStore, setEditingStore] = useState<CommerceStore | null>(null);
  const [storeSaving, setStoreSaving] = useState(false);

  const [hours, setHours] = useState<CommerceStoreBusinessHour[]>([]);
  const [hourModalOpen, setHourModalOpen] = useState(false);
  const [editingHour, setEditingHour] = useState<CommerceStoreBusinessHour | null>(null);

  const [staffList, setStaffList] = useState<CommerceStaff[]>([]);
  const [staffModalOpen, setStaffModalOpen] = useState(false);
  const [editingStaff, setEditingStaff] = useState<CommerceStaff | null>(null);

  const [shifts, setShifts] = useState<CommerceStaffShift[]>([]);
  const [shiftModalOpen, setShiftModalOpen] = useState(false);
  const [editingShift, setEditingShift] = useState<CommerceStaffShift | null>(null);

  const fetchStores = async (current = 1, searchKeyword = keyword) => {
    try {
      setLoading(true);
      const result = await commerceService.getStores(current, PAGE_SIZE, searchKeyword);
      setStoreData(result);
      setPage(current);
    } catch (error: any) {
      message.error(error.message || '加载门店失败');
    } finally {
      setLoading(false);
    }
  };

  const fetchAllStores = async () => {
    try {
      const result = await commerceService.getStores(1, 100);
      setAllStores(result.records || []);
      if (!selectedStoreId && result.records?.length) {
        setSelectedStoreId(result.records[0].id);
      }
    } catch {
      // 忽略下拉数据加载失败
    }
  };

  useEffect(() => {
    fetchStores(1, '');
    fetchAllStores();
  }, []);

  const openCreateStore = () => {
    setEditingStore(null);
    setStoreModalOpen(true);
    storeForm.resetFields();
    storeForm.setFieldsValue({ businessStatus: 'OPEN' });
  };

  const openEditStore = (store: CommerceStore) => {
    setEditingStore(store);
    setStoreModalOpen(true);
    storeForm.setFieldsValue(store);
  };

  const handleSaveStore = async () => {
    try {
      const values = await storeForm.validateFields();
      setStoreSaving(true);
      if (editingStore) {
        await commerceService.updateStore(editingStore.id, values);
        message.success('门店已更新');
      } else {
        await commerceService.createStore(values);
        message.success('门店已创建');
      }
      setStoreModalOpen(false);
      await Promise.all([fetchStores(page), fetchAllStores()]);
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error.message || '保存失败');
    } finally {
      setStoreSaving(false);
    }
  };

  const toggleStoreStatus = (store: CommerceStore) => {
    const next = store.businessStatus === 'OPEN' ? 'CLOSED' : 'OPEN';
    Modal.confirm({
      title: next === 'OPEN' ? '恢复营业' : '关闭门店',
      content: `确定将「${store.name}」设为${next === 'OPEN' ? '营业中' : '已关闭'}吗？`,
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        try {
          await commerceService.changeStoreStatus(store.id, next);
          message.success('门店状态已更新');
          await fetchStores(page);
        } catch (error: any) {
          message.error(error.message || '更新失败');
        }
      },
    });
  };

  const fetchHours = async (storeId: number) => {
    try {
      setLoading(true);
      setHours(await commerceService.getHours(storeId));
    } catch (error: any) {
      message.error(error.message || '加载营业时间失败');
    } finally {
      setLoading(false);
    }
  };

  const fetchStaff = async (storeId: number) => {
    try {
      setLoading(true);
      setStaffList(await commerceService.getStaff(storeId));
    } catch (error: any) {
      message.error(error.message || '加载员工失败');
    } finally {
      setLoading(false);
    }
  };

  const fetchShifts = async (staffId: number) => {
    try {
      setLoading(true);
      setShifts(await commerceService.getShifts(staffId));
    } catch (error: any) {
      message.error(error.message || '加载班次失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if ((activeTab === 'hours' || activeTab === 'staff') && selectedStoreId) {
      fetchHours(selectedStoreId);
      fetchStaff(selectedStoreId);
    }
    if (activeTab === 'shifts' && selectedStoreId) {
      fetchStaff(selectedStoreId);
    }
  }, [activeTab, selectedStoreId]);

  useEffect(() => {
    if (selectedStaffId) {
      fetchShifts(selectedStaffId);
    }
  }, [selectedStaffId]);

  const openCreateHour = () => {
    setEditingHour(null);
    setHourModalOpen(true);
    hourForm.resetFields();
    hourForm.setFieldsValue({ dayOfWeek: 1, closed: 0 });
  };

  const openEditHour = (hour: CommerceStoreBusinessHour) => {
    setEditingHour(hour);
    setHourModalOpen(true);
    hourForm.setFieldsValue({
      dayOfWeek: hour.dayOfWeek,
      openTime: dayjs(hour.openTime, 'HH:mm:ss'),
      closeTime: dayjs(hour.closeTime, 'HH:mm:ss'),
      specialDate: hour.specialDate ? dayjs(hour.specialDate) : undefined,
      closed: hour.closed,
    });
  };

  const handleSaveHour = async () => {
    if (!selectedStoreId) return;
    try {
      const values = await hourForm.validateFields();
      const payload = {
        dayOfWeek: values.dayOfWeek,
        openTime: values.openTime.format('HH:mm:ss'),
        closeTime: values.closeTime.format('HH:mm:ss'),
        specialDate: values.specialDate ? values.specialDate.format('YYYY-MM-DD') : undefined,
        closed: values.closed ?? 0,
      };
      if (editingHour) {
        await commerceService.updateHour(editingHour.id, payload);
        message.success('营业时间已更新');
      } else {
        await commerceService.createHour(selectedStoreId, payload);
        message.success('营业时间已创建');
      }
      setHourModalOpen(false);
      await fetchHours(selectedStoreId);
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error.message || '保存失败');
    }
  };

  const openCreateStaff = () => {
    setEditingStaff(null);
    setStaffModalOpen(true);
    staffForm.resetFields();
    staffForm.setFieldsValue({ status: 'ACTIVE' });
  };

  const openEditStaff = (staff: CommerceStaff) => {
    setEditingStaff(staff);
    setStaffModalOpen(true);
    staffForm.setFieldsValue(staff);
  };

  const handleSaveStaff = async () => {
    if (!selectedStoreId) return;
    try {
      const values = await staffForm.validateFields();
      if (editingStaff) {
        await commerceService.updateStaff(editingStaff.id, values);
        message.success('员工已更新');
      } else {
        await commerceService.createStaff(selectedStoreId, values);
        message.success('员工已创建');
      }
      setStaffModalOpen(false);
      await fetchStaff(selectedStoreId);
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error.message || '保存失败');
    }
  };

  const openCreateShift = () => {
    if (!selectedStaffId) {
      message.warning('请先选择员工');
      return;
    }
    setEditingShift(null);
    setShiftModalOpen(true);
    shiftForm.resetFields();
    shiftForm.setFieldsValue({ capacity: 1, status: 'ACTIVE' });
  };

  const openEditShift = (shift: CommerceStaffShift) => {
    setEditingShift(shift);
    setShiftModalOpen(true);
    shiftForm.setFieldsValue({
      shiftDate: dayjs(shift.shiftDate),
      startTime: dayjs(shift.startTime, 'HH:mm:ss'),
      endTime: dayjs(shift.endTime, 'HH:mm:ss'),
      capacity: shift.capacity,
      status: shift.status,
    });
  };

  const handleSaveShift = async () => {
    if (!selectedStaffId) return;
    try {
      const values = await shiftForm.validateFields();
      const payload = {
        shiftDate: values.shiftDate.format('YYYY-MM-DD'),
        startTime: values.startTime.format('HH:mm:ss'),
        endTime: values.endTime.format('HH:mm:ss'),
        capacity: values.capacity,
        status: values.status,
      };
      if (editingShift) {
        await commerceService.updateShift(editingShift.id, payload);
        message.success('班次已更新');
      } else {
        await commerceService.createShift(selectedStaffId, payload);
        message.success('班次已创建');
      }
      setShiftModalOpen(false);
      await fetchShifts(selectedStaffId);
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error.message || '保存失败');
    }
  };

  const storeColumns: ColumnsType<CommerceStore> = [
    {
      title: '门店',
      dataIndex: 'name',
      key: 'name',
      render: (value: string, record) => (
        <Space direction="vertical" size={0}>
          <span style={{ fontWeight: 500 }}>{value}</span>
          <span style={{ color: '#999', fontSize: 12 }}>{record.storeCode}</span>
        </Space>
      ),
    },
    { title: '城市', dataIndex: 'city', key: 'city' },
    { title: '地址', dataIndex: 'address', key: 'address' },
    { title: '电话', dataIndex: 'phone', key: 'phone' },
    {
      title: '状态',
      dataIndex: 'businessStatus',
      key: 'businessStatus',
      width: 100,
      render: (value: string) =>
        value === 'OPEN' ? <Tag color="success">营业中</Tag> : <Tag>已关闭</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      width: 260,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => openEditStore(record)}>
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            onClick={() => {
              setSelectedStoreId(record.id);
              setActiveTab('hours');
            }}
          >
            营业时间
          </Button>
          <Button
            type="link"
            size="small"
            onClick={() => {
              setSelectedStoreId(record.id);
              setActiveTab('staff');
            }}
          >
            员工
          </Button>
          <Button type="link" size="small" onClick={() => toggleStoreStatus(record)}>
            {record.businessStatus === 'OPEN' ? '关店' : '开店'}
          </Button>
        </Space>
      ),
    },
  ];

  const hourColumns: ColumnsType<CommerceStoreBusinessHour> = [
    {
      title: '星期',
      dataIndex: 'dayOfWeek',
      key: 'dayOfWeek',
      render: (value: number) => DAY_OPTIONS.find((item) => item.value === value)?.label || value,
    },
    {
      title: '时间',
      key: 'time',
      render: (_, record) => `${record.openTime} - ${record.closeTime}`,
    },
    {
      title: '特殊日期',
      dataIndex: 'specialDate',
      key: 'specialDate',
      render: (value?: string) => value || '-',
    },
    {
      title: '状态',
      dataIndex: 'closed',
      key: 'closed',
      render: (value: number) => (value === 1 ? <Tag>停业</Tag> : <Tag color="success">营业</Tag>),
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => openEditHour(record)}>
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            danger
            onClick={() => {
              Modal.confirm({
                title: '删除营业时间',
                content: '确定删除该营业时间吗？',
                onOk: async () => {
                  await commerceService.deleteHour(record.id);
                  message.success('已删除');
                  if (selectedStoreId) await fetchHours(selectedStoreId);
                },
              });
            }}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ];

  const staffColumns: ColumnsType<CommerceStaff> = [
    { title: '姓名', dataIndex: 'staffName', key: 'staffName' },
    { title: '手机号', dataIndex: 'mobile', key: 'mobile' },
    { title: '角色', dataIndex: 'roleCode', key: 'roleCode' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (value: string) =>
        value === 'ACTIVE' ? <Tag color="success">在职</Tag> : <Tag>停用</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      width: 220,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => openEditStaff(record)}>
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            onClick={() => {
              setSelectedStaffId(record.id);
              setActiveTab('shifts');
            }}
          >
            班次
          </Button>
          <Button
            type="link"
            size="small"
            onClick={async () => {
              await commerceService.changeStaffStatus(
                record.id,
                record.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
              );
              message.success('员工状态已更新');
              if (selectedStoreId) await fetchStaff(selectedStoreId);
            }}
          >
            {record.status === 'ACTIVE' ? '停用' : '启用'}
          </Button>
        </Space>
      ),
    },
  ];

  const shiftColumns: ColumnsType<CommerceStaffShift> = [
    {
      title: '日期',
      dataIndex: 'shiftDate',
      key: 'shiftDate',
      render: (value: string) => formatDateTime(value),
    },
    {
      title: '时间',
      key: 'time',
      render: (_, record) => `${record.startTime} - ${record.endTime}`,
    },
    { title: '容量', dataIndex: 'capacity', key: 'capacity' },
    { title: '状态', dataIndex: 'status', key: 'status' },
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => openEditShift(record)}>
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            danger
            onClick={async () => {
              await commerceService.deleteShift(record.id);
              message.success('班次已删除');
              if (selectedStaffId) await fetchShifts(selectedStaffId);
            }}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <Card title="门店管理">
      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        items={[
          {
            key: 'stores',
            label: '门店',
            children: (
              <>
                <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between' }}>
                  <Input.Search
                    allowClear
                    placeholder="门店名称/编码/城市"
                    style={{ width: 260 }}
                    onSearch={(value) => {
                      setKeyword(value);
                      fetchStores(1, value);
                    }}
                  />
                  <Button type="primary" icon={<PlusOutlined />} onClick={openCreateStore}>
                    新增门店
                  </Button>
                </div>
                <Table
                  rowKey="id"
                  loading={loading}
                  columns={storeColumns}
                  dataSource={storeData?.records || []}
                  scroll={{ x: 1100 }}
                  pagination={{
                    current: page,
                    pageSize: PAGE_SIZE,
                    total: storeData?.total || 0,
                    showSizeChanger: false,
                    onChange: (next) => fetchStores(next),
                  }}
                />
              </>
            ),
          },
          {
            key: 'hours',
            label: '营业时间',
            children: (
              <>
                <Space style={{ marginBottom: 12 }}>
                  <Select
                    placeholder="选择门店"
                    style={{ width: 240 }}
                    value={selectedStoreId}
                    onChange={setSelectedStoreId}
                    options={allStores.map((store) => ({ label: store.name, value: store.id }))}
                  />
                  <Button type="primary" icon={<PlusOutlined />} onClick={openCreateHour}>
                    新增营业时间
                  </Button>
                </Space>
                <Table
                  rowKey="id"
                  loading={loading}
                  columns={hourColumns}
                  dataSource={hours}
                  pagination={false}
                />
              </>
            ),
          },
          {
            key: 'staff',
            label: '员工',
            children: (
              <>
                <Space style={{ marginBottom: 12 }}>
                  <Select
                    placeholder="选择门店"
                    style={{ width: 240 }}
                    value={selectedStoreId}
                    onChange={setSelectedStoreId}
                    options={allStores.map((store) => ({ label: store.name, value: store.id }))}
                  />
                  <Button type="primary" icon={<PlusOutlined />} onClick={openCreateStaff}>
                    新增员工
                  </Button>
                </Space>
                <Table rowKey="id" loading={loading} columns={staffColumns} dataSource={staffList} pagination={false} />
              </>
            ),
          },
          {
            key: 'shifts',
            label: '班次',
            children: (
              <>
                <Space style={{ marginBottom: 12 }}>
                  <Select
                    placeholder="选择门店"
                    style={{ width: 200 }}
                    value={selectedStoreId}
                    onChange={(value) => {
                      setSelectedStoreId(value);
                      setSelectedStaffId(undefined);
                    }}
                    options={allStores.map((store) => ({ label: store.name, value: store.id }))}
                  />
                  <Select
                    placeholder="选择员工"
                    style={{ width: 200 }}
                    value={selectedStaffId}
                    onChange={setSelectedStaffId}
                    options={staffList.map((staff) => ({ label: staff.staffName, value: staff.id }))}
                  />
                  <Button type="primary" icon={<PlusOutlined />} onClick={openCreateShift}>
                    新增班次
                  </Button>
                </Space>
                <Table rowKey="id" loading={loading} columns={shiftColumns} dataSource={shifts} pagination={false} />
              </>
            ),
          },
        ]}
      />

      <Modal
        title={editingStore ? `编辑门店：${editingStore.name}` : '新增门店'}
        open={storeModalOpen}
        onCancel={() => setStoreModalOpen(false)}
        onOk={handleSaveStore}
        confirmLoading={storeSaving}
        okText="保存"
        cancelText="取消"
        width={760}
        destroyOnHidden
      >
        <Form form={storeForm} layout="vertical">
          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item label="门店编码" name="storeCode" rules={[{ required: true, message: '请输入门店编码' }]} style={{ width: 220 }}>
              <Input />
            </Form.Item>
            <Form.Item label="门店名称" name="name" rules={[{ required: true, message: '请输入门店名称' }]} style={{ width: 220 }}>
              <Input />
            </Form.Item>
            <Form.Item label="电话" name="phone" style={{ width: 180 }}>
              <Input />
            </Form.Item>
          </Space>
          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item label="省" name="province" style={{ width: 160 }}>
              <Input />
            </Form.Item>
            <Form.Item label="市" name="city" style={{ width: 160 }}>
              <Input />
            </Form.Item>
            <Form.Item label="区" name="district" style={{ width: 160 }}>
              <Input />
            </Form.Item>
            <Form.Item label="状态" name="businessStatus" style={{ width: 140 }}>
              <Select options={STORE_STATUS_OPTIONS} />
            </Form.Item>
          </Space>
          <Form.Item label="详细地址" name="address">
            <Input />
          </Form.Item>
          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item label="经度" name="longitude" style={{ width: 180 }}>
              <InputNumber style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="纬度" name="latitude" style={{ width: 180 }}>
              <InputNumber style={{ width: '100%' }} />
            </Form.Item>
          </Space>
          <Form.Item label="门店说明" name="description">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editingHour ? '编辑营业时间' : '新增营业时间'}
        open={hourModalOpen}
        onCancel={() => setHourModalOpen(false)}
        onOk={handleSaveHour}
        okText="保存"
        cancelText="取消"
        destroyOnHidden
      >
        <Form form={hourForm} layout="vertical">
          <Form.Item label="星期" name="dayOfWeek" rules={[{ required: true, message: '请选择星期' }]}>
            <Select options={DAY_OPTIONS} />
          </Form.Item>
          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item label="开始时间" name="openTime" rules={[{ required: true, message: '请选择开始时间' }]}>
              <TimePicker format="HH:mm:ss" style={{ width: 160 }} />
            </Form.Item>
            <Form.Item label="结束时间" name="closeTime" rules={[{ required: true, message: '请选择结束时间' }]}>
              <TimePicker format="HH:mm:ss" style={{ width: 160 }} />
            </Form.Item>
          </Space>
          <Form.Item label="特殊日期" name="specialDate">
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="停业" name="closed">
            <Select options={[{ label: '营业', value: 0 }, { label: '停业', value: 1 }]} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editingStaff ? `编辑员工：${editingStaff.staffName}` : '新增员工'}
        open={staffModalOpen}
        onCancel={() => setStaffModalOpen(false)}
        onOk={handleSaveStaff}
        okText="保存"
        cancelText="取消"
        destroyOnHidden
      >
        <Form form={staffForm} layout="vertical">
          <Form.Item label="姓名" name="staffName" rules={[{ required: true, message: '请输入姓名' }]}>
            <Input />
          </Form.Item>
          <Form.Item label="手机号" name="mobile">
            <Input />
          </Form.Item>
          <Form.Item label="角色编码" name="roleCode">
            <Input placeholder="如 GROOMER" />
          </Form.Item>
          <Form.Item label="状态" name="status">
            <Select options={[{ label: '在职', value: 'ACTIVE' }, { label: '停用', value: 'INACTIVE' }]} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editingShift ? '编辑班次' : '新增班次'}
        open={shiftModalOpen}
        onCancel={() => setShiftModalOpen(false)}
        onOk={handleSaveShift}
        okText="保存"
        cancelText="取消"
        destroyOnHidden
      >
        <StaffShiftForm form={shiftForm} />
      </Modal>
    </Card>
  );
};

export default StoreManagementPage;
