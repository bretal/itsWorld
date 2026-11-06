import React, { useState } from 'react';
import { Card, Form, Input, InputNumber, Select, Button, Upload, Avatar, message, Spin, Row, Col, DatePicker } from 'antd';
import { CameraOutlined, ArrowLeftOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import type { UploadFile } from 'antd/es/upload/interface';
import { useApp } from '@/hooks/useApp';
import * as petService from '@/services/petService';
import * as uploadService from '@/services/uploadService';
import type { PetCreateRequest } from '@/types/pet';

const PET_TYPES = [
  { label: '狗', value: 'dog' },
  { label: '猫', value: 'cat' },
  { label: '兔子', value: 'rabbit' },
  { label: '仓鼠', value: 'hamster' },
  { label: '其他', value: 'other' },
];

const GENDERS = [
  { label: '未知', value: 0 },
  { label: '男', value: 1 },
  { label: '女', value: 2 },
];

const PetCreatePage: React.FC = () => {
  const navigate = useNavigate();
  const { setPets, setCurrentPetId, setCurrentPet } = useApp();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [avatarUrl, setAvatarUrl] = useState('');

  const handleAvatarChange = async (fileList: UploadFile[]) => {
    if (fileList.length === 0 || !fileList[0].originFileObj) {
      return;
    }
    try {
      setLoading(true);
      const url = await uploadService.uploadImage(fileList[0].originFileObj as File, 'pet');
      setAvatarUrl(url);
      message.success('头像上传成功');
    } catch (error: any) {
      message.error(error.message || '头像上传失败');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (values: any) => {
    try {
      setLoading(true);
      const petData: PetCreateRequest = {
        ...values,
        birthDate: values.birthDate ? values.birthDate.format('YYYY-MM-DD') : undefined,
        avatarUrl,
      };
      const newPetId = await petService.createPet(petData);
      const list = await petService.listPets();
      setPets(list || []);
      const created = list.find((pet) => pet.id === newPetId) || list[0];
      if (created) {
        setCurrentPetId(created.id);
        setCurrentPet(created);
      }
      message.success('宠物已创建');
      navigate('/pet');
    } catch (error: any) {
      message.error(error.message || '创建失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: 800, margin: '0 auto' }}>
      <Card
        title={
          <span>
            <ArrowLeftOutlined onClick={() => navigate('/pet')} style={{ marginRight: 8, cursor: 'pointer' }} />
            创建宠物档案
          </span>
        }
      >
        <Spin spinning={loading}>
          <Form
            form={form}
            layout="vertical"
            onFinish={handleSubmit}
          >
            <Row gutter={[16, 16]}>
              <Col xs={24} sm={8} style={{ textAlign: 'center' }}>
                <Avatar
                  size={120}
                  src={avatarUrl || undefined}
                  style={{ marginBottom: 16, backgroundColor: '#fde3cf', color: '#f56a00' }}
                >
                  🐾
                </Avatar>
                <Upload
                  maxCount={1}
                  beforeUpload={() => false}
                  onChange={(info) => handleAvatarChange(info.fileList)}
                >
                  <Button icon={<CameraOutlined />}>上传头像</Button>
                </Upload>
              </Col>

              <Col xs={24} sm={16}>
                <Form.Item
                  label="宠物名称"
                  name="petName"
                  rules={[{ required: true, message: '请输入宠物名称' }]}
                >
                  <Input placeholder="输入宠物名称" size="large" />
                </Form.Item>

                <Form.Item
                  label="宠物类型"
                  name="petType"
                  rules={[{ required: true, message: '请选择宠物类型' }]}
                >
                  <Select options={PET_TYPES} placeholder="选择宠物类型" size="large" />
                </Form.Item>

                <Form.Item
                  label="品种"
                  name="breed"
                >
                  <Input placeholder="输入品种" size="large" />
                </Form.Item>

                <Form.Item
                  label="性别"
                  name="gender"
                >
                  <Select options={GENDERS} placeholder="选择性别" size="large" />
                </Form.Item>

                <Form.Item
                  label="出生日期"
                  name="birthDate"
                >
                  <DatePicker
                    size="large"
                    format="YYYY-MM-DD"
                    placeholder="选择出生日期"
                    style={{ width: '100%' }}
                  />
                </Form.Item>

                <Form.Item
                  label="体重（kg）"
                  name="weight"
                >
                  <InputNumber min={0} step={0.1} placeholder="输入体重" style={{ width: '100%' }} size="large" />
                </Form.Item>

                <Form.Item
                  label="个性签名"
                  name="description"
                >
                  <Input.TextArea rows={3} placeholder="输入宠物个性签名" />
                </Form.Item>

                <Form.Item>
                  <Button type="primary" htmlType="submit" block size="large" loading={loading}>
                    创建宠物
                  </Button>
                </Form.Item>
              </Col>
            </Row>
          </Form>
        </Spin>
      </Card>
    </div>
  );
};

export default PetCreatePage;
