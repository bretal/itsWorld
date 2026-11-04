import React, { useState } from 'react';
import { Card, Form, Input, Button, message, Spin } from 'antd';
import { LockOutlined } from '@ant-design/icons';
import * as userService from '@/services/userService';

interface ChangePasswordFormValues {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

const ChangePasswordPage: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (values: ChangePasswordFormValues) => {
    if (values.newPassword !== values.confirmPassword) {
      message.error('两次输入的新密码不一致');
      return;
    }

    try {
      setLoading(true);
      await userService.changePassword({
        oldPassword: values.oldPassword,
        newPassword: values.newPassword,
      });
      message.success('密码已修改');
      form.resetFields();
    } catch (error: any) {
      message.error(error.message || '修改失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: 500, margin: '0 auto' }}>
      <Card title="修改密码">
        <Spin spinning={loading}>
          <Form
            form={form}
            layout="vertical"
            onFinish={handleSubmit}
          >
            <Form.Item
              label="当前密码"
              name="oldPassword"
              rules={[{ required: true, message: '请输入当前密码' }]}
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder="输入当前密码"
                size="large"
              />
            </Form.Item>

            <Form.Item
              label="新密码"
              name="newPassword"
              rules={[
                { required: true, message: '请输入新密码' },
                { min: 8, message: '密码至少8个字符' },
              ]}
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder="输入新密码（至少8个字符）"
                size="large"
              />
            </Form.Item>

            <Form.Item
              label="确认新密码"
              name="confirmPassword"
              rules={[{ required: true, message: '请再次输入新密码' }]}
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder="再次输入新密码"
                size="large"
              />
            </Form.Item>

            <Form.Item>
              <Button type="primary" htmlType="submit" block size="large" loading={loading}>
                修改密码
              </Button>
            </Form.Item>
          </Form>
        </Spin>
      </Card>
    </div>
  );
};

export default ChangePasswordPage;
