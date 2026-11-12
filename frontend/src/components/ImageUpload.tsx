import { useState } from 'react';
import { Button, Image, Space, Upload, message } from 'antd';
import { uploadImage } from '@/services/uploadService';

interface Props {
  value?: string;
  onChange?: (value: string) => void;
}

/** 表单存储上传接口返回的 URL，管理员只需选择本地图片。 */
export default function ImageUpload({ value, onChange }: Props) {
  const [uploading, setUploading] = useState(false);
  return (
    <Space direction="vertical">
      {value && <Image src={value} width={96} height={96} style={{ objectFit: 'contain', borderRadius: 8 }} />}
      <Space>
        <Upload
          accept=".jpg,.jpeg,.png,.gif,.webp"
          showUploadList={false}
          disabled={uploading}
          beforeUpload={async file => {
            if (!/\.(jpe?g|png|gif|webp)$/i.test(file.name)) {
              message.error('请选择 JPG、PNG、GIF 或 WebP 图片');
              return Upload.LIST_IGNORE;
            }
            if (file.size > 10 * 1024 * 1024) {
              message.error('图片大小不能超过 10MB');
              return Upload.LIST_IGNORE;
            }
            setUploading(true);
            try {
              const url = await uploadImage(file, 'shop');
              onChange?.(url);
              message.success('图片上传成功');
            } catch (error) {
              message.error(error instanceof Error ? error.message : '图片上传失败，请重试');
            } finally {
              setUploading(false);
            }
            return Upload.LIST_IGNORE;
          }}
        >
          <Button loading={uploading}>{value ? '更换图片' : '上传图片'}</Button>
        </Upload>
        {value && <Button disabled={uploading} onClick={() => onChange?.('')}>清除</Button>}
      </Space>
      <span style={{ color: '#999', fontSize: 12 }}>支持 JPG、PNG、GIF、WebP，最大 10MB</span>
    </Space>
  );
}
