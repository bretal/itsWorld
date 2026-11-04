// Community 相关类型定义

export interface Post {
  id: number;
  userId: number;
  username: string;
  userAvatar?: string;
  petId?: number;
  title: string;
  content: string;
  tags?: string[];
  photoUrls?: string[];
  location?: string;
  likeCount?: number;
  commentCount?: number;
  shareCount?: number;
  isEssence?: number; // 0=否, 1=精华帖
  isLiked?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface PostCreateRequest {
  title: string;
  content: string;
  petId?: number;
  tags?: string[];
  photoUrls?: string[];
  location?: string;
}

export interface PostUpdateRequest {
  title?: string;
  content?: string;
  tags?: string[];
  photoUrls?: string[];
  location?: string;
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
  userId: number;
  followingId: number;
  createdAt?: string;
}
