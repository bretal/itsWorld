import React, { useEffect, useState } from 'react';
import { Card, Form, Input, InputNumber, Select, Button, Avatar, message, Spin, Row, Col, Tabs, Upload, List, DatePicker } from 'antd';
import { ArrowLeftOutlined, CameraOutlined } from '@ant-design/icons';
import { useNavigate, useParams } from 'react-router-dom';
import dayjs from 'dayjs';
import type { UploadFile } from 'antd/es/upload/interface';
import * as petService from '@/services/petService';
import * as diaryService from '@/services/diaryService';
import * as uploadService from '@/services/uploadService';
import type { Pet } from '@/types/pet';
import type { Diary } from '@/types/diary';

const GENDERS = [
  { label: '未知', value: 0 },
  { label: '男', value: 1 },
  { label: '女', value: 2 },
];

const PetDetailPage: React.FC = () => {
  const navigate = useNavigate();
  const { petId } = useParams<{ petId: string }>();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(true);
  const [pet, setPet] = useState<Pet | null>(null);
  const [avatarUrl, setAvatarUrl] = useState('');
  const [diaries, setDiaries] = useState<Diary[]>([]);

  useEffect(() => {
    const fetchPet = async () => {
      try {
        if (petId) {
          const id = parseInt(petId);
          const data = await petService.getPet(id);
          setPet(data);
          setAvatarUrl(data.avatarUrl || '');
          form.setFieldsValue({
            ...data,
            birthDate: data.birthDate ? dayjs(data.birthDate) : undefined,
          });
          const diaryList = await diaryService.listDiaries(id);
          setDiaries(diaryList || []);
        }
      } catch (error: any) {
        message.error(error.message || '加载宠物信息失败');
      } finally {
        setLoading(false);
      }
    };

    fetchPet();
  }, [petId, form]);

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
      if (petId) {
        const updated = await petService.updatePet(parseInt(petId), {
          ...values,
          birthDate: values.birthDate ? values.birthDate.format('YYYY-MM-DD') : undefined,
          avatarUrl,
        });
        setPet(updated);
        message.success('宠物信息已更新');
      }
    } catch (error: any) {
      message.error(error.message || '更新失败');
    } finally {
      setLoading(false);
    }
  };

  if (!pet) {
    return <Spin spinning={loading} />;
  }

  return (
    <div style={{ maxWidth: 900, margin: '0 auto' }}>
      <Card
        title={
          <span>
            <ArrowLeftOutlined onClick={() => navigate('/pet')} style={{ marginRight: 8, cursor: 'pointer' }} />
            {pet.petName}
          </span>
        }
      >
        <Row gutter={[24, 24]}>
          <Col xs={24} sm={8}>
            <div style={{ textAlign: 'center' }}>
              <Avatar
                size={150}
                src={avatarUrl || undefined}
                style={{ marginBottom: 20, backgroundColor: '#fde3cf', color: '#f56a00' }}
              >
                🐾
              </Avatar>
              <div>
                <Upload
                  maxCount={1}
                  beforeUpload={() => false}
                  onChange={(info) => handleAvatarChange(info.fileList)}
                >
                  <Button icon={<CameraOutlined />}>更换头像</Button>
                </Upload>
              </div>
              <h3>{pet.petName}</h3>
              <p style={{ color: '#666' }}>{pet.breed || '品种未知'}</p>
            </div>
          </Col>

          <Col xs={24} sm={16}>
            <Tabs
              items={[
                {
                  key: '1',
                  label: '基本信息',
                  children: (
                    <Spin spinning={loading}>
                      <Form
                        form={form}
                        layout="vertical"
                        onFinish={handleSubmit}
                      >
                        <Form.Item
                          label="宠物名称"
                          name="petName"
                        >
                          <Input disabled />
                        </Form.Item>

                        <Form.Item
                          label="宠物类型"
                          name="petType"
                        >
                          <Input disabled />
                        </Form.Item>

                        <Form.Item
                          label="品种"
                          name="breed"
                        >
                          <Input placeholder="输入品种" />
                        </Form.Item>

                        <Form.Item
                          label="性别"
                          name="gender"
                        >
                          <Select options={GENDERS} />
                        </Form.Item>

                        <Form.Item
                          label="出生日期"
                          name="birthDate"
                        >
                          <DatePicker
                            format="YYYY-MM-DD"
                            placeholder="选择出生日期"
                            style={{ width: '100%' }}
                          />
                        </Form.Item>

                        <Form.Item
                          label="体重（kg）"
                          name="weight"
                        >
                          <InputNumber min={0} step={0.1} style={{ width: '100%' }} />
                        </Form.Item>

                        <Form.Item
                          label="个性签名"
                          name="description"
                        >
                          <Input.TextArea rows={3} />
                        </Form.Item>

                        <Form.Item>
                          <Button type="primary" htmlType="submit" loading={loading}>
                            保存修改
                          </Button>
                        </Form.Item>
                      </Form>
                    </Spin>
                  ),
                },
                {
                  key: '2',
                  label: '日记',
                  children: (
                    <List
                      dataSource={diaries}
                      locale={{ emptyText: '暂无日记' }}
                      renderItem={(diary) => (
                        <List.Item>
                          <List.Item.Meta
                            title={`${diary.diaryDate}${diary.greeting ? ' · ' + diary.greeting : ''}`}
                            description={diary.content}
                          />
                          <span>点赞 {diary.likeCount || 0}</span>
                        </List.Item>
                      )}
                    />
                  ),
                },
              ]}
            />
          </Col>
        </Row>
      </Card>
    </div>
  );
};

export default PetDetailPage;
