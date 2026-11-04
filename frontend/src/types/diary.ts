// Diary 相关类型定义

export interface Diary {
  id: number;
  userId: number;
  petId: number;
  diaryDate: string;
  greeting?: string;
  healthScore?: number; // 0-12
  content: string;
  photoUrls?: string[];
  likeCount?: number;
  commentCount?: number;
  isPublic?: number; // 0=私密, 1=公开
  createdAt?: string;
  updatedAt?: string;
}

export interface DiaryCreateRequest {
  petId: number;
  diaryDate: string;
  location?: string;
  mood?: string;
  activities?: string[];
  healthScore?: number;
  content: string;
  photoUrls?: string[];
  isPublic?: number;
}

export interface DiaryUpdateRequest {
  location?: string;
  mood?: string;
  activities?: string[];
  healthScore?: number;
  content?: string;
  photoUrls?: string[];
  isPublic?: number;
}

export interface DiaryComment {
  id: number;
  diaryId: number;
  userId: number;
  username: string;
  userAvatar?: string;
  content: string;
  createdAt?: string;
}
