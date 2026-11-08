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
  ShopCategory,
  ShopProductDetail,
  ShopProductEntity,
  ShopProductImage,
  ShopSkuView,
} from '@/types/commerce';

const PAGE_SIZE = 10;

const SALE_STATUS_OPTIONS = [
  { label: '上架', value: 'ON_SALE' },
  { label: '下架', value: 'OFF_SALE' },
];

const ProductManagementPage: React.FC = () => {
  const [categoryForm] = Form.useForm();
  const [productForm] = Form.useForm();
  const [skuForm] = Form.useForm();

  const [activeTab, setActiveTab] = useState('categories');
  const [loading, setLoading] = useState(false);
  const [categories, setCategories] = useState<ShopCategory[]>([]);
  const [productData, setProductData] = useState<PageData<ShopProductEntity> | null>(null);
  const [page, setPage] = useState(1);
  const [categoryFilter, setCategoryFilter] = useState<number | undefined>(undefined);
  const [keyword, setKeyword] = useState('');

  const [categoryModalOpen, setCategoryModalOpen] = useState(false);
  const [editingCategory, setEditingCategory] = useState<ShopCategory | null>(null);

  const [productModalOpen, setProductModalOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState<ShopProductEntity | null>(null);
  const [productCoverUrl, setProductCoverUrl] = useState('');
  const [uploading, setUploading] = useState(false);

  const [detailDrawerOpen, setDetailDrawerOpen] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState<ShopProductEntity | null>(null);
  const [productDetail, setProductDetail] = useState<ShopProductDetail | null>(null);
  const [skuModalOpen, setSkuModalOpen] = useState(false);
  const [editingSku, setEditingSku] = useState<ShopSkuView | null>(null);

  const fetchCategories = async () => {
    try {
      setCategories(await commerceService.getShopCategories());
    } catch (error: any) {
      message.error(error.message || '加载分类失败');
    }
  };

  const fetchProducts = async (current = 1, searchKeyword = keyword, category = categoryFilter) => {
    try {
      setLoading(true);
      const result = await commerceService.getShopProducts(current, PAGE_SIZE, category, searchKeyword);
      setProductData(result);
      setPage(current);
    } catch (error: any) {
      message.error(error.message || '加载商品失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCategories();
    fetchProducts(1, '', undefined);
  }, []);

  const categoryNameMap = new Map<number, string>();
  categories.forEach((category) => categoryNameMap.set(category.id, category.name));

  const openCreateCategory = () => {
    setEditingCategory(null);
    setCategoryModalOpen(true);
    categoryForm.resetFields();
    categoryForm.setFieldsValue({ sort: 0, status: 'ENABLED' });
  };

  const openEditCategory = (category: ShopCategory) => {
    setEditingCategory(category);
    setCategoryModalOpen(true);
    categoryForm.setFieldsValue(category);
  };

  const handleSaveCategory = async () => {
    try {
      const values = await categoryForm.validateFields();
      if (editingCategory) {
        await commerceService.updateShopCategory(editingCategory.id, values);
        message.success('分类已更新');
      } else {
        await commerceService.createShopCategory(values);
        message.success('分类已创建');
      }
      setCategoryModalOpen(false);
      await fetchCategories();
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error.message || '保存失败');
    }
  };

  const openCreateProduct = () => {
    setEditingProduct(null);
    setProductModalOpen(true);
    productForm.resetFields();
    productForm.setFieldsValue({ saleStatus: 'OFF_SALE', sort: 0 });
    setProductCoverUrl('');
  };

  const openEditProduct = (product: ShopProductEntity) => {
    setEditingProduct(product);
    setProductModalOpen(true);
    productForm.setFieldsValue(product);
    setProductCoverUrl(product.coverUrl || '');
  };

  const handleCoverUpload = async (fileList: UploadFile[]) => {
    if (!fileList.length || !fileList[0].originFileObj) return;
    try {
      setUploading(true);
      const url = await uploadService.uploadImage(fileList[0].originFileObj as File, 'shop');
      setProductCoverUrl(url);
      message.success('封面已上传');
    } catch (error: any) {
      message.error(error.message || '上传失败');
    } finally {
      setUploading(false);
    }
  };

  const handleSaveProduct = async () => {
    try {
      const values = await productForm.validateFields();
      const payload = {
        categoryId: values.categoryId,
        productCode: values.productCode,
        name: values.name,
        subtitle: values.subtitle,
        description: values.description,
        coverUrl: productCoverUrl || undefined,
        saleStatus: values.saleStatus,
        sort: values.sort,
      };
      if (editingProduct) {
        await commerceService.updateShopProduct(editingProduct.id, payload);
        message.success('商品已更新');
      } else {
        await commerceService.createShopProduct(payload);
        message.success('商品已创建');
      }
      setProductModalOpen(false);
      await fetchProducts(page);
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error.message || '保存失败');
    }
  };

  const openDetailDrawer = async (product: ShopProductEntity) => {
    setSelectedProduct(product);
    setDetailDrawerOpen(true);
    try {
      setProductDetail(await commerceService.getShopProductDetail(product.id));
    } catch (error: any) {
      message.error(error.message || '加载商品详情失败');
    }
  };

  const refreshDetail = async () => {
    if (selectedProduct) {
      setProductDetail(await commerceService.getShopProductDetail(selectedProduct.id));
    }
  };

  const handleAddImage = async (fileList: UploadFile[]) => {
    if (!selectedProduct || !fileList.length || !fileList[0].originFileObj) return;
    try {
      setUploading(true);
      const url = await uploadService.uploadImage(fileList[0].originFileObj as File, 'shop');
      await commerceService.addShopProductImage(selectedProduct.id, url, 0);
      message.success('图片已添加');
      await refreshDetail();
    } catch (error: any) {
      message.error(error.message || '图片添加失败');
    } finally {
      setUploading(false);
    }
  };

  const openCreateSku = () => {
    setEditingSku(null);
    setSkuModalOpen(true);
    skuForm.resetFields();
    skuForm.setFieldsValue({ salePriceYuan: 0, marketPriceYuan: 0, status: 'ENABLED' });
  };

  const openEditSku = (sku: ShopSkuView) => {
    setEditingSku(sku);
    setSkuModalOpen(true);
    skuForm.setFieldsValue({
      skuCode: sku.skuCode,
      specJson: sku.specJson,
      salePriceYuan: centsToYuan(sku.salePrice),
      marketPriceYuan: centsToYuan(sku.marketPrice),
      weightGram: sku.weightGram,
      status: sku.status,
    });
  };

  const handleSaveSku = async () => {
    if (!selectedProduct) return;
    try {
      const values = await skuForm.validateFields();
      if (values.specJson) {
        try {
          JSON.parse(values.specJson);
        } catch {
          message.error('规格 JSON 格式不正确');
          return;
        }
      }
      const payload = {
        skuCode: values.skuCode,
        specJson: values.specJson || undefined,
        salePrice: yuanToCents(values.salePriceYuan),
        marketPrice: yuanToCents(values.marketPriceYuan),
        weightGram: values.weightGram,
        status: values.status,
      };
      if (editingSku) {
        await commerceService.updateShopSku(editingSku.id, payload);
        message.success('SKU已更新');
      } else {
        await commerceService.createShopSku(selectedProduct.id, payload);
        message.success('SKU已创建');
      }
      setSkuModalOpen(false);
      await refreshDetail();
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error(error.message || '保存失败');
    }
  };

  const categoryColumns: ColumnsType<ShopCategory> = [
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
              await commerceService.changeShopCategoryStatus(
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

  const productColumns: ColumnsType<ShopProductEntity> = [
    {
      title: '商品',
      key: 'product',
      render: (_, record) => (
        <Space>
          {record.coverUrl ? (
            <img src={record.coverUrl} alt="" style={{ width: 40, height: 40, objectFit: 'cover', borderRadius: 4 }} />
          ) : null}
          <Space direction="vertical" size={0}>
            <span style={{ fontWeight: 500 }}>{record.name}</span>
            <span style={{ color: '#999', fontSize: 12 }}>{record.productCode}</span>
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
    { title: '副标题', dataIndex: 'subtitle', key: 'subtitle' },
    {
      title: '销售状态',
      dataIndex: 'saleStatus',
      key: 'saleStatus',
      width: 100,
      render: (value: string) => (value === 'ON_SALE' ? <Tag color="success">上架</Tag> : <Tag>下架</Tag>),
    },
    {
      title: '操作',
      key: 'action',
      width: 280,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => openEditProduct(record)}>
            编辑
          </Button>
          <Button type="link" size="small" onClick={() => openDetailDrawer(record)}>
            图片/SKU
          </Button>
          <Button
            type="link"
            size="small"
            onClick={async () => {
              await commerceService.changeShopProductSaleStatus(
                record.id,
                record.saleStatus === 'ON_SALE' ? 'OFF_SALE' : 'ON_SALE'
              );
              message.success('商品销售状态已更新');
              await fetchProducts(page);
            }}
          >
            {record.saleStatus === 'ON_SALE' ? '下架' : '上架'}
          </Button>
        </Space>
      ),
    },
  ];

  const imageColumns: ColumnsType<ShopProductImage> = [
    {
      title: '图片',
      key: 'image',
      render: (_, record) => (
        <img src={record.imageUrl} alt="" style={{ width: 80, height: 80, objectFit: 'cover' }} />
      ),
    },
    { title: '排序', dataIndex: 'sort', key: 'sort', width: 80 },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_, record) => (
        <Button
          type="link"
          size="small"
          danger
          onClick={async () => {
            await commerceService.deleteShopProductImage(record.id);
            message.success('图片已删除');
            await refreshDetail();
          }}
        >
          删除
        </Button>
      ),
    },
  ];

  const skuColumns: ColumnsType<ShopSkuView> = [
    { title: 'SKU编码', dataIndex: 'skuCode', key: 'skuCode' },
    { title: '规格', dataIndex: 'specJson', key: 'specJson', render: (value?: string) => value || '-' },
    {
      title: '销售价(元)',
      dataIndex: 'salePrice',
      key: 'salePrice',
      render: (value: number) => formatMoney(value),
    },
    {
      title: '市场价(元)',
      dataIndex: 'marketPrice',
      key: 'marketPrice',
      render: (value: number) => formatMoney(value),
    },
    { title: '库存', dataIndex: 'availableQty', key: 'availableQty', width: 80 },
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
      width: 160,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => openEditSku(record)}>
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            onClick={async () => {
              await commerceService.changeShopSkuStatus(
                record.id,
                record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
              );
              message.success('SKU状态已更新');
              await refreshDetail();
            }}
          >
            {record.status === 'ENABLED' ? '停用' : '启用'}
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <Card title="商品管理">
      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        items={[
          {
            key: 'categories',
            label: '商品分类',
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
            key: 'products',
            label: '商品列表',
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
                        fetchProducts(1, keyword, value);
                      }}
                      options={categories.map((category) => ({ label: category.name, value: category.id }))}
                    />
                    <Input.Search
                      allowClear
                      placeholder="商品名称/编码"
                      style={{ width: 220 }}
                      onSearch={(value) => {
                        setKeyword(value);
                        fetchProducts(1, value, categoryFilter);
                      }}
                    />
                  </Space>
                  <Button type="primary" icon={<PlusOutlined />} onClick={openCreateProduct}>
                    新增商品
                  </Button>
                </div>
                <Table
                  rowKey="id"
                  loading={loading}
                  columns={productColumns}
                  dataSource={productData?.records || []}
                  scroll={{ x: 1000 }}
                  pagination={{
                    current: page,
                    pageSize: PAGE_SIZE,
                    total: productData?.total || 0,
                    showSizeChanger: false,
                    onChange: (next) => fetchProducts(next),
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
          <Form.Item label="图标URL" name="iconUrl">
            <Input />
          </Form.Item>
          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item label="排序" name="sort" style={{ width: 120 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="状态" name="status" style={{ width: 140 }}>
              <Select options={[{ label: '启用', value: 'ENABLED' }, { label: '停用', value: 'DISABLED' }]} />
            </Form.Item>
          </Space>
        </Form>
      </Modal>

      <Modal
        title={editingProduct ? `编辑商品：${editingProduct.name}` : '新增商品'}
        open={productModalOpen}
        onCancel={() => setProductModalOpen(false)}
        onOk={handleSaveProduct}
        okText="保存"
        cancelText="取消"
        width={720}
        destroyOnHidden
      >
        <Form form={productForm} layout="vertical">
          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item label="商品编码" name="productCode" rules={[{ required: true, message: '请输入商品编码' }]} style={{ width: 200 }}>
              <Input />
            </Form.Item>
            <Form.Item label="商品名称" name="name" rules={[{ required: true, message: '请输入商品名称' }]} style={{ width: 240 }}>
              <Input />
            </Form.Item>
            <Form.Item label="分类" name="categoryId" style={{ width: 180 }}>
              <Select allowClear options={categories.map((category) => ({ label: category.name, value: category.id }))} />
            </Form.Item>
          </Space>
          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item label="副标题" name="subtitle" style={{ width: 300 }}>
              <Input />
            </Form.Item>
            <Form.Item label="销售状态" name="saleStatus" style={{ width: 140 }}>
              <Select options={SALE_STATUS_OPTIONS} />
            </Form.Item>
            <Form.Item label="排序" name="sort" style={{ width: 100 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
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
              {productCoverUrl && <img src={productCoverUrl} alt="" style={{ width: 80, height: 80, objectFit: 'cover' }} />}
            </Space>
          </Form.Item>
          <Form.Item label="商品详情" name="description">
            <Input.TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title={`商品图片与SKU：${selectedProduct?.name || ''}`}
        open={detailDrawerOpen}
        onClose={() => setDetailDrawerOpen(false)}
        width={900}
        destroyOnHidden
      >
        <Tabs
          items={[
            {
              key: 'images',
              label: '图片',
              children: (
                <>
                  <div style={{ marginBottom: 12 }}>
                    <Upload
                      maxCount={1}
                      beforeUpload={() => false}
                      onChange={(info) => handleAddImage(info.fileList)}
                      showUploadList={false}
                    >
                      <Button icon={<CameraOutlined />} loading={uploading}>
                        上传图片
                      </Button>
                    </Upload>
                  </div>
                  <Table
                    rowKey="id"
                    columns={imageColumns}
                    dataSource={productDetail?.images || []}
                    pagination={false}
                  />
                </>
              ),
            },
            {
              key: 'skus',
              label: 'SKU',
              children: (
                <>
                  <div style={{ marginBottom: 12 }}>
                    <Button type="primary" icon={<PlusOutlined />} onClick={openCreateSku}>
                      新增SKU
                    </Button>
                  </div>
                  <Table
                    rowKey="id"
                    columns={skuColumns}
                    dataSource={productDetail?.skus || []}
                    pagination={false}
                  />
                </>
              ),
            },
          ]}
        />
      </Drawer>

      <Modal
        title={editingSku ? `编辑SKU：${editingSku.skuCode}` : '新增SKU'}
        open={skuModalOpen}
        onCancel={() => setSkuModalOpen(false)}
        onOk={handleSaveSku}
        okText="保存"
        cancelText="取消"
        width={620}
        destroyOnHidden
      >
        <Form form={skuForm} layout="vertical">
          <Form.Item label="SKU编码" name="skuCode" rules={[{ required: true, message: '请输入SKU编码' }]}>
            <Input />
          </Form.Item>
          <Form.Item label="规格JSON" name="specJson" tooltip='例如 {"weight":"500g"}'>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item label="销售价(元)" name="salePriceYuan" rules={[{ required: true, message: '请输入销售价' }]} style={{ width: 150 }}>
              <InputNumber min={0} step={0.01} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="市场价(元)" name="marketPriceYuan" rules={[{ required: true, message: '请输入市场价' }]} style={{ width: 150 }}>
              <InputNumber min={0} step={0.01} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="重量(克)" name="weightGram" style={{ width: 130 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="状态" name="status" style={{ width: 120 }}>
              <Select options={[{ label: '启用', value: 'ENABLED' }, { label: '停用', value: 'DISABLED' }]} />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </Card>
  );
};

export default ProductManagementPage;
