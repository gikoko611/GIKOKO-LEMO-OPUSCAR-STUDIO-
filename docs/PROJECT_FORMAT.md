# G.I KO KO Studio Project Format

The Android app submits this JSON as the project_json workflow input. Keep it below 45,000 characters.

Example:
{
  "schema_version": 1,
  "title": "The Golden Kingdom",
  "prompt": "A funny action adventure in an ancient Myanmar royal palace.",
  "character_bible": "A clever but clumsy royal guard. Keep the same gold costume, face shape, colors and personality in every scene.",
  "scenes": [
    "Palace courtyard — introduce the guard",
    "Throne room — a suspicious royal order",
    "Palace kitchen — comic chase",
    "Royal hall — funny reveal"
  ],
  "dialogue": [
    {"english": "I have everything under control.", "myanmar": "အားလုံးကို ကျွန်တော် ထိန်းချုပ်ထားပါတယ်။"},
    {"english": "Then why is the palace on fire?", "myanmar": "ဒါဆို နန်းတော်က ဘာလို့ မီးလောင်နေတာလဲ။"}
  ],
  "voice": "Bilingual voice-over",
  "background_music": true,
  "sfx": true,
  "myanmar_subtitles": true,
  "style": "cartoon2d",
  "duration_minutes": "1",
  "resolution": "480p"
}

## Audio behavior
- English voice: Edge TTS en-US-GuyNeural
- Myanmar voice: Edge TTS my-MM-ThihaNeural
- Bilingual voice: both voices are placed on the dialogue timeline.
- Myanmar subtitles are written to SRT and embedded as an MP4 soft-subtitle track.
- Music and scene accents are procedurally synthesized; no commercial music files are bundled.
- Edge TTS is an online service. It requires network access from the GitHub runner and may be rate-limited or unavailable. A TTS failure fails the job rather than silently exporting a video without the selected voice.

## Rendering behavior
- The workflow renders one-minute chunks, retries each failed chunk up to three times, and stores completed chunks in GitHub Actions cache.
- A failed run can be retried from the APK with GitHub's rerun-failed-jobs endpoint; the cache key changes with the run attempt so completed chunks can be restored.
- The current scene renderer draws procedural 2D/cinematic graphics, a repeated character silhouette, scene titles and dialogue overlays. The character bible is retained for AI story generation and continuity metadata; it does not yet generate photorealistic or prompt-generated character art.
- 30-minute 720p renders can be lengthy and may exceed practical runner time/usage limits. Test 1 minute at 480p first.
