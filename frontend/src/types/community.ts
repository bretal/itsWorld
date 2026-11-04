// Community 相关类型定义

export interface Post {
  id: number;
  userId: number;
  username: string;
  userAvatar?: string;
  petId?: number;
  title: string;
  content: string;
  photoUrls?: string[];
  likeCount?: number;
  commentCount?: number;
  isLiked?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface PostCreateRequest {
  title: string;
  content: string;
  petId?: number;
  photoUrls?: string[];
}

export interface PostUpdateRequest {
  title?: string;
  content?: string;
  photoUrls?: string[];
}

export interface PostComment {
  id: number;
  postId: number;
  userId: number;
  username: string;
  userAvatar?: string;
  content: string;
  likeCount?: number;
  createdAt?: string;
}

export interface CommentRequest {
  content: string;
}

export interface UserFollow {
  id: number;
  followerId: number;
  followingId: number;
  createdAt?: string;
}
