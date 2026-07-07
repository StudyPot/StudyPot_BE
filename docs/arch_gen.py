# -*- coding: utf-8 -*-
"""StudyPot backend infrastructure / deployment architecture -> standalone SVG (for PPT).

Renders an infra-centric view: clients -> Caddy edge -> Spring Boot API container
-> data stores (MySQL / Redis / RabbitMQ), external services (Google / OpenAI),
and the GitHub Actions -> GHCR -> Oracle Cloud VM deploy pipeline.
"""

# ── palette (reuse ERD domain colours so the deck stays consistent) ──────
C = {
    "ink":    "#1a1a1a",
    "sub":    "#5f6368",
    "line":   "#9aa0a6",
    "vmbg":   "#F4F7FA",
    "vmborder": "#B7C2CE",
    "composebg": "#FFFFFF",
    # node families (fill_header, border, soft_fill)
    "client": ("#378ADD", "#185FA5", "#E6F1FB"),  # blue
    "edge":   ("#1D9E75", "#0F6E56", "#E1F5EE"),  # teal
    "api":    ("#7F77DD", "#534AB7", "#EEEDFE"),  # purple
    "data":   ("#639922", "#3B6D11", "#EAF3DE"),  # green
    "broker": ("#D85A30", "#993C1D", "#FAECE7"),  # coral
    "ext":    ("#EF9F27", "#854F0B", "#FAEEDA"),  # amber
    "ci":     ("#D4537E", "#993556", "#FBEAF0"),  # pink
    "dns":    ("#4B62C9", "#2E3F8F", "#E7EBFA"),  # indigo (AWS Route 53)
}

W, H = 1640, 1080

def esc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

P = []
def add(s): P.append(s)

add(f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" '
    f'viewBox="0 0 {W} {H}" font-family="Helvetica,Arial,sans-serif">')
add(f'<rect width="{W}" height="{H}" fill="#ffffff"/>')

# arrow markers
add('<defs>')
add('<marker id="arr" markerWidth="11" markerHeight="11" refX="8" refY="4" '
    'orient="auto" markerUnits="userSpaceOnUse">'
    '<path d="M0 0 L8 4 L0 8 Z" fill="#5f6368"/></marker>')
add('<marker id="arrL" markerWidth="11" markerHeight="11" refX="8" refY="4" '
    'orient="auto" markerUnits="userSpaceOnUse">'
    '<path d="M0 0 L8 4 L0 8 Z" fill="#B07A2A"/></marker>')
add('</defs>')

# title
add(f'<text x="40" y="46" font-size="27" font-weight="bold" fill="{C["ink"]}">'
    f'StudyPot &#8212; Backend Infrastructure &amp; Deployment</text>')
add(f'<text x="40" y="70" font-size="14" fill="{C["sub"]}">'
    f'Spring Boot 4 / Java 21 &#183; Spring Data JDBC &#183; Docker Compose on AWS EC2 (rumiclean) &#183; Route 53 DNS</text>')


def node(x, y, w, h, fam, title, lines=None, fs_title=15, rx=9):
    head, border, soft = C[fam]
    HEADH = 30
    add(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{rx}" '
        f'fill="#ffffff" stroke="{border}" stroke-width="1.7"/>')
    add(f'<path d="M {x} {y+HEADH} L {x} {y+rx} Q {x} {y} {x+rx} {y} '
        f'L {x+w-rx} {y} Q {x+w} {y} {x+w} {y+rx} L {x+w} {y+HEADH} Z" fill="{head}"/>')
    add(f'<text x="{x+w/2:.0f}" y="{y+20}" font-size="{fs_title}" font-weight="bold" '
        f'fill="#ffffff" text-anchor="middle">{esc(title)}</text>')
    if lines:
        cy = y + HEADH + 19
        for ln in lines:
            add(f'<text x="{x+14}" y="{cy}" font-size="12.5" fill="#202124">{esc(ln)}</text>')
            cy += 18
    return (x, y, w, h)


def label_box(x, y, w, h, text, fill, border, fs=13):
    add(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="7" fill="{fill}" '
        f'stroke="{border}" stroke-width="1.4"/>')
    add(f'<text x="{x+w/2:.0f}" y="{y+h/2+fs*0.36:.0f}" font-size="{fs}" '
        f'font-weight="bold" fill="{border}" text-anchor="middle">{esc(text)}</text>')


def arrow(x1, y1, x2, y2, color="#5f6368", marker="arr", dash=None, wdt=1.7):
    d = f' stroke-dasharray="{dash}"' if dash else ""
    add(f'<line x1="{x1:.0f}" y1="{y1:.0f}" x2="{x2:.0f}" y2="{y2:.0f}" '
        f'stroke="{color}" stroke-width="{wdt}"{d} marker-end="url(#{marker})"/>')


def elbow(x1, y1, x2, y2, color="#5f6368", marker="arr", wdt=1.7, dash=None):
    midx = (x1 + x2) / 2
    d = f' stroke-dasharray="{dash}"' if dash else ""
    add(f'<path d="M {x1:.0f} {y1:.0f} H {midx:.0f} V {y2:.0f} H {x2:.0f}" '
        f'fill="none" stroke="{color}" stroke-width="{wdt}"{d} marker-end="url(#{marker})"/>')


def edgetext(x, y, t, anchor="middle", color="#5f6368", fs=11.5):
    add(f'<text x="{x:.0f}" y="{y:.0f}" font-size="{fs}" fill="{color}" '
        f'text-anchor="{anchor}">{esc(t)}</text>')


# ───────── CLIENTS (top) ─────────
cb = node(40, 110, 200, 92, "client", "Web Client",
          ["React SPA (Netlify)", "cookie + Bearer"])
cm = node(40, 230, 200, 92, "client", "Mobile Browser",
          ["iOS Safari / cross-site", "Bearer JWT"])

# ───────── DNS (Route 53) ─────────
r53 = node(250, 156, 96, 120, "dns", "Route 53",
           ["DNS", "A record", "studypot.", "rumiclean.", "com"], fs_title=14)

# ───────── EXTERNAL SERVICES (right) ─────────
ext_x = 1330
g = node(ext_x, 150, 250, 86, "ext", "Google OAuth 2.0",
         ["openid · email · profile"])
oai = node(ext_x, 270, 250, 86, "ext", "OpenAI API",
           ["gpt-4o-mini · /responses", "curriculum · retro · chat"])

# ───────── ORACLE CLOUD VM boundary ─────────
vm_x, vm_y, vm_w, vm_h = 360, 110, 930, 600
add(f'<rect x="{vm_x}" y="{vm_y}" width="{vm_w}" height="{vm_h}" rx="16" '
    f'fill="{C["vmbg"]}" stroke="{C["vmborder"]}" stroke-width="2" stroke-dasharray="2 5"/>')
add(f'<text x="{vm_x+20}" y="{vm_y+27}" font-size="15" font-weight="bold" '
    f'fill="#56616E">AWS EC2 &#183; Docker Compose (deploy/rumiclean)</text>')

# Caddy edge
caddy = node(vm_x+30, vm_y+55, 230, 118, "edge", "Caddy (reverse proxy)",
             ["studypot.rumiclean.com", "TLS / Let's Encrypt", "gzip — except SSE stream",
              "→ studypot-api:8080"])

# API container (with internal layers)
api_x, api_y, api_w, api_h = vm_x+320, vm_y+55, 300, 380
ah, ab, asoft = C["api"]
add(f'<rect x="{api_x}" y="{api_y}" width="{api_w}" height="{api_h}" rx="11" '
    f'fill="#ffffff" stroke="{ab}" stroke-width="2"/>')
add(f'<path d="M {api_x} {api_y+32} L {api_x} {api_y+11} Q {api_x} {api_y} {api_x+11} {api_y} '
    f'L {api_x+api_w-11} {api_y} Q {api_x+api_w} {api_y} {api_x+api_w} {api_y+11} '
    f'L {api_x+api_w} {api_y+32} Z" fill="{ah}"/>')
add(f'<text x="{api_x+api_w/2:.0f}" y="{api_y+21}" font-size="15" font-weight="bold" '
    f'fill="#ffffff" text-anchor="middle">studypot-api  ·  :8080</text>')

# inner layer chips
iy = api_y + 46
inner = [
    ("Security  —  OAuth2 login · JWT · cookie+Bearer", asoft),
    ("Controllers  (REST · SSE stream)", asoft),
    ("Services  (domain logic · @Async)", asoft),
    ("Repositories  —  Spring Data JDBC", asoft),
    ("Schedulers  —  week / report / retro", asoft),
    ("RabbitMQ worker + in-process events", asoft),
    ("LLM client  →  OpenAI", asoft),
    ("Flyway migrations  (V1–V12)", asoft),
]
for txt, fill in inner:
    add(f'<rect x="{api_x+14}" y="{iy}" width="{api_w-28}" height="32" rx="6" '
        f'fill="{fill}" stroke="{ab}" stroke-width="1"/>')
    add(f'<text x="{api_x+26}" y="{iy+21}" font-size="12.3" fill="#26215C">{esc(txt)}</text>')
    iy += 40

# Data stores row (bottom of VM)
ds_y = vm_y + 470
mysql = node(vm_x+30, ds_y, 250, 96, "data", "MySQL 8.0",
             ["studypot-mysql:3306", "UUIDv7 BINARY(16) · JSON"])
redis = node(vm_x+300, ds_y, 250, 96, "broker", "Redis 7",
             ["studypot-redis:6379", "rate-limit · cache (LRU)"])
rabbit = node(vm_x+570, ds_y, 250, 96, "broker", "RabbitMQ 4",
              ["studypot-rabbitmq:5672", "notification.events queue"])

# ───────── CI/CD pipeline (bottom band) ─────────
ci_y = 770
add(f'<text x="40" y="{ci_y-12}" font-size="15" font-weight="bold" fill="#993556">'
    f'CI / CD  &#183;  deploy pipeline</text>')
ch, cbd, csoft = C["ci"]
steps = [
    ("Push / PR", "GitHub repo"),
    ("GitHub Actions", "gradlew check · bootJar"),
    ("Build image", "Docker multi-stage"),
    ("GHCR", "container registry"),
    ("SSH deploy", "AWS EC2 · compose up"),
]
sx, sw, sgap = 40, 280, 36
sy = ci_y
for i, (t, sub) in enumerate(steps):
    x = sx + i * (sw + sgap)
    label_box(x, sy, sw, 64, t, csoft, cbd, fs=15)
    add(f'<text x="{x+sw/2:.0f}" y="{sy+50}" font-size="11.5" fill="#7a3b54" '
        f'text-anchor="middle">{esc(sub)}</text>')
    if i < len(steps) - 1:
        arrow(x+sw, sy+32, x+sw+sgap-3, sy+32, color=cbd, marker="arrL", wdt=2)

# ───────── EDGES ─────────
# clients -> Route 53 (DNS resolve) -> caddy
arrow(cb[0]+cb[2], cb[1]+cb[3]/2, r53[0], r53[1]+44)
arrow(cm[0]+cm[2], cm[1]+cm[3]/2, r53[0], r53[1]+78)
arrow(r53[0]+r53[2], r53[1]+r53[3]/2, caddy[0], caddy[1]+caddy[3]/2)
edgetext((cb[0]+cb[2]+r53[0])/2, cb[1]+cb[3]/2-8, "DNS")
edgetext((r53[0]+r53[2]+caddy[0])/2, r53[1]+r53[3]/2-8, "HTTPS 443")

# caddy -> api
arrow(caddy[0]+caddy[2], caddy[1]+caddy[3]/2, api_x, api_y+60)
edgetext((caddy[0]+caddy[2]+api_x)/2, caddy[1]+caddy[3]/2-8, ":8080")

# api -> data stores
arrow(api_x+60, api_y+api_h, mysql[0]+mysql[2]/2, mysql[1], wdt=1.6)
arrow(api_x+api_w/2, api_y+api_h, redis[0]+redis[2]/2, redis[1], wdt=1.6)
arrow(api_x+api_w-50, api_y+api_h, rabbit[0]+rabbit[2]/2, rabbit[1], wdt=1.6)
edgetext(mysql[0]+mysql[2]/2-30, api_y+api_h+30, "JDBC", color="#3B6D11")
edgetext(rabbit[0]+rabbit[2]/2+30, api_y+api_h+30, "publish/consume", color="#993C1D")

# api -> external (right)
arrow(api_x+api_w, api_y+60, g[0], g[1]+g[3]/2)
arrow(api_x+api_w, api_y+150, oai[0], oai[1]+oai[3]/2)
edgetext((api_x+api_w+g[0])/2, api_y+52, "OAuth", color="#854F0B")
edgetext((api_x+api_w+oai[0])/2, api_y+142, "HTTPS", color="#854F0B")

# SSE note from api back to clients
add(f'<path d="M {api_x} {api_y+90} H {caddy[0]+caddy[2]/2} V {ci_y-60}" '
    f'fill="none" stroke="{C["edge"][1]}" stroke-width="1.4" stroke-dasharray="5 4"/>')
edgetext(caddy[0]+caddy[2]/2, api_y+82, "SSE notifications", color="#0F6E56")

# CI/CD -> VM (deploy target)
elbow(steps_x := sx + 4*(sw+sgap) + sw/2, ci_y, vm_x+vm_w/2, vm_y+vm_h+6,
      color=cbd, marker="arrL", wdt=2, dash="6 4")
edgetext(vm_x+vm_w/2+120, vm_y+vm_h+24, "docker compose up -d", color="#993556")

# ───────── LEGEND ─────────
lg_y = H - 30
add(f'<text x="40" y="{lg_y}" font-size="12.5" fill="#444">Legend:</text>')
lx = 110
for fam, lab in [("client","Client"),("dns","Route 53 (DNS)"),("edge","Edge / proxy"),
                 ("api","App (Spring Boot)"),("data","Database"),("broker","Cache / broker"),
                 ("ext","External API"),("ci","CI/CD")]:
    head = C[fam][0]
    add(f'<rect x="{lx}" y="{lg_y-12}" width="14" height="14" rx="3" fill="{head}"/>')
    add(f'<text x="{lx+20}" y="{lg_y}" font-size="12.5" fill="#444">{lab}</text>')
    lx += 40 + len(lab) * 7.0 + 24

add('</svg>')

with open("docs/StudyPot_Architecture.svg", "w") as f:
    f.write("\n".join(P))
print("wrote docs/StudyPot_Architecture.svg", W, "x", H)
