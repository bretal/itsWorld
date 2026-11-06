import { ENV } from '../config/env';

const imageCache: Record<string, string> = {};

/**
 * 把历史数据里的 localhost MinIO 地址换成当前局域网地址。
 */
export function normalizeMinioUrl(url?: string): string {
  if (!url) {
    return '';
  }
  let normalized = url
    .replace('http://localhost:9000', ENV.minioBaseUrl)
    .replace('http://127.0.0.1:9000', ENV.minioBaseUrl);

  // 生产历史数据可能还是带 9000 端口的直连地址；统一改走同域名 /files。
  if (ENV.baseUrl.startsWith('https://lvhaorankk.top')) {
    normalized = normalized.replace(
      'https://lvhaorankk.top:9000/',
      'https://lvhaorankk.top/files/',
    );
  }
  return normalized;
}

/**
 * 真机上 <image> 直接加载 HTTP 图片可能失败；先用 getImageInfo 下载到本地临时文件，
 * 再把临时路径交给 <image>。失败时回退到原 URL。
 */
export async function resolveImages(urls?: string[]): Promise<string[]> {
  if (!urls || !urls.length) {
    return [];
  }
  return Promise.all(urls.map((url) => resolveImage(url)));
}

export function resolveImage(url?: string): Promise<string> {
  const normalized = normalizeMinioUrl(url);
  if (!normalized) {
    return Promise.resolve('');
  }
  if (!/^https?:\/\//.test(normalized)) {
    return Promise.resolve(normalized);
  }
  if (imageCache[normalized]) {
    return Promise.resolve(imageCache[normalized]);
  }

  return new Promise<string>((resolve) => {
    wx.getImageInfo({
      src: normalized,
      success: (result: any) => {
        const localPath = result.path || normalized;
        imageCache[normalized] = localPath;
        resolve(localPath);
      },
      fail: () => {
        resolve(normalized);
      },
    });
  });
}
