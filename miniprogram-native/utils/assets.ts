import { ENV } from '../config/env';

/**
 * 静态资源 URL（tab 图标、启动背景、logo 等）。
 * 资源放在 MinIO 的 itsworld-static bucket，不占用小程序包体积。
 */
export function assetUrl(path: string): string {
  return `${ENV.staticBaseUrl}/${path.replace(/^\/+/, '')}`;
}
