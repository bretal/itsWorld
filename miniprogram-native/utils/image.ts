import { ENV } from '../config/env';

const imageCache: Record<string, string> = {};

/**
 * 将历史 MinIO 地址映射到当前环境的文件入口。
 */
export function normalizeMinioUrl(url?: string): string {
  if (!url) {
    return '';
  }
  if (/[?&]X-Amz-Signature=/i.test(url)) {
    return url;
  }
  let normalized = url
    .replace('http://localhost:9000', ENV.minioBaseUrl)
    .replace('http://127.0.0.1:9000', ENV.minioBaseUrl);

  // 旧社区帖子曾保存局域网直连地址。限定社区桶及私有主机，
  // 避免改写第三方图片或带签名的文件请求。
  normalized = normalized.replace(
    /^http:\/\/(?:localhost|127\.0\.0\.1|192\.168\.\d{1,3}\.\d{1,3}|10(?:\.\d{1,3}){3}|172\.(?:1[6-9]|2\d|3[01])\.\d{1,3}\.\d{1,3}):(?:9000|19000)(?=\/itsworld-community\/)/i,
    ENV.minioBaseUrl,
  );

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
