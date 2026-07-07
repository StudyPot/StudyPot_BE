# -*- coding: utf-8 -*-
"""StudyPot backend architecture -> draw.io with AWS (mxgraph.aws4) icons + PNG preview.

The .drawio uses real draw.io AWS icon shapes (Route 53, EC2, RDS, ElastiCache, SQS,
ELB, Users) so it renders as a proper AWS architecture diagram. The PNG/SVG preview
is an approximation (coloured tiles) because those icons are internal draw.io shapes
that cairosvg can't render — open the .drawio in draw.io to see the real icons.
"""

W, H = 1700, 1000

# AWS category colours (2023 palette)
AWSCOL = {
    "net":   "#8C4FFF",   # networking & content delivery (Route 53, ELB)
    "comp":  "#ED7100",   # compute (EC2)
    "db":    "#527FFF",   # database (RDS, ElastiCache)
    "appint":"#E7157B",   # app integration (SQS)
    "gen":   "#232F3E",   # general (users)
}

N = []
def add(**kw): N.append(kw)

# ── titles ──
add(id="t1", kind="title", x=40, y=16, w=900, h=30,
    title="StudyPot — Backend Infrastructure (AWS)")
add(id="t2", kind="subtitle", x=40, y=48, w=1200, h=20,
    title="Spring Boot 4 / Java 21 · Docker Compose on a single EC2 · Route 53 DNS · MySQL/Redis/RabbitMQ run as containers (not managed services)")

# ── AWS Cloud group (drawn first = behind) ──
add(id="cloud", kind="awsgroup", x=360, y=110, w=760, h=600,
    gricon="group_aws_cloud_alt", stroke="#232F3E", title="AWS Cloud · ap-northeast-2")

# ── clients (left, outside cloud) ──
add(id="web", kind="awsicon", x=70, y=150, w=78, h=78,
    resicon="user", fill=AWSCOL["gen"], glyph="Web",
    label="Web Client<br/>React SPA (Netlify)")
add(id="mob", kind="awsicon", x=70, y=305, w=78, h=78,
    resicon="user", fill=AWSCOL["gen"], glyph="Mob",
    label="Mobile Browser<br/>iOS Safari · JWT")

# ── Route 53 (real AWS) ──
add(id="r53", kind="awsicon", x=250, y=228, w=78, h=78,
    resicon="route_53", fill=AWSCOL["net"], glyph="R53",
    label="Route 53<br/>DNS · A record")

# ── EC2 host icon ──
add(id="ec2", kind="awsicon", x=392, y=150, w=78, h=78,
    resicon="ec2", fill=AWSCOL["comp"], glyph="EC2",
    label="EC2 instance<br/>Docker host")

# ── Docker Compose boundary (dashed box inside cloud) ──
add(id="compose", kind="dbox", x=388, y=270, w=708, h=410,
    stroke="#1BA1E2", title="Docker Compose · deploy/rumiclean")

# ── Caddy (proxy) ──
add(id="caddy", kind="awsicon", x=420, y=320, w=78, h=78,
    resicon="elastic_load_balancing", fill=AWSCOL["net"], glyph="Proxy",
    label="Caddy<br/>reverse proxy · TLS")

# ── Spring Boot API (box, the centrepiece) ──
add(id="api", kind="box", x=590, y=315, w=200, h=92,
    fill="#D5E8D4", stroke="#82B366", fontcolor="#1E4620",
    title="Spring Boot · studypot-api",
    lines=[":8080 · REST + SSE", "OAuth2 · JWT · JDBC · Flyway"])

# ── data stores (bottom row inside compose) ──
add(id="mysql", kind="awsicon", x=430, y=500, w=78, h=78,
    resicon="rds", fill=AWSCOL["db"], glyph="SQL",
    label="MySQL 8.0<br/>(container)")
add(id="redis", kind="awsicon", x=640, y=500, w=78, h=78,
    resicon="elasticache", fill=AWSCOL["db"], glyph="Redis",
    label="Redis 7<br/>cache (container)")
add(id="rabbit", kind="awsicon", x=850, y=500, w=78, h=78,
    resicon="simple_queue_service_sqs", fill=AWSCOL["appint"], glyph="MQ",
    label="RabbitMQ 4<br/>queue (container)")

# ── external APIs (right) ──
add(id="extbox", kind="dbox", x=1360, y=150, w=300, h=230,
    stroke="#D6B656", title="External APIs")
add(id="google", kind="box", x=1382, y=196, w=256, h=68,
    fill="#FFF2CC", stroke="#D6B656", fontcolor="#5A4A00",
    title="Google OAuth 2.0", lines=["openid · email · profile"])
add(id="openai", kind="box", x=1382, y=292, w=256, h=68,
    fill="#FFF2CC", stroke="#D6B656", fontcolor="#5A4A00",
    title="OpenAI API", lines=["gpt-4o-mini · curriculum/retro/chat"])

# ── CI/CD lane ──
add(id="citxt", kind="subtitle", x=40, y=742, w=400, h=20,
    title="CI / CD · deploy pipeline")
_steps = [
    ("ci0", "Push / PR", "GitHub repo"),
    ("ci1", "GitHub Actions", "gradlew check · bootJar"),
    ("ci2", "Build image", "Docker multi-stage"),
    ("ci3", "GHCR", "container registry"),
    ("ci4", "SSH deploy", "EC2 · compose up -d"),
]
_sx = 40
for sid, t, sub in _steps:
    add(id=sid, kind="box", x=_sx, y=770, w=280, h=64,
        fill="#F5F5F5", stroke="#666666", fontcolor="#333333",
        title=t, lines=[sub])
    _sx += 316

NODE = {n["id"]: n for n in N}

# edges: src, tgt, label, dashed
E = [
    ("web", "r53", "DNS", False),
    ("mob", "r53", "", False),
    ("r53", "caddy", "HTTPS 443", False),
    ("caddy", "api", ":8080", False),
    ("api", "mysql", "JDBC", False),
    ("api", "redis", "", False),
    ("api", "rabbit", "AMQP", False),
    ("api", "google", "OAuth", False),
    ("api", "openai", "HTTPS", False),
    ("ci0", "ci1", "", False),
    ("ci1", "ci2", "", False),
    ("ci2", "ci3", "", False),
    ("ci3", "ci4", "", False),
    ("ci4", "cloud", "deploy", True),
]

# ════════════════════════ draw.io (mxGraph XML) ════════════════════════
def xesc(s):
    return (s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
             .replace('"', "&quot;"))

def aws_icon_style(resicon, fill):
    return ("sketch=0;outlineConnect=0;fontColor=#232F3E;gradientColor=none;"
            f"fillColor={fill};strokeColor=#ffffff;dashed=0;verticalLabelPosition=bottom;"
            "verticalAlign=top;align=center;html=1;fontSize=11;fontStyle=0;aspect=fixed;"
            f"shape=mxgraph.aws4.resourceIcon;resIcon=mxgraph.aws4.{resicon};")

def aws_group_style(gricon, stroke):
    return ("rounded=0;outlineConnect=0;gradientColor=none;html=1;whiteSpace=wrap;fontSize=12;"
            "fontStyle=0;container=0;pointerEvents=0;collapsible=0;recursiveResize=0;"
            f"shape=mxgraph.aws4.group;grIcon=mxgraph.aws4.{gricon};"
            f"strokeColor={stroke};fillColor=none;verticalAlign=top;align=left;"
            f"spacingLeft=30;fontColor={stroke};dashed=0;")

def style_for(n):
    k = n["kind"]
    if k == "title":
        return "text;html=1;align=left;verticalAlign=middle;fontSize=21;fontStyle=1;fontColor=#1a1a1a;"
    if k == "subtitle":
        return "text;html=1;align=left;verticalAlign=middle;fontSize=12;fontColor=#5f6368;"
    if k == "awsicon":
        return aws_icon_style(n["resicon"], n["fill"])
    if k == "awsgroup":
        return aws_group_style(n["gricon"], n["stroke"])
    if k == "dbox":
        return (f"rounded=1;arcSize=4;whiteSpace=wrap;html=1;fillColor=none;strokeColor={n['stroke']};"
                f"dashed=1;dashPattern=6 4;verticalAlign=top;align=left;spacingLeft=10;spacingTop=6;"
                f"fontStyle=1;fontSize=12;fontColor={n['stroke']};")
    # box / step
    return (f"rounded=1;arcSize=12;whiteSpace=wrap;html=1;fillColor={n['fill']};strokeColor={n['stroke']};"
            f"align=center;verticalAlign=middle;fontSize=12;fontColor={n['fontcolor']};")

def box_value(n):
    parts = [f"<b>{n['title']}</b>"] if n.get("title") else []
    parts += n.get("lines", [])
    return xesc("<br/>".join(parts))

def build_drawio():
    o = ['<mxfile host="app.diagrams.net" type="device">',
         '  <diagram name="StudyPot AWS" id="studypot-aws">',
         f'    <mxGraphModel dx="1400" dy="900" grid="1" gridSize="10" guides="1" tooltips="1" '
         f'connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="{W}" pageHeight="{H}" '
         f'math="0" shadow="0">',
         '      <root>',
         '        <mxCell id="0" />',
         '        <mxCell id="1" parent="0" />']
    for n in N:
        if n["kind"] in ("title", "subtitle"):
            val = xesc(n["title"])
        elif n["kind"] == "awsicon":
            val = xesc(n["label"])
        elif n["kind"] in ("awsgroup", "dbox"):
            val = xesc(n["title"])
        else:
            val = box_value(n)
        o.append(f'        <mxCell id="{n["id"]}" value="{val}" style="{style_for(n)}" '
                 f'vertex="1" parent="1">')
        o.append(f'          <mxGeometry x="{n["x"]}" y="{n["y"]}" width="{n["w"]}" '
                 f'height="{n["h"]}" as="geometry" />')
        o.append('        </mxCell>')
    for i, (s, t, lab, dash) in enumerate(E):
        st = ("edgeStyle=orthogonalEdgeStyle;rounded=1;html=1;endArrow=block;endFill=1;"
              "strokeColor=#5f6368;fontSize=10;fontColor=#5f6368;"
              + ("dashed=1;dashPattern=6 4;strokeColor=#993556;fontColor=#993556;" if dash else ""))
        o.append(f'        <mxCell id="e{i}" value="{xesc(lab)}" style="{st}" edge="1" '
                 f'parent="1" source="{s}" target="{t}">')
        o.append('          <mxGeometry relative="1" as="geometry" />')
        o.append('        </mxCell>')
    o += ['      </root>', '    </mxGraphModel>', '  </diagram>', '</mxfile>']
    return "\n".join(o)

# ════════════════════════ SVG preview (approximation) ════════════════════════
def sesc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

def anchor(n):
    return dict(L=(n["x"], n["y"]+n["h"]/2), R=(n["x"]+n["w"], n["y"]+n["h"]/2),
                T=(n["x"]+n["w"]/2, n["y"]), B=(n["x"]+n["w"]/2, n["y"]+n["h"]),
                cx=n["x"]+n["w"]/2, cy=n["y"]+n["h"]/2)

def pick(a, b):
    dx, dy = b["cx"]-a["cx"], b["cy"]-a["cy"]
    if abs(dx) >= abs(dy):
        return (a["R"], b["L"]) if dx >= 0 else (a["L"], b["R"])
    return (a["B"], b["T"]) if dy >= 0 else (a["T"], b["B"])

def build_svg():
    P = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" '
         f'viewBox="0 0 {W} {H}" font-family="Helvetica,Arial,sans-serif">',
         f'<rect width="{W}" height="{H}" fill="#ffffff"/>',
         '<defs>'
         '<marker id="a" markerWidth="10" markerHeight="10" refX="7" refY="3.5" orient="auto" '
         'markerUnits="userSpaceOnUse"><path d="M0 0 L7 3.5 L0 7 Z" fill="#5f6368"/></marker>'
         '<marker id="ad" markerWidth="10" markerHeight="10" refX="7" refY="3.5" orient="auto" '
         'markerUnits="userSpaceOnUse"><path d="M0 0 L7 3.5 L0 7 Z" fill="#993556"/></marker>'
         '</defs>',
         '<text x="'+str(W-40)+'" y="86" font-size="12" fill="#b00" text-anchor="end">'
         'preview only — real AWS icons render in draw.io</text>']

    for s, t, lab, dash in E:
        a, b = anchor(NODE[s]), anchor(NODE[t])
        (x1, y1), (x2, y2) = pick(a, b)
        col = "#993556" if dash else "#5f6368"
        mk = "ad" if dash else "a"
        da = ' stroke-dasharray="6 4"' if dash else ""
        midx = (x1+x2)/2
        P.append(f'<path d="M {x1:.0f} {y1:.0f} H {midx:.0f} V {y2:.0f} H {x2:.0f}" fill="none" '
                 f'stroke="{col}" stroke-width="1.5"{da} marker-end="url(#{mk})"/>')
        if lab:
            P.append(f'<text x="{midx:.0f}" y="{(y1+y2)/2-4:.0f}" font-size="11" fill="{col}" '
                     f'text-anchor="middle">{sesc(lab)}</text>')

    def multiline(cx, y0, html, fs, col, weight_first=False):
        for i, ln in enumerate(html.split("<br/>")):
            w = "bold" if (weight_first and i == 0) else "normal"
            P.append(f'<text x="{cx:.0f}" y="{y0+i*15:.0f}" font-size="{fs}" font-weight="{w}" '
                     f'fill="{col}" text-anchor="middle">{sesc(ln)}</text>')

    for n in N:
        k = n["kind"]; x, y, w, h = n["x"], n["y"], n["w"], n["h"]
        if k == "title":
            P.append(f'<text x="{x}" y="{y+22}" font-size="21" font-weight="bold" fill="#1a1a1a">'
                     f'{sesc(n["title"])}</text>'); continue
        if k == "subtitle":
            P.append(f'<text x="{x}" y="{y+15}" font-size="12" fill="#5f6368">{sesc(n["title"])}</text>'); continue
        if k == "awsgroup":
            P.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="3" fill="none" '
                     f'stroke="{n["stroke"]}" stroke-width="2"/>')
            P.append(f'<text x="{x+30}" y="{y+20}" font-size="13" font-weight="bold" '
                     f'fill="{n["stroke"]}">{sesc(n["title"])}</text>')
            P.append(f'<rect x="{x+6}" y="{y+6}" width="18" height="18" rx="3" fill="{n["stroke"]}"/>')
            continue
        if k == "dbox":
            P.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="6" fill="none" '
                     f'stroke="{n["stroke"]}" stroke-width="1.6" stroke-dasharray="6 4"/>')
            P.append(f'<text x="{x+10}" y="{y+18}" font-size="12" font-weight="bold" '
                     f'fill="{n["stroke"]}">{sesc(n["title"])}</text>')
            continue
        if k == "awsicon":
            # coloured tile (icon proxy) + label below
            ts = 60; tx = x + (w-ts)/2; ty = y
            P.append(f'<rect x="{tx:.0f}" y="{ty}" width="{ts}" height="{ts}" rx="9" '
                     f'fill="{n["fill"]}"/>')
            P.append(f'<text x="{x+w/2:.0f}" y="{ty+ts/2+4:.0f}" font-size="12" font-weight="bold" '
                     f'fill="#ffffff" text-anchor="middle">{sesc(n["glyph"])}</text>')
            multiline(x+w/2, y+ts+13, n["label"], 10.5, "#232F3E", weight_first=True)
            continue
        # box / step
        P.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="9" fill="{n["fill"]}" '
                 f'stroke="{n["stroke"]}" stroke-width="1.6"/>')
        nlines = n.get("lines", []); nl = len(nlines)
        total = 19 + nl*15
        ty = y + (h-total)/2 + 14
        P.append(f'<text x="{x+w/2:.0f}" y="{ty:.0f}" font-size="13" font-weight="bold" '
                 f'fill="{n["fontcolor"]}" text-anchor="middle">{sesc(n["title"])}</text>')
        ty += 18
        for ln in nlines:
            P.append(f'<text x="{x+w/2:.0f}" y="{ty:.0f}" font-size="11" fill="{n["fontcolor"]}" '
                     f'text-anchor="middle">{sesc(ln)}</text>')
            ty += 15

    P.append('</svg>')
    return "\n".join(P)


with open("docs/StudyPot_Architecture.drawio", "w") as f:
    f.write(build_drawio())
with open("docs/StudyPot_Architecture.svg", "w") as f:
    f.write(build_svg())
print("wrote docs/StudyPot_Architecture.drawio + .svg  nodes:", len(N), "edges:", len(E))
