# G.I Ko Ko ViD Gen

**Powered By I KO KO** — Android-first cartoon and cinematic story production studio.

## Studio modules

### Facebook Creator Mode — Free-first mixed content\n- Added a Facebook Creator Mode preset for mixed comedy, Myanmar history/culture, storytelling, moral, educational, action/adventure and motivational content.\n- Defaults to 2-minute vertical 9:16 Reels, bilingual English + Myanmar narration/subtitles, background music and sound effects; select 1 or 3 minutes as needed.\n- Story prompts include a first-3-second hook, short scene beats, character continuity and a satisfying ending.\n- Free-first policy: Gemini Free Tier text/vision only when available; Edge TTS is the default voice engine; FFmpeg/Python performs video rendering. Paid Omni/Veo video generation and paid Gemini TTS are not called by default. Free Tier quotas and GitHub Actions usage limits still apply.\n\n### 0.4.0 Character Reference + Free Animation Upgrade
- English-first studio interface with twelve custom vector feature icons and a neon cyber visual system.
- Ten built-in starter templates: Myanmar palace comedy, folk tale, action adventure, rural mystery, kids fable, travel documentary, product showcase, motivational short, music video and movie recap.
- Twelve illustrated tools: Starter Templates, Character Lab, Dialogue Desk, Subtitle Preview, Audio Lab, Cloud Queue, Project Library, Brand Kit, Format Presets, Diagnostics, Character Reference Lab and Motion Presets.
- Three selectable premium themes in Settings: NEBULA PRISM, OBSIDIAN LUX and AURORA GLASS, with dynamic 3D-style bordered cards.
- Cloud Render supports 16:9, 9:16, 1:1 and 4:5 output ratios plus 24/30/60 FPS selection.
- The normal user interface is Cloud Render-first. Local Device Render remains in the codebase but is temporarily hidden from the user interface.


### 1. Android APK
- Use the AI Director chat composer to describe a film idea in natural language.
- Gemini Cloud Story Assist creates a title, consistent character bible, scene outline and bilingual dialogue using the `GEMINI_API_KEY` Actions secret.
- Edit the character bible, scene outline and bilingual dialogue before rendering.
- Upload a personal character image (compressed and stored with the saved project), choose Float / Bounce / Slide / Pulse / Still, and render that same image with smooth FFmpeg motion.
- Gemini Flash optionally inspects the uploaded image and adds a visual-identity description to the character bible using the existing `GEMINI_API_KEY` secret.
- Save the project locally on the phone.
- Select Hybrid, Cartoon 2D, Cartoon 3D or Cinematic; 1, 2, 3, 5, 10 or 30 minutes; 480p or 720p.
- Start cloud rendering, check workflow step progress, retry failed jobs and download the MP4 from inside the APK.
- GitHub Fine-grained token is encrypted with Android Keystore AES-GCM. It is not embedded in the APK.

### 2. Story & Timeline Engine
- OpenAI-compatible AI Story Assistant: configure Base URL, model and API key in the APK.
- AI drafts title, character bible, scenes, English/Myanmar dialogue and a JavaScript animation scene-plan function as JSON.
- The generated function calls a small `api.scene(...)` interface to define scene title, description, timing, background color and motion cue.
- Edit all fields manually and save locally.
- GitHub Actions runs the generated scene-plan function in a restricted Node.js VM (no filesystem, network, process or module APIs; dynamic code generation disabled), validates the result, then the self-contained FFmpeg/Python renderer generates MP4 segments directly from the saved project.
- Scene titles, character bible, recurring character animation, bilingual dialogue overlays and a full-film progress timeline are rendered into the video.
- AI-generated scene code drives scene timing, backgrounds, titles, descriptions and motion cues. Uploaded character images are animated as 2D image layers with FFmpeg transform motion. This does not generate new AI video frames, articulate limbs or synthesize lip-sync; Omni/Veo video APIs are not required.

### 3. Audio & Subtitle Engine
- Primary voice engine: Gemini 3.8 Flash TTS (`gemini-3.8-flash-tts`) for English and Burmese.
- English voice preset: Puck; Myanmar voice preset: Kore (language is automatically detected from the supplied script).
- Automatic Edge TTS fallback: English `en-US-GuyNeural`, Myanmar `my-MM-ThihaNeural`, when Gemini is not configured or its request fails.
- Bilingual voice option.
- Procedurally synthesized background music and scene accents.
- English + Myanmar bilingual SRT subtitles (stacked in the same caption cue), embedded as an MP4 soft-subtitle track and included as a separate artifact.
- Both Gemini TTS and Edge TTS require network access. Gemini TTS may be Preview/quota-limited; Burmese is listed for Gemini 3.8 Flash TTS, but not Flash-Lite TTS.

### 4. Cloud Render Queue
- GitHub Actions cloud render using the repository's own FFmpeg/Python renderer.
- One-minute MP4 segments, up to three retries per segment, and request-ID cache restoration.
- APK can retry failed jobs and poll GitHub Actions step progress.
- MP4 and SRT artifact export; artifact retention is 7 days.

## Gemini character reference setup
1. Reuse the existing repository secret `GEMINI_API_KEY`; no Omni or Veo API key is needed.
2. The cloud workflow sends the compressed uploaded image to Gemini Flash for visual description only. If Gemini analysis is unavailable, the uploaded image still animates with the selected free motion preset.
3. The image is carried in the GitHub Actions workflow input and is not committed as a repository image file. Workflow-run visibility follows the repository's GitHub visibility and access settings.

## Gemini TTS setup
1. Open the repository Settings → Secrets and variables → Actions.
2. Add a repository secret named `GEMINI_API_KEY` using a Gemini API key from Google AI Studio. Never paste the key into chat or commit it.
3. Cloud Render uses Gemini 3.8 Flash TTS as the primary engine and automatically falls back to Edge TTS if the Gemini request fails. If the secret is absent, Edge TTS is used.
4. The default model is `gemini-3.8-flash-tts`.

## AI render error repair
- When a manually dispatched Cloud Render run fails, a separate workflow extracts error-focused log lines and asks Gemini 2.5 Flash for a minimal Python patch.
- Proposed changes are restricted to existing `scripts/*.py` files and must pass `git apply --check`, Python compile checks, both pipeline CLI checks and `git diff --check`.
- A validated patch is opened as a Pull Request for review; it is never pushed directly to the default branch and does not automatically re-run the render.
- The repair workflow uses the same `GEMINI_API_KEY` repository secret. If the secret is missing, AI repair is skipped.

## Secure GitHub token setup
1. Open https://github.com/settings/personal-access-tokens/new
2. Select resource owner gikoko611.
3. Select Only select repositories and choose GIKOKO-LEMO-OPUSCAR-STUDIO-.
4. Grant Actions: Read and write for this repository.
5. Generate the token, paste it in the APK and tap SAVE TOKEN. Never paste the token into chat or commit it.
6. The app validates workflow access and stores the token encrypted on-device.

## Gemini Cloud AI Director setup
1. Add the repository secret `GEMINI_API_KEY` under Settings → Secrets and variables → Actions.
2. Save a GitHub fine-grained token in the APK with Actions read/write permission.
3. Enter a story idea in the AI Director chat and tap SEND TO AI DIRECTOR.
4. The app dispatches `.github/workflows/story-assist.yml`; Gemini 2.5 Flash returns a structured story artifact, which the app imports into Project Details.
5. The same `GEMINI_API_KEY` secret is used by Cloud Render for Gemini TTS and by the AI render-repair workflow. The key is never embedded in the APK.

Advanced OpenAI-compatible provider settings are temporarily hidden from the standard UI. Existing saved provider settings are preserved for later re-enabling.

## Visual assets and accessibility
- Ten lightweight, hand-authored Android Vector Drawable icons are bundled in `app/src/main/res/drawable/`; no network icon loading or heavyweight icon font is required.
- The app UI is English-first. Myanmar text is reserved for the user dialogue and the bilingual subtitle output.
- Interactive controls target at least 48dp where practical and use a compact two-column tool grid for phone screens.
- Android vector resources are original project artwork; no third-party illustration pack is redistributed. Google Material icon guidance is used as a design reference only.

## Build APK
Open GitHub Actions → Build G.I Ko Ko ViD Gen APK → Run workflow. Download artifact GI-KO-KO-VID-GEN-APK and install app-debug.apk on Android 10 / ARMv7.

## Important limitations
- The renderer is code-driven and procedural; the AI assistant now writes a constrained JavaScript scene plan, but does not generate new character images or photorealistic cinematic footage.
- Edge TTS is online and can be unavailable or rate-limited.
- GitHub Actions has account usage and runner time limits. A 30-minute film at 24 FPS is 43,200 frames; 30-minute 720p output is not guaranteed to complete within free usage.
- Start with a 1-minute 480p render and confirm the end-to-end flow before longer projects.
- See docs/PROJECT_FORMAT.md for the JSON schema and pipeline behavior.


## End-to-end render pipeline
1. In the APK, enter a story idea in **AI DIRECTOR CHAT** and tap **SEND TO AI DIRECTOR**. Gemini Cloud uses the selected duration and style to generate the story draft.
2. Review the generated title, character bible, scenes, bilingual dialogue and AI animation scene plan. Save the project.
3. Select **Hybrid**, **Bilingual voice-over**, **30 minutes**, and the required resolution, then tap **START RENDER**.
4. The APK dispatches `.github/workflows/cloud-render.yml`. GitHub Actions validates and executes `animation_code` through `scripts/animation_runtime.js`, then renders recoverable one-minute segments using `scripts/render_video.py` and joins them with FFmpeg.
5. `scripts/audio_pipeline.py` creates English/Myanmar Edge TTS, generated background audio, sound accents and English + Myanmar bilingual SRT subtitles. FFmpeg muxes audio/subtitles into the final MP4.
6. GitHub Actions publishes the MP4 as a direct GitHub Release asset (tag `render-<run_id>`) and also keeps the ZIP artifact as backup. The APK downloads the MP4 asset directly into the phone's Downloads folder; if the Release asset is unavailable, it falls back to extracting the ZIP artifact.

## Renderer scope
The new engine is self-contained and code-driven. It creates animated mascot motion graphics, backgrounds, titles, scene cards, dialogue overlays, audio and subtitles. It is **not** a diffusion model or a photorealistic generative text-to-video engine; AI providers write the story and constrained scene-plan code, while FFmpeg/Python produce the visuals. Image/video provider fields in Provider Control Center remain configuration slots and do not yet invoke external image/video generation APIs.
