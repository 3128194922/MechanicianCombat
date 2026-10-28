"""Inspect the existing rig for the experimental PlayerAnimator migration.

This is a read-only audit: it never changes the animation assets or renderer.
"""
import json
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/tkgun"
BODY = {"body", "chest", "head", "arm_left", "arm_right", "leg_left", "leg_right"}


def expressions(value):
    if isinstance(value, str):
        yield value
    elif isinstance(value, list):
        for item in value:
            yield from expressions(item)
    elif isinstance(value, dict):
        for item in value.values():
            yield from expressions(item)


def main():
    animations = json.loads((ASSETS / "animations/entity/bowgun_rig.animation.json").read_text(encoding="utf-8"))["animations"]
    geometry = json.loads((ASSETS / "geo/entity/bowgun_rig.geo.json").read_text(encoding="utf-8"))["minecraft:geometry"][0]
    bones = geometry["bones"]
    counts = Counter()
    affected = {}
    for name, animation in animations.items():
        result = {"body_position": [], "body_scale": [], "body_expressions": [], "weapon_scale": [], "events": []}
        for bone, channels in animation.get("bones", {}).items():
            if bone in BODY:
                for channel in ("position", "scale"):
                    if channel in channels:
                        result[f"body_{channel}"].append(bone)
                for channel in ("position", "rotation", "scale"):
                    for expression in expressions(channels.get(channel, [])):
                        result["body_expressions"].append(f"{bone}.{channel}: {expression}")
            elif "scale" in channels:
                result["weapon_scale"].append(bone)
        result["events"] = [kind for kind in ("sound_effects", "particle_effects", "timeline") if kind in animation]
        for category, values in result.items():
            counts[category] += bool(values)
        affected[name] = result
    print(json.dumps({
        "total_animations": len(animations),
        "animations_with_category": dict(counts),
        "body_hierarchy": [{"name": b["name"], "parent": b.get("parent"), "pivot": b.get("pivot")} for b in bones if b["name"] in BODY],
        "animations": affected,
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
