"""将主文档、数据字典与ADR排版为中文矢量PDF；目录和页数由实际构建产生。"""
from pathlib import Path
import re, html, io
from markdown_it import MarkdownIt
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import BaseDocTemplate, PageTemplate, Frame, Paragraph, Spacer, Table, TableStyle, PageBreak, KeepTogether
from reportlab.platypus.tableofcontents import TableOfContents
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.pdfgen.canvas import Canvas
from svglib.svglib import svg2rlg
from reportlab.graphics import renderPDF
from reportlab.platypus import Flowable

DOC=Path(__file__).resolve().parents[1]
pdfmetrics.registerFont(TTFont('Microsoft YaHei',r'C:\Windows\Fonts\msyh.ttc'))
pdfmetrics.registerFont(TTFont('Microsoft YaHei Bold',r'C:\Windows\Fonts\msyhbd.ttc'))
pdfmetrics.registerFontFamily('Microsoft YaHei',normal='Microsoft YaHei',bold='Microsoft YaHei Bold',italic='Microsoft YaHei',boldItalic='Microsoft YaHei Bold')
W,H=A4; LEFT=43; RIGHT=43; TOP=51; BOTTOM=45; AW=W-LEFT-RIGHT
styles={
 'body':ParagraphStyle('body',fontName='Microsoft YaHei',fontSize=12,leading=18,spaceAfter=7,wordWrap='CJK',splitLongWords=1),
 'h1':ParagraphStyle('h1',fontName='Microsoft YaHei Bold',fontSize=18,leading=27,spaceBefore=16,spaceAfter=14,keepWithNext=True,wordWrap='CJK'),
 'h2':ParagraphStyle('h2',fontName='Microsoft YaHei Bold',fontSize=14,leading=21,spaceBefore=15,spaceAfter=9,keepWithNext=True,wordWrap='CJK'),
 'h3':ParagraphStyle('h3',fontName='Microsoft YaHei Bold',fontSize=12,leading=18,spaceBefore=11,spaceAfter=7,keepWithNext=True,wordWrap='CJK'),
 'cell':ParagraphStyle('cell',fontName='Microsoft YaHei',fontSize=8.6,leading=13,wordWrap='CJK',splitLongWords=1),
 'code':ParagraphStyle('code',fontName='Microsoft YaHei',fontSize=8,leading=11,spaceAfter=1,wordWrap='CJK',splitLongWords=1),
 'caption':ParagraphStyle('caption',fontName='Microsoft YaHei',fontSize=10,leading=15,spaceBefore=6,spaceAfter=8,wordWrap='CJK'),
}

def inline(s):
    s=html.escape(s)
    s=re.sub(r'`([^`]+)`',lambda m:'<font color="#245268">'+m[1]+'</font>',s)
    s=re.sub(r'\*\*([^*]+)\*\*',r'<b>\1</b>',s)
    s=re.sub(r'\[([^\]]+)\]\((https?://[^)]+)\)',r'<link href="\2" color="#245268">\1</link>',s)
    return s.replace('\n','<br/>')

class Figure(Flowable):
    def __init__(self,path):
        super().__init__(); self.d=svg2rlg(str(path)); self.width=AW
        if self.d is None:raise ValueError(path)
        def fix_font(node):
            if hasattr(node,'fontName'):node.fontName='Microsoft YaHei'
            for child in getattr(node,'contents',[]):fix_font(child)
        fix_font(self.d)
        self.scale=min(AW/self.d.width,480/self.d.height)
        self.height=self.d.height*self.scale+12
    def draw(self):
        self.canv.saveState();self.canv.translate((AW-self.d.width*self.scale)/2,6);self.canv.scale(self.scale,self.scale);renderPDF.draw(self.d,self.canv,0,0);self.canv.restoreState()

class PageCountCanvas(Canvas):
    def __init__(self,*a,**k):super().__init__(*a,**k);self.saved=[]
    def showPage(self):self.saved.append(dict(self.__dict__));self._startPage()
    def save(self):
        total=len(self.saved)
        for state in self.saved:
            self.__dict__.update(state)
            self.setFont('Microsoft YaHei',9);self.setFillColor(colors.HexColor('#617483'))
            self.drawRightString(W-RIGHT,25,f'{self._pageNumber} / {total}')
            Canvas.showPage(self)
        Canvas.save(self)

class Doc(BaseDocTemplate):
    def __init__(self,path):
        super().__init__(str(path),pagesize=A4,leftMargin=LEFT,rightMargin=RIGHT,topMargin=TOP,bottomMargin=BOTTOM,title='拾光笔记架构设计说明书',author='项目设计文档')
        self.addPageTemplates(PageTemplate(id='main',frames=Frame(LEFT,BOTTOM,AW,H-TOP-BOTTOM,leftPadding=0,rightPadding=0,topPadding=0,bottomPadding=0),onPage=self.header))
    def header(self,c,d):
        c.saveState();c.setFont('Microsoft YaHei',9);c.setFillColor(colors.HexColor('#617483'));c.drawString(LEFT,H-29,'拾光笔记 · 五视图架构设计');c.drawRightString(W-RIGHT,H-29,'V1.0 | 2026-11-01 | 设计交付版')
        c.setStrokeColor(colors.HexColor('#d9e2e7'));c.line(LEFT,H-36,W-RIGHT,H-36);c.restoreState()
    def afterFlowable(self,f):
        if isinstance(f,Paragraph) and hasattr(f,'toc_key'):
            self.canv.bookmarkPage(f.toc_key)
            self.canv.addOutlineEntry(f.getPlainText(),f.toc_key,level=f.toc_level,closed=False)
            self.notify('TOCEntry',(f.toc_level,f.getPlainText(),self.page,f.toc_key))

counter=0
def parse(text,include_toc=True):
    global counter
    lines=text.splitlines(); story=[];i=0
    while i<len(lines):
        line=lines[i].strip()
        if not line:i+=1;continue
        if line.startswith('```'):
            code=[];i+=1
            while i<len(lines) and not lines[i].strip().startswith('```'):code.append(lines[i]);i+=1
            for c in code:story.append(Paragraph(html.escape(c).replace(' ','&#160;') or '&#160;',styles['code']))
            story.append(Spacer(1,8));i+=1;continue
        if line.startswith('|') and i+1<len(lines) and re.match(r'^\|[\s:|\-]+$',lines[i+1].strip()):
            rows=[]
            while i<len(lines) and lines[i].strip().startswith('|'):
                l=lines[i].strip()
                if not re.match(r'^\|[\s:|\-]+$',l):rows.append([x.strip() for x in l.strip('|').split('|')])
                i+=1
            n=max(map(len,rows));rows=[r+['']*(n-len(r)) for r in rows]
            # 对宽表降低字号，但保持中文换行。
            lengths=[max(min(len(r[j]),60) for r in rows) for j in range(n)]
            weights=[max(7,min(x,35)) for x in lengths]; widths=[AW*x/sum(weights) for x in weights]
            if n==5 and rows[0][0]=='字段':widths=[AW*x for x in [.20,.19,.07,.18,.36]]
            cells=[[Paragraph(inline(c),styles['cell']) for c in r] for r in rows]
            t=Table(cells,colWidths=widths,repeatRows=1,hAlign='LEFT',splitByRow=1)
            t.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),colors.HexColor('#e5eef3')),('TEXTCOLOR',(0,0),(-1,0),colors.HexColor('#163d51')),('VALIGN',(0,0),(-1,-1),'TOP'),('LEFTPADDING',(0,0),(-1,-1),5),('RIGHTPADDING',(0,0),(-1,-1),5),('TOPPADDING',(0,0),(-1,-1),5),('BOTTOMPADDING',(0,0),(-1,-1),5),('GRID',(0,0),(-1,-1),.35,colors.HexColor('#c8d6df')),('ROWBACKGROUNDS',(0,1),(-1,-1),[colors.white,colors.HexColor('#f6f8fa')])]))
            story.extend([t,Spacer(1,10)]);continue
        m=re.match(r'^(#{1,4})\s+(.+)$',line)
        if m:
            level=len(m[1]);style=styles['h1' if level==1 else 'h2' if level==2 else 'h3']
            if level==2 and re.match(r'^\d+\s',m[2]) and story:story.append(PageBreak())
            p=Paragraph(inline(m[2]),style)
            if include_toc and level<=2:
                counter+=1;p.toc_key=f'h{counter}';p.toc_level=0 if level==1 else 1
            story.append(p);i+=1;continue
        m=re.match(r'!\[([^\]]*)\]\(([^)]+)\)',line)
        if m:
            path=DOC/m[2]
            if path.exists():story.append(Figure(path))
            i+=1;continue
        if line.startswith('>'):line=line.lstrip('> ').strip()
        if line.startswith('- '):line='• '+line[2:]
        if line=='---':i+=1;continue
        para=[line];i+=1
        while i<len(lines) and lines[i].strip() and not re.match(r'^(#|\||!\[|```|- |>)',lines[i].strip()):para.append(lines[i].strip());i+=1
        joined=' '.join(para)
        style=styles['caption'] if re.match(r'^[图表]\d',joined) else styles['body']
        story.append(Paragraph(inline(joined),style))
    return story

def main():
    story=[Spacer(1,92),Paragraph('拾光笔记',ParagraphStyle('cover',fontName='Microsoft YaHei Bold',fontSize=34,leading=51,textColor=colors.HexColor('#173e52'))),Spacer(1,20),Paragraph('小红书核心业务复刻与创新<br/>架构设计说明书 · 五视图法',styles['h1']),Spacer(1,35),Paragraph('Java 21 / MySQL / MyBatis-Plus<br/>Vue 3 / Redis / RabbitMQ',styles['body']),Spacer(1,50),Paragraph('个人项目 · V1.0 · 2026-11-01<br/>本地 Docker Compose 设计<br/>设计交付，不包含虚构运行证据',styles['body']),PageBreak(),Paragraph('目录',styles['h1'])]
    toc=TableOfContents();toc.levelStyles=[ParagraphStyle('toc0',fontName='Microsoft YaHei Bold',fontSize=11,leading=18,spaceBefore=8),ParagraphStyle('toc1',fontName='Microsoft YaHei',fontSize=9.5,leading=15,leftIndent=12)]
    story.extend([toc,PageBreak()]);story+=parse((DOC/'架构设计说明书.md').read_text(encoding='utf8'))
    story.extend([PageBreak(),Paragraph('附录 E 全量数据字典',styles['h1'])]);story+=parse((DOC/'数据字典.md').read_text(encoding='utf8'),False)
    story.extend([PageBreak(),Paragraph('附录 F 架构决策明细',styles['h1'])])
    for p in sorted((DOC/'adr').glob('*.md')):
        story+=parse(p.read_text(encoding='utf8'),False);story.append(PageBreak())
    if isinstance(story[-1],PageBreak):story.pop()
    doc=Doc(DOC/'架构设计说明书.pdf');doc.multiBuild(story,canvasmaker=PageCountCanvas)
    print(DOC/'架构设计说明书.pdf')

if __name__=='__main__':main()
