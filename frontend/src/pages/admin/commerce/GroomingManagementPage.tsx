import ImageUpload from '@/components/ImageUpload';
import React, { useEffect, useState } from 'react';
import {
  Button,
  Card,
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
  Upload,
  message,
} from 'antd';
import { CameraOutlined, PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { UploadFile } from 'antd/es/upload/interface';
import * as commerceService from '@/services/adminCommerceService';
import * as uploadService from '@/services/uploadService';
import { centsToYuan, formatMoney, yuanToCents } from '@/utils/format';
import type { PageData } from '@/types/common';
import type {
  CommerceStore,
  GroomingCategory,
  GroomingPetRule,
  GroomingServiceEntity,
  GroomingServiceStore,
} from '@/types/commerce';

const PAGE_SIZE = 10;

const STATUS_OPTIONS = [
  { label: '启用', value: 'ENABLED' },
  { label: '停用', value: 'DISABLED' },
];

const GroomingManagementPage: React.FC = () => {
  const [categoryForm] = Form.useForm();
  const [serviceForm] = Form.useForm();
  const [serviceStoreForm] = Form.useForm();
  const [petRuleForm] = Form.useForm();

  const [activeTab, setActiveTab] = useState('categories');
  const [loading, setLoading] = useState(false);
  const [categories, setCategories] = useState<GroomingCategory[]>([]);
  const [serviceData, setServiceData] = useState<PageData<GroomingServiceEntity> | null>(null);
  const [stores, setStores] = useState<CommerceStore[]>([]);
  const [page, setPage] = useState(1);
  const [categoryFilter, setCategoryFilter] = useState<number | undefined>(undefined);
  const [keyword, setKeyword] = useState('');

  const [categoryModalOpen, setCategoryModalOpen] = useState(false);
  const [editingCategory, setEditingCategory] = useState<GroomingCategory | null>(null);

  const [serviceModalOpen, setServiceModalOpen] = useState(false);
  const [editingService, setEditingService] = useState<GroomingServiceEntity | null>(null);
  const [serviceCoverUrl, setServiceCoverUrl] = useState('');
  const [uploading, setUploading] = useState(false);

  const [storeDrawerOpen, setStoreDrawerOpen] = useState(false);
  const [selectedService, setSelectedService] = useState<GroomingServiceEntity | null>(null);
  const [serviceStores, setServiceStores] = useState<GroomingServiceStore[]>([]);
  const [serviceStoreModalOpen, setServiceStoreModalOpen] = useState(false);
  const [editingServiceStore, setEditingServiceStore] = useState<GroomingServiceStore | null>(null);

  const [ruleDrawerOpen, setRuleDrawerOpen] = useState(false);
  const [petRules, setPetRules] = useState<GroomingPetRule[]>([]);
  const [ruleModalOpen, setRuleModalOpen] = useState(false);
  const [editingRule, setEditingRule] = useState<GroomingPetRule | null>(null);

  const fetchCategories = async () => {
    try {
      setCategories(await commerceService.getGroomingCategories());
    } catch (error: any) {
      message.error(error.message || '加载分类失败');
    }
  };

  const fetchServices = async (current = 1, searchKeyword = keyword, category = categoryFilter) => {
    try {
      setLoading(true);
      const result = await commerceService.getGroomingServices(
        current,
        PAGE_SIZE,
        category,
        searchKeyword
      );
      setServiceData(result);
      setPage(current);
    } catch (error: any) {
      message.error(error.message || '加载服务失败');
    } finally {
      setLoading(false);
    }
  };

  const fetchStores = async () => {
    try {
      const result = await commerceService.getStores(1, 100);
      setStores(result.records || []);
    } catch {
      // 下拉数据失败不阻塞页面
    }
  };

  useEffect(() => {
    fetchCategories();
    fetchServices(1, '', undefined);
    fetchStores();
  }, []);

  const categoryNameMap = new Map<number, string>();
  categories.forEach((category) => categoryNameMap.set(category.id, category.name));
  const storeNameMap = new Map<number, string>();
  stores.forEach((store) => storeNameMap.set(store.id, store.name));

  const openCreateCategory = () => {
    setEditingCategory(null);
    setCategoryModalOpen(true);
    categoryForm.resetFields();
    categoryForm.setFieldsValue({ sort: 0, status: 'ENABLED' });
  };

  const openEditCategory = (category: GroomingCategory) => {
    setEditingCategory(category);
    setCategoryModalOpen(true);
    categoryForm.setFieldsValue(category);
  };

  const handleSaveCategory = async () => {
    try {
      const values = await categoryForm.validateFields();
      if (editingCategory) {
        await commerceService.updateGroomingCategory(editingCategory.id, values);
        message.success('分类已更新');
      } else {
        await commerceService.createGroomingCategory(values);
        message.success('分类已创建');
      }
      setCategoryModalOpen(false);
      await fetchCategories();
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error.message || '保存失败');
    }
  };

  const openCreateService = () => {
    setEditingService(null);
    setServiceModalOpen(true);
    serviceForm.resetFields();
    serviceForm.setFieldsValue({ durationMin: 60, basePriceYuan: 0, status: 'ENABLED', sort: 0 });
    setServiceCoverUrl('');
  };

  const openEditService = (service: GroomingServiceEntity) => {
    setEditingService(service);
    setServiceModalOpen(true);
    serviceForm.setFieldsValue({
      ...service,
      basePriceYuan: centsToYuan(service.basePrice),
    });
    setServiceCoverUrl(service.coverUrl || '');
  };

  const handleCoverUpload = async (fileList: UploadFile[]) => {
    if (!fileList.length || !fileList[0].originFileObj) return;
    try {
      setUploading(true);
      const url = await uploadService.uploadImage(
        fileList[0].originFileObj as File,
        'shop'
      );
      setServiceCoverUrl(url);
      message.success('封面已上传');
    } catch (error: any) {
      message.error(error.message || '上传失败');
    } finally {
      setUploading(false);
    }
  };

  const handleSaveService = async () => {
    try {
      const values = await serviceForm.validateFields();
      const payload = {
        categoryId: values.categoryId,
        serviceCode: values.serviceCode,
        name: values.name,
        coverUrl: serviceCoverUrl || undefined,
        description: values.description,
        durationMin: values.durationMin,
        basePrice: yuanToCents(values.basePriceYuan),
        status: values.status,
        sort: values.sort,
      };
      if (editingService) {
        await commerceService.updateGroomingService(editingService.id, payload);
        message.success('服务已更新');
      } else {
        await commerceService.createGroomingService(payload);
        message.success('服务已创建');
      }
      setServiceModalOpen(false);
      await fetchServices(page);
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error.message || '保存失败');
    }
  };

  const openStoreDrawer = async (service: GroomingServiceEntity) => {
    setSelectedService(service);
    setStoreDrawerOpen(true);
    try {
      setServiceStores(await commerceService.getGroomingServiceStores(service.id));
    } catch (error: any) {
      message.error(error.message || '加载门店价格失败');
    }
  };

  const openCreateServiceStore = () => {
    setEditingServiceStore(null);
    setServiceStoreModalOpen(true);
    serviceStoreForm.resetFields();
    serviceStoreForm.setFieldsValue({ capacityPerSlot: 1, status: 'ENABLED' });
  };

  const openEditServiceStore = (record: GroomingServiceStore) => {
    setEditingServiceStore(record);
    setServiceStoreModalOpen(true);
    serviceStoreForm.setFieldsValue({
      storeId: record.storeId,
      salePriceYuan: centsToYuan(record.salePrice),
      capacityPerSlot: record.capacityPerSlot,
      status: record.status,
    });
  };

  const handleSaveServiceStore = async () => {
    if (!selectedService) return;
    try {
      const values = await serviceStoreForm.validateFields();
      const payload = {
        storeId: values.storeId,
        salePrice: yuanToCents(values.salePriceYuan),
        capacityPerSlot: values.capacityPerSlot,
        status: values.status,
      };
      if (editingServiceStore) {
        await commerceService.updateGroomingServiceStore(editingServiceStore.id, payload);
        message.success('门店价格已更新');
      } else {
        await commerceService.createGroomingServiceStore(selectedService.id, payload);
        message.success('门店价格已创建');
      }
      setServiceStoreModalOpen(false);
      setServiceStores(await commerceService.getGroomingServiceStores(selectedService.id));
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error.message || '保存失败');
    }
  };

  const openRuleDrawer = async (service: GroomingServiceEntity) => {
    setSelectedService(service);
    setRuleDrawerOpen(true);
    try {
      setPetRules(await commerceService.getGroomingPetRules(service.id));
    } catch (error: any) {
      message.error(error.message || '加载宠物规则失败');
    }
  };

  const openCreateRule = () => {
    setEditingRule(null);
    setRuleModalOpen(true);
    petRuleForm.resetFields();
  };

  const openEditRule = (rule: GroomingPetRule) => {
    setEditingRule(rule);
    setRuleModalOpen(true);
    petRuleForm.setFieldsValue(rule);
  };

  const handleSaveRule = async () => {
    if (!selectedService) return;
    try {
      const values = await petRuleForm.validateFields();
      if (editingRule) {
        await commerceService.updateGroomingPetRule(editingRule.id, values);
        message.success('宠物规则已更新');
      } else {
        await commerceService.createGroomingPetRule(selectedService.id, values);
        message.success('宠物规则已创建');
      }
      setRuleModalOpen(false);
      setPetRules(await commerceService.getGroomingPetRules(selectedService.id));
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error.message || '保存失败');
    }
  };

  const categoryColumns: ColumnsType<GroomingCategory> = [
    { title: '分类名称', dataIndex: 'name', key: 'name' },
    { title: '排序', dataIndex: 'sort', key: 'sort', width: 80 },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (value: string) => (value === 'ENABLED' ? <Tag color="success">启用</Tag> : <Tag>停用</Tag>),
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => openEditCategory(record)}>
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            onClick={async () => {
              await commerceService.changeGroomingCategoryStatus(
                record.id,
                record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
              );
              message.success('分类状态已更新');
              await fetchCategories();
            }}
          >
            {record.status === 'ENABLED' ? '停用' : '启用'}
          </Button>
        </Space>
      ),
    },
  ];

  const serviceColumns: ColumnsType<GroomingServiceEntity> = [
    {
      title: '服务',
      key: 'service',
      render: (_, record) => (
        <Space>
          {record.coverUrl ? (
            <img src={record.coverUrl} alt="" style={{ width: 40, height: 40, objectFit: 'cover', borderRadius: 4 }} />
          ) : null}
          <Space direction="vertical" size={0}>
            <span style={{ fontWeight: 500 }}>{record.name}</span>
            <span style={{ color: '#999', fontSize: 12 }}>{record.serviceCode}</span>
          </Space>
        </Space>
      ),
    },
    {
      title: '分类',
      dataIndex: 'categoryId',
      key: 'categoryId',
      width: 120,
      render: (value?: number) => (value ? categoryNameMap.get(value) || `分类#${value}` : '-'),
    },
    { title: '时长(分钟)', dataIndex: 'durationMin', key: 'durationMin', width: 100 },
    {
      title: '基础价(元)',
      dataIndex: 'basePrice',
      key: 'basePrice',
      width: 110,
      render: (value: number) => formatMoney(value),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 90,
      render: (value: string) => (value === 'ENABLED' ? <Tag color="success">启用</Tag> : <Tag>停用</Tag>),
    },
    {
      title: '操作',
      key: 'action',
      width: 300,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => openEditService(record)}>
            编辑
          </Button>
          <Button type="link" size="small" onClick={() => openStoreDrawer(record)}>
            门店价格
          </Button>
          <Button type="link" size="small" onClick={() => openRuleDrawer(record)}>
            宠物规则
          </Button>
          <Button
            type="link"
            size="small"
            onClick={async () => {
              await commerceService.changeGroomingServiceStatus(
                record.id,
                record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
              );
              message.success('服务状态已更新');
              await fetchServices(page);
            }}
          >
            {record.status === 'ENABLED' ? '下架' : '上架'}
          </Button>
        </Space>
      ),
    },
  ];

  const serviceStoreColumns: ColumnsType<GroomingServiceStore> = [
    {
      title: '门店',
      dataIndex: 'storeId',
      key: 'storeId',
      render: (value: number) => storeNameMap.get(value) || `门店#${value}`,
    },
    {
      title: '售价(元)',
      dataIndex: 'salePrice',
      key: 'salePrice',
      render: (value: number) => formatMoney(value),
    },
    { title: '每时段容量', dataIndex: 'capacityPerSlot', key: 'capacityPerSlot' },
    { title: '状态', dataIndex: 'status', key: 'status' },
    {
      title: '操作',
      key: 'action',
      width: 140,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => openEditServiceStore(record)}>
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            danger
            onClick={async () => {
              await commerceService.deleteGroomingServiceStore(record.id);
              message.success('已删除');
              if (selectedService) {
                setServiceStores(await commerceService.getGroomingServiceStores(selectedService.id));
              }
            }}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ];

  const ruleColumns: ColumnsType<GroomingPetRule> = [
    { title: '宠物类型', dataIndex: 'speciesCode', key: 'speciesCode' },
    {
      title: '体重(克)',
      key: 'weight',
      render: (_, record) =>
        `${record.minWeightGram ?? '-'} ~ ${record.maxWeightGram ?? '-'}`,
    },
    {
      title: '月龄',
      key: 'age',
      render: (_, record) => `${record.minAgeMonth ?? '-'} ~ ${record.maxAgeMonth ?? '-'}`,
    },
    { title: '说明', dataIndex: 'note', key: 'note' },
    {
      title: '操作',
      key: 'action',
      width: 140,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => openEditRule(record)}>
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            danger
            onClick={async () => {
              await commerceService.deleteGroomingPetRule(record.id);
              message.success('已删除');
              if (selectedService) {
                setPetRules(await commerceService.getGroomingPetRules(selectedService.id));
              }
            }}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <Card title="美容服务">
      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        items={[
          {
            key: 'categories',
            label: '美容分类',
            children: (
              <>
                <div style={{ marginBottom: 12, textAlign: 'right' }}>
                  <Button type="primary" icon={<PlusOutlined />} onClick={openCreateCategory}>
                    新增分类
                  </Button>
                </div>
                <Table rowKey="id" columns={categoryColumns} dataSource={categories} pagination={false} />
              </>
            ),
          },
          {
            key: 'services',
            label: '服务项目',
            children: (
              <>
                <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between' }}>
                  <Space>
                    <Select
                      allowClear
                      placeholder="全部分类"
                      style={{ width: 160 }}
                      value={categoryFilter}
                      onChange={(value) => {
                        setCategoryFilter(value);
                        fetchServices(1, keyword, value);
                      }}
                      options={categories.map((category) => ({ label: category.name, value: category.id }))}
                    />
                    <Input.Search
                      allowClear
                      placeholder="服务名称/编码"
                      style={{ width: 220 }}
                      onSearch={(value) => {
                        setKeyword(value);
                        fetchServices(1, value, categoryFilter);
                      }}
                    />
                  </Space>
                  <Button type="primary" icon={<PlusOutlined />} onClick={openCreateService}>
                    新增服务
                  </Button>
                </div>
                <Table
                  rowKey="id"
                  loading={loading}
                  columns={serviceColumns}
                  dataSource={serviceData?.records || []}
                  scroll={{ x: 1050 }}
                  pagination={{
                    current: page,
                    pageSize: PAGE_SIZE,
                    total: serviceData?.total || 0,
                    showSizeChanger: false,
                    onChange: (next) => fetchServices(next),
                  }}
                />
              </>
            ),
          },
        ]}
      />

      <Modal
        title={editingCategory ? `编辑分类：${editingCategory.name}` : '新增分类'}
        open={categoryModalOpen}
        onCancel={() => setCategoryModalOpen(false)}
        onOk={handleSaveCategory}
        okText="保存"
        cancelText="取消"
        destroyOnHidden
      >
        <Form form={categoryForm} layout="vertical">
          <Form.Item label="分类名称" name="name" rules={[{ required: true, message: '请输入分类名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item label="图标" name="iconUrl">
            <ImageUpload />
          </Form.Item>
          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item label="排序" name="sort" style={{ width: 120 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="状态" name="status" style={{ width: 140 }}>
              <Select options={STATUS_OPTIONS} />
            </Form.Item>
          </Space>
        </Form>
      </Modal>

      <Modal
        title={editingService ? `编辑服务：${editingService.name}` : '新增服务'}
        open={serviceModalOpen}
        onCancel={() => setServiceModalOpen(false)}
        onOk={handleSaveService}
        okText="保存"
        cancelText="取消"
        width={720}
        destroyOnHidden
      >
        <Form form={serviceForm} layout="vertical">
          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item label="服务编码" name="serviceCode" rules={[{ required: true, message: '请输入服务编码' }]} style={{ width: 200 }}>
              <Input />
            </Form.Item>
            <Form.Item label="服务名称" name="name" rules={[{ required: true, message: '请输入服务名称' }]} style={{ width: 220 }}>
              <Input />
            </Form.Item>
            <Form.Item label="分类" name="categoryId" style={{ width: 180 }}>
              <Select allowClear options={categories.map((category) => ({ label: category.name, value: category.id }))} />
            </Form.Item>
          </Space>
          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item label="时长(分钟)" name="durationMin" rules={[{ required: true, message: '请输入时长' }]} style={{ width: 140 }}>
              <InputNumber min={1} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="基础价(元)" name="basePriceYuan" rules={[{ required: true, message: '请输入基础价' }]} style={{ width: 140 }}>
              <InputNumber min={0} step={0.01} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="排序" name="sort" style={{ width: 120 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="状态" name="status" style={{ width: 120 }}>
              <Select options={STATUS_OPTIONS} />
            </Form.Item>
          </Space>
          <Form.Item label="封面">
            <Space>
              <Upload
                maxCount={1}
                beforeUpload={() => false}
                onChange={(info) => handleCoverUpload(info.fileList)}
                showUploadList={false}
              >
                <Button icon={<CameraOutlined />} loading={uploading}>
                  上传封面
                </Button>
              </Upload>
              {serviceCoverUrl && <img src={serviceCoverUrl} alt="" style={{ width: 80, height: 80, objectFit: 'cover' }} />}
            </Space>
          </Form.Item>
          <Form.Item label="服务说明" name="description">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title={`门店价格：${selectedService?.name || ''}`}
        open={storeDrawerOpen}
        onClose={() => setStoreDrawerOpen(false)}
        width={760}
        destroyOnHidden
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreateServiceStore}>
            新增门店价格
          </Button>
        }
      >
        <Table rowKey="id" columns={serviceStoreColumns} dataSource={serviceStores} pagination={false} />
      </Drawer>

      <Drawer
        title={`宠物适配规则：${selectedService?.name || ''}`}
        open={ruleDrawerOpen}
        onClose={() => setRuleDrawerOpen(false)}
        width={760}
        destroyOnHidden
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreateRule}>
            新增规则
          </Button>
        }
      >
        <Table rowKey="id" columns={ruleColumns} dataSource={petRules} pagination={false} />
      </Drawer>

      <Modal
        title={editingServiceStore ? '编辑门店价格' : '新增门店价格'}
        open={serviceStoreModalOpen}
        onCancel={() => setServiceStoreModalOpen(false)}
        onOk={handleSaveServiceStore}
        okText="保存"
        cancelText="取消"
        destroyOnHidden
      >
        <Form form={serviceStoreForm} layout="vertical">
          <Form.Item label="门店" name="storeId" rules={[{ required: true, message: '请选择门店' }]}>
            <Select options={stores.map((store) => ({ label: store.name, value: store.id }))} />
          </Form.Item>
          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item label="售价(元)" name="salePriceYuan" rules={[{ required: true, message: '请输入售价' }]} style={{ width: 160 }}>
              <InputNumber min={0} step={0.01} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="每时段容量" name="capacityPerSlot" rules={[{ required: true, message: '请输入容量' }]} style={{ width: 140 }}>
              <InputNumber min={1} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="状态" name="status" style={{ width: 120 }}>
              <Select options={STATUS_OPTIONS} />
            </Form.Item>
          </Space>
        </Form>
      </Modal>

      <Modal
        title={editingRule ? '编辑宠物规则' : '新增宠物规则'}
        open={ruleModalOpen}
        onCancel={() => setRuleModalOpen(false)}
        onOk={handleSaveRule}
        okText="保存"
        cancelText="取消"
        width={620}
        destroyOnHidden
      >
        <Form form={petRuleForm} layout="vertical">
          <Form.Item label="宠物类型" name="speciesCode" rules={[{ required: true, message: '请输入宠物类型' }]}>
            <Input placeholder="dog / cat / rabbit ..." />
          </Form.Item>
          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item label="最低体重(克)" name="minWeightGram" style={{ width: 150 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="最高体重(克)" name="maxWeightGram" style={{ width: 150 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="最低月龄" name="minAgeMonth" style={{ width: 120 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="最高月龄" name="maxAgeMonth" style={{ width: 120 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
          </Space>
          <Form.Item label="说明" name="note">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

export default GroomingManagementPage;
