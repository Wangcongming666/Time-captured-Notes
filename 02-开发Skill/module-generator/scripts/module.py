import argparse, json, re
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--root',type=Path,default=Path('.'));p.add_argument('--check',action='store_true');p.add_argument('--module');p.add_argument('--output',type=Path);a=p.parse_args()
if a.check:
    errors=[];count=0
    for file in (a.root/'03-源码/backend/src/main/java').rglob('*.java'):
        count+=1;text=file.read_text(encoding='utf-8');parts=file.parts
        for imp in re.findall(r'import\s+([\w.]+)(?:\.\*)?;',text):
            if 'domain' in parts and any(x in imp for x in ['org.springframework','notes.platform','notes.web','notes.workflows']):errors.append(f'{file}: domain -> {imp}')
            if 'modules' in parts and '.modules.' in imp:
                own=parts[parts.index('modules')+1];other=imp.split('.modules.')[1].split('.')
                if other[0]!=own and len(other)>1 and other[1]!='api':errors.append(f'{file}: cross-module implementation -> {imp}')
            if 'web' in parts and '.persistence.Database' in imp:errors.append(f'{file}: web accesses Database')
    if count==0:errors.append('No Java source files found; refusing vacuous success')
    print(json.dumps({'files':count,'errors':errors},ensure_ascii=False));raise SystemExit(bool(errors))
if not a.module or not re.fullmatch('[a-z]+',a.module) or not a.output:p.error('Provide --module and --output, or --check')
if a.output.exists():raise SystemExit('Refusing to overwrite existing destination')
for layer in ['api','application','domain','infrastructure']:(a.output/layer).mkdir(parents=True,exist_ok=True)
template=(Path(__file__).resolve().parent.parent/'templates/Repository.java.tpl').read_text(encoding='utf-8')
(a.output/'domain'/f'{a.module.title()}Repository.java').write_text(template.replace('${module}',a.module).replace('${className}',a.module.title()),encoding='utf-8')
print(json.dumps({'generated':str(a.output),'module':a.module},ensure_ascii=False))
