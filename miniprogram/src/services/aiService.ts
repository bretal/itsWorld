import api from './api';

/**
 * 识别宠物情绪
 */
export const recognizeMood = (imageUrl: string): Promise<any> => {
  return api.post('/api/ai/mood', { imageUrl });
};

/**
 * 获取AI识别历史
 */
export const getMoodHistory = (page?: number, size?: number): Promise<any[]> => {
  return api.get('/api/ai/mood/history', { params: { page, size } });
};

export default {
  recognizeMood,
  getMoodHistory,
};
