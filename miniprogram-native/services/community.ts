import { request } from '../utils/request';
import type { PageData } from '../types/page';

export interface Post {
  id: number;
  userId: number;
  username?: string;
  userAvatar?: string;
  petId?: number;
  title?: string;
  content?: string;
  photoUrls?: string[];
  likeCount?: number;
  commentCount?: number;
  isLiked?: boolean;
  isFollowing?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface PostPayload {
  title: string;
  content: string;
  petId?: number;
  photoUrls?: string[];
}

export interface Comment {
  id: number;
  postId: number;
  userId: number;
  username?: string;
  userAvatar?: string;
  content: string;
  replyToCommentId?: number;
  replyToUsername?: string;
  likeCount?: number;
  isLiked?: boolean;
  createdAt?: string;
}

export function getHotPosts(): Promise<Post[]> {
  return request<Post[]>({ url: '/community/hot' });
}

export function getRecommendPosts(): Promise<Post[]> {
  return request<Post[]>({ url: '/community/recommend' });
}

export function getFollowingFeed(): Promise<Post[]> {
  return request<Post[]>({ url: '/community/following/feed' });
}

export function getComments(postId: number): Promise<Comment[]> {
  return request<Comment[]>({ url: `/community/${postId}/comments` });
}

export type CommunityTab = 'recommend' | 'hot' | 'following';

export type FollowTab = 'following' | 'followers';

export interface FollowUser {
  id: number;
  nickName?: string;
  avatarUrl?: string;
  bio?: string;
  isFollowing: boolean;
  followsMe: boolean;
}

export interface FollowStats {
  followingCount: number;
  followerCount: number;
}

export function getFollowStats(): Promise<FollowStats> {
  return request<FollowStats>({ url: '/community/follow/stats' });
}

export function getFollowUsers(tab: FollowTab, page: number): Promise<PageData<FollowUser>> {
  return request<PageData<FollowUser>>({
    url: `/community/${tab}/page`,
    data: { page, size: 20 },
  });
}

export function searchCommunityUsers(keyword: string, page: number): Promise<PageData<FollowUser>> {
  return request<PageData<FollowUser>>({
    url: '/community/users/search',
    data: { keyword, page, size: 20 },
  });
}

export function setUserFollowing(userId: number, following: boolean): Promise<void> {
  return request<void>({
    url: `/community/follow/${userId}`,
    method: following ? 'POST' : 'DELETE',
  });
}

export function confirmUnfollow(name: string): Promise<boolean> {
  return new Promise((resolve) => {
    wx.showModal({
      title: `取消关注${name}？`,
      content: '对方的帖子将从关注动态移除。',
      cancelText: '保留关注',
      confirmText: '取消关注',
      confirmColor: '#773ba0',
      success: (result: { confirm: boolean }) => resolve(result.confirm),
      fail: () => resolve(false),
    });
  });
}


export function getPostPage(
  type: CommunityTab,
  page = 1,
  size = 10,
): Promise<PageData<Post>> {
  return request<PageData<Post>>({
    url: '/community/page',
    data: { type, page, size },
  });
}

export function getCommentsPage(
  postId: number,
  page = 1,
  size = 10,
): Promise<PageData<Comment>> {
  return request<PageData<Comment>>({
    url: `/community/${postId}/comments/page`,
    data: { page, size },
  });
}

export function getPost(postId: number): Promise<Post> {
  return request<Post>({ url: `/community/${postId}` });
}

export function createPost(payload: PostPayload): Promise<number> {
  return request<number>({
    url: '/community',
    method: 'POST',
    data: payload,
    showLoading: true,
  });
}

export function likePost(postId: number): Promise<void> {
  return request<void>({ url: `/community/${postId}/like`, method: 'POST' });
}

export function unlikePost(postId: number): Promise<void> {
  return request<void>({
    url: `/community/${postId}/like`,
    method: 'DELETE',
  });
}

export function createComment(postId: number, content: string): Promise<number> {
  return request<number>({
    url: `/community/${postId}/comment`,
    method: 'POST',
    data: { content },
    showLoading: true,
  });
}
