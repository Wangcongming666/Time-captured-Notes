"""Create a database snapshot or restore into a new validation database, never over live data."""
import argparse,datetime,re,subprocess
from pathlib import Path
source=Path(__file__).resolve().parent.parent
p=argparse.ArgumentParser();p.add_argument('--restore',type=Path);p.add_argument('--database');a=p.parse_args()
prefix=['docker','compose','exec','-T','mysql','sh','-c']
def mysql(*args,data=None):
    return subprocess.run(prefix+['MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --user=root --batch --skip-column-names "$@"','mysql',*args],input=data,stdout=subprocess.PIPE,stderr=subprocess.PIPE,cwd=source,check=True).stdout
if a.restore:
    if not a.database or not re.fullmatch('shiguang_restore_[a-z0-9_]+',a.database):p.error('Restore requires a new shiguang_restore_* database; the live database is never overwritten')
    exists=mysql('--execute',f"SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='{a.database}'").strip()
    if exists!=b'0':raise SystemExit('Destination already exists; refusing to overwrite')
    mysql('--execute',f'CREATE DATABASE `{a.database}` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci')
    mysql(a.database,data=a.restore.read_bytes())
    tables=mysql('--execute',f"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='{a.database}'").decode().strip()
    print(f'Restored to {a.database}; tables={tables}. Live shiguang unchanged.')
else:
    folder=source.parent/'.runtime/backups';folder.mkdir(parents=True,exist_ok=True)
    target=folder/(datetime.datetime.now().strftime('%Y%m%d-%H%M%S')+'.sql')
    with target.open('xb') as out:
        result=subprocess.run(prefix+['MYSQL_PWD="$MYSQL_PASSWORD" mysqldump --user="$MYSQL_USER" --single-transaction --no-tablespaces --set-gtid-purged=OFF "$MYSQL_DATABASE"'],stdout=out,stderr=subprocess.PIPE,cwd=source)
    if result.returncode:raise SystemExit('Snapshot failed; inspect database service and permissions')
    print(f'Database snapshot: {target}. Media volume must be backed up separately during a write pause.')
