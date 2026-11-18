import { useRef, useState } from 'react';
import { Button, Card, Descriptions, Image, Select, Space, Typography, Upload, message } from 'antd';
import type { UploadFile } from 'antd';
import { post } from '@/services/api';

interface Asset {
  url: string;
  originalWidth: number;
  originalHeight: number;
  originalBytes: number;
  width: number;
  height: number;
  bytes: number;
}

const formatSize = (bytes: number) => `${(bytes / 1024).toFixed(1)} KB`;

export default function MiniProgramAssetsPage() {
  const [files, setFiles] = useState<UploadFile[]>([]);
  const [purpose, setPurpose] = useState('onboarding');
  const [asset, setAsset] = useState<Asset>();
  const [uploading, setUploading] = useState(false);
  const inFlight = useRef(false);

  async function upload() {
    const file = files[0]?.originFileObj;
    if (!file || inFlight.current) return;
    inFlight.current = true;
    setUploading(true);
    setAsset(undefined);
    const form = new FormData();
    form.append('file', file);
    form.append('purpose', purpose);
    try {
      setAsset(await post<Asset>('/api/admin/miniprogram-assets', form, { timeout: 60_000 }));
    } catch (error) {
      message.error(error instanceof Error ? error.message : '上传失败');
    } finally {
      inFlight.current = false;
      setUploading(false);
    }
  }

  async function copyLink(url: string) {
    try {
      await navigator.clipboard.writeText(url);
      message.success('已复制链接');
    } catch {
      message.info('请选中地址手动复制');
    }
  }

  return (
    <Card title="小程序素材">
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Typography.Text type="secondary">
          上传 PNG 或 JPEG（最多 10 MB），生成长边最多 1080 像素的 WebP。
          请在生产管理端上传需要在体验版使用的素材。
        </Typography.Text>
        <label>
          用途{' '}
          <Select value={purpose} onChange={setPurpose} disabled={uploading}
            options={[{ value: 'onboarding', label: '新手引导' }]} />
        </label>
        <Upload
          accept=".png,.jpg,.jpeg"
          disabled={uploading}
          fileList={files}
          maxCount={1}
          beforeUpload={(file) => {
            if (file.size > 10 * 1024 * 1024) {
              message.error('图片不能超过 10 MB');
              return Upload.LIST_IGNORE;
            }
            return false;
          }}
          onChange={({ fileList }) => setFiles(fileList)}
        >
          <Button disabled={uploading}>选择图片</Button>
        </Upload>
        <Button type="primary" disabled={!files.length} loading={uploading} onClick={upload}>
          转换并上传
        </Button>
        {asset && (
          <>
            <Image width={240} src={asset.url} alt="上传后的素材" />
            <Descriptions column={1} items={[
              { key: 'original', label: '原图', children: `${asset.originalWidth} × ${asset.originalHeight} · ${formatSize(asset.originalBytes)}` },
              { key: 'webp', label: 'WebP', children: `${asset.width} × ${asset.height} · ${formatSize(asset.bytes)}` },
              { key: 'url', label: '已验证的访问地址', children: <Typography.Text style={{ overflowWrap: 'anywhere' }}>{asset.url}</Typography.Text> },
            ]} />
            <Button onClick={() => copyLink(asset.url)}>复制链接</Button>
          </>
        )}
      </Space>
    </Card>
  );
}
