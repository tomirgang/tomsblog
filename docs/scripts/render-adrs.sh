#!/bin/bash
# Converts ADR Markdown files to HTML with a navigation bar.
# Usage: ./render-adrs.sh <source-dir> <output-dir>

SRC="${1:-docs/adr}"
OUT="${2:-build/site/adrs}"

mkdir -p "$OUT"

NAV_BAR='<div style="background:#343a40;padding:8px 16px;font-size:14px;font-family:sans-serif"><a href="../index.html" style="color:#fff;text-decoration:none;margin-right:16px">\&larr; Dokumentation</a><a href="../tomsblog-arc42/main/index.html" style="color:#fff;text-decoration:none;margin-right:16px">Architektur (arc42)</a><a href="../tomsblog-design/main/index.html" style="color:#fff;text-decoration:none;margin-right:16px">Software Detail Design</a><a href="../requirements/index.html" style="color:#fff;text-decoration:none;margin-right:16px">Requirements</a><a href="index.html" style="color:#fff;text-decoration:none">ADRs</a></div>'

# Generate index page
cat > "$OUT/index.html" << 'HEADER'
<!DOCTYPE html>
<html>
<head>
  <title>Architecture Decision Records</title>
  <meta charset="utf-8" />
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; margin: 0; padding: 0; }
    .content { max-width: 900px; margin: 0 auto; padding: 24px; }
    table { border-collapse: collapse; width: 100%; }
    th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; }
    th { background: #f5f5f5; }
    a { color: #0366d6; text-decoration: none; }
    a:hover { text-decoration: underline; }
  </style>
</head>
<body>
HEADER

echo "$NAV_BAR" | sed 's/\\&larr;/\&larr;/g' >> "$OUT/index.html"

cat >> "$OUT/index.html" << 'MID'
<div class="content">
<h1>Architecture Decision Records</h1>
<table>
<tr><th>ADR</th><th>Titel</th><th>Status</th></tr>
MID

for md in "$SRC"/*.md; do
  [ -f "$md" ] || continue
  filename=$(basename "$md" .md)
  # Extract title from first H1
  title=$(grep -m1 "^# " "$md" | sed 's/^# //')
  # Extract status
  status=$(grep -A2 "^## Status" "$md" | tail -1 | tr -d '[:space:]')
  # Get ADR number
  adr_num=$(echo "$filename" | grep -oP '^\d+')

  echo "<tr><td><a href=\"${filename}.html\">${adr_num}</a></td><td>${title}</td><td>${status}</td></tr>" >> "$OUT/index.html"

  # Convert individual ADR to HTML
  cat > "$OUT/${filename}.html" << EOF
<!DOCTYPE html>
<html>
<head>
  <title>${title}</title>
  <meta charset="utf-8" />
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; margin: 0; padding: 0; }
    .content { max-width: 900px; margin: 0 auto; padding: 24px; line-height: 1.6; }
    h1 { border-bottom: 1px solid #eee; padding-bottom: 8px; }
    h2 { margin-top: 24px; color: #333; }
    code { background: #f5f5f5; padding: 2px 6px; border-radius: 3px; font-size: 90%; }
    pre { background: #f5f5f5; padding: 16px; border-radius: 6px; overflow-x: auto; }
    pre code { background: none; padding: 0; }
    table { border-collapse: collapse; width: 100%; margin: 16px 0; }
    th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; }
    th { background: #f5f5f5; }
    ul, ol { padding-left: 24px; }
    a { color: #0366d6; }
  </style>
</head>
<body>
EOF

  echo "$NAV_BAR" | sed 's/\\&larr;/\&larr;/g' >> "$OUT/${filename}.html"

  # Simple markdown to HTML conversion
  echo '<div class="content">' >> "$OUT/${filename}.html"
  python3 -c "
import sys, re

with open('$md', 'r') as f:
    content = f.read()

# Convert markdown to basic HTML
lines = content.split('\n')
html_lines = []
in_code = False
in_list = False
in_table = False

for line in lines:
    # Code blocks
    if line.startswith('\`\`\`'):
        if in_code:
            html_lines.append('</code></pre>')
            in_code = False
        else:
            html_lines.append('<pre><code>')
            in_code = True
        continue
    if in_code:
        html_lines.append(line.replace('<', '&lt;').replace('>', '&gt;'))
        continue

    # Tables
    if '|' in line and line.strip().startswith('|'):
        cells = [c.strip() for c in line.split('|')[1:-1]]
        if all(set(c) <= set('- :') for c in cells):
            continue  # separator row
        if not in_table:
            html_lines.append('<table>')
            in_table = True
        html_lines.append('<tr>' + ''.join(f'<td>{c}</td>' for c in cells) + '</tr>')
        continue
    elif in_table:
        html_lines.append('</table>')
        in_table = False

    # Headers
    if line.startswith('# '):
        html_lines.append(f'<h1>{line[2:]}</h1>')
    elif line.startswith('## '):
        html_lines.append(f'<h2>{line[3:]}</h2>')
    elif line.startswith('### '):
        html_lines.append(f'<h3>{line[4:]}</h3>')
    # List items
    elif line.startswith('- '):
        if not in_list:
            html_lines.append('<ul>')
            in_list = True
        html_lines.append(f'<li>{line[2:]}</li>')
    else:
        if in_list:
            html_lines.append('</ul>')
            in_list = False
        if line.strip():
            # Inline code
            line = re.sub(r'\x60([^\x60]+)\x60', r'<code>\1</code>', line)
            # Bold
            line = re.sub(r'\*\*([^*]+)\*\*', r'<strong>\1</strong>', line)
            html_lines.append(f'<p>{line}</p>')

if in_list:
    html_lines.append('</ul>')
if in_table:
    html_lines.append('</table>')

print('\n'.join(html_lines))
" >> "$OUT/${filename}.html"

  echo '</div></body></html>' >> "$OUT/${filename}.html"
done

cat >> "$OUT/index.html" << 'FOOTER'
</table>
</div>
</body>
</html>
FOOTER

echo "Rendered $(ls "$OUT"/*.html | wc -l) ADR pages to $OUT/"
