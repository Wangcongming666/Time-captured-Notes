"""生成需求驱动的架构附件；不创建业务工程，不连接任何业务服务。"""
from pathlib import Path
import json, re, html
import yaml

ROOT = Path(__file__).resolve().parents[2]
DOC = ROOT / '01-架构设计文档'
for p in ['images', 'adr', 'attachments', 'tools']:
    (DOC / p).mkdir(exist_ok=True)

def write(path, content):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    content=re.sub(r'(?m)(\|[^\n]+\|)\n\n(?=\|)',r'\1\n',content)
    path.write_text(content.strip() + '\n', encoding='utf-8')

TABLES = []
def table(name, owner, desc, fields, unique=(), indexes=()):
    # 字段格式：名称、SQL类型、可空、默认值、业务含义、引用表.列。
    base = [('id','BIGINT UNSIGNED',False,'AUTO_INCREMENT','技术主键',''),
            ('created_at','DATETIME(3)',False,'CURRENT_TIMESTAMP(3)','UTC创建时间',''),
            ('updated_at','DATETIME(3)',False,'CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)','UTC更新时间','')]
    TABLES.append(dict(name=name, owner=owner, desc=desc, fields=base+fields, unique=list(unique), indexes=list(indexes)))

def f(n,t,d,ref='',nullable=False,default=None): return (n,t,nullable,default,d,ref)
table('t_user','identity','账号与公开资料',[
 f('username','VARCHAR(32)','登录名：小写ASCII字母、数字、下划线，4–32字符'),
 f('password_hash','VARCHAR(100)','BCrypt盐哈希，禁止返回'), f('nickname','VARCHAR(40)','公开昵称'),
 f('bio','VARCHAR(300)','个人简介',default="''"),f('avatar_media_id','BIGINT UNSIGNED','头像媒体引用；避免与媒体表循环FK，应用验证',nullable=True),
 f('status','VARCHAR(16)','ACTIVE/BANNED',default="'ACTIVE'"),f('auth_version','INT UNSIGNED','封禁、改权后递增，校验已有会话',default='0')],unique=[('username',)])
table('t_role','identity','固定权限角色',[f('code','VARCHAR(24)','USER/REVIEWER/ADMIN'),f('name','VARCHAR(40)','显示名')],unique=[('code',)])
table('t_user_role','identity','账号角色关联',[f('user_id','BIGINT UNSIGNED','账号','t_user.id'),f('role_id','BIGINT UNSIGNED','角色','t_role.id')],unique=[('user_id','role_id')])
table('t_session','identity','数据库权威登录态；Redis不保存唯一撤销事实',[
 f('token_hash','CHAR(64)','256bit随机令牌的SHA256；原令牌只在HttpOnly Cookie'),f('user_id','BIGINT UNSIGNED','会话账号','t_user.id'),
 f('auth_version','INT UNSIGNED','签发时账号授权版本'),f('csrf_hash','CHAR(64)','同步CSRF令牌哈希'),f('expires_at','DATETIME(3)','固定24小时到期'),
 f('revoked_at','DATETIME(3)','退出时撤销',nullable=True)],unique=[('token_hash',)],indexes=[('expires_at',),('user_id','revoked_at')])
table('t_topic','governance','话题及分类',[
 f('name','VARCHAR(40)','展示话题名'),f('category','VARCHAR(40)','分类'),f('status','VARCHAR(16)','ACTIVE/DISABLED',default="'ACTIVE'")],unique=[('name',)],indexes=[('category','status')])
table('t_media','content','私有文件元数据；不存储可绕过授权的公开URL',[
 f('owner_id','BIGINT UNSIGNED','上传人','t_user.id'),f('storage_key','VARCHAR(180)','随机路径，不采用用户原始文件名'),
 f('purpose','VARCHAR(16)','NOTE/PRACTICE/AVATAR'),f('mime','VARCHAR(40)','经实际解码确认的图像MIME'),f('size_bytes','BIGINT UNSIGNED','处理后字节数'),
 f('width','INT UNSIGNED','像素宽'),f('height','INT UNSIGNED','像素高'),f('sha256','CHAR(64)','文件校验值'),f('status','VARCHAR(16)','STAGED/BOUND/DELETED',default="'STAGED'")],unique=[('storage_key',)],indexes=[('owner_id','status','created_at')])
table('t_note','content','笔记聚合根；编辑重审期间旧内容也不公开',[
 f('author_id','BIGINT UNSIGNED','作者','t_user.id'),f('title','VARCHAR(160)','应用校验1–40 Unicode代码点'),
 f('body','TEXT','纯文本正文1–2000代码点'),f('status','VARCHAR(20)','DRAFT/PENDING/PUBLISHED/REJECTED/OFFLINE/DELETED',default="'DRAFT'"),
 f('revision','INT UNSIGNED','审核快照修订号，每次编辑及每次新提交递增',default='1'),f('version','INT UNSIGNED','乐观锁版本，每次修改递增',default='0'),
 f('published_at','DATETIME(3)','最近一次通过时间',nullable=True),f('reject_reason','VARCHAR(500)','作者可见驳回/下架原因',nullable=True)],indexes=[('status','published_at','id'),('author_id','status','id')])
table('t_note_media','content','笔记图片顺序',[f('note_id','BIGINT UNSIGNED','笔记','t_note.id'),f('media_id','BIGINT UNSIGNED','图片','t_media.id'),f('position','TINYINT UNSIGNED','顺序1–9')],unique=[('note_id','media_id'),('note_id','position')],indexes=[('media_id',)])
table('t_note_topic','content','笔记话题引用',[f('note_id','BIGINT UNSIGNED','笔记','t_note.id'),f('topic_id','BIGINT UNSIGNED','话题','t_topic.id')],unique=[('note_id','topic_id')],indexes=[('topic_id','note_id')])
table('t_interest','discovery','兴趣与话题降权偏好',[f('user_id','BIGINT UNSIGNED','账号','t_user.id'),f('topic_id','BIGINT UNSIGNED','话题','t_topic.id'),f('mode','VARCHAR(16)','LIKE/REDUCE')],unique=[('user_id','topic_id')])
table('t_hidden_note','discovery','不感兴趣笔记',[f('user_id','BIGINT UNSIGNED','账号','t_user.id'),f('note_id','BIGINT UNSIGNED','隐藏笔记','t_note.id')],unique=[('user_id','note_id')])
table('t_comment','social','两级展示的评论；隐藏父评论保留占位',[
 f('author_id','BIGINT UNSIGNED','作者','t_user.id'),f('note_id','BIGINT UNSIGNED','笔记','t_note.id'),f('root_id','BIGINT UNSIGNED','根评论；根记录为空','t_comment.id',nullable=True),
 f('reply_to_id','BIGINT UNSIGNED','被回复评论','t_comment.id',nullable=True),f('body','VARCHAR(2000)','1–500代码点'),f('status','VARCHAR(16)','VISIBLE/HIDDEN/DELETED',default="'VISIBLE'"),f('version','INT UNSIGNED','评论治理并发版本',default='0')],indexes=[('note_id','root_id','id'),('author_id','id')])
table('t_like','social','当前点赞事实，取消物理移除关系',[
 f('user_id','BIGINT UNSIGNED','点赞用户','t_user.id'),f('note_id','BIGINT UNSIGNED','笔记','t_note.id')],unique=[('user_id','note_id')],indexes=[('note_id','id')])
table('t_follow','social','当前关注事实',[f('follower_id','BIGINT UNSIGNED','关注人','t_user.id'),f('followee_id','BIGINT UNSIGNED','被关注人','t_user.id')],unique=[('follower_id','followee_id')],indexes=[('followee_id','id')])
table('t_folder','collection','私有收藏夹',[f('owner_id','BIGINT UNSIGNED','所有人','t_user.id'),f('name','VARCHAR(60)','1–30代码点；同人可重名'),f('version','INT UNSIGNED','乐观锁',default='0')],indexes=[('owner_id','id')])
table('t_favorite','collection','收藏夹条目；人数统计须跨夹去重',[
 f('folder_id','BIGINT UNSIGNED','收藏夹','t_folder.id'),f('user_id','BIGINT UNSIGNED','冗余所属用户；事务内核对folder.owner_id','t_user.id'),f('note_id','BIGINT UNSIGNED','笔记','t_note.id')],unique=[('folder_id','note_id')],indexes=[('user_id','note_id'),('note_id','user_id')])
table('t_practice','collection','私人实践过程与结果；公开反馈依附此记录',[
 f('owner_id','BIGINT UNSIGNED','所有人','t_user.id'),f('note_id','BIGINT UNSIGNED','源笔记','t_note.id'),f('status','VARCHAR(16)','PLANNED/DOING/DONE/ABANDONED',default="'PLANNED'"),
 f('version','INT UNSIGNED','并发控制版本',default='0'),f('result_text','TEXT','本人实践结果，不复制源笔记正文',nullable=True),f('helpfulness','VARCHAR(16)','HELPFUL/NEUTRAL/NOT_APPLICABLE',nullable=True),
 f('public_status','VARCHAR(16)','PRIVATE/PENDING/PUBLISHED/REJECTED/OFFLINE',default="'PRIVATE'"),f('feedback_revision','INT UNSIGNED','结果变化或每次新申请公开时递增',default='0'),
 f('source_revision','INT UNSIGNED','公开审核关联的原笔记revision；不同则不公开',nullable=True),
 f('started_at','DATETIME(3)','开始时间',nullable=True),f('finished_at','DATETIME(3)','完成或放弃时间',nullable=True)],indexes=[('owner_id','id'),('note_id','public_status')])
table('t_practice_media','collection','实践结果图片',[f('practice_id','BIGINT UNSIGNED','实践','t_practice.id'),f('media_id','BIGINT UNSIGNED','用户新上传图片','t_media.id'),f('position','TINYINT UNSIGNED','1–9')],unique=[('practice_id','media_id'),('practice_id','position')],indexes=[('media_id',)])
table('t_practice_history','collection','实践状态时间线',[f('practice_id','BIGINT UNSIGNED','实践','t_practice.id'),f('from_status','VARCHAR(16)','前状态',nullable=True),f('to_status','VARCHAR(16)','后状态'),f('actor_id','BIGINT UNSIGNED','本人操作者','t_user.id')],indexes=[('practice_id','id')])
table('t_review','governance','笔记或公开实践反馈的版本化审核任务',[
 f('target_type','VARCHAR(16)','NOTE/PRACTICE'),f('target_id','BIGINT UNSIGNED','多态目标，应用校验存在；不设虚假FK'),
 f('target_revision','INT UNSIGNED','审核内容版本'),f('status','VARCHAR(16)','PENDING/APPROVED/REJECTED/CANCELLED',default="'PENDING'"),f('applicant_id','BIGINT UNSIGNED','申请人','t_user.id'),
 f('reviewer_id','BIGINT UNSIGNED','决策审核员','t_user.id',nullable=True),f('reason','VARCHAR(500)','驳回必填',nullable=True),f('decided_at','DATETIME(3)','决策时间',nullable=True),f('version','INT UNSIGNED','抢占决定的版本',default='0')],unique=[('target_type','target_id','target_revision')],indexes=[('status','id')])
table('t_report','governance','举报工单；非结案去重用可空开放键',[
 f('reporter_id','BIGINT UNSIGNED','举报人','t_user.id'),f('target_type','VARCHAR(16)','NOTE/COMMENT/PRACTICE'),f('target_id','BIGINT UNSIGNED','多态业务目标'),
 f('reason','VARCHAR(500)','举报理由'),f('status','VARCHAR(16)','PENDING/PROCESSING/UPHELD/REJECTED',default="'PENDING'"),f('open_key','CHAR(64)','SHA256(举报人/类型/目标)，结案置NULL；唯一约束允许多条NULL',nullable=True),
 f('handler_id','BIGINT UNSIGNED','经办人','t_user.id',nullable=True),f('result_reason','VARCHAR(500)','处理结论',nullable=True),f('closed_at','DATETIME(3)','结案时间',nullable=True),f('version','INT UNSIGNED','并发版本',default='0')],unique=[('open_key',)],indexes=[('status','id'),('reporter_id','id')])
table('t_notification','notification','幂等站内收件箱；不保存敏感正文快照',[
 f('recipient_id','BIGINT UNSIGNED','收件人','t_user.id'),f('event_id','CHAR(36)','稳定业务事件UUID'),f('type','VARCHAR(32)','LIKE/COMMENT/FOLLOW/REVIEW/REPORT/BAN'),
 f('actor_id','BIGINT UNSIGNED','行为人','t_user.id',nullable=True),f('target_type','VARCHAR(16)','NOTE/COMMENT/PRACTICE/USER/REPORT'),f('target_id','BIGINT UNSIGNED','跳转目标'),
 f('read_at','DATETIME(3)','为空表示未读',nullable=True)],unique=[('recipient_id','event_id')],indexes=[('recipient_id','read_at','id')])
table('t_outbox','platform','业务事务内事件；按消费者定向投递，消费完成才终结',[
 f('event_id','CHAR(36)','业务事件UUID'),f('consumer','VARCHAR(32)','notification/statistics/cache中的一个'),f('routing_key','VARCHAR(100)','事件名加消费者'),
 f('payload','JSON','version=1事件载荷，只含最少业务字段'),f('status','VARCHAR(16)','PENDING/LEASED/SENT/DONE/DEAD',default="'PENDING'"),
 f('attempts','INT UNSIGNED','业务消费失败次数，最多5次处理含首次',default='0'),f('publish_attempts','INT UNSIGNED','投递尝试次数；Broker故障退避重试不因5次限额终止',default='0'),f('available_at','DATETIME(3)','下次可投递时间'),f('lease_until','DATETIME(3)','领取租期',nullable=True),f('lease_token','CHAR(36)','本次领取随机UUID；AMQP头携带，回调CAS避免过期持有者覆盖',nullable=True),
 f('last_error','VARCHAR(500)','脱敏错误原因',nullable=True)],unique=[('event_id','consumer')],indexes=[('status','available_at','id')])
table('t_inbox','platform','消费去重，与业务副作用及outbox终结在同事务',[
 f('consumer','VARCHAR(32)','消费者名'),f('event_id','CHAR(36)','业务事件UUID'),f('processed_at','DATETIME(3)','成功处理时间')],unique=[('consumer','event_id')])
table('t_command','platform','创建/提交/决策类命令幂等响应',[
 f('actor_id','BIGINT UNSIGNED','操作者','t_user.id'),f('scope','VARCHAR(80)','路由和目标范围'),f('request_key','VARCHAR(80)','Idempotency-Key'),f('request_hash','CHAR(64)','规范化请求哈希'),
 f('response_json','JSON','成功业务响应，不含令牌和个人敏感信息'),f('expires_at','DATETIME(3)','至少7日保留')],unique=[('actor_id','scope','request_key')],indexes=[('expires_at',)])
table('t_audit','governance','追加式治理审计，应用账号禁止UPDATE/DELETE',[
 f('actor_id','BIGINT UNSIGNED','操作者','t_user.id'),f('action','VARCHAR(60)','审核、封禁、改权等动作'),f('target_type','VARCHAR(16)','目标类型'),f('target_id','BIGINT UNSIGNED','目标标识'),
 f('before_json','JSON','脱敏前状态',nullable=True),f('after_json','JSON','脱敏后状态'),f('reason','VARCHAR(500)','理由'),f('trace_id','VARCHAR(64)','关联请求')],indexes=[('target_type','target_id','id'),('actor_id','id')])
table('t_daily_activity','statistics','登录账号有效访问的按日去重事实',[
 f('day','DATE','Asia/Shanghai统计日'),f('user_id','BIGINT UNSIGNED','有效访问账号','t_user.id')],unique=[('day','user_id')])
table('t_view_window','statistics','30分钟访问去重事实，短期保留',[
 f('subject_hash','CHAR(64)','HMAC(账号ID或随机匿名Cookie)，不保存原始IP'),f('note_id','BIGINT UNSIGNED','笔记','t_note.id'),f('window_start','DATETIME(3)','UTC半小时边界')],unique=[('subject_hash','note_id','window_start')],indexes=[('created_at',)])
table('t_note_metric','statistics','笔记可重建累计展示指标',[
 f('note_id','BIGINT UNSIGNED','笔记','t_note.id'),f('view_count','BIGINT UNSIGNED','累计有效浏览',default='0'),f('like_count','BIGINT UNSIGNED','有效点赞',default='0'),
 f('favorite_count','BIGINT UNSIGNED','跨夹去重收藏人数',default='0'),f('comment_count','BIGINT UNSIGNED','可见评论数',default='0'),f('calculated_at','DATETIME(3)','最近计算时间')],unique=[('note_id',)])
table('t_daily_metric','statistics','按日汇总：平台、创作者和治理/实践指标',[
 f('day','DATE','Asia/Shanghai统计日'),f('scope','VARCHAR(16)','PLATFORM/AUTHOR/NOTE/GOVERNANCE/PRACTICE'),f('subject_id','BIGINT UNSIGNED','AUTHOR为作者ID，NOTE为笔记ID，其余0；不是多义外键',default='0'),
 f('metric','VARCHAR(40)','指标代码'),f('value','DECIMAL(20,4)','非负计数或时长；比例由分子/分母计算',default='0'),f('calculated_at','DATETIME(3)','水位时间')],unique=[('day','scope','subject_id','metric')])

write(DOC/'attachments/schema-model.json',json.dumps(TABLES,ensure_ascii=False,indent=2))
sql=['-- 设计契约，未执行数据库建表或迁移。MySQL 8.4 / InnoDB / UTC。',
     'SET NAMES utf8mb4;', "SET time_zone = '+00:00';"]
dict_md=['# 数据字典与DDL设计契约','版本1.0；设计完成，尚未执行迁移。所有字段逐项列出；每表共同字段仍完整展开。',
 '## 通用规则','所有表采用InnoDB、utf8mb4_0900_ai_ci；登录名应用统一小写。ID为数据库自增BIGINT，JSON接口一律字符串，避免JavaScript精度丢失。时间DATETIME(3)存UTC，展示和统计日转北京时间。禁止全局套用逻辑删除插件：关系物理取消，笔记使用业务状态。FK默认RESTRICT，不级联删除历史。',
 '多态target_id、subject_id及avatar_media_id不声明伪外键，应用在同事务校验。t_favorite的user_id与folder.owner_id须事务内一致；唯一索引防重复，不能单靠先查后插。',
 '字段默认值“无”表示必须显式传入；NULL表示可空且默认NULL。SQL文件为后续Flyway V1起点，不代表已部署。']
for ti,t in enumerate(TABLES,1):
    lines=[]
    dict_md += [f"## {ti}. {t['name']}（{t['desc']}）",f"所有权：{t['owner']}。",'| 字段 | 类型/长度 | 可空 | 默认值 | 约束/业务含义 |','|---|---|---|---|---|']
    for n,typ,null,default,desc,ref in t['fields']:
        d = 'NULL' if null and default is None else ('无' if default is None else default)
        dict_md.append(f"| {n} | {typ} | {'是' if null else '否'} | {d} | {'PK；' if n=='id' else ''}{'FK→'+ref+'；' if ref else ''}{desc} |")
        line=f'  `{n}` {typ} '+('NULL' if null else 'NOT NULL')
        if default == 'AUTO_INCREMENT':line+=' AUTO_INCREMENT'
        elif default is not None:line+=' DEFAULT '+default
        elif null:line+=' DEFAULT NULL'
        lines.append(line)
    lines.append('  PRIMARY KEY (`id`)')
    for j,cols in enumerate(t['unique'],1):lines.append(f'  UNIQUE KEY `uk_{ti}_{j}` ('+', '.join('`'+c+'`' for c in cols)+')')
    for j,cols in enumerate(t['indexes'],1):lines.append(f'  KEY `idx_{ti}_{j}` ('+', '.join('`'+c+'`' for c in cols)+')')
    # FK延迟到所有表创建后，允许自引用。
    sql.append(f"CREATE TABLE `{t['name']}` (\n"+',\n'.join(lines)+'\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;')
    dict_md.append('唯一键：'+('；'.join(', '.join(x) for x in t['unique']) or '仅主键')+'。索引：'+('；'.join(', '.join(x) for x in t['indexes']) or '主键/唯一键')+'。')
for ti,t in enumerate(TABLES,1):
    for j,(n,typ,null,default,desc,ref) in enumerate(t['fields']):
        if ref:
            rt,rc=ref.split('.')
            sql.append(f"ALTER TABLE `{t['name']}` ADD CONSTRAINT `fk_{ti}_{j}` FOREIGN KEY (`{n}`) REFERENCES `{rt}` (`{rc}`) ON DELETE RESTRICT;")
sql += ["ALTER TABLE t_follow ADD CONSTRAINT ck_follow_self CHECK (follower_id <> followee_id);",
        "ALTER TABLE t_note_media ADD CONSTRAINT ck_note_position CHECK (position BETWEEN 1 AND 9);",
        "ALTER TABLE t_practice_media ADD CONSTRAINT ck_practice_position CHECK (position BETWEEN 1 AND 9);"]
write(DOC/'attachments/schema.sql','\n\n'.join(sql))
write(DOC/'数据字典.md','\n\n'.join(dict_md))

# API模型：每个操作含明确schema，所有响应统一封装。
S={}
def obj(name,props,required=None):
    S[name]={'type':'object','additionalProperties':False,'properties':props,'required':list(props) if required is None else required}
def string(desc='',**kw):return dict(type='string',description=desc,**kw)
def integer(desc='',**kw):return dict(type='integer',description=desc,**kw)
def enum(*v):return {'type':'string','enum':list(v)}
def arr(ref,**kw):return {'type':'array','items':{'$ref':'#/components/schemas/'+ref},**kw}
def ref(n):return {'$ref':'#/components/schemas/'+n}
ID=string('数据库ID，十进制字符串',pattern='^[1-9][0-9]*$')
TIME=string('UTC时间',format='date-time')
obj('Error',{'code':string(),'message':string(),'traceId':string(),'data':{'nullable':True}})
obj('Empty',{},[])
obj('IdResult',{'id':ID})
obj('VersionResult',{'id':ID,'version':integer(),'status':string()})
obj('LoginRequest',{'username':string(minLength=4,maxLength=32,pattern='^[a-z0-9_]+$'),'password':string(minLength=8,maxLength=72)})
obj('RegisterRequest',{'username':string(minLength=4,maxLength=32,pattern='^[a-z0-9_]+$'),'password':string(minLength=8,maxLength=72),'nickname':string(minLength=1,maxLength=40)})
obj('CsrfResult',{'token':string('后续写请求放X-CSRF-TOKEN；登录后重新获取')})
obj('User',{'id':ID,'nickname':string(),'bio':string(),'avatarMediaId':dict(ID,nullable=True),'roles':{'type':'array','items':enum('USER','REVIEWER','ADMIN')},'status':enum('ACTIVE','BANNED')})
obj('ProfileRequest',{'nickname':string(minLength=1,maxLength=40),'bio':string(maxLength=300),'avatarMediaId':dict(ID,nullable=True)},['nickname','bio'])
obj('AccountStatusRequest',{'status':enum('ACTIVE','BANNED'),'reason':string(minLength=1,maxLength=500)})
obj('RolesRequest',{'roles':{'type':'array','items':enum('USER','REVIEWER','ADMIN'),'minItems':1,'uniqueItems':True},'reason':string(minLength=1,maxLength=500)})
obj('Topic',{'id':ID,'name':string(),'category':string(),'status':enum('ACTIVE','DISABLED')})
obj('TopicRequest',{'name':string(minLength=1,maxLength=40),'category':string(minLength=1,maxLength=40),'status':enum('ACTIVE','DISABLED')})
obj('NoteRequest',{'title':string(maxLength=40),'body':string(maxLength=2000),'mediaIds':{'type':'array','items':ID,'maxItems':9,'uniqueItems':True},'topicIds':{'type':'array','items':ID,'maxItems':5,'uniqueItems':True},'version':integer(minimum=0)},['title','body','mediaIds','topicIds'])
obj('NoteUpdateRequest',S['NoteRequest']['properties'],['title','body','mediaIds','topicIds','version'])
obj('VersionRequest',{'version':integer(minimum=0)})
obj('Note',{'id':ID,'authorId':ID,'title':string(),'body':string(),'status':enum('DRAFT','PENDING','PUBLISHED','REJECTED','OFFLINE','DELETED'),'version':integer(),'revision':integer(),'mediaIds':{'type':'array','items':ID},'topicIds':{'type':'array','items':ID},'publishedAt':dict(TIME,nullable=True),'reason':dict(string(),nullable=True)})
obj('Card',{'id':ID,'authorId':ID,'title':string(),'coverMediaId':ID,'reason':enum('INTEREST','FOLLOWING','POPULAR','LATEST'),'score':{'type':'number'},'likeCount':integer(),'favoriteCount':integer()})
obj('PreferenceRequest',{'interests':{'type':'array','items':ID,'maxItems':30,'uniqueItems':True},'reducedTopics':{'type':'array','items':ID,'maxItems':30,'uniqueItems':True}})
obj('Preference',{'interests':{'type':'array','items':ID},'reducedTopics':{'type':'array','items':ID}})
obj('CommentRequest',{'body':string(minLength=1,maxLength=500),'replyToId':dict(ID,nullable=True)},['body'])
obj('Comment',{'id':ID,'authorId':ID,'noteId':ID,'rootId':dict(ID,nullable=True),'replyToId':dict(ID,nullable=True),'body':string(),'status':enum('VISIBLE','HIDDEN','DELETED'),'createdAt':TIME})
obj('FolderRequest',{'name':string(minLength=1,maxLength=30),'version':integer(minimum=0)},['name'])
obj('FolderUpdateRequest',S['FolderRequest']['properties'],['name','version'])
obj('Folder',{'id':ID,'name':string(),'version':integer()})
obj('Favorite',{'id':ID,'noteId':ID,'available':{'type':'boolean'},'title':dict(string(),nullable=True)})
obj('PracticeCreate',{'noteId':ID})
obj('PracticeTransition',{'version':integer(minimum=0),'status':enum('DOING','DONE','ABANDONED')})
obj('PracticeResultRequest',{'version':integer(minimum=0),'resultText':string(minLength=1,maxLength=2000),'helpfulness':enum('HELPFUL','NEUTRAL','NOT_APPLICABLE'),'mediaIds':{'type':'array','items':ID,'maxItems':9,'uniqueItems':True}})
obj('Practice',{'id':ID,'noteId':ID,'status':enum('PLANNED','DOING','DONE','ABANDONED'),'version':integer(),'publicStatus':enum('PRIVATE','PENDING','PUBLISHED','REJECTED','OFFLINE'),'resultText':dict(string(),nullable=True),'helpfulness':dict(string(),nullable=True),'sourceAvailable':{'type':'boolean'},'mediaIds':{'type':'array','items':ID}})
obj('History',{'fromStatus':dict(string(),nullable=True),'toStatus':string(),'createdAt':TIME})
obj('PublicFeedback',{'id':ID,'noteId':ID,'authorId':ID,'resultText':string(),'helpfulness':string(),'mediaIds':{'type':'array','items':ID}})
obj('Review',{'id':ID,'targetType':enum('NOTE','PRACTICE'),'targetId':ID,'targetRevision':integer(),'version':integer(),'status':enum('PENDING','APPROVED','REJECTED','CANCELLED'),'applicantId':ID})
obj('ReviewDecision',{'version':integer(minimum=0),'decision':enum('APPROVE','REJECT'),'reason':string(maxLength=500)},['version','decision'])
obj('ReportRequest',{'targetType':enum('NOTE','COMMENT','PRACTICE'),'targetId':ID,'reason':string(minLength=1,maxLength=500)})
obj('Report',{'id':ID,'targetType':string(),'targetId':ID,'status':enum('PENDING','PROCESSING','UPHELD','REJECTED'),'version':integer(),'resultReason':dict(string(),nullable=True)})
obj('ReportDecision',{'version':integer(minimum=0),'status':enum('PROCESSING','UPHELD','REJECTED'),'reason':string(minLength=1,maxLength=500)})
obj('ReasonRequest',{'reason':string(minLength=1,maxLength=500),'version':integer(minimum=0)})
obj('Notification',{'id':ID,'type':string(),'actorId':dict(ID,nullable=True),'targetType':string(),'targetId':ID,'targetAvailable':{'type':'boolean'},'readAt':dict(TIME,nullable=True),'createdAt':TIME})
obj('Unread',{'count':integer(minimum=0)})
obj('Audit',{'id':ID,'actorId':ID,'action':string(),'targetType':string(),'targetId':ID,'reason':string(),'traceId':string(),'createdAt':TIME})
obj('MetricPoint',{'day':string(format='date'),'metric':string(),'value':{'type':'number'}})
obj('Dashboard',{'points':arr('MetricPoint'),'calculatedAt':TIME,'timezone':enum('Asia/Shanghai')})
obj('RankItem',{'subjectId':ID,'label':string(),'metric':string(),'value':{'type':'number'}})
obj('MediaUpload',{'file':string('JPG/PNG/WebP，实际文件≤10MB',format='binary'),'purpose':enum('NOTE','PRACTICE','AVATAR')})
obj('Media',{'id':ID,'mime':string(),'width':integer(),'height':integer(),'sizeBytes':integer()})
obj('MarkAllRead',{'throughId':ID})
for name in ['User','Topic','Note','Card','Comment','Folder','Favorite','Practice','History','PublicFeedback','Review','Report','Notification','Audit','RankItem']:
    obj(name+'Page',{'items':arr(name),'nextCursor':dict(string(),nullable=True),'hasMore':{'type':'boolean'}})

OPS=[]
def op(method,path,summary,tag,out='Empty',body=None,roles='USER',fr='',page=False,idem=False):
    OPS.append(dict(method=method,path='/api/v1'+path,summary=summary,tag=tag,out=out,body=body,roles=roles,fr=fr,page=page,idem=idem))
op('get','/auth/csrf','获取CSRF令牌','identity','CsrfResult',roles='PUBLIC',fr='FR-01')
op('post','/auth/register','注册账号','identity','IdResult','RegisterRequest','PUBLIC','FR-01')
op('post','/auth/login','登录并设置HttpOnly Cookie','identity','User','LoginRequest','PUBLIC','FR-01')
op('post','/auth/logout','撤销当前会话','identity',fr='FR-01')
op('get','/me','本人资料和角色','identity','User',fr='FR-02')
op('put','/me/profile','更新本人资料','identity','User','ProfileRequest',fr='FR-02')
op('get','/users/{userId}','公开个人资料（不返回角色/状态）','identity','User',roles='PUBLIC',fr='FR-02')
op('put','/admin/users/{userId}/status','封禁或解封，递增auth_version','identity','Empty','AccountStatusRequest','ADMIN','FR-03',idem=True)
op('put','/admin/users/{userId}/roles','设置角色，保留至少一名可用管理员','identity','Empty','RolesRequest','ADMIN','FR-03',idem=True)
op('post','/media','上传私有图片并校验解码','content','Media','MediaUpload',fr='FR-05',idem=False)
op('get','/media/{mediaId}/content','经实时授权读取媒体二进制','content',roles='PUBLIC',fr='FR-05')
op('post','/notes','创建本人草稿','content','VersionResult','NoteRequest',fr='FR-04',idem=True)
op('get','/me/notes','本人所有状态的笔记','content','NotePage',fr='FR-04',page=True)
op('get','/me/notes/{noteId}','本人笔记含驳回原因','content','Note',fr='FR-07')
op('put','/notes/{noteId}','编辑草稿/驳回/下架/已发布笔记','content','VersionResult','NoteUpdateRequest',fr='FR-04,FR-07')
op('post','/notes/{noteId}/submit','提交审核，校验必填与版本','content','VersionResult','VersionRequest',fr='FR-06',idem=True)
op('post','/notes/{noteId}/withdraw','撤回待审核笔记并取消任务','content','VersionResult','VersionRequest',fr='FR-07',idem=True)
op('delete','/notes/{noteId}','删除本人笔记（状态终结）','content',fr='FR-07')
op('get','/notes/{noteId}','公开详情；不可见统一404','content','Note',roles='PUBLIC',fr='FR-08')
op('get','/feed','发现/关注/推荐流：mode参数','discovery','CardPage',roles='PUBLIC',fr='FR-08,FR-10',page=True)
op('get','/search','关键词/话题/作者检索','discovery','CardPage',roles='PUBLIC',fr='FR-09',page=True)
op('get','/me/preferences','读取兴趣设置','discovery','Preference',fr='FR-10,FR-11')
op('put','/me/preferences','替换兴趣和降权话题，交集禁止','discovery','Preference','PreferenceRequest',fr='FR-11')
op('put','/me/hidden-notes/{noteId}','标记不感兴趣','discovery',fr='FR-11')
op('delete','/me/hidden-notes/{noteId}','取消隐藏','discovery',fr='FR-11')
op('delete','/me/preferences','重置兴趣/降权/隐藏并失效推荐快照','discovery',fr='FR-11')
op('put','/notes/{noteId}/like','设为已点赞，重复不发新事件','social',fr='FR-12')
op('delete','/notes/{noteId}/like','取消点赞；已取消返回成功','social',fr='FR-12')
op('post','/notes/{noteId}/comments','评论或回复，两级展示','social','IdResult','CommentRequest',fr='FR-13',idem=True)
op('get','/notes/{noteId}/comments','可见评论分页','social','CommentPage',roles='PUBLIC',fr='FR-13',page=True)
op('get','/comments/{commentId}/replies','根评论回复分页','social','CommentPage',roles='PUBLIC',fr='FR-13',page=True)
op('delete','/comments/{commentId}','删除本人评论并留占位','social',fr='FR-13')
op('put','/users/{userId}/follow','关注账号，禁止自己','social',fr='FR-14')
op('delete','/users/{userId}/follow','取消关注','social',fr='FR-14')
op('get','/users/{userId}/following','公开关注列表','social','UserPage',roles='PUBLIC',fr='FR-14',page=True)
op('get','/users/{userId}/followers','公开粉丝列表','social','UserPage',roles='PUBLIC',fr='FR-14',page=True)
op('post','/folders','新建私有收藏夹','collection','IdResult','FolderRequest',fr='FR-15',idem=True)
op('get','/folders','本人收藏夹分页','collection','FolderPage',fr='FR-15',page=True)
op('put','/folders/{folderId}','重命名本人收藏夹','collection','Folder','FolderUpdateRequest',fr='FR-15')
op('delete','/folders/{folderId}','删除夹和条目，不删除源笔记/实践','collection',fr='FR-15')
op('put','/folders/{folderId}/notes/{noteId}','加入收藏，跨夹人数去重','collection',fr='FR-16')
op('delete','/folders/{folderId}/notes/{noteId}','移出收藏','collection',fr='FR-16')
op('get','/folders/{folderId}/notes','夹内条目；失效仅返回占位','collection','FavoritePage',fr='FR-16',page=True)
op('post','/practices','为已收藏且可见笔记创建实践','collection','VersionResult','PracticeCreate',fr='FR-17',idem=True)
op('get','/practices','本人实践分页','collection','PracticePage',fr='FR-17',page=True)
op('get','/practices/{practiceId}','本人实践详情','collection','Practice',fr='FR-17,FR-18')
op('post','/practices/{practiceId}/transition','本人推进实践状态','collection','VersionResult','PracticeTransition',fr='FR-17',idem=True)
op('get','/practices/{practiceId}/history','本人实践状态时间线','collection','HistoryPage',fr='FR-17',page=True)
op('put','/practices/{practiceId}/result','DONE后填写结果，修改撤销原公开','collection','VersionResult','PracticeResultRequest',fr='FR-18')
op('post','/practices/{practiceId}/publish-request','申请公开结果并创建版本审核','collection','VersionResult','VersionRequest',fr='FR-18',idem=True)
op('post','/practices/{practiceId}/unpublish','撤回公开意愿并取消审核','collection','VersionResult','VersionRequest',fr='FR-18',idem=True)
op('get','/notes/{noteId}/feedback','原笔记当前版本可见且审核通过的反馈','collection','PublicFeedbackPage',roles='PUBLIC',fr='FR-18',page=True)
op('get','/notifications','本人分类通知，不存储目标正文','notification','NotificationPage',fr='FR-19,FR-20',page=True)
op('get','/notifications/unread','本人未读数','notification','Unread',fr='FR-21')
op('put','/notifications/{notificationId}/read','单条已读','notification',fr='FR-21')
op('post','/notifications/read-all','标记throughId以前本人通知已读','notification','Empty','MarkAllRead',fr='FR-21')
op('get','/reviews','审核任务分页','governance','ReviewPage',roles='REVIEWER',fr='FR-22',page=True)
op('get','/reviews/{reviewId}','授权审核预览，服务端验证任务和版本','governance','Review',roles='REVIEWER',fr='FR-22')
op('post','/reviews/{reviewId}/decision','审核决策、禁止自审','governance','VersionResult','ReviewDecision','REVIEWER','FR-22',idem=True)
op('post','/reports','举报公开可见对象','governance','IdResult','ReportRequest',fr='FR-23',idem=True)
op('get','/me/reports','本人举报进度，不显示内部敏感信息','governance','ReportPage',fr='FR-23',page=True)
op('get','/admin/reports','治理举报列表','governance','ReportPage',roles='REVIEWER',fr='FR-23',page=True)
op('post','/admin/reports/{reportId}/decision','受理/结案并同步治理目标','governance','VersionResult','ReportDecision','REVIEWER','FR-23',idem=True)
op('post','/admin/notes/{noteId}/offline','下架笔记并失效公开反馈','governance','Empty','ReasonRequest','REVIEWER','FR-22',idem=True)
op('post','/admin/comments/{commentId}/hide','治理隐藏评论','governance','Empty','ReasonRequest','REVIEWER','FR-13,FR-23',idem=True)
op('post','/admin/practices/{practiceId}/offline','下架公开反馈','governance','Empty','ReasonRequest','REVIEWER','FR-18,FR-23',idem=True)
op('get','/topics','公开有效话题分页','governance','TopicPage',roles='PUBLIC',fr='FR-24',page=True)
op('post','/admin/topics','新增话题','governance','IdResult','TopicRequest','ADMIN','FR-24',idem=True)
op('put','/admin/topics/{topicId}','编辑/停用话题','governance','Topic','TopicRequest','ADMIN','FR-24')
op('get','/admin/audits','脱敏审计查询','governance','AuditPage',roles='REVIEWER|ADMIN',fr='FR-24',page=True)
op('get','/statistics/me','本人7/30日趋势','statistics','Dashboard',fr='FR-25')
op('get','/statistics/me/ranking','本人内容排行','statistics','RankItemPage',fr='FR-25',page=True)
op('get','/admin/statistics/platform','平台趋势','statistics','Dashboard',roles='ADMIN',fr='FR-26')
op('get','/admin/statistics/ranking','热门内容/话题','statistics','RankItemPage',roles='ADMIN',fr='FR-26',page=True)
op('get','/admin/statistics/governance','审核/举报/实践指标','statistics','Dashboard',roles='REVIEWER|ADMIN',fr='FR-27')
paths={}
for i,o in enumerate(OPS,1):
    params=[]
    for n in re.findall(r'{(\w+)}',o['path']):params.append({'name':n,'in':'path','required':True,'schema':ID})
    if o['page']:
        params += [{'name':'cursor','in':'query','schema':string('HMAC签名游标，绑定账号/查询条件')},{'name':'size','in':'query','schema':integer(minimum=1,maximum=50,default=20)}]
    if o['idem']:params.append({'name':'Idempotency-Key','in':'header','required':True,'schema':string(minLength=16,maxLength=80)})
    if o['method'] not in ['get']:
        params.append({'name':'X-CSRF-TOKEN','in':'header','required':True,'schema':string()})
    if o['method']=='delete' and o['path'] in ['/api/v1/notes/{noteId}']:
        params.append({'name':'version','in':'query','required':True,'schema':integer(minimum=0)})
    if o['path'].endswith('/feed'):
        params += [{'name':'mode','in':'query','schema':enum('discover','following','recommend')},{'name':'category','in':'query','schema':string()}]
    if o['path'].endswith('/search'):
        params += [{'name':'q','in':'query','schema':string(maxLength=100)},{'name':'topicId','in':'query','schema':ID},{'name':'authorId','in':'query','schema':ID},{'name':'sort','in':'query','schema':enum('latest','hot','relevance')}]
    if 'statistics' in o['path']:
        params += [{'name':'days','in':'query','schema':integer(enum=[7,30],default=7)}]
    if o['path'].endswith('/notifications'):
        params.append({'name':'type','in':'query','schema':enum('LIKE','COMMENT','FOLLOW','REVIEW','REPORT','BAN')})
    if o['path'].endswith('/reviews'):
        params.append({'name':'status','in':'query','schema':enum('PENDING','APPROVED','REJECTED','CANCELLED')})
    response={'description':'成功；创建201，其余200。字节读取除外。','content':{'application/json':{'schema':{'type':'object','required':['code','message','data','traceId'],'properties':{'code':{'type':'string','enum':['OK']},'message':string(),'data':ref(o['out']),'traceId':string()}}}}}
    responses={'201' if o['method']=='post' and o['out']=='IdResult' else '200':response}
    if o['body']=='RegisterRequest': responses={'201':response}
    if o['path'].endswith('/content'):
        responses={'200':{'description':'授权图片；Cache-Control: private, no-store；每次请求实时验证权限和状态','content':{'image/jpeg':{'schema':{'type':'string','format':'binary'}},'image/png':{'schema':{'type':'string','format':'binary'}},'image/webp':{'schema':{'type':'string','format':'binary'}}}}}
    for status in ['400','401','403','404','409','413','415','429','503']:
        responses[status]={'description':{'400':'参数错误','401':'未认证或会话撤销','403':'权限/CSRF/自审禁止','404':'不存在或不可见','409':'状态/版本冲突或幂等键异参','413':'文件/请求过大','415':'文件类型不支持','429':'限流','503':'依赖故障/积压保护'}[status],'content':{'application/json':{'schema':ref('Error')}}}
    d={'operationId':f"op{i:03d}",'summary':o['summary'],'tags':[o['tag']],'description':f"需求：{o['fr']}。角色：{o['roles']}。服务端强制对象归属和状态校验；ADMIN不自动拥有REVIEWER权限。",'x-requirements':o['fr'].split(','),'x-roles':o['roles'].split('|'),'parameters':params,'responses':responses,'security':[] if o['roles']=='PUBLIC' else [{'sessionCookie':[]}]}
    if o['body']:
        mime='multipart/form-data' if o['body']=='MediaUpload' else 'application/json'
        d['requestBody']={'required':True,'content':{mime:{'schema':ref(o['body'])}}}
    if o['path'].endswith('/login'):d['responses']['200']['headers']={'Set-Cookie':{'description':'SG_SESSION=随机令牌; HttpOnly; SameSite=Lax; Path=/; 云端加Secure','schema':string()}}
    paths.setdefault(o['path'],{})[o['method']]=d
api={'openapi':'3.0.3','info':{'title':'拾光笔记 API 设计契约','version':'1.0.0','description':'设计完成，未实现。时间UTC ISO8601，ID字符串；Cookie会话+同步CSRF；后续必须从实现生成并比较契约。'},'servers':[{'url':'http://localhost:8080','description':'计划中的本地反向代理地址，尚未部署'}],'paths':paths,'components':{'securitySchemes':{'sessionCookie':{'type':'apiKey','in':'cookie','name':'SG_SESSION'}},'schemas':S}}
write(DOC/'attachments/openapi.yaml',yaml.safe_dump(api,allow_unicode=True,sort_keys=False,width=110))
api_md=['# 接口设计与约定','本文件与attachments/openapi.yaml共同构成设计契约；尚未实现。',
 '## 通用语义','REST前缀/api/v1；ID十进制字符串，UTC ISO8601；成功Result<T>={code:"OK",message,data,traceId}。错误保留真实HTTP状态，禁止全部返回200。对象不可见统一404。列表默认20、最大50，签名游标绑定查询和身份；禁止未限制的全量查询。',
 '登录使用数据库权威随机会话令牌，HttpOnly、SameSite=Lax；生产HTTPS加Secure。写请求必须携带X-CSRF-TOKEN，注册/登录也需先获取匿名CSRF；认证成功后轮换会话与CSRF。浏览器同源通过Nginx访问，不开放任意CORS凭证域。密码另校验UTF-8字节≤72，避免BCrypt截断。',
 '创建/审核/发布/状态转换使用Idempotency-Key，至少16字符；相同账号+scope+key同参返回第一次成功结果，异参409；成功响应幂等记录与业务同事务，失败回滚。关系设置采用PUT/DELETE，不用toggle。',
 '## 接口清单','| 编号 | 方法与路径 | 业务 | 角色 | 输入 | 输出data | 需求 |','|---|---|---|---|---|---|---|']
for i,o in enumerate(OPS,1):api_md.append(f"| IF-{i:03d} | {o['method'].upper()} {o['path']} | {o['summary']} | {o['roles']} | {o['body'] or '路径/查询参数'} | {'图片二进制' if o['path'].endswith('/content') else o['out']} | {o['fr']} |")
api_md += ['## 关键前置/后置条件与异常',
 '| 命令 | 前置条件 | 后置条件 | 异常 |','|---|---|---|---|',
 '| 提交审核 | 本人、DRAFT、version一致、标题正文/图片/话题完整 | note=PENDING；同版本review唯一；outbox与命令结果一并提交 | 403越权，409版本或状态冲突，400不完整 |',
 '| 审核通过 | REVIEWER、非申请人、review=PENDING、revision与目标一致 | 目标及review同时改变，审计与通知事件同事务 | 403自审，409过期任务；全部回滚 |',
 '| 点赞PUT | 账号有效、目标公开 | 唯一关系存在；仅从无到有时发事件 | 不可见404；重复成功但不增计数 |',
 '| 创建实践 | 本人仍收藏、原笔记公开 | PLANNED记录和初始history | 404源不可见，409尚未收藏 |',
 '| 公开反馈 | 本人、DONE、有结果、源笔记公开 | feedback=PENDING并绑定源revision；待审不公开 | 409非法状态或源已改变 |',
 '| 读取媒体 | 上传者或当前公开绑定对象，或拥有有效任务的审核员 | 返回字节，不输出磁盘路径；no-store | 无权限统一404；错误格式415 |',
 '| 封禁/改权 | ADMIN、理由、不能移除最后可用管理员 | auth_version递增；既有会话下一次失效；审计/事件提交 | 409最后管理员，403权限 |',
 '## 查询与字段裁剪',
 '公开User接口只返回id/nickname/bio/avatarMediaId；统一User schema在公开视图中需以PublicUser响应替换，不能直接序列化内部对象。公开Note不含version/rejectReason等内部字段；对应独立PublicNote schema。审核详情必须返回任务绑定的内容预览，不能依靠公开详情读取待审内容。',
 '通知不保留正文快照，查询时按目标当前可见性生成链接；目标失效只显示“内容不可用”。举报人身份不得出现在被举报人响应。评论根被删除后只留占位，回复依然分页；不得恢复根正文。',
 '上传在文件实际解码后重编码并清除EXIF；限制1–9图、单图10MB、像素上限2500万。此接口文件字段大小由反向代理和后端共同校验，OpenAPI无法单靠schema表达字节上限。',
 '## 请求示例',
 '提交笔记：POST /api/v1/notes/123/submit，Cookie: SG_SESSION=…，X-CSRF-TOKEN: …，Idempotency-Key: 36位随机UUID，JSON {"version":2}；成功data为{"id":"123","version":3,"status":"PENDING"}。',
 '并发审核：两请求相同reviewId/version=0；第一次成功，第二次409 VERSION_CONFLICT；只有第一次产生审核记录与通知事件。',
 '## 后续实现要求',
 '所有接口以契约schema为边界，公开与本人/审核视图分别映射；自动校验响应无额外字段。OpenAPI覆盖本文件全部操作；部署后通过契约测试与实现比对，覆盖率不以设计文件数量代替。']
# 公共视图独立schema，避免敏感字段通过共用DTO外泄。
obj('PublicUser',{'id':ID,'nickname':string(),'bio':string(),'avatarMediaId':dict(ID,nullable=True)})
obj('PublicNote',{'id':ID,'authorId':ID,'title':string(),'body':string(),'mediaIds':{'type':'array','items':ID},'topicIds':{'type':'array','items':ID},'publishedAt':TIME})
obj('ReviewDetail',{'review':ref('Review'),'note':dict(ref('Note'),nullable=True),'practice':dict(ref('Practice'),nullable=True)})
for o in OPS:
    if o['path']=='/api/v1/users/{userId}' and o['method']=='get':o['out']='PublicUser'
    if o['path']=='/api/v1/notes/{noteId}' and o['method']=='get':o['out']='PublicNote'
    if o['path']=='/api/v1/reviews/{reviewId}' and o['method']=='get':o['out']='ReviewDetail'
    if o['out'] in ['PublicUser','PublicNote','ReviewDetail']:
        paths[o['path']][o['method']]['responses']['200']['content']['application/json']['schema']['properties']['data']=ref(o['out'])
S['UserPage']['properties']['items']=arr('PublicUser')
# OpenAPI3.0中$ref同级nullable会被忽略；用allOf包裹。
def normalize_nullable(value):
    if isinstance(value,dict):
        if '$ref' in value and value.get('nullable'):
            return {'allOf':[{'$ref':value['$ref']}],'nullable':True}
        return {k:normalize_nullable(v) for k,v in value.items()}
    if isinstance(value,list):return [normalize_nullable(v) for v in value]
    return value
api=normalize_nullable(api)
write(DOC/'attachments/openapi.yaml',yaml.safe_dump(api,allow_unicode=True,sort_keys=False,width=110))
api_md=[line.replace('统一User schema在公开视图中需以PublicUser响应替换','契约已使用PublicUser响应').replace('对应独立PublicNote schema','契约已使用独立PublicNote schema') for line in api_md]
api_md=[(line.replace('| User |','| PublicUser |') if 'GET /api/v1/users/{userId} |' in line else line.replace('| Note |','| PublicNote |') if 'GET /api/v1/notes/{noteId} |' in line else line.replace('| Review |','| ReviewDetail |') if 'GET /api/v1/reviews/{reviewId} |' in line else line).replace('REVIEWER|ADMIN','REVIEWER/ADMIN') for line in api_md]
write(DOC/'接口设计说明.md','\n\n'.join(api_md))
write(DOC/'attachments/api-catalog.json',json.dumps(OPS,ensure_ascii=False,indent=2))
print(json.dumps({'tables':len(TABLES),'operations':len(OPS),'schemas':len(S)},ensure_ascii=False))
