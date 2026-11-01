"""Generate random local credentials without printing them or overwriting existing files."""
from pathlib import Path
import secrets
target=Path(__file__).resolve().parent.parent/'.env'
if target.exists():raise SystemExit('.env already exists; preserve existing database credentials')
target.write_text(''.join(f'{key}={secrets.token_urlsafe(36)}\n' for key in ['DB_PASSWORD','DB_ROOT_PASSWORD','RABBIT_PASSWORD','CURSOR_HMAC_KEY','DEMO_PASSWORD']),encoding='utf-8')
print('Created .env. Read DEMO_PASSWORD locally; never commit this file.')
