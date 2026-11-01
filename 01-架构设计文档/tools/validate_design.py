"""只检查设计资产结构与交叉引用，不代表服务实现、SQL执行或性能验收。"""
from pathlib import Path
import json,re,yaml,xml.etree.ElementTree as ET
DOC=Path(__file__).resolve().parents[1]
errors=[];checks=[]
def check(name,condition):
    checks.append({'name':name,'passed':bool(condition)})
    if not condition:errors.append(name)
model=json.loads((DOC/'attachments/schema-model.json').read_text(encoding='utf8'))
schema=yaml.safe_load((DOC/'attachments/openapi.yaml').read_text(encoding='utf8'))
sql=(DOC/'attachments/schema.sql').read_text(encoding='utf8')
names={t['name'] for t in model}; cols={t['name']:{f[0] for f in t['fields']} for t in model}
check('30张表同源且DDL无缺失',len(names)==30 and all(f'CREATE TABLE `{n}`' in sql for n in names))
for t in model:
    for index in t['unique']+t['indexes']:check('索引字段 '+t['name']+str(index),set(index)<=cols[t['name']])
    for f in t['fields']:
        if f[5]:
            a,b=f[5].split('.');check('外键 '+t['name']+'.'+f[0],a in names and b in cols.get(a,set()))
refs=[]; operation_ids=[]; fr=set();operations=0
def walk(o):
    if isinstance(o,dict):
        if '$ref' in o:refs.append(o['$ref'])
        for v in o.values():walk(v)
    elif isinstance(o,list):
        for v in o:walk(v)
walk(schema)
check('OpenAPI本地schema引用全部存在',all(x.startswith('#/components/schemas/') and x.split('/')[-1] in schema['components']['schemas'] for x in refs))
for path,methods in schema['paths'].items():
    for method,op in methods.items():
        operations+=1;operation_ids.append(op['operationId']);fr.update(op['x-requirements'])
        actual={p['name'] for p in op['parameters'] if p['in']=='path'}
        check('路径参数 '+method+' '+path,actual==set(re.findall(r'{(\w+)}',path)))
        check('成功与错误响应 '+method+' '+path,any(x.startswith('2') for x in op['responses']) and {'401','403','409','503'}<=set(op['responses']))
        if method!='get':check('CSRF契约 '+method+' '+path,any(p['name']=='X-CSRF-TOKEN' and p['required'] for p in op['parameters']))
check('75个操作ID唯一',len(operation_ids)==len(set(operation_ids))==75)
check('FR01至27全部有接口映射',{f'FR-{i:02}' for i in range(1,28)}<=fr)
check('公开User不含角色或状态',not {'roles','status','passwordHash'} & set(schema['components']['schemas']['PublicUser']['properties']))
check('审核有私有预览契约',{'note','practice'}<=set(schema['components']['schemas']['ReviewDetail']['properties']))
check('笔记更新必须有version','version' in schema['components']['schemas']['NoteUpdateRequest']['required'])
figs=json.loads((DOC/'images/figures.json').read_text(encoding='utf8'))
for f in figs:
    fp=DOC/'images'/(f['id']+'.svg')
    try:ET.parse(fp);valid=True
    except Exception:valid=False
    check('SVG可解析 '+f['id'],valid)
    check('图源存在 '+f['id'],(DOC/'images'/(f['id']+'.mmd')).exists())
main=(DOC/'架构设计说明书.md').read_text(encoding='utf8')
for link in re.findall(r'!\[[^\]]*\]\(([^)]+)\)',main):check('主文档图链接 '+link,(DOC/link).exists())
adrs=sorted((DOC/'adr').glob('ADR-*.md'))
check('12条ADR',len(adrs)==12)
for p in adrs:
    text=p.read_text(encoding='utf8')
    check('ADR结构 '+p.name,all(x in text for x in ['## 背景','## 备选方案','## 决策','## 理由','## 影响','## 验证方式']))
plan=(DOC/'开发实施计划.md').read_text(encoding='utf8')
check('12个任务',len(re.findall(r'^## T\d{2} ',plan,re.M))==12)
check('实施复选框未伪勾选','[x]' not in plan.lower())
check('并发审核和Outbox终态规则',all(x in main for x in ['每次新的审核提交','lease_token','DONE是不可','按mediaId升序','t_daily_activity','NoteViewed']))
check('未自动创建业务源码目录',not (DOC.parent/'03-源码').exists())
report={'scope':'设计结构与交叉引用；未执行API/DDL/Compose/业务测试','tables':len(model),'fields':sum(len(t['fields']) for t in model),'httpOperations':operations,'schemas':len(schema['components']['schemas']),'figures':len(figs),'adrs':len(adrs),'checks':len(checks),'errors':errors,'details':checks}
(DOC/'attachments/validation-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8')
print(json.dumps({k:v for k,v in report.items() if k!='details'},ensure_ascii=False))
raise SystemExit(1 if errors else 0)
