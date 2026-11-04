import React, { useState } from 'react';
import { Card, Row, Col, Statistic, Form, InputNumber, Button, message, List } from 'antd';
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import { ResponsiveContainer, LineChart as RechartsLineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip } from 'recharts';

interface WeightRecord {
  date: string;
  weight: number;
}

const HealthPage: React.FC = () => {
  const [form] = Form.useForm();
  const [weights, setWeights] = useState<WeightRecord[]>([
    { date: '2026-10-20', weight: 25.5 },
    { date: '2026-10-22', weight: 25.7 },
    { date: '2026-10-25', weight: 25.3 },
    { date: '2026-10-28', weight: 25.1 },
    { date: '2026-10-31', weight: 25.0 },
  ]);
  const [feedingLogs, setFeedingLogs] = useState<any[]>([
    { id: 1, time: '08:00', type: '早餐', amount: '100g' },
    { id: 2, time: '18:00', type: '晚餐', amount: '100g' },
  ]);

  const handleAddWeight = async (values: any) => {
    try {
      const newWeight: WeightRecord = {
        date: new Date().toISOString().split('T')[0],
        weight: values.weight,
      };
      setWeights([...weights, newWeight]);
      form.resetFields();
      message.success('体重记录已添加');
    } catch (error) {
      message.error('添加失败');
    }
  };

  const handleAddFeeding = async (values: any) => {
    try {
      const newLog = {
        id: Date.now(),
        time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
        type: values.type || '进食',
        amount: values.amount,
      };
      setFeedingLogs([...feedingLogs, newLog]);
      form.resetFields();
      message.success('喂食记录已添加');
    } catch (error) {
      message.error('添加失败');
    }
  };

  const handleDeleteFeeding = (id: number) => {
    setFeedingLogs(feedingLogs.filter((log) => log.id !== id));
    message.success('已删除');
  };

  const latestWeight = weights[weights.length - 1]?.weight || 0;
  const previousWeight = weights[weights.length - 2]?.weight || latestWeight;
  const weightChange = (latestWeight - previousWeight).toFixed(1);

  return (
    <div>
      <Row gutter={[16, 16]}>
        {/* 体重卡片 */}
        <Col xs={24} sm={12} lg={8}>
          <Card>
            <Statistic
              title="当前体重"
              value={latestWeight}
              suffix="kg"
              precision={1}
            />
            <div style={{ color: weightChange > '0' ? '#ff7a45' : '#52c41a', marginTop: 8 }}>
              {weightChange > '0' ? '+' : ''}{weightChange} kg
            </div>
          </Card>
        </Col>

        {/* 今日喂食 */}
        <Col xs={24} sm={12} lg={8}>
          <Card>
            <Statistic
              title="今日喂食次数"
              value={feedingLogs.length}
              suffix="次"
            />
          </Card>
        </Col>

        {/* 健康指数 */}
        <Col xs={24} sm={12} lg={8}>
          <Card>
            <Statistic
              title="健康指数"
              value={95}
              suffix="/100"
            />
          </Card>
        </Col>
      </Row>

      {/* 体重追踪图表 */}
      <Card title="体重追踪" style={{ marginTop: 16 }}>
        <ResponsiveContainer width="100%" height={300}>
          <RechartsLineChart data={weights}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" />
            <YAxis />
            <Tooltip />
            <Line type="monotone" dataKey="weight" stroke="#1890ff" dot={{ r: 4 }} />
          </RechartsLineChart>
        </ResponsiveContainer>
      </Card>

      {/* 体重记录 */}
      <Card title="添加体重记录" style={{ marginTop: 16 }}>
        <Form form={form} layout="inline" onFinish={handleAddWeight}>
          <Form.Item
            name="weight"
            label="体重(kg)"
            rules={[{ required: true, message: '请输入体重' }]}
          >
            <InputNumber min={0} step={0.1} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit">
              添加
            </Button>
          </Form.Item>
        </Form>
      </Card>

      {/* 喂食记录 */}
      <Card title="喂食记录" style={{ marginTop: 16 }}>
        <List
          dataSource={feedingLogs}
          renderItem={(item) => (
            <List.Item
              actions={[
                <Button
                  type="link"
                  danger
                  size="small"
                  icon={<DeleteOutlined />}
                  onClick={() => handleDeleteFeeding(item.id)}
                >
                  删除
                </Button>,
              ]}
            >
              <List.Item.Meta
                title={`${item.time} - ${item.type}`}
                description={`${item.amount}`}
              />
            </List.Item>
          )}
        />
        <Form form={form} layout="inline" onFinish={handleAddFeeding} style={{ marginTop: 16 }}>
          <Form.Item
            name="type"
            label="类型"
            initialValue="进食"
          >
            <input type="text" placeholder="进食类型" style={{ padding: 4 }} />
          </Form.Item>
          <Form.Item
            name="amount"
            label="分量"
            rules={[{ required: true, message: '请输入分量' }]}
          >
            <input type="text" placeholder="分量" style={{ padding: 4 }} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" icon={<PlusOutlined />}>
              添加
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};

export default HealthPage;
