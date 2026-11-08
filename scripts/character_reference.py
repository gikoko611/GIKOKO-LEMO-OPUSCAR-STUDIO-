#!/usr/bin/env python3
"""Optional Gemini visual analysis for a user-supplied character image.
The image is also animated by the local FFmpeg/Pillow renderer; no video API is used.
"""
import argparse
import base64
import json
import os
import sys
from pathlib import Path
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--project", required=True)
    parser.add_argument("--out", required=True)
    args = parser.parse_args()
    source = Path(args.project)
    project = json.loads(source.read_text(encoding="utf-8"))
    image_data = str(project.get("character_reference_base64") or "").strip()
    api_key = os.environ.get("GEMINI_API_KEY", "").strip()
    if not image_data:
        print("CHARACTER_REFERENCE: no image uploaded; using built-in mascot.")
    elif not api_key:
        print("CHARACTER_REFERENCE: Gemini key unavailable; animating the uploaded image without AI analysis.")
    else:
        try:
            image_bytes = base64.b64decode(image_data, validate=True)
            mime = str(project.get("character_reference_mime") or "image/webp")
            model = os.environ.get("GEMINI_VISION_MODEL", "gemini-3.8-flash").strip()
            prompt = (
                "Analyze this uploaded animation character reference. Describe only visible visual facts: "
                "face, hair/head shape, body proportions, clothing, colors, accessories and distinctive marks. "
                "Do not invent a name, personality, age or backstory. Return one concise paragraph under 900 characters. "
                "End with: Preserve this exact visual design in every scene."
            )
            payload = {
                "contents": [{
                    "parts": [
                        {"inline_data": {"mime_type": mime, "data": base64.b64encode(image_bytes).decode("ascii")}},
                        {"text": prompt}
                    ]
                }],
                "generationConfig": {"temperature": 0.2, "maxOutputTokens": 300}
            }
            request = Request(
                "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent",
                data=json.dumps(payload).encode("utf-8"),
                headers={"Content-Type": "application/json", "x-goog-api-key": api_key},
                method="POST"
            )
            with urlopen(request, timeout=90) as response:
                result = json.loads(response.read().decode("utf-8"))
            parts = result.get("candidates", [{}])[0].get("content", {}).get("parts", [])
            description = " ".join(str(part.get("text", "")).strip() for part in parts if part.get("text")).strip()
            if description:
                project["reference_visual_description"] = description[:1200]
                existing = str(project.get("character_bible") or "").strip()
                marker = "REFERENCE IMAGE VISUAL IDENTITY:"
                if marker not in existing:
                    project["character_bible"] = (existing + "\n\n" + marker + " " + description).strip()[:4000]
                print("CHARACTER_REFERENCE: Gemini visual analysis completed using " + model)
            else:
                print("CHARACTER_REFERENCE: Gemini returned no description; retaining existing character bible.")
        except (HTTPError, URLError, TimeoutError, ValueError, KeyError, IndexError) as exc:
            print("CHARACTER_REFERENCE: Gemini analysis skipped (" + type(exc).__name__ + ": " + str(exc)[:300] + ").", file=sys.stderr)
        except Exception as exc:
            print("CHARACTER_REFERENCE: non-fatal analysis error: " + type(exc).__name__ + ": " + str(exc)[:300], file=sys.stderr)
    Path(args.out).write_text(json.dumps(project, ensure_ascii=False), encoding="utf-8")
    print("CHARACTER_REFERENCE: project updated; no Omni/Veo video endpoint was called.")

if __name__ == "__main__":
    main()
