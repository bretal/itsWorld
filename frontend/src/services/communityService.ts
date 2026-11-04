import { get, post, del } from './api';
import type { Post, PostCreateRequest, CommentRequest } from '@/types/community';

/**
 * 创建帖子
 */
export const createPost = (data: PostCreateRequest): Promise<number> => {
  return post('/api/community', data);
};

/**
 * 获取用户的帖子
 */
export const getUserPosts = (page?: number, size?: number): Promise<Post[]> => {
  return get('/api/community/user', { params: { page, size } });
};

/**
 * 获取帖子详情
 */
export const getPost = (id: number): Promise<Post> => {
  return get(`/api/community/${id}`);
};

/**
 * 获取热门帖子
 */
export const getHotPosts = (page?: number, size?: number): Promise<Post[]> => {
  return get('/api/community/hot', { params: { page, size } });
};

/**
 * 获取推荐帖子
 */
export const getRecommendPosts = (page?: number, size?: number): Promise<Post[]> => {
  return get('/api/community/recommend', { params: { page, size } });
};

/**
 * 删除帖子
 */
export const deletePost = (id: number): Promise<void> => {
  return del(`/api/community/${id}`);
};

/**
 * 点赞帖子
 */
export const likePost = (id: number): Promise<void> => {
  return post(`/api/community/${id}/like`);
};

/**
 * 取消点赞
 */
export const unlikePost = (id: number): Promise<void> => {
  return del(`/api/community/${id}/like`);
};

/**
 * 发表评论
 */
export const commentPost = (id: number, data: CommentRequest): Promise<number> => {
  return post(`/api/community/${id}/comment`, data);
};

/**
 * 删除评论
 */
export const deleteComment = (commentId: number): Promise<void> => {
  return del(`/api/community/comment/${commentId}`);
};

/**
 * 关注用户
 */
export const followUser = (userId: number): Promise<void> => {
  return post(`/api/community/follow/${userId}`);
};

/**
 * 取消关注
 */
export const unfollowUser = (userId: number): Promise<void> => {
  return del(`/api/community/follow/${userId}`);
};

/**
 * 获取关注列表
 */
export const getFollowing = (page?: number, size?: number): Promise<any[]> => {
  return get('/api/community/following', { params: { page, size } });
};

/**
 * 获取粉丝列表
 */
export const getFollowers = (page?: number, size?: number): Promise<any[]> => {
  return get('/api/community/followers', { params: { page, size } });
};

export default {
  createPost,
  getUserPosts,
  getPost,
  getHotPosts,
  getRecommendPosts,
  deletePost,
  likePost,
  unlikePost,
  commentPost,
  deleteComment,
  followUser,
  unfollowUser,
  getFollowing,
  getFollowers,
};
