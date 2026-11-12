import argparse, datetime, hashlib, hmac, json, mimetypes, os, subprocess, urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
IMAGES = ROOT / 'imgs/pet-products-download/精选商品图'
CATALOG = {
 '零食': [
 ('Lyopet 牛肉鸡肉冻干猫零食','牛肉鸡肉',3990), ('宠物冻干零食组合','展示组合',5990),
 ('Meaty Treaty 蓝鳍金枪鱼冻干零食','猫犬款',5990), ('Nature Island 鸡胸肉冻干零食','80g',4990),
 ('Freeze Dry Australia 鸡肝冻干零食','80g',3990), ('伊纳宝 Churu 犬用零食','展示款',3590),
 ('伊纳宝 Churu 洁齿零食','展示款',2990), ('毛毛好室幼龄猫狗冻干零食','展示款',4990),
 ('Beastie Bites 猫狗冻干零食','展示款',4590)],
 '日用品': [
 ('猫咪日用品套装','展示组合',8900), ('宠物日用品精选组合','展示组合',7900),
 ('绿色自动伸缩犬牵引绳','3m / 绿色',4990), ('Zulema 红色尼龙犬牵引绳','150cm × 2cm / 红色',2990),
 ('犬外出牵引与折叠碗套装','展示组合',5990), ('宠物生活用品展示组合','展示组合',6900),
 ('KIBBO 橙色宠物日用品套装','橙色展示款',8900), ('宠物日用配件组合','展示组合',6900),
 ('Emma Pets 宠物用品展示组合','展示组合',7900), ('Goldfish 宠物用品展示组合','展示组合',7900)],
 '宠粮': [
 ('渴望与爱肯拿宠粮展示组合','展示组合',15900), ('犬用干粮展示组合','展示组合',9900),
 ('Martin Group 宠粮展示款','展示款',8900), ('犬粮精选展示组合','展示组合',10900),
 ('渴望与爱肯拿品牌宠粮展示款','展示款',12900), ('渴望与爱肯拿犬粮','1.8kg / 2kg 展示组合',16900),
 ('渴望宠粮展示款','展示款',12900), ('爱肯拿与渴望猫粮','1.8kg 展示款',15900),
 ('Feringa 羊肉鸡肉成猫主食罐','400g',3900), ('Absolute Holistic 牛肉羊肉犬粮','展示款',12900)],
 '护理用品': [
 ('The Good Paws 宠物护理套装','展示组合',8900), ('Sashpet 宠物梳洗护理用品','展示款',5900),
 ('薰衣草香犬用洗护液','展示款',3900), ('猫犬三合一修剪洗护按摩套装','三合一组合',5900),
 ('犬用梳理清洁工具组合','展示组合',6900), ('大型长毛犬家庭梳理套装','展示组合',9900),
 ('Wah Puppy 幼犬入门梳理套装','展示组合',7900), ('Pets Empire 猫犬五合一护理套装','五合一组合',8900),
 ('Blade Exchange 家用梳理组合','展示组合',8900), ('PetMeds 宠物护理展示组合','展示组合',6900)],
}

def literal(value):
    return 'CONVERT(0x' + str(value).encode('utf-8').hex() + ' USING utf8mb4) COLLATE utf8mb4_unicode_ci'

def upload(data, path, content_type, access, secret):
    host = '127.0.0.1:9000'
    url = 'http://' + host + path
    digest = hashlib.sha256(data).hexdigest()
    stamp = datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    day = stamp[:8]
    signed = 'host;x-amz-content-sha256;x-amz-date'
    headers = f'host:{host}\nx-amz-content-sha256:{digest}\nx-amz-date:{stamp}\n'
    canonical = f'PUT\n{path}\n\n{headers}\n{signed}\n{digest}'
    scope = f'{day}/us-east-1/s3/aws4_request'
    payload = f'AWS4-HMAC-SHA256\n{stamp}\n{scope}\n{hashlib.sha256(canonical.encode()).hexdigest()}'
    key = ('AWS4' + secret).encode()
    for part in (day, 'us-east-1', 's3', 'aws4_request'):
        key = hmac.new(key, part.encode(), hashlib.sha256).digest()
    signature = hmac.new(key, payload.encode(), hashlib.sha256).hexdigest()
    auth = f'AWS4-HMAC-SHA256 Credential={access}/{scope}, SignedHeaders={signed}, Signature={signature}'
    request = urllib.request.Request(url, data=data, method='PUT', headers={
        'Authorization': auth, 'x-amz-date': stamp, 'x-amz-content-sha256': digest,
        'Content-Type': content_type,
    })
    with urllib.request.urlopen(request, timeout=20):
        pass
    with urllib.request.urlopen(url, timeout=20) as response:
        if hashlib.sha256(response.read()).hexdigest() != digest:
            raise RuntimeError(f'Image verification failed: {path}')

def prepare():
    sources = json.loads((IMAGES / 'sources.json').read_text(encoding='utf-8-sig'))
    expected = {row['file']: row['sha256'] for row in sources}
    assets = []
    sql = ['START TRANSACTION;']
    for group, (category, products) in enumerate(CATALOG.items(), 1):
        c = literal(category)
        sql += [
            f"INSERT INTO shop_category (name,sort,status) SELECT {c},{group},'ENABLED' WHERE NOT EXISTS (SELECT 1 FROM shop_category WHERE name={c} AND parent_id IS NULL);",
            f"SET @category=(SELECT id FROM shop_category WHERE name={c} AND parent_id IS NULL ORDER BY (status='ENABLED') DESC,id LIMIT 1);",
            "UPDATE shop_category SET status='ENABLED' WHERE id=@category AND status<>'ENABLED';",
        ]
        for index, (name, spec, price) in enumerate(products, 1):
            folder = IMAGES / f'{group:02d}-{category}'
            file, = folder.glob(f'{index:02d}.*')
            data = file.read_bytes()
            digest = expected.pop(file.relative_to(IMAGES).as_posix())
            if hashlib.sha256(data).hexdigest() != digest:
                raise RuntimeError(f'Unexpected image contents: {file}')
            code = f'LOCAL-PET-{group:02d}-{index:02d}'
            path = f'/itsworld-static/local-pet-catalog/{code.lower()}-{digest[:12]}{file.suffix}'
            assets.append((data, path, mimetypes.guess_type(file.name)[0] or 'application/octet-stream'))
            cover = literal('http://localhost:9000' + path)
            title = literal(name)
            details = literal('本地测试商品。图片为展示参考，价格与初始库存为测试数据。')
            variant = literal(json.dumps({'规格': spec}, ensure_ascii=False))
            sql += [
                f"INSERT INTO shop_product (product_code,category_id,name,subtitle,description,cover_url,sale_status,sort) VALUES ('{code}',@category,{title},{c},{details},{cover},'OFF_SALE',{index}) ON DUPLICATE KEY UPDATE id=id;",
                f"SET @product=(SELECT id FROM shop_product WHERE product_code='{code}');",
                f"INSERT INTO shop_product_image (product_id,image_url,sort) SELECT @product,{cover},0 WHERE NOT EXISTS (SELECT 1 FROM shop_product_image WHERE product_id=@product AND image_url={cover});",
                f"INSERT INTO shop_sku (product_id,sku_code,spec_json,sale_price,market_price,status) VALUES (@product,'{code}-01',{variant},{price},{price},'ENABLED') ON DUPLICATE KEY UPDATE id=id;",
                f"SET @sku=(SELECT id FROM shop_sku WHERE sku_code='{code}-01' AND product_id=@product);",
                "INSERT INTO shop_inventory (sku_id,warehouse_id,available_qty) VALUES (@sku,0,30) ON DUPLICATE KEY UPDATE id=id;",
                f"INSERT INTO shop_inventory_log (sku_id,biz_type,change_available,before_available,after_available,idempotency_key,remark) SELECT @sku,'ADMIN_ADJUST',30,0,30,'{code}-INITIAL','Local catalog seed' WHERE NOT EXISTS (SELECT 1 FROM shop_inventory_log WHERE idempotency_key='{code}-INITIAL');",
                "UPDATE shop_product p JOIN shop_sku s ON s.id=@sku JOIN shop_inventory i ON i.sku_id=s.id AND i.warehouse_id=0 SET p.sale_status='ON_SALE' WHERE p.id=@product AND s.status='ENABLED' AND i.available_qty>0;",
            ]
    if expected:
        raise RuntimeError(f'Images missing catalog entries: {list(expected)}')
    sql += ['COMMIT;', """SELECT c.name AS category, COUNT(*) AS on_sale
FROM shop_product p JOIN shop_category c ON c.id=p.category_id
JOIN shop_sku s ON s.product_id=p.id AND s.sku_code=CONCAT(p.product_code,'-01')
JOIN shop_inventory i ON i.sku_id=s.id AND i.warehouse_id=0
WHERE p.product_code LIKE 'LOCAL-PET-%' AND p.sale_status='ON_SALE'
AND c.status='ENABLED' AND s.status='ENABLED' AND i.available_qty>0
GROUP BY c.name;"""]
    return assets, '\n'.join(sql)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    assets, sql = prepare()
    print(f'Validated {len(assets)} images and {len(CATALOG)} categories', flush=True)
    if not args.apply:
        return
    port = subprocess.check_output(['docker', 'port', 'its-world-mysql', '3306/tcp'], text=True)
    if ':13306' not in port:
        raise RuntimeError('Expected local MySQL mapping on port 13306')
    env = {}
    for line in (ROOT / '.env').read_text(encoding='utf-8-sig').splitlines():
        if '=' in line and not line.lstrip().startswith('#'):
            name, value = line.split('=', 1)
            env[name.strip()] = value.strip().strip('"').strip("'")
    access = os.environ.get('MINIO_ROOT_USER') or env['MINIO_ROOT_USER']
    secret = os.environ.get('MINIO_ROOT_PASSWORD') or env['MINIO_ROOT_PASSWORD']
    for data, path, kind in assets:
        upload(data, path, kind, access, secret)
    print(f'Uploaded and verified {len(assets)} images', flush=True)
    command = ['docker', 'exec', '-i', 'its-world-mysql', 'sh', '-c',
               'exec mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" its_world']
    subprocess.run(command, input=sql.encode('utf-8'), check=True)

if __name__ == '__main__':
    main()
