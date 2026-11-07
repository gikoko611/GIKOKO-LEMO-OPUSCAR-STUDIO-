# G.I KO KO LEMO OPUSCAR PROFESSIONAL STUDIO

**Powered By I KO KO** — Android-first cartoon and cinematic story production studio.

## Studio modules

### 1. Android APK
- Write a film prompt and title.
- Edit a character bible, scene outline and bilingual dialogue.
- Save the project locally on the phone.
- Select Cartoon 2D, Cartoon 3D or Cinematic; 1, 5, 10 or 30 minutes; 480p or 720p.
- Start cloud rendering, check workflow step progress, retry failed jobs and download the MP4 from inside the APK.
- GitHub Fine-grained token is encrypted with Android Keystore AES-GCM. It is not embedded in the APK.

### 2. Story & Timeline Engine
- OpenAI-compatible AI Story Assistant: configure Base URL, model and API key in the APK.
- AI drafts title, character bible, scenes and English/Myanmar dialogue as JSON.
- Edit all fields manually and save locally.
- The renderer uses the scene list to calculate scene timing and displays scene titles/dialogue on the timeline.
- Character silhouette remains consistent across scenes. The current renderer is procedural motion graphics, not an AI image/video generator.

### 3. Audio & Subtitle Engine
- English voice: Edge TTS en-US-GuyNeural.
- Myanmar voice: Edge TTS my-MM-ThihaNeural.
- Bilingual voice option.
- Procedurally synthesized background music and scene accents.
- Myanmar SRT subtitles embedded as an MP4 soft-subtitle track and included as a separate artifact.
- Edge TTS requires network access and may be rate-limited; it is not an offline voice engine.

### 4. Cloud Render Queue
- GitHub Actions cloud render.
- One-minute MP4 segments, two renderer workers, three retries per segment.
- Segment cache keyed by request ID and workflow attempt.
- APK can retry failed jobs and poll GitHub Actions step progress.
- MP4 and SRT artifact export; artifact retention is 7 days.

## Secure GitHub token setup
1. Open https://github.com/settings/personal-access-tokens/new
2. Select resource owner gikoko611.
3. Select Only select repositories and choose GIKOKO-LEMO-OPUSCAR-STUDIO-.
4. Grant Actions: Read and write for this repository.
5. Generate the token, paste it in the APK and tap SAVE TOKEN. Never paste the token into chat or commit it.
6. The app validates workflow access and stores the token encrypted on-device.

## AI provider setup
- Configure an HTTPS OpenAI-compatible Base URL.
- Enter the model ID and API key.
- Tap SAVE AI PROVIDER; the API key is encrypted with Android Keystore.
- Tap AI STORY ASSIST to draft scenes and dialogue. Without an AI key, the app offers a basic offline scene-outline helper instead.

## Build APK
Open GitHub Actions → Build G.I KO KO Studio APK → Run workflow. Download artifact GIKOKO-LEMO-OPUSCAR-STUDIO-APK and install app-debug.apk on Android 10 / ARMv7.

## Important limitations
- The renderer is code-driven and procedural; the AI assistant writes story data but does not yet generate new character images or cinematic footage.
- Edge TTS is online and can be unavailable or rate-limited.
- GitHub Actions has account usage and runner time limits. A 30-minute film at 24 FPS is 43,200 frames; 30-minute 720p output is not guaranteed to complete within free usage.
- Start with a 1-minute 480p render and confirm the end-to-end flow before longer projects.
- See docs/PROJECT_FORMAT.md for the JSON schema and pipeline behavior.
