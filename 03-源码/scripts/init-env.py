"""Initialize local configuration without overwriting credentials or manual edits."""
from pathlib import Path
import json
import secrets
import shlex

root = Path(__file__).resolve().parent.parent
target = root / '.env'
if not target.exists():
    target.write_text(''.join(f'{key}={secrets.token_urlsafe(36)}\n' for key in [
        'DB_PASSWORD', 'DB_ROOT_PASSWORD', 'RABBIT_PASSWORD', 'CURSOR_HMAC_KEY', 'DEMO_PASSWORD'
    ]), encoding='utf-8')
    print('Created Docker .env.')
else:
    print('Preserved existing Docker .env and credentials.')
values = {}
for line in target.read_text(encoding='utf-8').splitlines():
    if line.strip() and not line.lstrip().startswith('#') and '=' in line:
        key, value = line.split('=', 1)
        value = value.strip()
        if value.startswith(("'", '"')):
            parts = shlex.split(value, comments=True, posix=True)
            if len(parts) != 1:
                raise SystemExit('Invalid quoted .env value for ' + key.strip())
            value = parts[0]
        else:
            value = value.split(' #', 1)[0].rstrip()
        values[key.strip()] = value

def setting(key, default=''):
    return json.dumps('${' + key + ':' + values.get(key, default) + '}', ensure_ascii=False)

dev = root / 'backend/src/main/resources/application-dev.yml'
if dev.exists():
    print('Preserved application-dev.yml and manual edits.')
else:
    template = '''# 开发配置集中在此文件，默认启用 dev；真实本地凭据不提交 Git。
# Docker 环境变量覆盖连接地址，本机运行默认连接 localhost。
server:
  port: ${SERVER_PORT:8080}
management:
  server:
    port: ${MANAGEMENT_SERVER_PORT:8081}
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/shiguang?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true}
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:root}
    hikari:
      maximum-pool-size: 16
      minimum-idle: 4
      connection-timeout: 1000
      transaction-isolation: TRANSACTION_READ_COMMITTED
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: @@REDIS_PASSWORD@@
      timeout: 100ms
  rabbitmq:
    host: ${RABBIT_HOST:localhost}
    port: ${RABBIT_PORT:5672}
    username: ${RABBIT_USER:shiguang}
    password: @@RABBIT_PASSWORD@@
    virtual-host: shiguang
  servlet:
    multipart:
      max-file-size: ${UPLOAD_MAX_FILE_SIZE:100MB}
      max-request-size: ${UPLOAD_MAX_REQUEST_SIZE:101MB}
app:
  media-root: ${MEDIA_ROOT:./uploads}
  media:
    storage: ${MEDIA_STORAGE:local}
    video:
      max-bytes: ${VIDEO_MAX_BYTES:104857600}
      max-seconds: ${VIDEO_MAX_SECONDS:300}
      ffmpeg: ${FFMPEG_PATH:ffmpeg}
      ffprobe: ${FFPROBE_PATH:ffprobe}
    minio:
      endpoint: @@MINIO_ENDPOINT@@
      access-key: @@MINIO_ACCESS_KEY@@
      secret-key: @@MINIO_SECRET_KEY@@
      bucket: @@MINIO_BUCKET@@
      create-bucket: ${MINIO_CREATE_BUCKET:false}
  cursor-key: @@CURSOR_HMAC_KEY@@
  secure-cookie: ${SECURE_COOKIE:false}
  seed-demo: ${SEED_DEMO:true}
  demo-password: @@DEMO_PASSWORD@@
'''
    for key in ['REDIS_PASSWORD', 'RABBIT_PASSWORD', 'CURSOR_HMAC_KEY',
                'DEMO_PASSWORD', 'MINIO_ENDPOINT', 'MINIO_ACCESS_KEY', 'MINIO_SECRET_KEY']:
        template = template.replace('@@' + key + '@@', setting(key))
    template = template.replace('@@MINIO_BUCKET@@', setting('MINIO_BUCKET', 'shiguang-media'))
    dev.write_text(template, encoding='utf-8')
    print('Created backend/src/main/resources/application-dev.yml. Keep it local.')
