"""Upload only assistant v2 assets to the existing local static bucket using S3 SigV4.
Credentials come from the environment or local ignored .env and are never printed.
"""
from pathlib import Path
import os, hashlib, hmac, datetime, urllib.request, urllib.parse
root=Path(__file__).resolve().parents[1]
config={}
if (root/'.env').exists():
 for line in (root/'.env').read_text(encoding='utf-8-sig').splitlines():
  if '=' in line and not line.lstrip().startswith('#'):
   k,v=line.split('=',1); config[k.strip()]=v.strip().strip('"').strip("'")
def value(*names):
 for n in names:
  if os.environ.get(n) or config.get(n): return os.environ.get(n) or config[n]
 raise SystemExit('Missing local MinIO credential configuration')
access=value('MINIO_ACCESS_KEY','MINIO_ROOT_USER'); secret=value('MINIO_SECRET_KEY','MINIO_ROOT_PASSWORD')
endpoint=os.environ.get('ASSISTANT_ASSET_ENDPOINT','http://127.0.0.1:9000')
files=[root/'imgs/assistant-v2'/f'{n}.png' for n in ['paw','bowl','shower','care','heart','pill','shield','bug','comb']]+[root/'imgs/assistant-v2/hero.webp']
for file in files:
 data=file.read_bytes(); digest=hashlib.sha256(data).hexdigest()
 now=datetime.datetime.now(datetime.timezone.utc); stamp=now.strftime('%Y%m%dT%H%M%SZ'); day=now.strftime('%Y%m%d')
 path='/itsworld-static/miniapp-v1/assistant-v2/'+file.name
 host=urllib.parse.urlsplit(endpoint).netloc
 headers=f'host:{host}\nx-amz-content-sha256:{digest}\nx-amz-date:{stamp}\n'
 signed='host;x-amz-content-sha256;x-amz-date'
 canonical=f'PUT\n{path}\n\n{headers}\n{signed}\n{digest}'
 scope=f'{day}/us-east-1/s3/aws4_request'
 string=f'AWS4-HMAC-SHA256\n{stamp}\n{scope}\n{hashlib.sha256(canonical.encode()).hexdigest()}'
 def sign(k,v): return hmac.new(k,v.encode(),hashlib.sha256).digest()
 key=sign(sign(sign(sign(('AWS4'+secret).encode(),day),'us-east-1'),'s3'),'aws4_request')
 signature=hmac.new(key,string.encode(),hashlib.sha256).hexdigest()
 auth=f'AWS4-HMAC-SHA256 Credential={access}/{scope}, SignedHeaders={signed}, Signature={signature}'
 request=urllib.request.Request(endpoint+path,data=data,method='PUT',headers={'Authorization':auth,'x-amz-date':stamp,'x-amz-content-sha256':digest,'Content-Type':'image/webp' if file.suffix=='.webp' else 'image/png','Cache-Control':'public, max-age=86400'})
 with urllib.request.urlopen(request,timeout=15) as response: assert response.status==200
 with urllib.request.urlopen(endpoint+path,timeout=15) as response:
  assert hashlib.sha256(response.read()).hexdigest()==digest
 print('Uploaded and verified:',file.name)
