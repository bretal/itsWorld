#!/bin/bash

# MinIO 初始化脚本
# 创建所需的 bucket 和 lifecycle 策略

# 等待 MinIO 启动
sleep 10

# MinIO 服务地址
MINIO_HOST="minio:9000"
MINIO_USER="${MINIO_ROOT_USER:?MINIO_ROOT_USER is required}"
MINIO_PASS="${MINIO_ROOT_PASSWORD:?MINIO_ROOT_PASSWORD is required}"

# 使用 mc (minio client) 配置别名
mc alias set minio http://$MINIO_HOST $MINIO_USER $MINIO_PASS --api S3v4 || true

# 创建 bucket：用户头像
mc mb minio/itsworld-avatars --ignore-existing || true
mc version enable minio/itsworld-avatars || true

# 创建 bucket：日记图片
mc mb minio/itsworld-diary --ignore-existing || true
mc version enable minio/itsworld-diary || true

# 创建 bucket：社区内容
mc mb minio/itsworld-community --ignore-existing || true
mc version enable minio/itsworld-community || true

# 创建 bucket：临时文件（自动清理）
mc mb minio/itsworld-temp --ignore-existing || true
# 设置生命周期策略：7天自动删除
mc ilm import minio/itsworld-temp << EOF
{
  "Rules": [
    {
      "ID": "auto-delete-temp",
      "Filter": {},
      "Expiration": {
        "Days": 7
      },
      "NoncurrentVersionExpiration": {
        "NoncurrentDays": 7
      }
    }
  ]
}
EOF

echo "MinIO buckets 创建完成！"
echo "Buckets:"
echo "  - itsworld-avatars (用户头像)"
echo "  - itsworld-diary (日记图片)"
echo "  - itsworld-community (社区内容)"
echo "  - itsworld-temp (临时文件，7天过期)"
