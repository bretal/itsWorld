import React, { useEffect, useState } from 'react';
import {
  Button,
  Card,
  List,
  Select,
  Space,
  Spin,
  Table,
  Tabs,
  Tag,
  message,
} from 'antd';
import { ArrowLeftOutlined, GiftOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import type { ColumnsType } from 'antd/es/table';
import * as memberService from '@/services/memberService';
import { formatDateTime, formatDiscount, formatMoney } from '@/utils/format';
import type { Coupon, MemberGift } from '@/types/member';

const COUPON_STATUS_LABELS: Record<string, { text: string; color: string }> = {
  AVAILABLE: { text: '可用', color: 'success' },
  LOCKED: { text: '锁定中', color: 'warning' },
  USED: { text: '已使用', color: 'default' },
  EXPIRED: { text: '已过期', color: 'error' },
};

const couponValue = (coupon: Coupon) => {
  if (coupon.couponType === 'CASH') {
    return `¥${formatMoney(coupon.amountCents)}`;
  }
  return formatDiscount(coupon.discountRate);
};

const couponCondition = (coupon: Coupon) => {
  if (coupon.minAmountCents && coupon.minAmountCents > 0) {
    return `满 ¥${formatMoney(coupon.minAmountCents)} 可用`;
  }
  return '无门槛';
};

const MemberCouponsPage: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [coupons, setCoupons] = useState<Coupon[]>([]);
  const [gifts, setGifts] = useState<MemberGift[]>([]);
  const [couponStatus, setCouponStatus] = useState<string | undefined>(undefined);

  const fetchCoupons = async (status?: string) => {
    const data = await memberService.getCoupons(status);
    setCoupons(data || []);
  };

  const fetchGifts = async () => {
    const data = await memberService.getGifts();
    setGifts(data || []);
  };

  const fetchAll = async () => {
    try {
      setLoading(true);
      await Promise.all([fetchCoupons(couponStatus), fetchGifts()]);
    } catch (error: any) {
      message.error(error.message || '加载优惠券/礼包失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAll();
  }, []);

  const handleStatusChange = async (value?: string) => {
    try {
      setLoading(true);
      setCouponStatus(value);
      await fetchCoupons(value);
    } catch (error: any) {
      message.error(error.message || '加载优惠券失败');
    } finally {
      setLoading(false);
    }
  };

  const handleClaimGift = async (gift: MemberGift) => {
    try {
      setActionLoading(true);
      await memberService.claimGift(gift.id);
      message.success('礼包领取成功');
      await fetchGifts();
    } catch (error: any) {
      message.error(error.message || '领取失败');
    } finally {
      setActionLoading(false);
    }
  };

  const couponColumns: ColumnsType<Coupon> = [
    {
      title: '优惠券',
      dataIndex: 'templateName',
      key: 'templateName',
      render: (value: string, record) => (
        <Space direction="vertical" size={0}>
          <span style={{ fontWeight: 500 }}>{value}</span>
          <span style={{ color: '#fa541c' }}>{couponValue(record)}</span>
        </Space>
      ),
    },
    {
      title: '使用条件',
      key: 'condition',
      render: (_, record) => couponCondition(record),
    },
    {
      title: '有效期',
      key: 'validity',
      render: (_, record) => (
        <span style={{ fontSize: 12, color: '#666' }}>
          {formatDateTime(record.validFrom)} 至 {record.validTo ? formatDateTime(record.validTo) : '长期'}
        </span>
      ),
    },
    {
      title: '券码',
      dataIndex: 'couponCode',
      key: 'couponCode',
      render: (value: string) => <span style={{ fontSize: 12 }}>{value}</span>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (value: string) => {
        const status = COUPON_STATUS_LABELS[value] || { text: value, color: 'default' };
        return <Tag color={status.color}>{status.text}</Tag>;
      },
    },
  ];

  return (
    <Spin spinning={loading}>
      <Card
        title={
          <span>
            <ArrowLeftOutlined
              onClick={() => navigate('/member')}
              style={{ marginRight: 8, cursor: 'pointer' }}
            />
            优惠券与礼包
          </span>
        }
      >
        <Tabs
          items={[
            {
              key: 'coupons',
              label: '我的优惠券',
              children: (
                <>
                  <div style={{ marginBottom: 12, textAlign: 'right' }}>
                    <Select
                      allowClear
                      placeholder="全部状态"
                      style={{ width: 140 }}
                      value={couponStatus}
                      onChange={handleStatusChange}
                      options={[
                        { label: '可用', value: 'AVAILABLE' },
                        { label: '锁定中', value: 'LOCKED' },
                        { label: '已使用', value: 'USED' },
                        { label: '已过期', value: 'EXPIRED' },
                      ]}
                    />
                  </div>
                  <Table
                    rowKey="id"
                    columns={couponColumns}
                    dataSource={coupons}
                    locale={{ emptyText: '暂无优惠券' }}
                    pagination={{ pageSize: 10, showSizeChanger: false }}
                  />
                </>
              ),
            },
            {
              key: 'gifts',
              label: '会员礼包',
              children: (
                <List
                  dataSource={gifts}
                  locale={{ emptyText: '暂无可领取礼包' }}
                  renderItem={(gift) => (
                    <List.Item
                      actions={[
                        gift.claimed ? (
                          <Tag key="claimed" color="success">
                            已领取
                          </Tag>
                        ) : (
                          <Button
                            key="claim"
                            type="primary"
                            size="small"
                            disabled={!gift.claimable}
                            loading={actionLoading}
                            onClick={() => handleClaimGift(gift)}
                          >
                            {gift.claimable ? '领取' : gift.lockReason || '不可领取'}
                          </Button>
                        ),
                      ]}
                    >
                      <List.Item.Meta
                        avatar={<GiftOutlined style={{ fontSize: 28, color: '#fa541c' }} />}
                        title={
                          <Space>
                            <span>{gift.giftName}</span>
                            {gift.requiredLevelName && (
                              <Tag color="gold">需{gift.requiredLevelName}</Tag>
                            )}
                          </Space>
                        }
                        description={
                          <span>
                            {gift.description || '-'}
                            {gift.pointsReward > 0 ? ` · 积分 +${gift.pointsReward}` : ''}
                            {gift.couponTemplateName ? ` · ${gift.couponTemplateName}` : ''}
                          </span>
                        }
                      />
                    </List.Item>
                  )}
                />
              ),
            },
          ]}
        />
      </Card>
    </Spin>
  );
};

export default MemberCouponsPage;
