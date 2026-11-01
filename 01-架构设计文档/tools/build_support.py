from pathlib import Path
import json, html, re
from build_design import DOC, ROOT, write, TABLES, OPS

FIGS=[]
def graph(id,title,nodes,edges,cols=3):
    # 显式布局和语义同源保存JSON、Mermaid与SVG。
    w=1000; bw=265; bh=86; gapx=60; gapy=76
    positions={n[0]:(35+(i%cols)*(bw+gapx),35+(i//cols)*(bh+gapy)) for i,n in enumerate(nodes)}
    h=70+((len(nodes)+cols-1)//cols)*(bh+gapy)
    svg=[f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}">',
         '<defs><marker id="a" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto"><path d="M0,0 L8,4 L0,8 Z" fill="#50687c"/></marker></defs>',
         '<rect width="100%" height="100%" fill="white"/>']
    mm=['flowchart TB']
    for a,b,label,*rest in edges:
        x,y=positions[a]; xx,yy=positions[b]
        x+=bw/2;y+=bh;xx+=bw/2
        if yy<=y: x=positions[a][0]+bw;y=positions[a][1]+bh/2;xx=positions[b][0];yy=positions[b][1]+bh/2
        dashed=rest and rest[0]=='async'
        svg.append(f'<path d="M{x},{y} L{xx},{yy}" stroke="#50687c" stroke-width="1.5" fill="none" marker-end="url(#a)"'+(' stroke-dasharray="6 4"' if dashed else '')+'/>')
        if label:
            mx=(x+xx)/2;my=(y+yy)/2
            svg += [f'<rect x="{mx-65}" y="{my-12}" width="130" height="22" fill="white"/>',f'<text x="{mx}" y="{my+3}" text-anchor="middle" font-size="13" font-family="Microsoft YaHei">{html.escape(label)}</text>']
        mm.append(f'  {a} '+('-.->' if dashed else '-->')+f'|"{label}"| {b}' if label else f'  {a} --> {b}')
    for nid,label in nodes:
        x,y=positions[nid];parts=label.split('\n')
        svg += [f'<rect x="{x}" y="{y}" width="{bw}" height="{bh}" rx="5" fill="#edf4f7" stroke="#245268" stroke-width="1.5"/>']
        for j,t in enumerate(parts):svg.append(f'<text x="{x+bw/2}" y="{y+28+j*22}" text-anchor="middle" font-size="16" font-family="Microsoft YaHei">{html.escape(t)}</text>')
        mm.append(f'  {nid}["'+label.replace('\n','<br/>')+'"]')
    svg.append('</svg>')
    write(DOC/'images'/f'{id}.svg','\n'.join(svg));write(DOC/'images'/f'{id}.mmd','\n'.join(mm))
    FIGS.append(dict(id=id,title=title,type='graph',nodes=nodes,edges=edges))

def seq(id,title,actors,messages):
    w=1100;dx=(w-100)/max(1,len(actors)-1);h=130+len(messages)*70
    pos={a[0]:50+i*dx for i,a in enumerate(actors)}
    svg=[f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}">',
        '<defs><marker id="a" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto"><path d="M0,0 L8,4 L0,8 Z" fill="#50687c"/></marker></defs>','<rect width="100%" height="100%" fill="white"/>']
    mm=['sequenceDiagram']
    for aid,name in actors:
        x=pos[aid];svg += [f'<rect x="{max(0,x-65)}" y="15" width="130" height="45" fill="#edf4f7" stroke="#245268"/>',f'<text x="{x}" y="44" text-anchor="middle" font-family="Microsoft YaHei" font-size="15">{html.escape(name)}</text>',f'<path d="M{x},60 V{h-20}" stroke="#9aaeb9" stroke-dasharray="5 5"/>'];mm.append(f'  participant {aid} as {name}')
    for i,(a,b,label) in enumerate(messages):
        x=pos[a];xx=pos[b];y=105+i*70
        if x==xx:
            svg.append(f'<path d="M{x},{y} h35 v20 h-35" stroke="#50687c" fill="none" marker-end="url(#a)"/>');tx=x+12;ty=y-10
        else:
            svg.append(f'<path d="M{x},{y} H{xx}" stroke="#50687c" fill="none" marker-end="url(#a)"/>');tx=(x+xx)/2;ty=y-12
        svg.append(f'<text x="{tx}" y="{ty}" text-anchor="middle" font-family="Microsoft YaHei" font-size="14">{html.escape(label)}</text>');mm.append(f'  {a}->>{b}: {label}')
    svg.append('</svg>');write(DOC/'images'/f'{id}.svg','\n'.join(svg));write(DOC/'images'/f'{id}.mmd','\n'.join(mm));FIGS.append(dict(id=id,title=title,type='sequence',actors=actors,messages=messages))

graph('2-1-context','系统上下文',[('U','Person\n普通用户/游客'),('R','Person\n内容审核员'),('A','Person\n系统管理员'),('S','Software System\n拾光笔记'),('B','External Environment\n独立备份介质')],[('U','S','浏览/创作/实践'),('R','S','内容治理'),('A','S','权限/运营'),('S','B','加密备份')])
graph('3-1-modules','功能结构',[('S','拾光笔记\n8个模块'),('I','M1 账号权限\n注册/资料/封禁'),('C','M2 笔记创作\n草稿/图片/发布'),('D','M3 发现推荐\n信息流/搜索/偏好'),('SO','M4 社区互动\n点赞/评论/关注'),('CO','M5 收藏实践\n归档/计划/反馈'),('N','M6 消息通知\n互动/治理/已读'),('G','M7 内容治理\n审核/举报/话题'),('ST','M8 统计分析\n作者/平台/治理')], [('S',n,'') for n in ['I','C','D','SO','CO','N','G','ST']])
graph('3-2-containers','容器',[('B','Browser\nVue3 SPA'),('N','Container\nNginx反向代理'),('A','Container\nJava21 / Boot3.5'),('DB','Container\nMySQL8.4'),('R','Container\nRedis缓存'),('MQ','Container\nRabbitMQ'),('F','Storage\n私有媒体卷'),('P','Container / optional\nPrometheus'),('G','Container / optional\nGrafana')],[('B','N','HTTP'),('N','A','HTTP /api'),('A','DB','JDBC'),('A','R','RESP'),('A','MQ','AMQP','async'),('A','F','文件API'),('P','A','HTTP管理端口'),('G','P','查询')])
graph('3-3-layers','分层',[('W','接入层 web\nController / DTO'),('WF','应用编排 workflows\n事务 / 授权 / 跨模块'),('API','模块 api\n契约 / record'),('APP','模块 application\n实现导出API'),('D','模块 domain\n实体 / 仓储端口'),('INF','模块 infrastructure\nPO / Mapper / Adapter'),('S','shared\n稳定技术契约'),('P','platform\n安全/消息/查询实现')],[('W','WF','编译依赖'),('WF','API','调用契约'),('APP','API','实现'),('APP','D','调用'),('INF','D','实现端口'),('WF','S','技术契约'),('P','S','实现')])
graph('3-4-domain','领域关系',[('U','User\n身份聚合'),('N','Note\n内容聚合根'),('M','Media / Topic\n有序图与话题'),('C','Comment / Like\n互动事实'),('F','Folder / Favorite\n私有归档'),('P','Practice\n实践聚合根'),('R','Review / Report\n治理聚合'),('NT','Notification\n收件箱'),('FO','Follow\n关注关系')],[('U','N','1 : 0..* 创作'),('N','M','1 : 1..* 引用'),('N','C','1 : 0..*'),('U','F','1 : 0..*'),('F','N','0..* : 1'),('U','P','1 : 0..*'),('P','N','0..* : 1'),('R','N','版本化目标'),('R','P','版本化目标'),('U','NT','1 : 0..*'),('U','FO','关注/被关注')])
seq('3-5-submit','跨模块提交',[('U','用户'),('W','提交用例'),('C','ContentApi'),('G','GovernanceApi'),('DB','MySQL')],[('U','W','提交 noteId / version / key'),('W','DB','BEGIN + 校验幂等'),('W','C','校验完整性并改PENDING'),('C','DB','更新note/version'),('W','G','创建同revision审核任务'),('G','DB','插入唯一review'),('W','DB','写outbox/command并COMMIT'),('W','U','返回PENDING')])
seq('3-6-upload','文件环境交互',[('U','用户'),('W','上传用例'),('I','图像处理'),('FS','OS文件系统'),('DB','MySQL')],[('U','W','文件与purpose，≤10MB'),('W','I','解码、限像素、去EXIF重编码'),('I','FS','临时文件→原子rename'),('W','DB','元数据事务'),('DB','W','提交成功或失败'),('W','FS','失败则删除，孤立文件定时清理'),('W','U','mediaId；不返回公开直链')])
seq('3-7-stale','失败审核',[('R','审核员'),('W','审核用例'),('DB','MySQL')],[('R','W','reviewId/version/决策'),('W','DB','BEGIN + 锁目标与任务'),('DB','W','任务revision已过期'),('W','DB','ROLLBACK，不产生成功事件'),('W','R','409 STALE_REVIEW + traceId')])
seq('3-8-broker','独立消息系统调用',[('J','Outbox用例'),('DB','MySQL'),('MQ','RabbitMQ系统')],[('J','DB','短事务领取定向事件'),('DB','J','LEASED，释放行锁'),('J','MQ','AMQP persistent + mandatory'),('MQ','J','basic.return 或 publisher confirm'),('J','DB','成功置SENT；失败留待重发')])
graph('4-1-er','实体关系',[('U','t_user / t_role\nuser_role / session'),('N','t_note\n版本与状态'),('M','t_media\nnote_media'),('T','t_topic\nnote_topic'),('C','t_comment / t_like\nt_follow'),('F','t_folder\nt_favorite'),('P','t_practice\nmedia / history'),('RV','t_review / t_report\nt_audit'),('NT','t_notification\nrecipient + event唯一'),('OB','t_outbox / t_inbox\nt_command'),('DS','t_interest\nt_hidden_note'),('ST','t_view_window\nnote_metric / daily_metric')],[('U','N','1 : 0..*'),('N','M','1 : 0..9'),('N','T','多对多'),('N','C','1 : 0..*'),('U','F','1 : 0..*'),('F','N','通过favorite'),('P','N','0..* : 1'),('U','P','1 : 0..*'),('RV','N','多态目标应用校验'),('RV','P','多态目标应用校验'),('U','NT','1 : 0..*'),('U','DS','1 : 0..*'),('N','ST','1 : 0..*')])
graph('4-2-dataflow','数据流',[('W','业务用例事务\n事实+review+command'),('DB','MySQL Outbox\n同事务PENDING'),('D','Dispatcher\n租约领取/确认'),('MQ','RabbitMQ\n定向至少一次'),('C','消费者事务\ninbox+副作用+DONE'),('R','Redis派生缓存\n允许失效/重建'),('A','归档/对账\n90日协调保留')],[('W','DB','原子提交'),('DB','D','领取'),('D','MQ','AMQP','async'),('MQ','C','投递','async'),('C','R','刷新/失效'),('C','A','完成后归档'),('DB','D','SENT超时重驱动')])
graph('5-1-compile','编译依赖',[('B','NotesApplication\n组合根例外'),('W','web\n仅HTTP'),('WF','workflows\n业务编排'),('API','8模块 api\n无跨模块import'),('APP','8模块 application\n实现API'),('D','8模块 domain\n纯规则/端口'),('I','8模块 infrastructure\n实现领域端口'),('S','shared\n技术契约'),('P','platform\n技术集成')],[('B','W','装配'),('W','WF','依赖'),('WF','API','依赖'),('APP','API','实现'),('APP','D','依赖'),('I','D','实现'),('P','S','实现'),('WF','S','依赖')])
graph('5-2-ci','构建流水线',[('A','提交\n短期分支'),('B','检查\n格式/类型/分层'),('C','测试\n单元/覆盖率'),('D','集成\nMySQL/MQ/契约'),('E','前端\n构建/E2E'),('F','制品\n镜像/漏洞/许可'),('G','演示部署\n健康/冒烟'),('H','审核\n证据/文档同步')],[('A','B',''),('B','C',''),('C','D',''),('D','E',''),('E','F',''),('F','G',''),('G','H','')])
seq('6-1-concurrency','并发点赞',[('A','请求A'),('B','请求B'),('DB','MySQL'),('C','计数消费者')],[('A','DB','锁note/验证公开/INSERT唯一like'),('B','DB','同用户同note，等待目标锁'),('A','DB','写事件并COMMIT'),('B','DB','关系已存在，无新事件，成功'),('C','DB','按当前事实COUNT重算'),('DB','C','最终计数1，不因重复增2')])
seq('6-2-redelivery','消费重投',[('MQ','RabbitMQ'),('C','消费者'),('DB','MySQL')],[('MQ','C','投递eventId'),('C','DB','BEGIN：Inbox+通知+Outbox DONE'),('DB','C','COMMIT成功'),('C','C','ACK之前进程崩溃'),('MQ','C','重新投递同eventId'),('C','DB','Inbox唯一键命中，已完成'),('C','MQ','ACK，无重复通知')])
seq('6-3-cachefail','缓存故障',[('U','浏览请求'),('A','应用'),('R','Redis'),('DB','MySQL')],[('U','A','列表/详情'),('A','R','缓存查询100ms预算'),('R','A','超时/熔断'),('A','A','bulkhead16 + 有限分页'),('A','DB','索引查询与实时可见性'),('DB','A','公开数据'),('A','U','成功；超限429/503')])
graph('6-4-state','笔记状态',[('D','DRAFT\n本人私有'),('P','PENDING\n等待审核'),('ON','PUBLISHED\n唯一公开状态'),('R','REJECTED\n原因仅本人'),('O','OFFLINE\n治理下架'),('DEL','DELETED\n终结')],[('D','P','完整提交'),('P','ON','审核通过'),('P','R','驳回'),('P','D','本人撤回'),('ON','P','本人编辑重审'),('ON','O','审核员下架'),('R','D','本人整改'),('O','D','本人整改'),('D','DEL','本人删除'),('P','DEL','本人删除'),('ON','DEL','本人删除'),('R','DEL','本人删除'),('O','DEL','本人删除')])
write(DOC/'images/6-4-state.mmd','''stateDiagram-v2
[*] --> DRAFT
DRAFT --> PENDING: 提交
PENDING --> PUBLISHED: 审核通过
PENDING --> REJECTED: 驳回
PENDING --> DRAFT: 撤回
PUBLISHED --> PENDING: 编辑重审
PUBLISHED --> OFFLINE: 下架
REJECTED --> DRAFT: 整改
OFFLINE --> DRAFT: 整改
DRAFT --> DELETED: 删除
PENDING --> DELETED: 删除
PUBLISHED --> DELETED: 删除
REJECTED --> DELETED: 删除
OFFLINE --> DELETED: 删除
DELETED --> [*]''')
graph('7-1-deploy','本地物理部署',[('B','宿主浏览器\n127.0.0.1:8080'),('N','edge网络\nNginx :80'),('A','edge + back\napp :8080/8081'),('DB','back internal\nmysql :3306'),('R','back internal\nredis :6379'),('MQ','back internal\nRabbit :5672'),('F','媒体持久卷\n仅挂app'),('P','monitor profile\nPrometheus/Grafana'),('BK','宿主外接介质\n加密离线备份')],[('B','N','HTTP'),('N','A','/api'),('A','DB','JDBC'),('A','R','RESP'),('A','MQ','AMQP','async'),('A','F','私有文件'),('P','A','管理HTTP'),('DB','BK','备份'),('F','BK','一致性备份')])
write(DOC/'images/figures.json',json.dumps(FIGS,ensure_ascii=False,indent=2))

# 完整ER保留逐实体PK/FK与基数；正文概览不取代它。
er=['erDiagram'];er_nodes=[];er_edges=[]
for t in TABLES:
    er.append('  '+t['name']+' {')
    er_nodes.append((t['name'],t['name']+'\n'+t['owner']))
    for n,typ,null,default,desc,fk in t['fields']:
        key=' PK' if n=='id' else ' FK' if fk else ''
        typ2=re.sub(r'[^A-Za-z0-9_]','_',typ)
        er.append(f'    {typ2} {n}{key}')
        if fk:
            parent=fk.split('.')[0]
            er.append('') if False else None
            if parent!=t['name']:er_edges.append((parent,t['name'],'1 : 0..* '+n))
    er.append('  }')
for t in TABLES:
    for n,typ,null,default,desc,fk in t['fields']:
        if fk:er.append(f'  {fk.split(".")[0]} '+('|o' if null else '||')+f'--o{{ {t["name"]} : "{n}"')
graph('4-1-er-full','完整30实体ER',er_nodes,er_edges)
write(DOC/'images/4-1-er-full.mmd','\n'.join(er))
class_source=['classDiagram']
for cls,fields in {
 'User':['long id','AccountStatus status','int authVersion'],
 'Note':['long id','NoteStatus status','int revision','submit()','edit()'],
 'Media':['long id','long ownerId','MediaPurpose purpose'],
 'Topic':['long id','TopicStatus status'],
 'Comment':['long id','long rootId','CommentStatus status'],
 'Follow':['long followerId','long followeeId'],
 'Folder':['long id','long ownerId'],
 'Favorite':['long folderId','long noteId'],
 'Practice':['long id','PracticeStatus status','int feedbackRevision','transition()'],
 'Review':['long id','int targetRevision','ReviewStatus status','decide()'],
 'Report':['long id','ReportStatus status','close()'],
 'Notification':['long id','long recipientId','markRead()']}.items():
    class_source.append('  class '+cls+' {\n'+''.join('    +'+x+'\n' for x in fields)+'  }')
class_source += ['User "1" --> "0..*" Note : creates','Note "1" --> "1..9" Media : binds','Note "0..*" --> "1..5" Topic : tags','Note "1" --> "0..*" Comment : receives','User "1" --> "0..*" Folder : owns','Folder "1" *-- "0..*" Favorite','Favorite "0..*" --> "1" Note','User "1" --> "0..*" Practice : owns','Practice "0..*" --> "1" Note : refers','Review --> Note : NOTE target','Review --> Practice : PRACTICE target','User "1" --> "0..*" Notification : receives','User "1" --> "0..*" Follow : follows']
write(DOC/'images/3-4-domain-class.mmd','\n'.join(class_source))
write(DOC/'images/figures.json',json.dumps(FIGS,ensure_ascii=False,indent=2))

ADRS=[
('后端框架选型','开发/运行','固定Java21，需事务、安全、AMQP和测试集成。','Spring Boot3.5.16','Spring Boot4：维护更新但生态迁移面更大；手工Servlet：依赖与事务装配成本高。','3系BOM减少Spring组件版本混搭；尚未实际编译。','接受非最新主版本及未来升级工作量。','T01 Maven依赖解析与Java21构建；发现不受支持或高危未修复时升补丁/重评主版本。'),
('前端框架选型','逻辑/开发','用户已选Vue3，需要用户端与治理后台一致契约。','Vue3.5.40+TypeScript+Vite，单工程按features划分','React：同样可行但用户未选；两套前端：维护重复。','前端业务和后端八模块一一对应，路由守卫不能代替后端权限。','构建工具补丁和辅助库须T01解析后锁定，不预称兼容通过。','T01类型检查/构建；T11移动宽度与权限E2E；如团队偏好变化需先修改用户选择。'),
('数据存储与持久层','数据/开发','结构化关系、审核与事件需本地事务；用户指定MySQL和MyBatis-Plus。','MySQL8.4.13+MP3.5.18 Boot3 starter，领域/PO分离','JPA：不符合指定持久层；文档库：关系约束成本高；内存模拟：违反任务书。','FK/唯一键与事务比应用先查后插可靠。','需要显式SQL、迁移与映射，不能直接暴露PO。','T03/04真实MySQL并发/EXPLAIN；字典与DDL同源检查；无H2替代。'),
('缓存粒度与降级','数据/运行','公共读取、推荐与统计频繁，但不可由过期缓存授权。','Redis只保存可重建派生数据，allkeys-lfu；安全事实回主库','Redis独占会话撤销事实：故障/淘汰可能影响授权；仅JVM缓存：失效与扩容复杂。','缓存故障不会造成退出/封禁失效；主库可见性统一过滤。','多一次安全查询，必须索引和bulkhead保护DB。','T05/11停Redis测试、计数对账；命中率不足先调粒度而非去掉授权。'),
('认证授权与防重放','逻辑/数据/运行','退出、封禁需及时生效；浏览器同源访问。','DB权威随机会话、哈希存储、HttpOnly Cookie、同步CSRF、RBAC+对象权限','纯无状态JWT：即时撤销仍需额外状态；localStorage令牌：XSS暴露面更大。','每次查session/user版本，明确定义失效；写命令幂等，不把CSRF等同防所有重放。','会话查询开销，需匿名CSRF生命周期与Cookie策略测试。','T01/11退出、改权、封禁、CSRF、跨站及最后管理员场景。'),
('部署形态与媒体存储','物理/开发','用户暂无服务器，个人项目先本地Compose。','一宿主多容器、私有媒体卷、可选monitor profile','K8s/多云：资源与维护过度；云对象存储：本期未采购。','先保证可重现启动、隔离和可恢复，云演进单列。','宿主/磁盘单点、媒体代理带宽、本地无公网证据。','T11干净环境10分钟启动与恢复；多宿主前先迁媒体。'),
('可靠异步消息','数据/运行','业务提交与AMQP无法同事务，重复投递与确认后丢盘需覆盖。','每消费者定向Outbox+RabbitMQ持久队列+Inbox，消费完成置DONE','仅afterCommit发布：进程崩溃窗口丢消息；分布式2PC：复杂且不适合本期。','DB保留未完成事实，SENT超时可重发；副作用去重和业务同事务。','额外表与调度；至少一次而非跨系统exactly once；DEAD需人工。','T02注入提交后发布前、消费后ACK前、broker重建故障；Inbox保留与重放协调。'),
('业务模块与编排边界','逻辑/开发','八模块职责需清晰；提交和审核跨模块同时修改对象。','模块互不直接import，上层workflows调用api协调同库事务；readmodel只读跨表','模块互调：易形成content↔governance环；拆微服务：个人规模成本大。','编译图无环，跨域事务在显式用例边界可测试。','workflow扇出增加；只读跨表查询需随schema迁移同步。','T01 ArchUnit+依赖矩阵，T04审核事务失败回滚。'),
('图片授权与安全边界','逻辑/物理','草稿、下架和私有实践不能从图片直链绕过。','媒体随机路径、实际解码去EXIF、仅app挂卷、后端每请求授权、no-store','Nginx公开静态图片：无法及时撤销；长期签名URL：过期前仍能读。','统一笔记与实践可见性；不承诺撤回客户端已经下载的字节。','应用带宽/IO压力；需缩略图、限像素和独立线程池。','T03/T08下架前后媒体请求、外人mediaId猜测、EXIF/路径和伪装文件测试。'),
('版本化审核与反馈依附','逻辑/数据/运行','旧任务不得批准新内容；公开实践不能绕过源笔记治理。','review绑定target_revision；笔记重审暂停公开；反馈绑定source_revision并独立审核','旧版本继续公开：需双版本和复杂媒体归属；反馈自动恢复：易暴露已失效内容。','状态简单且可验证，原文修改后旧反馈即时不可公开。','编辑期间曝光中断；重新发布后需反馈重新申请。','T04/T08并发审核、撤回/编辑、原文失效后恢复与媒体访问测试。'),
('规则推荐与分页','逻辑/数据/运行','需要可解释、可控推荐和稳定分页；不引入训练或付费AI。','兴趣/关注/热度规则，120秒候选快照，实时过滤可见性/隐藏','深度模型：样本和解释成本高；实时分数翻页：重排重复。','固定测试集可复现；理由与真实命中一致。','Redis快照过期或故障时需提示刷新或降级为稳定最新；不能假装个性化。','T05同快照不重复、偏好改后失效、降权占比和理由测试。'),
('统计口径与对账','数据/运行','消息会乱序/重复；跨收藏夹人数去重、每日指标需可核对。','当前计数按事实重算，日流量从保留事件聚合；水位明确；定时对账','直接重复增量：负数/重复；前端for循环查库：违反课程要求。','最终结果不依赖重复事件次数；日汇总按业务时间而非消费到达时间。','聚合SQL和事件保留成本；日历史不能从当前点赞表反推。','T10重复乱序/跨日/空分母/多夹收藏对账；无真实数据不声称业务规模。')]
for i,(title,views,context,decision,alts,reason,cost,verify) in enumerate(ADRS,1):
    write(DOC/'adr'/f'ADR-{i:03d}-{title}.md',f'''# ADR-{i:03d}：{title}

- 日期：2026-11-01
- 状态：设计采纳，实施待验证；用户确认总体方案，具体工程取舍由本设计提出
- 涉及视图：{views}

## 背景
{context}

## 备选方案
| 方案 | 收益 | 代价 |
|---|---|---|
| 采用方案：{decision} | {reason} | {cost} |
| 其他备选 | {alts} | 参见逐项理由，未因“流行”采用 |

## 决策
{decision}。

## 理由
{reason} 放弃其他备选的理由：{alts}

## 影响
{cost} 涉及{views}视图，需同步主说明书、契约、源码和Skill规范。

## 验证方式与重新评估
{verify} 本记录不是测试报告，若验证失败则保留原记录并以新ADR取代，不倒写历史。
''')

# Compose为设计资产；未来业务构建上下文此时不存在。
write(DOC/'attachments/compose.design.yaml','''# 设计示例，不是已可运行源码。需未来03-源码构建上下文、配置和真实secret。
name: shiguang
services:
  mysql:
    image: mysql:8.4.13
    environment:
      MYSQL_DATABASE: shiguang
      MYSQL_USER: shiguang_app
      MYSQL_PASSWORD: ${DB_PASSWORD:?configure DB_PASSWORD}
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD:?configure DB_ROOT_PASSWORD}
      TZ: UTC
    command: [--character-set-server=utf8mb4, --collation-server=utf8mb4_0900_ai_ci, --innodb-buffer-pool-size=536870912]
    volumes: [mysql_data:/var/lib/mysql]
    networks: [back]
    mem_limit: 1536m
    restart: unless-stopped
  redis:
    image: redis:8.2.11
    # 密码从只读redis.conf注入；文件包含requirepass，不提交真实文件。
    command: [redis-server, /run/secrets/redis.conf]
    volumes: [./secrets/redis.conf:/run/secrets/redis.conf:ro]
    networks: [back]
    mem_limit: 384m
    restart: unless-stopped
  rabbitmq:
    image: rabbitmq:4.4.0-management
    environment:
      RABBITMQ_DEFAULT_USER: shiguang
      RABBITMQ_DEFAULT_PASS: ${RABBIT_PASSWORD:?configure RABBIT_PASSWORD}
      RABBITMQ_DEFAULT_VHOST: shiguang
    volumes: [rabbit_data:/var/lib/rabbitmq]
    networks: [back]
    mem_limit: 768m
    restart: unless-stopped
  app:
    build: {context: ../../03-源码/backend}
    environment:
      SPRING_PROFILES_ACTIVE: staging
      DB_URL: jdbc:mysql://mysql:3306/shiguang?connectionTimeZone=UTC
      DB_PASSWORD: ${DB_PASSWORD:?configure DB_PASSWORD}
      REDIS_HOST: redis
      RABBIT_HOST: rabbitmq
      RABBIT_PASSWORD: ${RABBIT_PASSWORD:?configure RABBIT_PASSWORD}
      CURSOR_HMAC_KEY: ${CURSOR_HMAC_KEY:?configure CURSOR_HMAC_KEY}
      MEDIA_ROOT: /data/media
      JAVA_TOOL_OPTIONS: -Xms256m -Xmx768m
    volumes: [media_data:/data/media]
    networks: [edge, back]
    mem_limit: 1536m
    read_only: true
    tmpfs: [/tmp]
    restart: unless-stopped
  nginx:
    build: {context: ../../03-源码/frontend}
    ports: [127.0.0.1:8080:80]
    networks: [edge]
    mem_limit: 128m
    restart: unless-stopped
networks:
  edge: {}
  back: {internal: true}
volumes:
  mysql_data: {}
  rabbit_data: {}
  media_data: {}
''')

TASKS=[
('T01','基础工程、身份与门禁','FR-01,FR-02,FR-03',
 ['backend/pom.xml','backend/src/main/java/cn/shiguang/notes/workflows/identity/LoginUseCase.java','backend/src/test/java/cn/shiguang/notes/architecture/LayeringTest.java','frontend/src/features/identity/LoginPage.vue','docs/api/openapi.yaml'],
 '输入：用户已确认Java21/Vue3与八模块边界；schema/OpenAPI设计。输出：IdentityApi、ActorContext、Result<T>、BizException、Clock契约；DB权威会话与CSRF。',
 '登录/退出/封禁后旧会话无效、登录名唯一、匿名CSRF、密码72字节边界、最后管理员保护、非法包依赖被检测。',
 'mvnw.cmd verify -Dtest=IdentityUseCaseTest,LayeringTest', 'feat: establish identity and architecture boundaries'),
('T02','Outbox、Inbox与可靠调度','FR-19,FR-20',
 ['backend/src/main/java/cn/shiguang/notes/shared/contract/EventEnvelope.java','backend/src/main/java/cn/shiguang/notes/platform/application/OutboxDispatcher.java','backend/src/main/java/cn/shiguang/notes/platform/messaging/IdempotentConsumer.java','backend/src/test/java/cn/shiguang/notes/integration/OutboxIT.java'],
 '输入：T01事务/Clock/错误；输出：EventAppender.append(EventEnvelope,Set<ConsumerName>)；定向Outbox和幂等消费壳。',
 '业务回滚不发消息、提交后崩溃仍补发、mandatory不可路由、消费提交后ACK前崩溃不重复、消费完成先于confirm落库不覆盖DONE、旧失败副本CAS不改DONE、失败调度提交后ACK前崩溃旧token不绕过退避且不重复attempts、SENT重驱动和DEAD重放。',
 'mvnw.cmd verify -Dit.test=OutboxIT', 'feat: add recoverable outbox delivery'),
('T03','笔记草稿、媒体与实时授权','FR-04,FR-05,FR-07',
 ['backend/src/main/java/cn/shiguang/notes/modules/content/domain/Note.java','backend/src/main/java/cn/shiguang/notes/modules/content/api/ContentApi.java','backend/src/main/java/cn/shiguang/notes/workflows/content/ReadMediaUseCase.java','frontend/src/features/content/NoteEditor.vue','backend/src/test/java/cn/shiguang/notes/integration/MediaAuthorizationIT.java'],
 '输入：ActorContext、IdentityApi、EventAppender；输出：ContentApi.submit(actorId,noteId,version)、NoteSnapshot、MediaStoragePort及图片读取接口。',
 '草稿仅本人、版本冲突、伪装/超限/超像素文件、上传补偿、媒体owner/purpose、下架后媒体与详情404、EXIF移除。',
 'mvnw.cmd verify -Dtest=NoteTest -Dit.test=MediaAuthorizationIT', 'feat: implement note drafts and protected media'),
('T04','审核、举报和完整笔记状态机','FR-06,FR-07,FR-22,FR-23,FR-24',
 ['backend/src/main/java/cn/shiguang/notes/modules/governance/api/GovernanceApi.java','backend/src/main/java/cn/shiguang/notes/workflows/content/SubmitNoteUseCase.java','backend/src/main/java/cn/shiguang/notes/workflows/governance/DecideReviewUseCase.java','frontend/src/features/governance/ReviewQueuePage.vue','backend/src/test/java/cn/shiguang/notes/integration/ReviewIT.java'],
 '输入：T02事件、T03ContentApi；输出：GovernanceApi.openReview(ReviewTarget,applicantId)、ReviewSnapshot、ReviewDecision及治理查询。',
 '禁止自审、旧revision409、双审核只有一次成功、撤回取消任务且原样重提创建新revision、已发布编辑暂停公开、举报开放键唯一、结案目标状态与审计原子。',
 'mvnw.cmd verify -Dtest=ReviewPolicyTest -Dit.test=ReviewIT', 'feat: enforce versioned moderation lifecycle'),
('T05','发现搜索与可解释推荐','FR-08,FR-09,FR-10,FR-11',
 ['backend/src/main/java/cn/shiguang/notes/modules/discovery/domain/RecommendationPolicy.java','backend/src/main/java/cn/shiguang/notes/modules/discovery/api/DiscoveryApi.java','backend/src/main/java/cn/shiguang/notes/platform/readmodel/FeedReadModel.java','frontend/src/features/discovery/FeedPage.vue','backend/src/test/java/cn/shiguang/notes/integration/FeedIT.java'],
 '输入：公开可见性契约、T04已发布事实；输出：DiscoveryApi.recommend(viewerId,FeedQuery):CardPage，签名游标与120秒候选快照。',
 '同快照分页不重、偏好变更使旧游标失效、隐藏即时过滤、降权话题占比降低、三种理由真实、中文检索召回/索引、Redis故障稳定降级。',
 'mvnw.cmd verify -Dtest=RecommendationPolicyTest -Dit.test=FeedIT', 'feat: add explainable discovery and search'),
('T06','点赞、评论与关注关系','FR-12,FR-13,FR-14',
 ['backend/src/main/java/cn/shiguang/notes/modules/social/api/SocialApi.java','backend/src/main/java/cn/shiguang/notes/workflows/social/LikeUseCase.java','frontend/src/features/social/CommentThread.vue','backend/src/test/java/cn/shiguang/notes/integration/SocialConcurrencyIT.java'],
 '输入：T02事件、T03ContentApi与身份；输出：SocialApi.ensureLike(actorId,noteId,desired):boolean，以及评论/关注API。',
 '100并发重复点赞一事实、PUT/DELETE不toggle、禁自关、评论两级与父删除占位、不可见目标阻断新增、唯一冲突不吞其他异常。',
 'mvnw.cmd verify -Dtest=SocialPolicyTest -Dit.test=SocialConcurrencyIT', 'feat: add idempotent social interactions'),
('T07','私有收藏夹与跨夹去重','FR-15,FR-16',
 ['backend/src/main/java/cn/shiguang/notes/modules/collection/api/CollectionApi.java','backend/src/main/java/cn/shiguang/notes/workflows/collection/FavoriteUseCase.java','frontend/src/features/collection/FolderPage.vue','backend/src/test/java/cn/shiguang/notes/integration/FavoriteIT.java'],
 '输入：T03公开内容与T02事件；输出：CollectionApi收藏/归属查询，FolderSnapshot与FavoriteSnapshot。',
 '跨人夹ID越权、同夹重复、跨夹仅一收藏人数、删夹不删笔记、失效条目不泄露标题/媒体、跨夹user_id与owner一致。',
 'mvnw.cmd verify -Dtest=FolderPolicyTest -Dit.test=FavoriteIT', 'feat: add private collections with deduplicated counts'),
('T08','实践闭环与公开反馈治理','FR-17,FR-18',
 ['backend/src/main/java/cn/shiguang/notes/modules/collection/domain/Practice.java','backend/src/main/java/cn/shiguang/notes/workflows/collection/PublishFeedbackUseCase.java','frontend/src/features/collection/PracticeDetail.vue','backend/src/test/java/cn/shiguang/notes/integration/FeedbackIT.java'],
 '输入：T07CollectionApi、T04GovernanceApi、T03媒体；输出：CollectionApi.transition(actorId,practiceId,version,to):PracticeSnapshot与反馈发布入口。',
 '非法状态、DONE结果、默认私有、自审禁止、原笔记下架/修改即时隐藏、重新发布不自动恢复、实践图片直链越权、编辑结果撤销原公开。',
 'mvnw.cmd verify -Dtest=PracticeTest -Dit.test=FeedbackIT', 'feat: complete private practice and moderated feedback'),
('T09','通知收件箱与已读','FR-19,FR-20,FR-21',
 ['backend/src/main/java/cn/shiguang/notes/modules/notification/api/NotificationApi.java','backend/src/main/java/cn/shiguang/notes/workflows/notification/ConsumeNotificationUseCase.java','frontend/src/features/notification/InboxPage.vue','backend/src/test/java/cn/shiguang/notes/integration/NotificationIT.java'],
 '输入：T02幂等消费、T04/T06事件；输出：NotificationApi.receive(EventEnvelope)、通知分页与unread接口。',
 '重复投递不重复、自行为不自通知、他人收件箱不可读、目标失效不泄露正文、throughId全部已读不吞之后新消息。',
 'mvnw.cmd verify -Dtest=NotificationPolicyTest -Dit.test=NotificationIT', 'feat: deliver private event-driven notifications'),
('T10','统计口径、聚合与图表','FR-25,FR-26,FR-27',
 ['backend/src/main/java/cn/shiguang/notes/workflows/statistics/AggregateDailyUseCase.java','backend/src/main/java/cn/shiguang/notes/platform/readmodel/StatisticsReadModel.java','frontend/src/features/statistics/DashboardPage.vue','backend/src/test/java/cn/shiguang/notes/integration/StatisticsIT.java'],
 '输入：T02事件时间、T06/T07事实、T08实践；输出：Dashboard(points,calculatedAt,timezone)、7/30日图表与排行。',
 '重复乱序事件、跨日迟到、跨夹收藏去重、空分母、浏览去重、已完成实践比例、非本人统计403、历史指标不从当前点赞数倒推。',
 'mvnw.cmd verify -Dit.test=StatisticsIT', 'feat: add reconcilable analytics dashboards'),
('T11','Compose、故障恢复、安全与端到端验收','FR-01–27',
 ['docker-compose.yml','deploy/nginx/default.conf','deploy/backup/backup.ps1','README.md','frontend/tests/e2e/core-flow.spec.ts','backend/src/test/java/cn/shiguang/notes/integration/FailureRecoveryIT.java'],
 '输入：T01–T10服务与契约；输出：可启动源码、固定镜像digest、健康检查、备份恢复、角色种子与验收报告。',
 '干净环境10分钟、Cookie/CSRF/越权、下架媒体、停Redis与MQ、DB恢复、100QPS记录规格、360/1440页面与键盘核心路径。',
 'docker compose config；mvnw.cmd verify；npm ci；npm run build；npx playwright test', 'test: verify deployment security and recovery'),
('T12','课程Skill、文档同步与演示材料','FR-01–27',
 ['../02-开发Skill/project-scaffold/SKILL.md','../02-开发Skill/module-generator/SKILL.md','../02-开发Skill/arch-doc-sync/SKILL.md','../02-开发Skill/usage-log.md','../05-附录/AI使用说明.md','../04-演示/答辩PPT.pdf'],
 '输入：真实代码、测试、Git和部署事实；输出：三Skill与资产、五视图追溯更新、≥5次真实使用、AI修正证据及演示。',
 'Skill约束与DEP-01–07一致、脚本真运行、OpenAPI实现比对、≥12截图、有口播5–8分钟视频≤300MB、每项自评依据可核查。',
 'python scripts/check-contracts.py；python scripts/check-layering.py；人工对照提交清单', 'docs: synchronize architecture and submission evidence')]
plan=['# 拾光笔记开发实施计划',
 '> 执行者：本计划只定义后续实施，不自动开始编码。实施时使用superpowers:executing-plans逐任务执行；如用户明确选择代理实施，再使用superpowers:subagent-driven-development。步骤以复选框追踪。',
 '**目标**：实现已确认的8模块、3角色、27条功能与两项创新，并形成课程可核验交付。',
 '**架构**：模块化单体、上层workflows协调模块api、MySQL本地事务、定向Outbox与RabbitMQ异步。Redis仅缓存可重建数据；私有媒体每请求授权。',
 '**技术栈**：Java21、Boot3.5.16、MySQL8.4.13、MP3.5.18、Vue3.5.40、Redis8.2.11、RabbitMQ4.4.0；依赖基线见架构文档5.1。',
 '**规格**：同目录架构设计说明书、数据字典、接口设计说明与attachments/openapi.yaml/schema.sql；需求来源为根目录需求规格说明书。',
 '## 全局约束',
 '所有源码路径相对于03-源码；本次不创建该目录。JDK编译目标21；包前缀cn.shiguang.notes；DEP-01–07；所有外部ID字符串；时间UTC/统计Asia/Shanghai；无真实secret入仓；所有公开内容和媒体主库校验；测试必须用真实MySQL/Rabbit验证事务与确认。',
 '在T01将schema.sql迁入Flyway V1并核验；后续变更新增迁移、不修改已运行迁移。完整OpenAPI拷至docs/api，公开/本人/审核DTO严格裁剪。',
 '## 审查重点',
 '1. 下架/删除和公开反馈的图片旁路：T03/T08直接请求旧mediaId应404。\n2. 消费提交后ACK前崩溃：T02/T09通知不重复，Outbox最终DONE。\n3. 原笔记编辑与旧审核/反馈：T04/T08旧revision不再公开。\n4. 并发取消/点赞与跨夹收藏：T06/T07/T10唯一事实和去重人数正确。\n5. Redis与MQ故障：T05/T11不放宽鉴权，不丢已提交事件且限界积压。',
 '## 公共类型与接口定义',
 '`ActorContext(long userId,Set<RoleCode> roles)`；`Result<T>(String code,String message,T data,String traceId)`；`VersionResult(String id,int version,String status)`。`EventEnvelope`字段严格沿架构3.4。',
 'ContentApi/GovernanceApi/SocialApi/CollectionApi/DiscoveryApi/NotificationApi由对应模块api定义；workflow只导入api。NoteSnapshot/PracticeSnapshot/ReviewSnapshot为模块不可变记录，不可直接序列化到公开HTTP；DTO字段由OpenAPI限定。',
 '`FeedQuery(String mode,String category,String cursor,int size)`；`CardPage(List<Card> items,String nextCursor,boolean hasMore)`；`ReviewTarget(TargetType type,long id,int revision)`；`ReviewDecision(int version,Decision decision,String reason)`。Clock使用java.time.Clock经构造注入。',
 '## 执行顺序与验证环境',
 'T01→T02→T03→T04→T05→T06→T07→T08→T09→T10→T11→T12；按依赖顺序，个人无需并行工作树。T01建单测工具链；T02起集成测试用Testcontainers，Docker不可用时明确未运行而非跳过后报成功。',
 'Maven命令在backend目录；npm/Playwright在frontend；Compose在03-源码。mvnw.cmd为PowerShell形式，Linux替换./mvnw。Surefire匹配*Test、Failsafe匹配*IT且verify绑定integration-test/verify；选择单测时设置failIfNoSpecifiedTests=false只对多项目适用，本期单工程无需。']
for tid,title,fr,files,interfaces,assertions,cmd,commit in TASKS:
    plan += [f'## {tid} {title}',f'需求：{fr}。', '**文件**：\n'+'\n'.join('- 新建：`'+x+'`' for x in files),'**接口输入与输出**：'+interfaces,
      '- [ ] 步骤1：按以下断言先写业务测试：'+assertions,
      '- [ ] 步骤2：执行测试，记录预期失败；应因目标能力缺失失败，不接受环境/依赖错误作为有效红灯。',
      '- [ ] 步骤3：按本任务文件和规格实现最小能力，完善正常/边界/异常，保持契约与依赖约束。',
      '- [ ] 步骤4：执行 `'+cmd+'`，预期相关用例全部通过、退出码0，保留实际输出；失败须修复，不能修改期望掩盖缺陷。',
      '- [ ] 步骤5：检查差异、补契约/文档/Skill使用记录，提交有意义变更：`'+commit+'`。实际提交必须基于真实进展。']
plan += ['## 性能、故障与恢复验收设计',
 '压测先预热2分钟、稳定10分钟，读写混合80:20，目标100QPS、初始1万篇；外部图像上传/下载单独测，不能混淆API延迟。记录机器、容器限额、数据大小、工具版本、随机种子、错误与P95。',
 'MQ演练：20条/秒写入、停broker60秒后恢复，核对每个成功业务eventId/consumer均DONE且无重复副作用；恢复积压规模与耗时实际记录。Redis演练：停60秒，公开查询限界降级，隐藏和权限保持；恢复后缓存可重建。',
 '备份恢复演练：冻结写入取得一致快照，恢复至新Compose项目/空卷，验证账号、10篇不同状态笔记、图片校验值、未完成Outbox重发和私有数据权限，实测RPO/RTO。',
 '## 本计划覆盖与自检',
 'FR01–03→T01；04–05→T03；06–07/22–24→T04；08–11→T05；12–14→T06；15–16→T07；17–18→T08；19–21→T02/T09；25–27→T10。NFR性能/故障/安全→T11；工程/文档/Skill/演示→T01/T12。',
 '先定义公共类型再使用；测试断言按任务所拥有风险分配；所有任务产出可独立演示或测试的切片。没有真实截止日期，本文不伪造工期承诺；需要排期时用实际剩余周数估算，保留≥6周Git证据约束。',
 '## 本轮状态',
 '计划已编写，所有实施复选框保持未勾选。本轮只交付开发与架构文档，不自动执行这些任务。用户确认设计后再按后续授权进入代码阶段。']
write(DOC/'开发实施计划.md','\n\n'.join(plan))
print(json.dumps({'figures':len(FIGS),'adrs':len(ADRS),'tasks':len(TASKS)},ensure_ascii=False))
