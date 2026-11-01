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
undefined_security=[];broken_refs=[];operation_ids=[]
for path,item in doc['paths'].items():
    for method,operation in item.items():
        if not isinstance(operation,dict):continue
        operation_ids.append(operation.get('operationId'))
        for requirement in operation.get('security',[]):
            for scheme in requirement:
                if scheme not in doc['components'].get('securitySchemes',{}):undefined_security.append(f'{method.upper()} {path}: {scheme}')
def check_refs(value):
    if isinstance(value,list):
        for item in value:check_refs(item)
    elif isinstance(value,dict):
        reference=value.get('$ref')
        if reference and reference.startswith('#/'):
            target=doc
            try:
                for part in reference[2:].split('/'):target=target[part]
            except (KeyError,TypeError):broken_refs.append(reference)
        for item in value.values():check_refs(item)
check_refs(doc)
result={'implemented':len(actual),'documented':len(declared),'coverage':len(actual&declared)/len(actual) if actual else 0,'missing':sorted(actual-declared),'unimplemented':sorted(declared-actual),'undefinedSecurity':undefined_security,'brokenRefs':broken_refs,'duplicateOperationIds':len(operation_ids)-len(set(operation_ids)),'routes':sorted(actual)}
if a.output:a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({k:v for k,v in result.items() if k!='routes'},ensure_ascii=False));raise SystemExit(bool(result['missing'] or result['unimplemented'] or undefined_security or broken_refs or result['duplicateOperationIds'] or not actual))
