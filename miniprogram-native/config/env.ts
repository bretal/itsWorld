/**
 * 微信开发者工具中的开发版连接本机后端。
 * 微信公众平台需配置 https://lvhaorankk.top 为合法域名。
 */
const isLocalDevelopment = typeof wx !== 'undefined'
  && wx.getAccountInfoSync?.().miniProgram.envVersion === 'develop'
  && wx.getSystemInfoSync?.().platform === 'devtools';

export const ENV = {
  baseUrl: isLocalDevelopment ? 'http://127.0.0.1:8080/api' : 'https://lvhaorankk.top/api',
  minioBaseUrl: isLocalDevelopment ? 'http://127.0.0.1:19000' : 'https://lvhaorankk.top/files',
  staticBaseUrl: 'https://lvhaorankk.top/files/itsworld-static',
};
