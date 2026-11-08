import { ENV } from '../config/env';
import { getToken } from './auth';

export type UploadType = 'avatar' | 'pet' | 'diary' | 'community' | 'store' | 'shop' | 'review';

export function uploadImage(filePath: string, type: UploadType): Promise<string> {
  return new Promise<string>((resolve, reject) => {
    wx.uploadFile({
      url: `${ENV.baseUrl}/upload?type=${type}`,
      filePath,
      name: 'file',
      header: {
        Authorization: `Bearer ${getToken()}`,
      },
      success: (response: any) => {
        try {
          const body = JSON.parse(response.data);
          if (body.code === 0) {
            resolve(body.data);
            return;
          }
          wx.showToast({ title: body.message || '上传失败', icon: 'none' });
          reject(new Error(body.message || 'UPLOAD_FAILED'));
        } catch (error) {
          wx.showToast({ title: '上传响应异常', icon: 'none' });
          reject(error);
        }
      },
      fail: (error: any) => {
        wx.showToast({ title: '图片上传失败', icon: 'none' });
        reject(error);
      },
    });
  });
}
