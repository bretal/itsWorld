import React, { useEffect, useState } from 'react';
import { Card, List, Spin, Button, Empty, Space, message, Modal, Tag } from 'antd';
import { PlusOutlined, LikeOutlined, DeleteOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useApp } from '@/hooks/useApp';
import * as diaryService from '@/services/diaryService';
import type { Diary } from '@/types/diary';
import styles from './DiaryTimeline.module.css';

const DiaryTimelinePage: React.FC = () => {
  const navigate = useNavigate();
  const { currentPet } = useApp();
  const [diaries, setDiaries] = useState<Diary[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchDiaries = async () => {
      try {
        if (currentPet) {
          setLoading(true);
          const data = await diaryService.listDiaries(currentPet.id);
          setDiaries(data || []);
        }
      } catch (error: any) {
        message.error('加载日记失败');
      } finally {
        setLoading(false);
      }
    };

    fetchDiaries();
  }, [currentPet]);

  const handleLike = async (id: number) => {
    try {
      await diaryService.likeDiary(id);
      const updated = diaries.map((d) =>
        d.id === id ? { ...d, likeCount: (d.likeCount || 0) + 1 } : d
      );
      setDiaries(updated);
    } catch (error: any) {
      message.error('操作失败');
    }
  };

  const handleDelete = (id: number) => {
    Modal.confirm({
      title: '删除日记',
      content: '确定要删除这条日记吗？',
      okText: '确定',
      cancelText: '取消',
      okType: 'danger',
      onOk: async () => {
        try {
          await diaryService.deleteDiary(id);
          message.success('日记已删除');
          setDiaries(diaries.filter((d) => d.id !== id));
        } catch (error: any) {
          message.error('删除失败');
        }
      },
    });
  };

  if (!currentPet) {
    return <Empty description="请先选择宠物" />;
  }

  return (
    <Card
      title="日记时间轴"
      extra={
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/diary/greeting')}>
          新建日记
        </Button>
      }
    >
      <Spin spinning={loading}>
        {diaries.length === 0 ? (
          <Empty description="暂无日记记录" />
        ) : (
          <List
            itemLayout="vertical"
            dataSource={diaries}
            renderItem={(diary) => (
              <List.Item
                key={diary.id}
                className={styles.diaryItem}
                actions={[
                  <Space>
                    <Button
                      type="link"
                      size="small"
                      icon={<LikeOutlined />}
                      onClick={() => handleLike(diary.id)}
                    >
                      {diary.likeCount || 0}
                    </Button>
                    <Button
                      type="link"
                      size="small"
                      danger
                      icon={<DeleteOutlined />}
                      onClick={() => handleDelete(diary.id)}
                    >
                      删除
                    </Button>
                  </Space>,
                ]}
              >
                <List.Item.Meta
                  title={
                    <Space>
                      <span>{diary.diaryDate}</span>
                      {diary.greeting && <Tag>{diary.greeting}</Tag>}
                    </Space>
                  }
                  description={
                    <>
                      <div className={styles.content}>{diary.content}</div>
                      {diary.photoUrls && diary.photoUrls.length > 0 && (
                        <div className={styles.photos}>
                          {diary.photoUrls.map((url, index) => (
                            <img key={index} src={url} alt={`photo-${index}`} className={styles.photo} />
                          ))}
                        </div>
                      )}
                      <div className={styles.meta}>
                        健康评分：{diary.healthScore || 0}/12 | 评论：{diary.commentCount || 0}
                      </div>
                    </>
                  }
                />
              </List.Item>
            )}
          />
        )}
      </Spin>
    </Card>
  );
};

export default DiaryTimelinePage;
