"""Restore only into a new destination; --check never changes source files."""
import argparse, json
from pathlib import Path
MODULES = ['identity','content','social','collection','governance','discovery','notification','statistics']
p=argparse.ArgumentParser();p.add_argument('--root',type=Path,default=Path('.'));p.add_argument('--output',type=Path);p.add_argument('--check',action='store_true');a=p.parse_args()
if a.output:
    if a.output.exists(): raise SystemExit('Refusing to overwrite an existing destination')
    a.output.mkdir(parents=True)
    for module in MODULES:
        for layer in ['api','application','domain','infrastructure']:
            folder=a.output/'03-源码/backend/src/main/java/cn/shiguang/notes/modules'/module/layer
            folder.mkdir(parents=True);(folder/'.gitkeep').touch()
    (a.output/'03-源码/frontend/src').mkdir(parents=True)
    source=a.root/'03-源码/.env.example'
    (a.output/'03-源码/.env.example').write_text(source.read_text(encoding='utf-8'),encoding='utf-8')
    print(json.dumps({'generated':str(a.output),'modules':MODULES},ensure_ascii=False))
elif a.check:
    missing=[m for m in MODULES if not (a.root/'03-源码/backend/src/main/java/cn/shiguang/notes/modules'/m).is_dir()]
    if not (a.root/'03-源码/.env.example').is_file(): missing.append('.env.example')
    print(json.dumps({'missing':missing,'modules':len(MODULES)},ensure_ascii=False));raise SystemExit(bool(missing))
else:p.error('Choose --output or --check')
