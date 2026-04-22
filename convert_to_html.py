#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
将Markdown教程文件转换为HTML格式
"""
import re
import os
from pathlib import Path

def convert_markdown_to_html(md_content):
    """将Markdown内容转换为HTML"""
    lines = md_content.split('\n')
    html_parts = []
    i = 0
    
    while i < len(lines):
        line = lines[i]
        
        # 标题
        if line.startswith('# '):
            html_parts.append(f'<h2>{escape_html(line[2:].strip())}</h2>')
            i += 1
        elif line.startswith('## '):
            html_parts.append(f'<h3>{escape_html(line[3:].strip())}</h3>')
            i += 1
        elif line.startswith('### '):
            html_parts.append(f'<h4>{escape_html(line[4:].strip())}</h4>')
            i += 1
        elif line.startswith('#### '):
            html_parts.append(f'<h5>{escape_html(line[5:].strip())}</h5>')
            i += 1
            
        # 代码块
        elif line.startswith('```'):
            lang = line[3:].strip()
            code_lines = []
            i += 1
            while i < len(lines) and not lines[i].startswith('```'):
                code_lines.append(escape_html(lines[i]))
                i += 1
            code_content = '\n'.join(code_lines)
            lang_display = lang if lang else 'code'
            html_parts.append(f'''
            <div class="code-wrapper">
                <div class="code-header">
                    <span class="code-lang">{lang_display.upper()}</span>
                    <button class="copy-btn" onclick="copyCode(this)">📋 复制</button>
                </div>
                <pre><code>{code_content}</code></pre>
            </div>
            ''')
            i += 1
            
        # 列表项
        elif line.startswith('- ') or line.startswith('* '):
            list_items = []
            while i < len(lines) and (lines[i].startswith('- ') or lines[i].startswith('* ')):
                content = lines[i][2:].strip()
                list_items.append(f'<li>{convert_inline(content)}</li>')
                i += 1
            html_parts.append('<ul>' + '\n'.join(list_items) + '</ul>')
            
        elif line.strip().startswith(('1. ', '2. ', '3. ', '4. ', '5. ', '6. ', '7. ', '8. ', '9. ', '0. ')):
            list_items = []
            while i < len(lines) and re.match(r'^\d+\.\s', lines[i]):
                content = re.sub(r'^\d+\.\s', '', lines[i]).strip()
                list_items.append(f'<li>{convert_inline(content)}</li>')
                i += 1
            html_parts.append('<ol>' + '\n'.join(list_items) + '</ol>')
            
        # 表格
        elif '|' in line and i + 1 < len(lines) and '|' in lines[i + 1] and '---' in lines[i + 1]:
            # 表头
            headers = [cell.strip() for cell in line.split('|')[1:-1]]
            html_parts.append('<table><thead><tr>')
            for header in headers:
                html_parts.append(f'<th>{convert_inline(header)}</th>')
            html_parts.append('</tr></thead><tbody>')
            i += 2  # 跳过表头分隔线
            
            # 表格内容
            while i < len(lines) and '|' in lines[i] and lines[i].strip():
                cells = [cell.strip() for cell in lines[i].split('|')[1:-1]]
                html_parts.append('<tr>')
                for cell in cells:
                    html_parts.append(f'<td>{convert_inline(cell)}</td>')
                html_parts.append('</tr>')
                i += 1
            html_parts.append('</tbody></table>')
            
        # 提示框 (> 开头的文本)
        elif line.startswith('> '):
            tip_content = []
            while i < len(lines) and lines[i].startswith('> '):
                tip_content.append(convert_inline(lines[i][2:].strip()))
                i += 1
            html_parts.append(f'<div class="tip-box"><p>{"<br>".join(tip_content)}</p></div>')
            
        # 空行
        elif line.strip() == '':
            i += 1
            
        # 普通段落
        elif line.strip():
            html_parts.append(f'<p>{convert_inline(line)}</p>')
            i += 1
        else:
            i += 1
    
    return '\n'.join(html_parts)

def escape_html(text):
    """转义HTML特殊字符"""
    text = text.replace('&', '&amp;')
    text = text.replace('<', '&lt;')
    text = text.replace('>', '&gt;')
    text = text.replace('"', '&quot;')
    return text

def convert_inline(text):
    """转换内联Markdown元素"""
    # 加粗
    text = re.sub(r'\*\*(.+?)\*\*', r'<strong>\1</strong>', text)
    text = re.sub(r'__(.+?)__', r'<strong>\1</strong>', text)
    
    # 斜体
    text = re.sub(r'\*(.+?)\*', r'<em>\1</em>', text)
    text = re.sub(r'_(.+?)_', r'<em>\1</em>', text)
    
    # 行内代码
    text = re.sub(r'`(.+?)`', r'<code style="background: #faf8f6; padding: 2px 6px; border-radius: 4px; font-family: monospace;">\1</code>', text)
    
    # 链接
    text = re.sub(r'\[(.+?)\]\((.+?)\)', r'<a href="\2">\1</a>', text)
    
    # 换行
    text = text.replace('\n', '<br>')
    
    return text

def process_file(md_file, template_file, output_dir):
    """处理单个Markdown文件"""
    # 读取模板
    with open(template_file, 'r', encoding='utf-8') as f:
        template = f.read()
    
    # 读取Markdown文件
    with open(md_file, 'r', encoding='utf-8') as f:
        md_content = f.read()
    
    # 提取标题
    first_line = md_content.split('\n')[0]
    if first_line.startswith('# '):
        title = first_line[2:].strip()
    else:
        title = Path(md_file).stem
    
    # 转换内容
    html_content = convert_markdown_to_html(md_content)
    
    # 替换模板变量
    html = template.replace('{{TITLE}}', title)
    html = html.replace('{{SUBTITLE}}', f'AiSale 校园二手交易平台学习指南 - {title}')
    html = html.replace('{{CONTENT}}', html_content)
    
    # 保存HTML文件
    output_file = Path(output_dir) / (Path(md_file).stem + '.html')
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(html)
    
    print(f'✓ 已生成: {output_file.name}')

def main():
    # 获取当前脚本所在目录
    script_dir = Path(__file__).parent
    
    # 文件路径
    template_file = script_dir / 'template.html'
    readme_file = script_dir / 'README.md'
    output_dir = script_dir
    
    # 创建README的HTML版本（作为首页）
    print('正在转换首页...')
    with open(readme_file, 'r', encoding='utf-8') as f:
        readme_content = f.read()
    
    with open(template_file, 'r', encoding='utf-8') as f:
        template = f.read()
    
    html_content = convert_markdown_to_html(readme_content)
    html = template.replace('{{TITLE}}', 'AiSale 校园二手交易平台 - 学习教程')
    html = html.replace('{{SUBTITLE}}', '从零开始完整学习Spring Boot + Vue 3全栈开发')
    html = html.replace('{{CONTENT}}', html_content)
    
    with open(output_dir / 'index.html', 'w', encoding='utf-8') as f:
        f.write(html)
    print('✓ 已生成: index.html')
    
    # 转换所有Markdown教程文件
    md_files = sorted([
        f for f in script_dir.glob('*.md')
        if f.name != 'README.md' and f.name != '.DS_Store'
        and re.match(r'^\d{2}-', f.name)
    ])
    
    print(f'\n找到 {len(md_files)} 个教程文件')
    print('开始转换...\n')
    
    for md_file in md_files:
        try:
            process_file(md_file, template_file, output_dir)
        except Exception as e:
            print(f'✗ 转换失败 {md_file.name}: {e}')
    
    print(f'\n✅ 转换完成！共生成 {len(md_files) + 1} 个HTML文件')

if __name__ == '__main__':
    main()
