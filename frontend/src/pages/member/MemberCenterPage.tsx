import React, { useEffect, useState } from 'react';
import {
  Button,
  Card,
  Col,
  Empty,
  List,
  Progress,
  Row,
  Space,
  Spin,
  Statistic,
  Tag,
  message,
} from 'antd';
import {
  CrownOutlined,
  GiftOutlined,
  RiseOutlined,
  WalletOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import * as memberService from '@/services/memberService';
import { formatMoney } from '@/utils/format';
import type { MemberOverview, MemberTask } from '@/types/member';

const MemberCenterPage: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [overview, setOverview] = useState<MemberOverview | null>(null);
  const [tasks, setTasks] = useState<MemberTask[]>([]);
  const [actionLoading, setActionLoading] = useState(false);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [overviewData, taskData] = await Promise.all([
        memberService.getOverview(),
        memberService.getTasks(),
      ]);
      setOverview(overviewData);
      setTasks(taskData || []);
    } catch (error: any) {
      message.error(error.message || '加载会员信息失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleCheckIn = async () => {
    try {
      setActionLoading(true);
      await memberService.checkIn();
      message.success('签到成功');
      await fetchData();
    } catch (error: any) {
      message.error(error.message || '签到失败');
    } finally {
      setActionLoading(false);
    }
  };

  const handleClaim = async (task: MemberTask) => {
    try {
      setActionLoading(true);
      const result = await memberService.claimTask(task.id);
      if (result.levelUp) {
        message.success(`领取成功，恭喜升级为${result.levelName || '新等级'}`);
      } else {
        message.success(`领取成功，积分 +${result.pointsAwarded}，成长值 +${result.growthAwarded}`);
      }
      await fetchData();
    } catch (error: any) {
      message.error(error.message || '领取失败');
    } finally {
      setActionLoading(false);
    }
  };

  const getTaskAction = (task: MemberTask) => {
    if (task.rewardStatus === 1) {
      return <Tag>已领取</Tag>;
    }
    if (task.claimable) {
      return (
        <Button type="primary" size="small" loading={actionLoading} onClick={() => handleClaim(task)}>
          领取
        </Button>
      );
    }
    if (task.taskCode === 'DAILY_CHECKIN' && task.progress < task.targetValue) {
      return (
        <Button size="small" loading={actionLoading} onClick={handleCheckIn}>
          签到
        </Button>
      );
    }
    const pathByCode: Record<string, string> = {
      DIARY_CREATE: '/diary/greeting',
      POST_CREATE: '/community',
      PET_CREATE: '/pet/create',
    };
    const path = pathByCode[task.taskCode];
    return path ? (
      <Button size="small" onClick={() => navigate(path)}>
        去完成
      </Button>
    ) : null;
  };

  if (loading && !overview) {
    return <Spin spinning style={{ display: 'block', marginTop: 80 }} />;
  }

  if (!overview) {
    return <Empty description="暂无会员信息" style={{ marginTop: 80 }} />;
  }

  return (
    <Spin spinning={loading}>
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="当前等级"
              value={overview.levelName}
              prefix={<CrownOutlined style={{ color: '#faad14', marginRight: 6 }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="成长值"
              value={overview.growthValue}
              prefix={<RiseOutlined style={{ color: '#1677ff', marginRight: 6 }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic title="可用积分" value={overview.pointsBalance} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="钱包可用（元）"
              value={formatMoney(overview.wallet.availableBalance)}
              prefix={<WalletOutlined style={{ color: '#52c41a', marginRight: 6 }} />}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={14}>
          <Card title="等级进度">
            <Progress percent={overview.progressPercent} status="active" />
            <div style={{ marginTop: 8, color: '#666' }}>
              {overview.nextLevelName
                ? `再获得 ${overview.growthToNext} 成长值可升级为${overview.nextLevelName}`
                : '已达最高等级'}
            </div>
            <div style={{ marginTop: 20 }}>
              <Space wrap>
                {overview.benefits?.length ? (
                  overview.benefits.map((benefit) => (
                    <Tag key={benefit.id} color="blue" title={benefit.description}>
                      {benefit.benefitName}
                      {benefit.benefitValue ? ` ${benefit.benefitValue}` : ''}
                    </Tag>
                  ))
                ) : (
                  <span style={{ color: '#999' }}>当前等级暂无权益配置</span>
                )}
              </Space>
            </div>
          </Card>

          <Card title="成长任务" style={{ marginTop: 16 }}>
            <List
              dataSource={tasks}
              locale={{ emptyText: '暂无任务' }}
              renderItem={(task) => (
                <List.Item
                  actions={[getTaskAction(task)]}
                >
                  <List.Item.Meta
                    title={
                      <Space>
                        <span>{task.taskName}</span>
                        <Tag color={task.periodType === 'ONCE' ? 'purple' : 'cyan'}>
                          {task.periodType === 'DAILY' ? '每日' : task.periodType === 'WEEKLY' ? '每周' : '一次性'}
                        </Tag>
                        {task.rewardStatus === 1 && <Tag color="success">已领取</Tag>}
                      </Space>
                    }
                    description={
                      <span>
                        {task.description || '-'} · 进度 {task.progress}/{task.targetValue} · 积分 +
                        {task.pointsReward} · 成长值 +{task.growthReward}
                      </span>
                    }
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>

        <Col xs={24} lg={10}>
          <Card title="快捷入口">
            <Row gutter={[16, 16]}>
              <Col span={12}>
                <Card hoverable onClick={() => navigate('/member/wallet')}>
                  <WalletOutlined style={{ fontSize: 28, color: '#52c41a' }} />
                  <div style={{ marginTop: 8 }}>我的钱包</div>
                </Card>
              </Col>
              <Col span={12}>
                <Card hoverable onClick={() => navigate('/member/coupons')}>
                  <GiftOutlined style={{ fontSize: 28, color: '#fa541c' }} />
                  <div style={{ marginTop: 8 }}>优惠券 / 礼包</div>
                </Card>
              </Col>
              <Col span={12}>
                <Card hoverable onClick={() => navigate('/member/levels')}>
                  <CrownOutlined style={{ fontSize: 28, color: '#faad14' }} />
                  <div style={{ marginTop: 8 }}>等级权益</div>
                </Card>
              </Col>
              <Col span={12}>
                <Card hoverable onClick={() => navigate('/member/points')}>
                  <RiseOutlined style={{ fontSize: 28, color: '#1677ff' }} />
                  <div style={{ marginTop: 8 }}>积分成长</div>
                </Card>
              </Col>
            </Row>
          </Card>
        </Col>
      </Row>
    </Spin>
  );
};

export default MemberCenterPage;
