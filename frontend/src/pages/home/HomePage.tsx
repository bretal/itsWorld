import React from 'react';
import { Card, Row, Col, Statistic, Button, Empty } from 'antd';
import { PlusOutlined, FileOutlined, TeamOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useApp } from '@/hooks/useApp';

const HomePage: React.FC = () => {
  const navigate = useNavigate();
  const { currentPet } = useApp();

  if (!currentPet) {
    return (
      <Empty
        description="请先创建宠物档案"
        style={{ marginTop: 60 }}
      >
        <Button type="primary" onClick={() => navigate('/pet/create')}>
          创建宠物档案
        </Button>
      </Empty>
    );
  }

  return (
    <div>
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="宠物名称"
              value={currentPet.petName}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="宠物类型"
              value={currentPet.petType}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="体重"
              value={currentPet.weight}
              suffix="kg"
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="性别"
              value={currentPet.gender === 1 ? '男' : currentPet.gender === 2 ? '女' : '未知'}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card
            hoverable
            onClick={() => navigate('/diary')}
            style={{ cursor: 'pointer' }}
          >
            <div style={{ textAlign: 'center', padding: '20px 0' }}>
              <FileOutlined style={{ fontSize: 32, color: '#1890ff' }} />
              <div style={{ marginTop: 10 }}>日记记录</div>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card
            hoverable
            onClick={() => navigate('/community')}
            style={{ cursor: 'pointer' }}
          >
            <div style={{ textAlign: 'center', padding: '20px 0' }}>
              <TeamOutlined style={{ fontSize: 32, color: '#52c41a' }} />
              <div style={{ marginTop: 10 }}>宠友圈</div>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card
            hoverable
            onClick={() => navigate('/diary/greeting')}
            style={{ cursor: 'pointer' }}
          >
            <div style={{ textAlign: 'center', padding: '20px 0' }}>
              <PlusOutlined style={{ fontSize: 32, color: '#722ed1' }} />
              <div style={{ marginTop: 10 }}>每日问候</div>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default HomePage;
