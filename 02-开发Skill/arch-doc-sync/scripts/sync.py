import argparse,json,re
from pathlib import Path
import yaml
p=argparse.ArgumentParser();p.add_argument('--root',type=Path,default=Path('.'));p.add_argument('--output',type=Path);a=p.parse_args()
normalize=lambda path:re.sub(r'\{[^}]+\}','{}',path)
actual=set()
for file in (a.root/'03-源码/backend/src/main/java/cn/shiguang/notes/web').rglob('*Controller.java'):
    text=file.read_text(encoding='utf-8')
    prefix=re.search(r'@RequestMapping\("([^"]+)"\)',text)
    if prefix:
        for method,path in re.findall(r'@(Get|Post|Put|Delete|Patch)Mapping\("([^"]+)"\)',text):actual.add(method.upper()+' '+normalize(prefix[1]+path))
doc=yaml.safe_load((a.root/'03-源码/docs/api/openapi.yaml').read_text(encoding='utf-8'))
declared={method.upper()+' '+normalize(path) for path,item in doc['paths'].items() for method in item if method in {'get','post','put','delete','patch'}}
result={'implemented':len(actual),'documented':len(declared),'coverage':len(actual&declared)/len(actual) if actual else 0,'missing':sorted(actual-declared),'unimplemented':sorted(declared-actual),'routes':sorted(actual)}
if a.output:a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({k:v for k,v in result.items() if k!='routes'},ensure_ascii=False));raise SystemExit(bool(result['missing'] or result['unimplemented'] or not actual))
