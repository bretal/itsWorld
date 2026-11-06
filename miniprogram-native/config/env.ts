/**
 * 环境配置
 * - develop：微信开发者工具/开发版，使用电脑局域网地址
 * - trial / release：体验版/正式版，使用生产环境 HTTPS 地址
 *
 * 生产域名需在微信公众平台配置 request/uploadFile/downloadFile 合法域名：
 * https://lvhaorankk.top
 */
function getEnvVersion(): string {
  try {
    const accountInfo = wx.getAccountInfoSync();
    return accountInfo?.miniProgram?.envVersion || 'develop';
  } catch (error) {
    return 'develop';
  }
}

const envVersion = getEnvVersion();
const isProduction = envVersion === 'trial' || envVersion === 'release';

const DEVELOPMENT_ENV = {
  // 当前开发机 WLAN IP：192.168.31.25；IP 变化时同步修改这里。
  baseUrl: 'http://192.168.31.25:8080/api',
  minioBaseUrl: 'http://192.168.31.25:9000',
};

const PRODUCTION_ENV = {
  baseUrl: 'https://lvhaorankk.top/api',
  // 生产图片目标路径；需 nginx 先落地 /files -> MinIO 反代。
  minioBaseUrl: 'https://lvhaorankk.top/files',
};

export const ENV = isProduction ? PRODUCTION_ENV : DEVELOPMENT_ENV;
