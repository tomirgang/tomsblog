document.querySelectorAll('pre > code.language-mermaid').forEach(function(codeEl) {
    var preEl = codeEl.parentElement;
    preEl.className = 'mermaid';
    preEl.textContent = codeEl.textContent;
});
hljs.highlightAll();
