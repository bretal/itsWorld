import React, { useState, useEffect } from 'react';
import { Card, Form, Input, Button, Avatar, Upload, message, Spin, Row, Col } from 'antd';
import { UserOutlined, CameraOutlined } from '@ant-design/icons';
import type { UploadFile } from 'antd/es/upload/interface';
import { useAuth } from '@/hooks/useAuth';
import * as userService from '@/services/userService';
import * as uploadService from '@/services/uploadService';

const UserProfilePage: React.FC = () => {
  const { user, setUser } = useAuth();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [avatarUrl, setAvatarUrl] = useState(user?.avatarUrl || '');

  useEffect(() => {
    if (user) {
      form.setFieldsValue({
        username: user.username,
        email: user.email,
        phone: user.phone,
        nickName: user.nickName,
        bio: user.bio,
        region: user.region,
      });
      setAvatarUrl(user.avatarUrl || '');
    }
  }, [user, form]);

  const handleAvatarChange = async (fileList: UploadFile[]) => {
    if (fileList.length === 0 || !fileList[0].originFileObj) {
      return;
    }
    try {
      setLoading(true);
      const url = await uploadService.uploadImage(fileList[0].originFileObj as File, 'avatar');
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
      const updateData = {
        ...values,
        avatarUrl,
      };
      const updatedUser = await userService.updateUserInfo(updateData);
      setUser(updatedUser);
      message.success('个人信息已更新');
    } catch (error: any) {
      message.error(error.message || '更新失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: 800, margin: '0 auto' }}>
      <Card title="个人信息" loading={!user}>
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
                  icon={<UserOutlined />}
                  src={avatarUrl}
                  style={{ marginBottom: 16 }}
                />
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
                  label="用户名"
                  name="username"
                >
                  <Input disabled />
                </Form.Item>

                <Form.Item
                  label="邮箱"
                  name="email"
                >
                  <Input disabled />
                </Form.Item>

                <Form.Item
                  label="手机号"
                  name="phone"
                >
                  <Input disabled />
                </Form.Item>

                <Form.Item
                  label="昵称"
                  name="nickName"
                >
                  <Input placeholder="输入昵称" />
                </Form.Item>

                <Form.Item
                  label="个人简介"
                  name="bio"
                >
                  <Input.TextArea rows={3} placeholder="输入个人简介" />
                </Form.Item>

                <Form.Item
                  label="所在地区"
                  name="region"
                >
                  <Input placeholder="输入所在地区" />
                </Form.Item>

                <Form.Item>
                  <Button type="primary" htmlType="submit" loading={loading} block>
                    保存修改
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

export default UserProfilePage;
