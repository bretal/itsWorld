import React, { useEffect, useState } from 'react';
import {
  Button,
  Card,
  Divider,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Upload,
  message,
} from 'antd';
import { CameraOutlined, MinusCircleOutlined, PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { UploadFile } from 'antd/es/upload/interface';
import * as commerceService from '@/services/adminCommerceService';
import * as uploadService from '@/services/uploadService';
import { centsToYuan, formatMoney, yuanToCents } from '@/utils/format';
import type { PageData } from '@/types/common';
import type {
  GroomingServiceEntity,
  PackageTemplate,
  PackageTemplateSaveRequest,
} from '@/types/commerce';

const PAGE_SIZE = 10;

const STATUS_OPTIONS = [
  { label: '启用', value: 'ENABLED' },
  { label: '停用', value: 'DISABLED' },
];

const PackageManagementPage: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<PageData<PackageTemplate> | null>(null);
  const [page, setPage] = useState(1);
  const [status, setStatus] = useState<string | undefined>(undefined);
  const [keyword, setKeyword] = useState('');
  const [services, setServices] = useState<GroomingServiceEntity[]>([]);

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<PackageTemplate | null>(null);
  const [coverUrl, setCoverUrl] = useState('');
  const [uploading, setUploading] = useState(false);
  const [saving, setSaving] = useState(false);

  const serviceOptions = services.map((service) => ({
    label: `${service.name}（${service.serviceCode}）`,
    value: service.id,
  }));

  const fetchPackages = async (
    current = 1,
    searchStatus = status,
    searchKeyword = keyword
  ) => {
    try {
      setLoading(true);
      const result = await commerceService.getPackageTemplates(
        current,
        PAGE_SIZE,
        searchStatus,
        searchKeyword
      );
      setData(result);
      setPage(current);
    } catch (error: any) {
      message.error(error.message || '加载套餐失败');
    } finally {
      setLoading(false);
    }
  };

  const fetchServices = async () => {
    try {
      const result = await commerceService.getGroomingServices(1, 200);
      setServices(result.records || []);
    } catch {
      // 下拉数据失败不阻塞
    }
  };

  useEffect(() => {
    fetchPackages(1, undefined, '');
    fetchServices();
  }, []);

  const openCreate = () => {
    setEditing(null);
    setCoverUrl('');
    setModalOpen(true);
    form.resetFields();
    form.setFieldsValue({
      salePriceYuan: 0,
      status: 'ENABLED',
      allowCrossStore: 0,
      items: [{}],
      options: [],
    });
  };

  const openEdit = async (record: PackageTemplate) => {
    try {
      const detail = await commerceService.getPackageTemplate(record.id);
      setEditing(detail);
      setCoverUrl(detail.coverUrl || '');
      setModalOpen(true);
      form.setFieldsValue({
        packageCode: detail.packageCode,
        name: detail.name,
        description: detail.description,
        salePriceYuan: centsToYuan(detail.salePrice),
        validDays: detail.validDays,
        status: detail.status,
        allowCrossStore: detail.allowCrossStore,
        items: (detail.items || []).map((item) => ({
          serviceId: item.serviceId,
          includedTimes: item.includedTimes,
          extraPriceYuan: centsToYuan(item.extraPrice || 0),
        })),
        options: (detail.options || []).map((option) => ({
          optionCode: option.optionCode,
          optionName: option.optionName,
          minSelect: option.minSelect,
          maxSelect: option.maxSelect,
          required: option.required,
          items: (option.items || []).map((item) => ({
            serviceId: item.serviceId,
            addPriceYuan: centsToYuan(item.addPrice || 0),
            includedTimes: item.includedTimes,
          })),
        })),
      });
    } catch (error: any) {
      message.error(error.message || '加载套餐详情失败');
    }
  };

  const handleCoverUpload = async (fileList: UploadFile[]) => {
    if (!fileList.length || !fileList[0].originFileObj) return;
    try {
      setUploading(true);
      const url = await uploadService.uploadImage(
        fileList[0].originFileObj as File,
        'shop'
      );
      setCoverUrl(url);
      message.success('封面已上传');
    } catch (error: any) {
      message.error(error.message || '上传失败');
    } finally {
      setUploading(false);
    }
  };

  const buildPayload = (values: any): PackageTemplateSaveRequest => ({
    packageCode: values.packageCode,
    name: values.name,
    coverUrl: coverUrl || undefined,
    description: values.description,
    salePrice: yuanToCents(values.salePriceYuan || 0),
    validDays: values.validDays,
    status: values.status,
    allowCrossStore: values.allowCrossStore,
    items: (values.items || [])
      .filter((item: any) => item?.serviceId)
      .map((item: any) => ({
        serviceId: item.serviceId,
        includedTimes: item.includedTimes,
        extraPrice: yuanToCents(item.extraPriceYuan || 0),
      })),
    options: (values.options || []).map((option: any, index: number) => ({
      optionCode: option.optionCode,
      optionName: option.optionName,
      minSelect: option.minSelect,
      maxSelect: option.maxSelect,
      required: option.required || 0,
      sort: index,
      items: (option.items || [])
        .filter((item: any) => item?.serviceId)
        .map((item: any) => ({
          serviceId: item.serviceId,
          addPrice: yuanToCents(item.addPriceYuan || 0),
          includedTimes: item.includedTimes,
          status: 'ENABLED',
        })),
    })),
  });

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      setSaving(true);
      const payload = buildPayload(values);
      if (editing) {
        await commerceService.updatePackageTemplate(editing.id, payload);
        message.success('套餐已更新');
      } else {
        await commerceService.createPackageTemplate(payload);
        message.success('套餐已创建');
      }
      setModalOpen(false);
      await fetchPackages(page);
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error.message || '保存失败');
    } finally {
      setSaving(false);
    }
  };

  const columns: ColumnsType<PackageTemplate> = [
    {
      title: '套餐',
      key: 'package',
      render: (_, record) => (
        <Space>
          {record.coverUrl ? (
            <img
              src={record.coverUrl}
              alt=""
              style={{ width: 44, height: 44, objectFit: 'cover', borderRadius: 4 }}
            />
          ) : null}
          <Space direction="vertical" size={0}>
            <span style={{ fontWeight: 500 }}>{record.name}</span>
            <span style={{ color: '#999', fontSize: 12 }}>{record.packageCode}</span>
          </Space>
        </Space>
      ),
    },
    {
      title: '售价（元）',
      dataIndex: 'salePrice',
      key: 'salePrice',
      width: 120,
      render: (value: number) => formatMoney(value),
    },
    {
      title: '有效期',
      dataIndex: 'validDays',
      key: 'validDays',
      width: 100,
      render: (value?: number) => (value ? `${value} 天` : '长期'),
    },
    {
      title: '固定项',
      key: 'items',
      width: 90,
      render: (_, record) => `${record.items?.length || 0} 项`,
    },
    {
      title: '选项组',
      key: 'options',
      width: 90,
      render: (_, record) => `${record.options?.length || 0} 组`,
    },
    {
      title: '跨店',
      dataIndex: 'allowCrossStore',
      key: 'allowCrossStore',
      width: 80,
      render: (value: number) => (value === 1 ? '允许' : '不允许'),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 90,
      render: (value: string) =>
        value === 'ENABLED' ? <Tag color="success">启用</Tag> : <Tag>停用</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      width: 160,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => openEdit(record)}>
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            onClick={async () => {
              try {
                await commerceService.changePackageTemplateStatus(
                  record.id,
                  record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
                );
                message.success('套餐状态已更新');
                await fetchPackages(page);
              } catch (error: any) {
                message.error(error.message || '操作失败');
              }
            }}
          >
            {record.status === 'ENABLED' ? '停用' : '启用'}
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <Card title="套餐管理">
      <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between' }}>
        <Space>
          <Select
            allowClear
            placeholder="全部状态"
            style={{ width: 140 }}
            value={status}
            onChange={(value) => {
              setStatus(value);
              fetchPackages(1, value, keyword);
            }}
            options={STATUS_OPTIONS}
          />
          <Input.Search
            allowClear
            placeholder="套餐名称/编码"
            style={{ width: 220 }}
            onSearch={(value) => {
              setKeyword(value);
              fetchPackages(1, status, value);
            }}
          />
        </Space>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          新增套餐
        </Button>
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
          onChange: (next) => fetchPackages(next),
        }}
      />

      <Modal
        title={editing ? `编辑套餐：${editing.name}` : '新增套餐'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSave}
        confirmLoading={saving}
        okText="保存"
        cancelText="取消"
        width={980}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <Space style={{ display: 'flex' }} align="baseline" wrap>
            <Form.Item
              label="套餐编码"
              name="packageCode"
              rules={[{ required: true, message: '请输入套餐编码' }]}
              style={{ width: 180 }}
            >
              <Input />
            </Form.Item>
            <Form.Item
              label="套餐名称"
              name="name"
              rules={[{ required: true, message: '请输入套餐名称' }]}
              style={{ width: 220 }}
            >
              <Input />
            </Form.Item>
            <Form.Item
              label="售价（元）"
              name="salePriceYuan"
              rules={[{ required: true, message: '请输入售价' }]}
              style={{ width: 140 }}
            >
              <InputNumber min={0} step={0.01} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="有效期（天）" name="validDays" style={{ width: 130 }}>
              <InputNumber min={1} style={{ width: '100%' }} placeholder="留空长期" />
            </Form.Item>
            <Form.Item label="状态" name="status" style={{ width: 110 }}>
              <Select options={STATUS_OPTIONS} />
            </Form.Item>
            <Form.Item label="跨店" name="allowCrossStore" style={{ width: 110 }}>
              <Select
                options={[
                  { label: '不允许', value: 0 },
                  { label: '允许', value: 1 },
                ]}
              />
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
              {coverUrl && (
                <img
                  src={coverUrl}
                  alt=""
                  style={{ width: 72, height: 72, objectFit: 'cover', borderRadius: 4 }}
                />
              )}
            </Space>
          </Form.Item>

          <Form.Item label="套餐说明" name="description">
            <Input.TextArea rows={2} />
          </Form.Item>

          <Divider titlePlacement="start">固定服务项</Divider>
          <Form.List name="items">
            {(fields, { add, remove }) => (
              <>
                {fields.map((field) => (
                  <Space key={field.key} align="baseline" style={{ display: 'flex' }} wrap>
                    <Form.Item
                      label="服务"
                      name={[field.name, 'serviceId']}
                      rules={[{ required: true, message: '请选择服务' }]}
                      style={{ width: 280 }}
                    >
                      <Select options={serviceOptions} showSearch optionFilterProp="label" />
                    </Form.Item>
                    <Form.Item
                      label="包含次数"
                      name={[field.name, 'includedTimes']}
                      rules={[{ required: true, message: '请输入次数' }]}
                      style={{ width: 110 }}
                    >
                      <InputNumber min={1} style={{ width: '100%' }} />
                    </Form.Item>
                    <Form.Item label="加价（元）" name={[field.name, 'extraPriceYuan']} style={{ width: 120 }}>
                      <InputNumber min={0} step={0.01} style={{ width: '100%' }} />
                    </Form.Item>
                    <Button
                      type="text"
                      danger
                      icon={<MinusCircleOutlined />}
                      onClick={() => remove(field.name)}
                    />
                  </Space>
                ))}
                <Button type="dashed" onClick={() => add()} icon={<PlusOutlined />} style={{ marginBottom: 16 }}>
                  添加固定服务项
                </Button>
              </>
            )}
          </Form.List>

          <Divider titlePlacement="start">选项组</Divider>
          <Form.List name="options">
            {(optionFields, { add: addOption, remove: removeOption }) => (
              <>
                {optionFields.map((optionField) => (
                  <Card
                    key={optionField.key}
                    size="small"
                    style={{ marginBottom: 12, background: '#fafafa' }}
                    title={`选项组 #${optionField.name + 1}`}
                    extra={
                      <Button
                        type="text"
                        danger
                        icon={<MinusCircleOutlined />}
                        onClick={() => removeOption(optionField.name)}
                      />
                    }
                  >
                    <Space style={{ display: 'flex' }} align="baseline" wrap>
                      <Form.Item
                        label="选项编码"
                        name={[optionField.name, 'optionCode']}
                        rules={[{ required: true, message: '请输入选项编码' }]}
                        style={{ width: 160 }}
                      >
                        <Input />
                      </Form.Item>
                      <Form.Item
                        label="选项名称"
                        name={[optionField.name, 'optionName']}
                        rules={[{ required: true, message: '请输入选项名称' }]}
                        style={{ width: 180 }}
                      >
                        <Input />
                      </Form.Item>
                      <Form.Item
                        label="最少选"
                        name={[optionField.name, 'minSelect']}
                        rules={[{ required: true, message: '请输入' }]}
                        style={{ width: 100 }}
                      >
                        <InputNumber min={0} style={{ width: '100%' }} />
                      </Form.Item>
                      <Form.Item
                        label="最多选"
                        name={[optionField.name, 'maxSelect']}
                        rules={[{ required: true, message: '请输入' }]}
                        style={{ width: 100 }}
                      >
                        <InputNumber min={0} style={{ width: '100%' }} />
                      </Form.Item>
                      <Form.Item label="必选" name={[optionField.name, 'required']} style={{ width: 100 }}>
                        <Select
                          options={[
                            { label: '否', value: 0 },
                            { label: '是', value: 1 },
                          ]}
                        />
                      </Form.Item>
                    </Space>
                    <Form.List name={[optionField.name, 'items']}>
                      {(itemFields, { add: addItem, remove: removeItem }) => (
                        <>
                          {itemFields.map((itemField) => (
                            <Space key={itemField.key} align="baseline" style={{ display: 'flex' }} wrap>
                              <Form.Item
                                label="候选服务"
                                name={[itemField.name, 'serviceId']}
                                rules={[{ required: true, message: '请选择服务' }]}
                                style={{ width: 280 }}
                              >
                                <Select options={serviceOptions} showSearch optionFilterProp="label" />
                              </Form.Item>
                              <Form.Item
                                label="加价（元）"
                                name={[itemField.name, 'addPriceYuan']}
                                style={{ width: 120 }}
                              >
                                <InputNumber min={0} step={0.01} style={{ width: '100%' }} />
                              </Form.Item>
                              <Form.Item
                                label="包含次数"
                                name={[itemField.name, 'includedTimes']}
                                rules={[{ required: true, message: '请输入次数' }]}
                                style={{ width: 110 }}
                              >
                                <InputNumber min={1} style={{ width: '100%' }} />
                              </Form.Item>
                              <Button
                                type="text"
                                danger
                                icon={<MinusCircleOutlined />}
                                onClick={() => removeItem(itemField.name)}
                              />
                            </Space>
                          ))}
                          <Button
                            type="dashed"
                            onClick={() => addItem({ includedTimes: 1, addPriceYuan: 0 })}
                            icon={<PlusOutlined />}
                          >
                            添加候选服务
                          </Button>
                        </>
                      )}
                    </Form.List>
                  </Card>
                ))}
                <Button type="dashed" onClick={() => addOption({ minSelect: 0, maxSelect: 1, required: 0 })} icon={<PlusOutlined />}>
                  添加选项组
                </Button>
              </>
            )}
          </Form.List>
        </Form>
      </Modal>
    </Card>
  );
};

export default PackageManagementPage;
