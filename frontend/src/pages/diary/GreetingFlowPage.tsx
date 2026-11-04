import React, { useState } from 'react';
import { Card, Form, Input, InputNumber, Button, Upload, message, Spin, Steps, Row, Col, Select, Empty } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useApp } from '@/hooks/useApp';
import * as diaryService from '@/services/diaryService';
import * as uploadService from '@/services/uploadService';
import type { DiaryCreateRequest } from '@/types/diary';

const MOODS = [
  { label: '😊 开心', value: 'happy' },
  { label: '😌 平静', value: 'calm' },
  { label: '🤪 调皮', value: 'naughty' },
  { label: '😴 困倦', value: 'sleepy' },
  { label: '😰 紧张', value: 'anxious' },
];

const ACTIVITIES = [
  { label: '进食', value: 'eating' },
  { label: '睡眠', value: 'sleeping' },
  { label: '玩耍', value: 'playing' },
  { label: '排泄', value: 'toilet' },
];

const STEP_ITEMS = [
  { title: '位置', description: '今天在哪里' },
  { title: '活动', description: '当前动态' },
  { title: '心情', description: '心情如何' },
  { title: '照片', description: '拍照记录' },
  { title: '总结', description: '今日总结' },
];

const formatLocalDate = (date: Date): string => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const GreetingFlowPage: React.FC = () => {
  const navigate = useNavigate();
  const { currentPet } = useApp();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [step, setStep] = useState(0);
  const [photoUrls, setPhotoUrls] = useState<string[]>([]);

  const handleUpload = async (file: File) => {
    try {
      setUploading(true);
      const url = await uploadService.uploadImage(file, 'diary');
      setPhotoUrls((prev) => [...prev, url]);
    } catch (error: any) {
      message.error(error.message || '照片上传失败');
    } finally {
      setUploading(false);
    }
    return false;
  };

  const handleRemovePhoto = (index: number) => {
    setPhotoUrls(photoUrls.filter((_, i) => i !== index));
  };

  const handleNext = async () => {
    if (step < STEP_ITEMS.length - 1) {
      setStep(step + 1);
      return;
    }

    try {
      const values = await form.validateFields();
      setLoading(true);

      const diaryData: DiaryCreateRequest = {
        petId: currentPet!.id,
        diaryDate: formatLocalDate(new Date()),
        location: values.location,
        mood: values.mood,
        activities: values.activities,
        healthScore: values.healthScore,
        content: values.content,
        photoUrls,
        isPublic: 1,
      };

      await diaryService.createDiary(diaryData);
      message.success('日记已保存');
      navigate('/diary');
    } catch (error: any) {
      message.error(error.message || '保存失败');
    } finally {
      setLoading(false);
    }
  };

  if (!currentPet) {
    return (
      <Empty description="请先选择宠物" style={{ marginTop: 60 }}>
        <Button type="primary" onClick={() => navigate('/pet')}>
          去宠物档案
        </Button>
      </Empty>
    );
  }

  return (
    <div style={{ maxWidth: 600, margin: '0 auto' }}>
      <Card
        title="每日问候"
        extra={<span onClick={() => navigate('/diary')} style={{ cursor: 'pointer', color: '#1890ff' }}>关闭</span>}
      >
        <Spin spinning={loading || uploading}>
          <Steps current={step} items={STEP_ITEMS} style={{ marginBottom: 24 }} />

          <Form form={form} layout="vertical">
            <div style={{ display: step === 0 ? 'block' : 'none' }}>
              <h3>今天 {currentPet.petName} 在哪里呢？</h3>
              <Form.Item name="location" label="位置">
                <Input placeholder="例如：家里、公园、医院" size="large" />
              </Form.Item>
            </div>

            <div style={{ display: step === 1 ? 'block' : 'none' }}>
              <h3>{currentPet.petName} 现在的状态</h3>
              <Form.Item name="activities" label="现在在做什么？">
                <Select
                  mode="multiple"
                  options={ACTIVITIES}
                  placeholder="选择活动（可多选）"
                  size="large"
                />
              </Form.Item>
            </div>

            <div style={{ display: step === 2 ? 'block' : 'none' }}>
              <h3>{currentPet.petName} 现在的心情</h3>
              <Form.Item name="mood" label="心情">
                <Select options={MOODS} placeholder="选择心情" size="large" />
              </Form.Item>
            </div>

            <div style={{ display: step === 3 ? 'block' : 'none' }}>
              <h3>拍照或上传照片</h3>
              <Upload
                multiple
                maxCount={10}
                showUploadList={false}
                beforeUpload={handleUpload}
              >
                <Button icon={<PlusOutlined />} loading={uploading}>选择照片</Button>
              </Upload>
              {photoUrls.length > 0 && (
                <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 16 }}>
                  {photoUrls.map((url, index) => (
                    <div key={url} style={{ position: 'relative' }}>
                      <img src={url} alt={`photo-${index}`} style={{ width: 80, height: 80, borderRadius: 4, objectFit: 'cover' }} />
                      <Button
                        size="small"
                        danger
                        style={{ position: 'absolute', top: 0, right: 0 }}
                        onClick={() => handleRemovePhoto(index)}
                      >
                        ✕
                      </Button>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div style={{ display: step === 4 ? 'block' : 'none' }}>
              <h3>今日总结</h3>
              <Form.Item
                name="content"
                label="今日记录"
                rules={[{ required: true, message: '请输入今日记录' }]}
              >
                <Input.TextArea rows={5} placeholder="记录今天的点滴..." />
              </Form.Item>

              <Form.Item name="healthScore" label="健康评分">
                <InputNumber min={0} max={12} style={{ width: '100%' }} />
              </Form.Item>
            </div>
          </Form>

          <Row gutter={16} style={{ marginTop: 24 }}>
            <Col span={12}>
              <Button onClick={() => (step === 0 ? navigate('/diary') : setStep(step - 1))} block>
                {step === 0 ? '取消' : '上一步'}
              </Button>
            </Col>
            <Col span={12}>
              <Button type="primary" onClick={handleNext} loading={loading || uploading} block>
                {step === STEP_ITEMS.length - 1 ? '完成' : '下一步'}
              </Button>
            </Col>
          </Row>
        </Spin>
      </Card>
    </div>
  );
};

export default GreetingFlowPage;
