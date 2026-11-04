import { post } from './api';

export type UploadType = 'avatar' | 'pet' | 'diary' | 'community';

/**
 * 上传图片到 MinIO，返回可访问的图片 URL
 */
export const uploadImage = (file: File, type: UploadType = 'avatar'): Promise<string> => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('type', type);
  return post<string>('/api/upload', formData, { timeout: 60000 });
};

export default {
  uploadImage,
};
