"""Validate representative real HTTP responses against the checked-in OpenAPI schemas."""
import copy,json,os,re,http.cookiejar,urllib.request,urllib.error,urllib.parse
from pathlib import Path
import yaml,jsonschema
root=Path(__file__).resolve().parent.parent
doc=yaml.safe_load((root/'docs/api/openapi.yaml').read_text(encoding='utf-8'))
def convert(value):
    if isinstance(value,list):return [convert(x) for x in value]
    if not isinstance(value,dict):return value
    result={k:convert(v) for k,v in value.items() if k!='nullable'}
    if value.get('nullable'):return {'anyOf':[result,{'type':'null'}]}
    return result
schema=convert(doc);resolver=jsonschema.RefResolver.from_schema(schema)
base=os.environ.get('E2E_BASE_URL','http://localhost:8080');opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()));csrf=None;results=[]
def call(path,method='GET',body=None,expected=200):
    global csrf
    headers={'Content-Type':'application/json'}
    if csrf:headers['X-CSRF-TOKEN']=csrf
    req=urllib.request.Request(base+'/api/v1'+urllib.parse.quote(path,safe='/?=&%'),data=None if body is None else json.dumps(body).encode(),headers=headers,method=method)
    try:
        response=opener.open(req);status=response.status;data=json.load(response)
    except urllib.error.HTTPError as e:status=e.code;data=json.load(e)
    matching=[item[method.lower()] for route,item in schema['paths'].items() if method.lower() in item and re.fullmatch(re.sub(r'\{[^}]+\}',r'[^/]+',route),'/api/v1'+path.split('?')[0])]
    errors=[]
    if status!=expected:errors.append(f'HTTP {status}, expected {expected}')
    if len(matching)!=1:errors.append('Ambiguous or undocumented route')
    else:
        contract=matching[0]['responses'].get(str(status),{}).get('content',{}).get('application/json',{}).get('schema')
        if not contract:errors.append('Undocumented response status')
        else:errors += [str(list(e.absolute_path))+': '+e.message for e in jsonschema.Draft7Validator(contract,resolver=resolver).iter_errors(data)]
    results.append({'method':method,'path':path,'status':status,'errors':errors})
    return data.get('data')
def login(name):
    global opener,csrf
    opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()));csrf=call('/auth/csrf')['token']
    password=os.environ.get('DEMO_PASSWORD')
    if not password:
        password=next(line.split('=',1)[1] for line in (root/'.env').read_text(encoding='utf-8').splitlines() if line.startswith('DEMO_PASSWORD='))
    call('/auth/login','POST',{'username':name,'password':password});csrf=call('/auth/csrf')['token']
feed=call('/feed');note=feed['items'][0]['id'];user=feed['items'][0]['authorId']
for path in ['/search?q=演示','/topics',f'/users/{user}',f'/notes/{note}',f'/notes/{note}/comments',f'/notes/{note}/feedback']:call(path)
login('demo_user')
for path in ['/me','/me/preferences','/folders','/practices','/notifications','/notifications/unread','/me/reports','/statistics/me','/statistics/me/ranking','/me/notes',f'/me/notes/{note}']:call(path)
login('demo_reviewer');call('/reviews');call('/admin/reports');call('/admin/statistics/governance')
login('demo_admin')
for path in ['/admin/users','/admin/topics','/admin/audits','/admin/statistics/platform','/admin/statistics/ranking']:call(path)
report={'samples':len(results),'passed':sum(not r['errors'] for r in results),'results':results}
target=root.parent/'docs/HTTP契约检查.json';target.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'samples':report['samples'],'passed':report['passed'],'failures':[r for r in results if r['errors']]},ensure_ascii=True))
raise SystemExit(report['passed']!=report['samples'])
