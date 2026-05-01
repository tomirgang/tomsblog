#!/bin/bash
# Injects a navigation bar into all Doorstop-generated HTML files.
# Usage: ./inject-nav.sh <directory>

DIR="${1:-.}"

NAV_HTML='<div style="background:#343a40;padding:8px 16px;font-size:14px;font-family:sans-serif"><a href="../index.html" style="color:#fff;text-decoration:none;margin-right:16px">\&larr; Dokumentation<\/a><a href="../tomsblog-arc42/main/index.html" style="color:#fff;text-decoration:none;margin-right:16px">Architektur (arc42)<\/a><a href="../tomsblog-design/main/index.html" style="color:#fff;text-decoration:none;margin-right:16px">Software Detail Design<\/a><\/div>'

find "$DIR" -name "*.html" -exec sed -i "s|<body>|<body>\n${NAV_HTML}|" {} \;
