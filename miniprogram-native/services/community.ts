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
