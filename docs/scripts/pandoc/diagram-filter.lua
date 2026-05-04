-- Pandoc Lua filter for Mermaid and PlantUML diagrams.
--
-- Fenced code blocks with language "mermaid" are rendered client-side
-- via mermaid.js (embedded as <pre class="mermaid">).
--
-- Fenced code blocks with language "plantuml" are rendered to SVG
-- using the PlantUML server (default: http://localhost:8180) or the
-- plantuml CLI, depending on availability.

local plantuml_server = os.getenv("PLANTUML_SERVER") or "http://localhost:8180"

--- Convert a Mermaid code block to a client-side rendered <pre>.
local function mermaid_block(code)
  return pandoc.RawBlock("html",
    '<pre class="mermaid">\n' .. code .. '\n</pre>')
end

--- Try to render PlantUML via the PlantUML server HTTP API.
--- Falls back to a styled <pre> block if the server is unreachable.
local function plantuml_block(code)
  local encoded = code
  -- Use plantuml CLI if available, otherwise fall back to plain display
  local tmpfile = os.tmpname()
  local infile = tmpfile .. ".puml"
  local outfile = tmpfile .. ".svg"

  local f = io.open(infile, "w")
  if f then
    f:write(encoded)
    f:close()

    local cmd = string.format("plantuml -tsvg -pipe < %s > %s 2>/dev/null", infile, outfile)
    local ok = os.execute(cmd)

    if ok then
      local svg = io.open(outfile, "r")
      if svg then
        local content = svg:read("*a")
        svg:close()
        os.remove(infile)
        os.remove(outfile)
        os.remove(tmpfile)
        if content and #content > 0 then
          return pandoc.RawBlock("html",
            '<div class="plantuml-diagram">\n' .. content .. '\n</div>')
        end
      end
    end

    os.remove(infile)
    os.remove(outfile)
  end
  os.remove(tmpfile)

  -- Fallback: render as code block with a note
  return pandoc.RawBlock("html",
    '<div class="plantuml-fallback">'
    .. '<p><em>PlantUML-Diagramm (plantuml CLI nicht verfuegbar):</em></p>'
    .. '<pre><code class="language-plantuml">'
    .. code:gsub("<", "&lt;"):gsub(">", "&gt;")
    .. '</code></pre></div>')
end

--- Pandoc filter: process CodeBlock elements.
function CodeBlock(block)
  local classes = block.classes
  if classes:includes("mermaid") then
    return mermaid_block(block.text)
  elseif classes:includes("plantuml") then
    return plantuml_block(block.text)
  end
  -- All other code blocks: let pandoc handle syntax highlighting
  return nil
end
