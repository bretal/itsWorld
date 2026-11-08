import React, { useEffect, useState } from 'react';
import { Card, Col, Empty, Row, Space, Spin, Tag, Timeline, message } from 'antd';
import { ArrowLeftOutlined, CrownOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import * as memberService from '@/services/memberService';
import { formatDateTime, formatDiscount } from '@/utils/format';
import type { MemberLevel, MemberLevelHistory } from '@/types/member';

const MemberLevelPage: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [levels, setLevels] = useState<MemberLevel[]>([]);
  const [history, setHistory] = useState<MemberLevelHistory[]>([]);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const [levelData, historyData] = await Promise.all([
          memberService.getLevels(),
          memberService.getLevelHistory(),
        ]);
        setLevels(levelData || []);
        setHistory(historyData || []);
      } catch (error: any) {
        message.error(error.message || '加载等级信息失败');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const levelNameMap = new Map<number, string>();
  levels.forEach((level) => levelNameMap.set(level.id, level.levelName));

  return (
    <Spin spinning={loading}>
      <Card
        title={
          <span>
            <ArrowLeftOutlined
              onClick={() => navigate('/member')}
              style={{ marginRight: 8, cursor: 'pointer' }}
            />
            等级与权益
          </span>
        }
      >
        {levels.length === 0 ? (
          <Empty description="暂无等级配置" />
        ) : (
          <Row gutter={[16, 16]}>
            {levels.map((level) => (
              <Col xs={24} sm={12} lg={6} key={level.id}>
                <Card
                  title={
                    <Space>
                      <CrownOutlined style={{ color: level.current ? '#faad14' : '#999' }} />
                      <span>{level.levelName}</span>
                      {level.current && <Tag color="gold">当前等级</Tag>}
                    </Space>
                  }
                  style={{ height: '100%' }}
                >
                  <div style={{ color: '#666', marginBottom: 8 }}>
                    成长值门槛：{level.growthThreshold}
                  </div>
                  <div style={{ color: '#666', marginBottom: 8 }}>
                    会员折扣：{formatDiscount(level.discountRate)}
                  </div>
                  <div style={{ color: '#999', marginBottom: 12 }}>
                    {level.description || '暂无说明'}
                  </div>
                  <Space wrap>
                    {level.benefits?.length ? (
                      level.benefits.map((benefit) => (
                        <Tag key={benefit.id} color="blue" title={benefit.description}>
                          {benefit.benefitName}
                          {benefit.benefitValue ? ` ${benefit.benefitValue}` : ''}
                        </Tag>
                      ))
                    ) : (
                      <span style={{ color: '#999' }}>暂无权益</span>
                    )}
                  </Space>
                </Card>
              </Col>
            ))}
          </Row>
        )}
      </Card>

      <Card title="等级变更历史" style={{ marginTop: 16 }}>
        {history.length === 0 ? (
          <Empty description="暂无等级变更记录" />
        ) : (
          <Timeline
            items={history.map((item) => ({
              color: 'blue',
              children: (
                <div>
                  <div>
                    {item.fromLevelId
                      ? levelNameMap.get(item.fromLevelId) || `等级#${item.fromLevelId}`
                      : '初始等级'}
                    {' → '}
                    {item.toLevelId
                      ? levelNameMap.get(item.toLevelId) || `等级#${item.toLevelId}`
                      : '-'}
                  </div>
                  <div style={{ color: '#666' }}>
                    成长值 {item.growthValue} · {item.reason || '-'}
                  </div>
                  <div style={{ color: '#999', fontSize: 12 }}>
                    {formatDateTime(item.createdAt)}
                  </div>
                </div>
              ),
            }))}
          />
        )}
      </Card>
    </Spin>
  );
};

export default MemberLevelPage;
