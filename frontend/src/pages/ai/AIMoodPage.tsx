import React, { useState } from 'react';
import { Card, Upload, Button, Spin, Result, Row, Col, Statistic, Tag, Space, message } from 'antd';
import { CameraOutlined, UploadOutlined } from '@ant-design/icons';
import type { UploadFile } from 'antd/es/upload/interface';
import * as aiService from '@/services/aiService';

const AIMoodPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [imageUrl, setImageUrl] = useState('');
  const [result, setResult] = useState<any>(null);

  const handleUpload = (fileList: UploadFile[]) => {
    if (fileList.length === 0) return;

    const file = fileList[0];
    const reader = new FileReader();

    reader.onload = async () => {
      const url = reader.result as string;
      setImageUrl(url);

      try {
        setLoading(true);
        const moodResult = await aiService.recognizeMood(url);
        setResult(moodResult);
      } catch (error: any) {
        message.error('识别失败，请重试');
      } finally {
        setLoading(false);
      }
    };

    reader.readAsDataURL(file.originFileObj as Blob);
  };

  return (
    <div style={{ maxWidth: 800, margin: '0 auto' }}>
      <Card title="AI 情绪识别">
        {!result ? (
          <Spin spinning={loading}>
            <div style={{ textAlign: 'center', padding: '40px 0' }}>
              <CameraOutlined style={{ fontSize: 48, color: '#1890ff', marginBottom: 20 }} />
              <div style={{ marginBottom: 20 }}>
                <h3>拍照或上传宠物照片</h3>
                <p style={{ color: '#666' }}>上传清晰的宠物照片，AI 将识别宠物的心情</p>
              </div>

              <Space>
                <Upload
                  maxCount={1}
                  beforeUpload={() => false}
                  onChange={(info) => handleUpload(info.fileList)}
                >
                  <Button icon={<UploadOutlined />} size="large">
                    选择照片
                  </Button>
                </Upload>
              </Space>

              {imageUrl && (
                <div style={{ marginTop: 20 }}>
                  <img
                    src={imageUrl}
                    alt="upload"
                    style={{ maxWidth: 300, maxHeight: 300, borderRadius: 8 }}
                  />
                </div>
              )}
            </div>
          </Spin>
        ) : (
          <Result
            status="success"
            title="识别完成"
            subTitle={`${result.mood} ${result.confidence}%`}
            extra={[
              <Button key="again" onClick={() => setResult(null)}>
                重新识别
              </Button>,
            ]}
          >
            <Row gutter={[16, 16]} style={{ marginTop: 24 }}>
              <Col xs={24} sm={12}>
                <Card>
                  <Statistic
                    title="心情判定"
                    value={result.mood}
                  />
                </Card>
              </Col>
              <Col xs={24} sm={12}>
                <Card>
                  <Statistic
                    title="置信度"
                    value={result.confidence}
                    suffix="%"
                  />
                </Card>
              </Col>
            </Row>

            {result.bodyLanguage && (
              <Card title="身体语言标签" style={{ marginTop: 16 }}>
                <Space wrap>
                  {result.bodyLanguage.map((tag: string) => (
                    <Tag key={tag} color="blue">{tag}</Tag>
                  ))}
                </Space>
              </Card>
            )}

            {result.tips && (
              <Card title="AI 小贴士" style={{ marginTop: 16 }}>
                <p>{result.tips}</p>
              </Card>
            )}

            {imageUrl && (
              <div style={{ marginTop: 16, textAlign: 'center' }}>
                <img
                  src={imageUrl}
                  alt="recognized"
                  style={{ maxWidth: 300, maxHeight: 300, borderRadius: 8 }}
                />
              </div>
            )}
          </Result>
        )}
      </Card>
    </div>
  );
};

export default AIMoodPage;
