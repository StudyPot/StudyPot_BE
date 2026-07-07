# -*- coding: utf-8 -*-
"""StudyPot backend architecture -> draw.io (.drawio / mxGraph XML) + PNG preview.

One data model emits two artifacts:
  - docs/StudyPot_Architecture.drawio   (open & edit in draw.io / diagrams.net)
  - docs/StudyPot_Architecture.svg/png  (inline preview, draw.io-styled)

Infra-centric: Clients -> Route 53 -> Caddy (AWS EC2 / Docker Compose) -> API
-> MySQL / Redis / RabbitMQ, external Google & OpenAI, GitHub Actions deploy.
"""

W, H = 1640, 1010

# palette: (fill, stroke, fontColor)
PAL = {
    "client": ("#DAE8FC", "#6C8EBF", "#0A2A50"),
    "dns":    ("#FFE6CC", "#D79B00", "#5A3B00"),
    "edge":   ("#D5E8D4", "#82B366", "#1E4620"),
    "api":    ("#E1D5E7", "#9673A6", "#3A2247"),
    "row":    ("#FFFFFF", "#9673A6", "#3A2247"),
    "db":     ("#D5E8D4", "#82B366", "#1E4620"),
    "broker": ("#F8CECC", "#B85450", "#5A1A18"),
    "ext":    ("#FFF2CC", "#D6B656", "#5A4A00"),
    "group":  ("#F5F7FA", "#7A8794", "#56616E"),
    "step":   ("#F5F5F5", "#666666", "#333333"),
}

# Each node: id, kind, x, y, w, h, pal, title, lines
N = []
def add_node(nid, kind, x, y, w, h, pal, title="", lines=None):
    N.append(dict(id=nid, kind=kind, x=x, y=y, w=w, h=h, pal=pal,
                  title=title, lines=lines or []))

# ── titles ──
add_node("t1", "title", 40, 16, 900, 30, None,
         "StudyPot — Backend Infrastructure & Deployment", [])
add_node("t2", "subtitle", 40, 48, 1100, 20, None,
         "Spring Boot 4 / Java 21 · Spring Data JDBC · Docker Compose on AWS EC2 (rumiclean) · Route 53 DNS", [])

# ── EC2 group (drawn first = behind) ──
add_node("ec2", "group", 360, 96, 930, 610, "group",
         "AWS EC2 · Docker Compose (deploy/rumiclean)", [])

# ── clients ──
add_node("web", "box", 40, 110, 200, 92, "client", "Web Client",
         ["React SPA (Netlify)", "cookie + Bearer"])
add_node("mob", "box", 40, 230, 200, 92, "client", "Mobile Browser",
         ["iOS Safari / cross-site", "Bearer JWT"])

# ── Route 53 ──
add_node("r53", "box", 250, 150, 96, 132, "dns", "Route 53",
         ["(DNS)", "A record", "studypot.", "rumiclean.com"])

# ── Caddy ──
add_node("caddy", "box", 390, 150, 230, 122, "edge", "Caddy (reverse proxy)",
         ["studypot.rumiclean.com", "TLS / Let's Encrypt", "gzip — except SSE", "→ studypot-api:8080"])

# ── API frame + inner rows ──
add_node("api", "apiframe", 680, 150, 300, 350, "api", "studypot-api  ·  :8080", [])
_rows = [
    "Security — OAuth2 · JWT · cookie+Bearer",
    "Controllers (REST · SSE stream)",
    "Services (domain logic · @Async)",
    "Repositories — Spring Data JDBC",
    "Schedulers — week / report / retro",
    "RabbitMQ worker + in-process events",
    "LLM client → OpenAI",
    "Flyway migrations (V1–V12)",
]
_ry = 196
for i, txt in enumerate(_rows):
    add_node(f"row{i}", "row", 694, _ry, 272, 30, "row", "", [txt])
    _ry += 38

# ── data stores ──
add_node("mysql", "box", 390, 596, 250, 92, "db", "MySQL 8.0",
         ["studypot-mysql:3306", "UUIDv7 BINARY(16) · JSON"])
add_node("redis", "box", 660, 596, 250, 92, "broker", "Redis 7",
         ["studypot-redis:6379", "rate-limit · cache (LRU)"])
add_node("rabbit", "box", 930, 596, 250, 92, "broker", "RabbitMQ 4",
         ["studypot-rabbitmq:5672", "notification.events queue"])

# ── external services ──
add_node("google", "box", 1340, 150, 250, 86, "ext", "Google OAuth 2.0",
         ["openid · email · profile"])
add_node("openai", "box", 1340, 270, 250, 86, "ext", "OpenAI API",
         ["gpt-4o-mini · /responses", "curriculum · retro · chat"])

# ── CI/CD ──
add_node("citxt", "subtitle", 40, 742, 400, 20, None, "CI / CD · deploy pipeline", [])
_steps = [
    ("ci0", "Push / PR", "GitHub repo"),
    ("ci1", "GitHub Actions", "gradlew check · bootJar"),
    ("ci2", "Build image", "Docker multi-stage"),
    ("ci3", "GHCR", "container registry"),
    ("ci4", "SSH deploy", "AWS EC2 · compose up"),
]
_sx = 40
for sid, t, sub in _steps:
    add_node(sid, "box", _sx, 770, 280, 64, "step", t, [sub])
    _sx += 280 + 36

# ── edges: src, tgt, label, dashed ──
E = [
    ("web", "r53", "DNS", False),
    ("mob", "r53", "", False),
    ("r53", "caddy", "HTTPS 443", False),
    ("caddy", "api", ":8080", False),
    ("api", "mysql", "JDBC", False),
    ("api", "redis", "", False),
    ("api", "rabbit", "publish/consume", False),
    ("api", "google", "OAuth", False),
    ("api", "openai", "HTTPS", False),
    ("ci0", "ci1", "", False),
    ("ci1", "ci2", "", False),
    ("ci2", "ci3", "", False),
    ("ci3", "ci4", "", False),
    ("ci4", "ec2", "compose up -d", True),
]

NODE = {n["id"]: n for n in N}

# ════════════════════════ draw.io (mxGraph XML) ════════════════════════
def xesc(s):
    return (s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
             .replace('"', "&quot;"))

def html_label(n):
    # raw HTML; the whole string is XML-attribute-escaped at the call site
    parts = []
    if n["title"]:
        parts.append(f"<b>{n['title']}</b>")
    for ln in n["lines"]:
        parts.append(ln)
    return "<br/>".join(parts)

def style_for(n):
    k = n["kind"]
    if k == "title":
        return "text;html=1;align=left;verticalAlign=middle;fontSize=21;fontStyle=1;fontColor=#1a1a1a;"
    if k == "subtitle":
        return "text;html=1;align=left;verticalAlign=middle;fontSize=13;fontColor=#5f6368;"
    fill, stroke, fc = PAL[n["pal"]]
    if k == "group":
        return (f"rounded=1;arcSize=4;whiteSpace=wrap;html=1;fillColor={fill};strokeColor={stroke};"
                f"dashed=1;dashPattern=2 6;verticalAlign=top;align=left;spacingLeft=14;spacingTop=8;"
                f"fontStyle=1;fontSize=13;fontColor={fc};")
    if k == "apiframe":
        return (f"rounded=1;arcSize=6;whiteSpace=wrap;html=1;fillColor={fill};strokeColor={stroke};"
                f"verticalAlign=top;align=center;spacingTop=8;fontStyle=1;fontSize=14;fontColor={fc};")
    if k == "row":
        return (f"rounded=1;arcSize=20;whiteSpace=wrap;html=1;fillColor={fill};strokeColor={stroke};"
                f"align=left;spacingLeft=10;fontSize=11;fontColor={fc};")
    # plain box
    return (f"rounded=1;arcSize=10;whiteSpace=wrap;html=1;fillColor={fill};strokeColor={stroke};"
            f"align=center;verticalAlign=middle;fontSize=12;fontColor={fc};"
            f"{'verticalAlign=top;spacingTop=6;' if k=='box' and n['lines'] and n['title'] else ''}")

def build_drawio():
    out = []
    out.append('<mxfile host="app.diagrams.net" type="device">')
    out.append('  <diagram name="StudyPot Architecture" id="studypot-arch">')
    out.append(f'    <mxGraphModel dx="1200" dy="800" grid="1" gridSize="10" guides="1" '
               f'tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" '
               f'pageWidth="{W}" pageHeight="{H}" math="0" shadow="0">')
    out.append('      <root>')
    out.append('        <mxCell id="0" />')
    out.append('        <mxCell id="1" parent="0" />')
    for n in N:
        style = style_for(n)
        val = xesc(html_label(n)) if n["kind"] not in ("title", "subtitle") else xesc(
            (n["title"] + (" " + " ".join(n["lines"]) if n["lines"] else "")))
        out.append(f'        <mxCell id="{n["id"]}" value="{val}" style="{style}" '
                   f'vertex="1" parent="1">')
        out.append(f'          <mxGeometry x="{n["x"]}" y="{n["y"]}" width="{n["w"]}" '
                   f'height="{n["h"]}" as="geometry" />')
        out.append('        </mxCell>')
    for i, (s, t, lab, dash) in enumerate(E):
        estyle = ("edgeStyle=orthogonalEdgeStyle;rounded=1;html=1;endArrow=block;"
                  "endFill=1;strokeColor=#5f6368;fontSize=10;fontColor=#5f6368;"
                  + ("dashed=1;dashPattern=6 4;strokeColor=#993556;fontColor=#993556;" if dash else ""))
        out.append(f'        <mxCell id="e{i}" value="{xesc(lab)}" style="{estyle}" '
                   f'edge="1" parent="1" source="{s}" target="{t}">')
        out.append('          <mxGeometry relative="1" as="geometry" />')
        out.append('        </mxCell>')
    out.append('      </root>')
    out.append('    </mxGraphModel>')
    out.append('  </diagram>')
    out.append('</mxfile>')
    return "\n".join(out)

# ════════════════════════ SVG preview (draw.io look) ════════════════════════
def sesc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

def anchor(n):
    return dict(L=(n["x"], n["y"]+n["h"]/2), R=(n["x"]+n["w"], n["y"]+n["h"]/2),
                T=(n["x"]+n["w"]/2, n["y"]), B=(n["x"]+n["w"]/2, n["y"]+n["h"]),
                cx=n["x"]+n["w"]/2, cy=n["y"]+n["h"]/2)

def pick(a, b):
    dx = b["cx"] - a["cx"]; dy = b["cy"] - a["cy"]
    if abs(dx) >= abs(dy):
        return (a["R"], b["L"]) if dx >= 0 else (a["L"], b["R"])
    return (a["B"], b["T"]) if dy >= 0 else (a["T"], b["B"])

def build_svg():
    P = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" '
         f'viewBox="0 0 {W} {H}" font-family="Helvetica,Arial,sans-serif">',
         f'<rect width="{W}" height="{H}" fill="#ffffff"/>',
         '<defs>'
         '<marker id="a" markerWidth="10" markerHeight="10" refX="7" refY="3.5" '
         'orient="auto" markerUnits="userSpaceOnUse">'
         '<path d="M0 0 L7 3.5 L0 7 Z" fill="#5f6368"/></marker>'
         '<marker id="ad" markerWidth="10" markerHeight="10" refX="7" refY="3.5" '
         'orient="auto" markerUnits="userSpaceOnUse">'
         '<path d="M0 0 L7 3.5 L0 7 Z" fill="#993556"/></marker>'
         '</defs>']

    # edges first
    for s, t, lab, dash in E:
        a, b = anchor(NODE[s]), anchor(NODE[t])
        (x1, y1), (x2, y2) = pick(a, b)
        col = "#993556" if dash else "#5f6368"
        mk = "ad" if dash else "a"
        da = ' stroke-dasharray="6 4"' if dash else ""
        midx = (x1 + x2) / 2
        P.append(f'<path d="M {x1:.0f} {y1:.0f} H {midx:.0f} V {y2:.0f} H {x2:.0f}" '
                 f'fill="none" stroke="{col}" stroke-width="1.5"{da} marker-end="url(#{mk})"/>')
        if lab:
            P.append(f'<text x="{midx:.0f}" y="{(y1+y2)/2-4:.0f}" font-size="11" '
                     f'fill="{col}" text-anchor="middle">{sesc(lab)}</text>')

    # nodes
    for n in N:
        k = n["kind"]; x, y, w, h = n["x"], n["y"], n["w"], n["h"]
        if k == "title":
            P.append(f'<text x="{x}" y="{y+22}" font-size="21" font-weight="bold" '
                     f'fill="#1a1a1a">{sesc(n["title"])}</text>'); continue
        if k == "subtitle":
            P.append(f'<text x="{x}" y="{y+15}" font-size="13" fill="#5f6368">'
                     f'{sesc(n["title"])}</text>'); continue
        fill, stroke, fc = PAL[n["pal"]]
        rx = 4 if k == "group" else 8
        da = ' stroke-dasharray="2 6"' if k == "group" else ""
        P.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{rx}" fill="{fill}" '
                 f'stroke="{stroke}" stroke-width="1.6"{da}/>')
        if k == "group":
            P.append(f'<text x="{x+14}" y="{y+22}" font-size="13" font-weight="bold" '
                     f'fill="{fc}">{sesc(n["title"])}</text>'); continue
        if k == "row":
            P.append(f'<text x="{x+10}" y="{y+h/2+4:.0f}" font-size="11" fill="{fc}">'
                     f'{sesc(n["lines"][0])}</text>'); continue
        if k == "apiframe":
            P.append(f'<text x="{x+w/2:.0f}" y="{y+21}" font-size="14" font-weight="bold" '
                     f'fill="{fc}" text-anchor="middle">{sesc(n["title"])}</text>'); continue
        # plain box: title bold + lines, vertically centered block
        nl = len(n["lines"]); lh = 17
        total = (1 if n["title"] else 0) * 19 + nl * lh
        ty = y + (h - total) / 2 + 14
        if n["title"]:
            P.append(f'<text x="{x+w/2:.0f}" y="{ty:.0f}" font-size="13.5" font-weight="bold" '
                     f'fill="{fc}" text-anchor="middle">{sesc(n["title"])}</text>')
            ty += 19
        for ln in n["lines"]:
            P.append(f'<text x="{x+w/2:.0f}" y="{ty:.0f}" font-size="11.5" fill="{fc}" '
                     f'text-anchor="middle">{sesc(ln)}</text>')
            ty += lh

    # legend
    ly = H - 22; lx = 40
    P.append(f'<text x="{lx}" y="{ly}" font-size="12" fill="#444">Legend:</text>'); lx += 64
    for key, lab in [("client","Client"),("dns","Route 53"),("edge","Edge / proxy"),
                     ("api","App (Spring Boot)"),("db","Database"),("broker","Cache / broker"),
                     ("ext","External API"),("step","CI/CD")]:
        fill, stroke, _ = PAL[key]
        P.append(f'<rect x="{lx}" y="{ly-11}" width="14" height="14" rx="3" fill="{fill}" '
                 f'stroke="{stroke}"/>')
        P.append(f'<text x="{lx+20}" y="{ly}" font-size="12" fill="#444">{lab}</text>')
        lx += 40 + len(lab) * 6.8 + 22
    P.append('</svg>')
    return "\n".join(P)


with open("docs/StudyPot_Architecture.drawio", "w") as f:
    f.write(build_drawio())
with open("docs/StudyPot_Architecture.svg", "w") as f:
    f.write(build_svg())
print("wrote docs/StudyPot_Architecture.drawio + .svg  nodes:", len(N), "edges:", len(E))
