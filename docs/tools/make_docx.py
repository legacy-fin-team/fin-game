#!/usr/bin/env python3
"""Собирает docs/FinGame-Документация.docx из docs/DOCUMENTATION.md.

Требуется pandoc 3.x. Запуск из корня репозитория:

    python3 docs/tools/make_docx.py

Что делает:
- заголовок первого уровня становится титулом, разделы — заголовками первого уровня;
- блоки Mermaid заменяются ссылкой на Markdown-версию (текстовая схема остаётся);
- скрытые пометки <!-- TODO ...: ... --> становятся видимыми «[TODO ...: ...]»;
- оглавление заполняется сразу (видно в любом просмотрщике) и обновляется Word при открытии;
- стили (A4, рамки таблиц, шрифты) берутся из docs/tools/reference.docx.
"""
import os
import re
import shutil
import subprocess
import sys
import tempfile
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SRC = os.path.join(ROOT, 'docs', 'DOCUMENTATION.md')
REF = os.path.join(ROOT, 'docs', 'tools', 'reference.docx')
OUT = os.path.join(ROOT, 'docs', 'FinGame-Документация.docx')
SUBTITLE = 'Мобильная игра по финансовой грамотности для детей. Android'


def prepare_markdown(text):
    lines = text.split('\n')
    if not lines[0].startswith('# '):
        sys.exit('DOCUMENTATION.md должен начинаться с заголовка первого уровня')
    title = lines[0][2:].strip()
    body = '\n'.join(lines[1:])
    body = re.sub(r'```mermaid\n.*?```',
                  '*Та же схема в формате Mermaid приведена в файле docs/DOCUMENTATION.md.*',
                  body, flags=re.S)
    body = re.sub(r'<!--\s*(TODO[^:]*):\s*(.*?)\s*-->',
                  lambda m: f'**[{m.group(1)}: {m.group(2)}]**', body, flags=re.S)
    if '<!--' in body:
        sys.exit('В документе остался HTML-комментарий, который не попадёт в DOCX')
    body = re.sub(r'^---$', '', body, flags=re.M)
    meta = (f'---\ntitle: "{title}"\nsubtitle: "{SUBTITLE}"\n'
            'lang: ru-RU\ntoc-title: "Содержание"\n---\n')
    return meta + body


def fill_toc(path):
    """Подставляет пункты оглавления в пустое поле TOC, которое создаёт pandoc."""
    zin = zipfile.ZipFile(path)
    doc = zin.read('word/document.xml').decode('utf-8')
    entries = []
    heading = r'<w:p>(?:(?!</w:p>).)*?<w:pStyle w:val="Heading([12])" />(?:(?!</w:p>).)*?</w:p>'
    for m in re.finditer(heading, doc, re.S):
        p = m.group(0)
        mark = re.search(r'<w:bookmarkStart[^>]*w:name="([^"]+)"', p)
        text = ''.join(re.findall(r'<w:t(?: [^>]*)?>([^<]*)</w:t>', p))
        entries.append((int(m.group(1)), mark.group(1) if mark else None, text))
    if not entries:
        sys.exit('В DOCX не найдено заголовков')

    def para(i, level, anchor, text):
        ind = '' if level == 1 else '<w:ind w:left="440" />'
        run = f'<w:r><w:t xml:space="preserve">{text}</w:t></w:r>'
        link = (f'<w:hyperlink w:anchor="{anchor}" w:history="1">{run}</w:hyperlink>'
                if anchor else run)
        head = ('<w:r><w:fldChar w:fldCharType="begin" w:dirty="true" /></w:r>'
                '<w:r><w:instrText xml:space="preserve">TOC \\o &quot;1-2&quot; \\h \\z \\u'
                '</w:instrText></w:r><w:r><w:fldChar w:fldCharType="separate" /></w:r>'
                if i == 0 else '')
        tail = '<w:r><w:fldChar w:fldCharType="end" /></w:r>' if i == len(entries) - 1 else ''
        return (f'<w:p><w:pPr><w:pStyle w:val="TOC{level}" />'
                f'<w:spacing w:before="0" w:after="40" />{ind}</w:pPr>{head}{link}{tail}</w:p>')

    toc = ''.join(para(i, *e) for i, e in enumerate(entries))
    field = re.search(r'<w:p><w:r><w:fldChar w:fldCharType="begin" w:dirty="true" />'
                      r'<w:instrText xml:space="preserve">TOC .*?'
                      r'<w:fldChar w:fldCharType="end" /></w:r></w:p>', doc, re.S)
    if not field:
        sys.exit('Поле оглавления не найдено')
    doc = doc[:field.start()] + toc + doc[field.end():]
    settings = zin.read('word/settings.xml').decode('utf-8')
    if 'updateFields' not in settings:
        settings = settings.replace('<w:zoom ', '<w:updateFields w:val="true" /><w:zoom ', 1)

    tmp = path + '.tmp'
    with zipfile.ZipFile(tmp, 'w', zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            data = zin.read(item.filename)
            if item.filename == 'word/document.xml':
                data = doc.encode('utf-8')
            elif item.filename == 'word/settings.xml':
                data = settings.encode('utf-8')
            zout.writestr(item, data)
    zin.close()
    shutil.move(tmp, path)
    return len(entries)


def main():
    with open(SRC, encoding='utf-8') as f:
        markdown = prepare_markdown(f.read())
    with tempfile.TemporaryDirectory() as tmp:
        md = os.path.join(tmp, 'doc.md')
        with open(md, 'w', encoding='utf-8') as f:
            f.write(markdown)
        subprocess.run(['pandoc', md, '-f', 'gfm+yaml_metadata_block', '-t', 'docx',
                        '--toc', '--toc-depth=2', '--shift-heading-level-by=-1',
                        '--reference-doc', REF, '-o', OUT], check=True)
    count = fill_toc(OUT)
    print(f'{os.path.relpath(OUT, ROOT)}: пунктов оглавления {count}')


if __name__ == '__main__':
    main()
