# -*- coding: utf-8 -*-
"""StudyPot ERD generator -> standalone SVG (for PPT)."""

# Domain palette (fill_header, border, text_dark, row_alt)
DOM = {
    "auth":   ("#378ADD", "#185FA5", "#042C53", "#E6F1FB"),  # blue
    "group":  ("#1D9E75", "#0F6E56", "#04342C", "#E1F5EE"),  # teal
    "curri":  ("#639922", "#3B6D11", "#173404", "#EAF3DE"),  # green
    "ai":     ("#7F77DD", "#534AB7", "#26215C", "#EEEDFE"),  # purple
    "board":  ("#EF9F27", "#854F0B", "#412402", "#FAEEDA"),  # amber
    "social": ("#D4537E", "#993556", "#4B1528", "#FBEAF0"),  # pink
    "notif":  ("#D85A30", "#993C1D", "#4A1B0C", "#FAECE7"),  # coral
}

# table: (domain, x, y, [ (field, kind) ])  kind: 'pk','fk',''
T = {
 # ----- AUTH -----
 "users": ("auth", 40, 60, [
    ("id", "pk"), ("email", ""), ("nickname", ""), ("skill_level", ""),
    ("interests (json)", ""), ("last_login_at", ""), ("created_at", "")]),
 "oauth_account": ("auth", 40, 320, [
    ("id", "pk"), ("user_id", "fk"), ("provider", ""),
    ("provider_user_id", ""), ("token_expires_at", "")]),
 "refresh_token": ("auth", 40, 540, [
    ("id", "pk"), ("user_id", "fk"), ("token_hash", ""),
    ("expires_at", ""), ("revoked_at", "")]),

 # ----- SOCIAL -----
 "user_follow": ("social", 40, 740, [
    ("id", "pk"), ("follower_user_id", "fk"), ("followee_user_id", "fk")]),
 "group_bookmark": ("social", 40, 900, [
    ("id", "pk"), ("user_id", "fk"), ("group_id", "fk")]),
 "group_review": ("social", 40, 1060, [
    ("id", "pk"), ("group_id", "fk"), ("member_id", "fk"),
    ("user_id", "fk"), ("rating", ""), ("content", "")]),

 # ----- STUDY GROUP -----
 "study_group": ("group", 360, 60, [
    ("id", "pk"), ("created_by", "fk"), ("name", ""), ("topic", ""),
    ("status", ""), ("max_members", ""), ("invite_code", ""),
    ("starts_at / ends_at", "")]),
 "group_member": ("group", 360, 360, [
    ("id", "pk"), ("group_id", "fk"), ("user_id", "fk"),
    ("permission", ""), ("status", ""), ("display_name", "")]),
 "group_rule": ("group", 360, 600, [
    ("id", "pk"), ("group_id", "fk"), ("created_by", "fk"),
    ("rule_type", ""), ("config (json)", ""), ("is_active", "")]),
 "group_onboarding_response": ("group", 360, 840, [
    ("id", "pk"), ("group_id", "fk"), ("member_id", "fk"),
    ("keyword_skill_levels", ""), ("task_preferences", ""), ("status", "")]),
 "member_availability_slot": ("group", 360, 1080, [
    ("id", "pk"), ("onboarding_response_id", "fk"), ("member_id", "fk"),
    ("day_of_week", ""), ("start_time / end_time", "")]),

 # ----- BOARD -----
 "group_board": ("board", 700, 60, [
    ("id", "pk"), ("group_id", "fk"), ("board_type", ""),
    ("name", ""), ("display_order", "")]),
 "group_board_post": ("board", 700, 280, [
    ("id", "pk"), ("board_id", "fk"), ("group_id", "fk"),
    ("author_member_id", "fk"), ("title", ""), ("status", "")]),
 "group_board_comment": ("board", 700, 520, [
    ("id", "pk"), ("post_id", "fk"), ("group_id", "fk"),
    ("author_member_id", "fk"), ("content", ""), ("status", "")]),

 # ----- CURRICULUM / PROGRESS -----
 "curriculum": ("curri", 1040, 60, [
    ("id", "pk"), ("group_id", "fk"), ("llm_usage_id", "fk"),
    ("title", ""), ("total_weeks", ""), ("status", "")]),
 "curriculum_week": ("curri", 1040, 300, [
    ("id", "pk"), ("curriculum_id", "fk"), ("week_number", ""),
    ("title", ""), ("status", ""), ("starts_at / ends_at", "")]),
 "weekly_task": ("curri", 1040, 540, [
    ("id", "pk"), ("curriculum_week_id", "fk"), ("display_order", ""),
    ("task_type", ""), ("title", ""), ("required", "")]),
 "member_week_progress": ("curri", 1040, 780, [
    ("id", "pk"), ("curriculum_week_id", "fk"), ("member_id", "fk"),
    ("status", ""), ("completed_at", ""), ("incomplete_reason", "")]),
 "task_completion": ("curri", 1040, 1040, [
    ("id", "pk"), ("progress_id", "fk"), ("weekly_task_id", "fk"),
    ("member_id", "fk"), ("status", ""), ("evidence_url", "")]),
 "rule_violation": ("curri", 1040, 1300, [
    ("id", "pk"), ("rule_id", "fk"), ("member_id", "fk"),
    ("task_completion_id", "fk"), ("status", ""), ("occurred_at", "")]),

 # ----- AI / LLM -----
 "llm_usage": ("ai", 1400, 60, [
    ("id", "pk"), ("user_id", "fk"), ("group_id", "fk"),
    ("purpose", ""), ("provider", ""), ("model", ""),
    ("input/output_tokens", ""), ("status", "")]),
 "retrospective": ("ai", 1400, 360, [
    ("id", "pk"), ("progress_id", "fk"), ("curriculum_week_id", "fk"),
    ("member_id", "fk"), ("llm_usage_id", "fk"), ("ai_feedback", ""),
    ("next_week_adjustment", ""), ("status", "")]),
 "ai_conversation": ("ai", 1400, 680, [
    ("id", "pk"), ("group_id", "fk"), ("member_id", "fk"),
    ("curriculum_week_id", "fk"), ("retrospective_id", "fk"),
    ("conversation_type", ""), ("status", "")]),
 "ai_conversation_message": ("ai", 1400, 960, [
    ("id", "pk"), ("conversation_id", "fk"), ("llm_usage_id", "fk"),
    ("sender_type", ""), ("content", "")]),
 "notification": ("notif", 1400, 1160, [
    ("id", "pk"), ("group_id", "fk"), ("recipient_user_id", "fk"),
    ("related_onboarding_response_id", "fk"), ("related_week_id", "fk"),
    ("related_task_completion_id", "fk"), ("related_retrospective_id", "fk"),
    ("notification_type", ""), ("status", "")]),
}

# FK relationships: (child_table, child_field_index_label, parent_table)
FK = [
 ("oauth_account", "users"), ("refresh_token", "users"),
 ("user_follow", "users"), ("user_follow", "users"),
 ("group_bookmark", "users"), ("group_bookmark", "study_group"),
 ("group_review", "study_group"), ("group_review", "group_member"), ("group_review", "users"),
 ("study_group", "users"),
 ("group_member", "study_group"), ("group_member", "users"),
 ("group_rule", "study_group"), ("group_rule", "users"),
 ("group_onboarding_response", "study_group"), ("group_onboarding_response", "group_member"),
 ("member_availability_slot", "group_onboarding_response"), ("member_availability_slot", "group_member"),
 ("group_board", "study_group"),
 ("group_board_post", "group_board"), ("group_board_post", "group_member"),
 ("group_board_comment", "group_board_post"), ("group_board_comment", "group_member"),
 ("curriculum", "study_group"), ("curriculum", "llm_usage"),
 ("curriculum_week", "curriculum"),
 ("weekly_task", "curriculum_week"),
 ("member_week_progress", "curriculum_week"), ("member_week_progress", "group_member"),
 ("task_completion", "member_week_progress"), ("task_completion", "weekly_task"), ("task_completion", "group_member"),
 ("rule_violation", "group_rule"), ("rule_violation", "group_member"), ("rule_violation", "task_completion"),
 ("llm_usage", "users"), ("llm_usage", "study_group"),
 ("retrospective", "member_week_progress"), ("retrospective", "curriculum_week"), ("retrospective", "group_member"), ("retrospective", "llm_usage"),
 ("ai_conversation", "study_group"), ("ai_conversation", "group_member"), ("ai_conversation", "curriculum_week"), ("ai_conversation", "retrospective"),
 ("ai_conversation_message", "ai_conversation"), ("ai_conversation_message", "llm_usage"),
 ("notification", "study_group"), ("notification", "users"), ("notification", "group_onboarding_response"),
 ("notification", "curriculum_week"), ("notification", "task_completion"), ("notification", "retrospective"),
]

# ── Wide landscape layout: (col, row) per table ──────────────────────
# 8 columns, dependencies flowing left → right; domain colours stay grouped.
LAYOUT = {
    "users": (0, 0), "oauth_account": (0, 1), "refresh_token": (0, 2),
    "user_follow": (1, 0), "group_bookmark": (1, 1), "group_review": (1, 2),
    "study_group": (2, 0), "group_member": (2, 1), "group_rule": (2, 2),
    "group_onboarding_response": (3, 0), "member_availability_slot": (3, 1), "group_board": (3, 2),
    "group_board_post": (4, 0), "group_board_comment": (4, 1), "curriculum": (4, 2),
    "curriculum_week": (5, 0), "weekly_task": (5, 1), "member_week_progress": (5, 2),
    "task_completion": (6, 0), "rule_violation": (6, 1), "llm_usage": (6, 2),
    "retrospective": (7, 0), "ai_conversation": (7, 1), "ai_conversation_message": (7, 2),
    "notification": (7, 3),
}
_COLW, _ROWH, _X0, _Y0 = 332, 262, 40, 80
T = {
    name: (dom, _X0 + LAYOUT[name][0] * _COLW, _Y0 + LAYOUT[name][1] * _ROWH, fields)
    for name, (dom, _x, _y, fields) in T.items()
}

BOXW = 268
HEAD = 30
ROWH = 21
PADX, FONT = 12, 13

def box_dims(name):
    dom, x, y, fields = T[name]
    h = HEAD + ROWH * len(fields) + 8
    return x, y, BOXW, h

_bottoms = [y + HEAD + ROWH * len(f) + 8 for (_, x, y, f) in T.values()]
_rights = [x + BOXW for (_, x, y, f) in T.values()]
CANVAS_W = max(_rights) + 40
CANVAS_H = max(_bottoms) + 70

def esc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

def anchors(name):
    x, y, w, h = box_dims(name)
    return {
        "L": (x, y + h / 2), "R": (x + w, y + h / 2),
        "T": (x + w / 2, y), "B": (x + w / 2, y + h),
        "cx": x + w / 2, "cy": y + h / 2, "x": x, "y": y, "w": w, "h": h,
    }

def pick_sides(a, b):
    # choose horizontal connection by relative x
    if b["cx"] >= a["cx"]:
        return a["R"], b["L"]
    return a["L"], b["R"]

parts = []
parts.append(
    f'<svg xmlns="http://www.w3.org/2000/svg" width="{CANVAS_W}" height="{CANVAS_H}" '
    f'viewBox="0 0 {CANVAS_W} {CANVAS_H}" font-family="Helvetica,Arial,sans-serif">')
parts.append(f'<rect x="0" y="0" width="{CANVAS_W}" height="{CANVAS_H}" fill="#ffffff"/>')
parts.append('<text x="40" y="40" font-size="26" font-weight="bold" fill="#1a1a1a">'
             'StudyPot ERD &#8212; AI Study Leader</text>')

# edges first (under boxes)
for child, parent in FK:
    a, b = anchors(child), anchors(parent)
    (x1, y1), (x2, y2) = pick_sides(a, b)
    dx = abs(x2 - x1)
    c = max(40, dx * 0.4)
    s1 = 1 if x2 >= x1 else -1
    s2 = -s1
    path = (f'M {x1:.0f} {y1:.0f} C {x1 + s1*c:.0f} {y1:.0f}, '
            f'{x2 + s2*c:.0f} {y2:.0f}, {x2:.0f} {y2:.0f}')
    parts.append(f'<path d="{path}" fill="none" stroke="#9aa0a6" stroke-width="1.3" opacity="0.75"/>')
    # crow's foot (many) at child end
    parts.append(f'<circle cx="{x1:.0f}" cy="{y1:.0f}" r="3.2" fill="#5f6368"/>')

# boxes on top
for name, (dom, x, y, fields) in T.items():
    head, border, tdark, alt = DOM[dom]
    w = BOXW
    h = HEAD + ROWH * len(fields) + 8
    parts.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="7" '
                 f'fill="#ffffff" stroke="{border}" stroke-width="1.6"/>')
    parts.append(f'<path d="M {x} {y+HEAD} L {x} {y+7} Q {x} {y} {x+7} {y} '
                 f'L {x+w-7} {y} Q {x+w} {y} {x+w} {y+7} L {x+w} {y+HEAD} Z" fill="{head}"/>')
    parts.append(f'<text x="{x+PADX}" y="{y+20}" font-size="14.5" font-weight="bold" '
                 f'fill="#ffffff">{esc(name)}</text>')
    cy = y + HEAD
    for i, (fld, kind) in enumerate(fields):
        rowfill = alt if i % 2 == 0 else "#ffffff"
        parts.append(f'<rect x="{x+1}" y="{cy}" width="{w-2}" height="{ROWH}" fill="{rowfill}"/>')
        marker = ""
        weight = "normal"
        col = "#202124"
        if kind == "pk":
            marker = "PK "
            weight = "bold"
        elif kind == "fk":
            marker = "FK "
            col = tdark
        label = f'{marker}{fld}'
        parts.append(f'<text x="{x+PADX}" y="{cy+15}" font-size="{FONT}" '
                     f'font-weight="{weight}" fill="{col}">{esc(label)}</text>')
        cy += ROWH

# legend
ly = CANVAS_H - 38
lx = 40
parts.append(f'<text x="{lx}" y="{ly}" font-size="13" fill="#444">Domains:</text>')
lx += 70
names = [("auth","Auth/User"),("group","Study group"),("curri","Curriculum/Progress"),
         ("ai","AI / LLM"),("board","Board"),("social","Social"),("notif","Notification")]
for key, lab in names:
    head = DOM[key][0]
    parts.append(f'<rect x="{lx}" y="{ly-12}" width="14" height="14" rx="3" fill="{head}"/>')
    parts.append(f'<text x="{lx+20}" y="{ly}" font-size="13" fill="#444">{lab}</text>')
    lx += 40 + len(lab) * 7.6 + 30

parts.append('</svg>')

with open("docs/StudyPot_ERD.svg", "w") as f:
    f.write("\n".join(parts))
print("wrote docs/StudyPot_ERD.svg", CANVAS_W, "x", CANVAS_H, "tables:", len(T), "fks:", len(FK))
