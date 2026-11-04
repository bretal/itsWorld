import React, { useCallback, useEffect, useState } from 'react';
import { Card, Tabs, List, Avatar, Button, Space, message, Modal, Input, Form, Empty, Spin } from 'antd';
import { LikeOutlined, CommentOutlined, PlusOutlined, UserAddOutlined } from '@ant-design/icons';
import { useAuth } from '@/hooks/useAuth';
import { useApp } from '@/hooks/useApp';
import * as communityService from '@/services/communityService';
import type { Post } from '@/types/community';

const CommunityPage: React.FC = () => {
  const { user } = useAuth();
  const { currentPet } = useApp();
  const [activeTab, setActiveTab] = useState('hot');
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(false);
  const [likedIds, setLikedIds] = useState<number[]>([]);
  const [following, setFollowing] = useState<number[]>([]);
  const [commentPostId, setCommentPostId] = useState<number | null>(null);
  const [commentContent, setCommentContent] = useState('');
  const [commentLoading, setCommentLoading] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [createLoading, setCreateLoading] = useState(false);
  const [createForm] = Form.useForm();

  const fetchPosts = useCallback(async (tab: string) => {
    try {
      setLoading(true);
      const data = tab === 'hot'
        ? await communityService.getHotPosts()
        : await communityService.getRecommendPosts();
      setPosts(data || []);
    } catch (error: any) {
      message.error(error.message || '加载帖子失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchPosts(activeTab);
  }, [activeTab, fetchPosts]);

  useEffect(() => {
    communityService.getFollowing()
      .then((ids) => setFollowing(ids || []))
      .catch(() => setFollowing([]));
  }, []);

  const handleLike = async (post: Post) => {
    const liked = likedIds.includes(post.id);
    try {
      if (liked) {
        await communityService.unlikePost(post.id);
        setLikedIds(likedIds.filter((id) => id !== post.id));
        setPosts(posts.map((item) => item.id === post.id
          ? { ...item, likeCount: Math.max((item.likeCount || 0) - 1, 0) }
          : item));
      } else {
        await communityService.likePost(post.id);
        setLikedIds([...likedIds, post.id]);
        setPosts(posts.map((item) => item.id === post.id
          ? { ...item, likeCount: (item.likeCount || 0) + 1 }
          : item));
      }
    } catch (error: any) {
      message.error(error.message || '操作失败');
    }
  };

  const handleFollow = async (userId: number) => {
    try {
      if (following.includes(userId)) {
        await communityService.unfollowUser(userId);
        setFollowing(following.filter((id) => id !== userId));
        message.success('已取消关注');
      } else {
        await communityService.followUser(userId);
        setFollowing([...following, userId]);
        message.success('已关注');
      }
    } catch (error: any) {
      message.error(error.message || '操作失败');
    }
  };

  const handleComment = async () => {
    if (commentPostId === null) {
      return;
    }
    if (!commentContent.trim()) {
      message.warning('请输入评论内容');
      return;
    }
    try {
      setCommentLoading(true);
      await communityService.commentPost(commentPostId, { content: commentContent.trim() });
      setPosts(posts.map((item) => item.id === commentPostId
        ? { ...item, commentCount: (item.commentCount || 0) + 1 }
        : item));
      message.success('评论成功');
      setCommentPostId(null);
      setCommentContent('');
    } catch (error: any) {
      message.error(error.message || '评论失败');
    } finally {
      setCommentLoading(false);
    }
  };

  const handleCreatePost = async () => {
    try {
      const values = await createForm.validateFields();
      setCreateLoading(true);
      await communityService.createPost({
        title: values.title,
        content: values.content,
        petId: currentPet?.id,
      });
      message.success('帖子已发布');
      setCreateOpen(false);
      createForm.resetFields();
      fetchPosts(activeTab);
    } catch (error: any) {
      if (error?.message) {
        message.error(error.message);
      }
    } finally {
      setCreateLoading(false);
    }
  };

  const renderPosts = () => {
    if (loading) {
      return <Spin spinning />;
    }
    if (posts.length === 0) {
      return <Empty description="暂无帖子" />;
    }
    return (
      <List
        dataSource={posts}
        renderItem={(post) => (
          <List.Item
            key={post.id}
            actions={[
              <Button
                type="link"
                size="small"
                icon={<LikeOutlined />}
                onClick={() => handleLike(post)}
                style={{ color: likedIds.includes(post.id) ? '#ff7a45' : undefined }}
              >
                {post.likeCount || 0}
              </Button>,
              <Button
                type="link"
                size="small"
                icon={<CommentOutlined />}
                onClick={() => {
                  setCommentPostId(post.id);
                  setCommentContent('');
                }}
              >
                {post.commentCount || 0}
              </Button>,
            ]}
          >
            <List.Item.Meta
              avatar={<Avatar src={post.userAvatar}>{post.username?.[0]}</Avatar>}
              title={(
                <Space>
                  <span>{post.username}</span>
                  {post.userId !== user?.id && (
                    <Button
                      size="small"
                      type={following.includes(post.userId) ? 'primary' : 'default'}
                      icon={<UserAddOutlined />}
                      onClick={() => handleFollow(post.userId)}
                    >
                      {following.includes(post.userId) ? '已关注' : '关注'}
                    </Button>
                  )}
                </Space>
              )}
              description={(
                <div>
                  <div style={{ fontSize: 14, fontWeight: 500, marginBottom: 4 }}>{post.title}</div>
                  <div style={{ color: '#666' }}>{post.content}</div>
                  {post.photoUrls && post.photoUrls.length > 0 && (
                    <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 8 }}>
                      {post.photoUrls.map((url, index) => (
                        <img
                          key={url}
                          src={url}
                          alt={`post-${post.id}-${index}`}
                          style={{ width: 96, height: 96, objectFit: 'cover', borderRadius: 4 }}
                        />
                      ))}
                    </div>
                  )}
                </div>
              )}
            />
          </List.Item>
        )}
      />
    );
  };

  return (
    <Card
      title="宠友圈社区"
      extra={(
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
          发布帖子
        </Button>
      )}
    >
      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        items={[
          { key: 'hot', label: '热门', children: renderPosts() },
          { key: 'recommend', label: '推荐', children: renderPosts() },
        ]}
      />

      <Modal
        title="发表评论"
        open={commentPostId !== null}
        onOk={handleComment}
        confirmLoading={commentLoading}
        onCancel={() => setCommentPostId(null)}
        okText="评论"
        cancelText="取消"
      >
        <Input.TextArea
          rows={4}
          value={commentContent}
          onChange={(e) => setCommentContent(e.target.value)}
          placeholder="输入评论..."
        />
      </Modal>

      <Modal
        title="发布帖子"
        open={createOpen}
        onOk={handleCreatePost}
        confirmLoading={createLoading}
        onCancel={() => setCreateOpen(false)}
        okText="发布"
        cancelText="取消"
      >
        <Form form={createForm} layout="vertical">
          <Form.Item
            name="title"
            label="标题"
            rules={[{ required: true, message: '请输入标题' }]}
          >
            <Input placeholder="输入帖子标题" />
          </Form.Item>
          <Form.Item
            name="content"
            label="内容"
            rules={[{ required: true, message: '请输入内容' }]}
          >
            <Input.TextArea rows={4} placeholder="分享你的宠物动态..." />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

export default CommunityPage;
