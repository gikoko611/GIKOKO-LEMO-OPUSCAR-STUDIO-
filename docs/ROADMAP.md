# G.I KO KO Studio — Feature Roadmap

## Available in the current source
- Android prompt, title, character-bible, scene and bilingual-dialogue editor.
- Local project save/restore.
- OpenAI-compatible AI Story Assistant with configurable Base URL, model and encrypted API key.
- Basic offline scene-outline helper when no AI provider is configured.
- Scene-based timing and repeated character silhouette in the procedural renderer.
- English, Myanmar or bilingual Edge TTS voice option.
- Procedural background music and scene accents.
- Myanmar SRT plus MP4 soft-subtitle mux.
- One-minute render chunks, three retries per segment, cache restore on rerun, status polling, retry and APK artifact download.

## Recommended next features

### AI pre-production
1. Story generator: logline → synopsis → acts → scenes → shot list.
2. Character Bible Lock: fixed age range, costume, palette, facial traits, voice and personality; inject the same reference into every scene prompt.
3. Continuity checker: detect costume, prop, location, time-of-day and character-name drift between scenes.
4. Dialogue studio: rewrite by emotion, translate EN↔MY, shorten for timing, assign speaker and voice.
5. Prompt enhancer: convert a rough idea into camera, lighting, composition, motion and style prompts.
6. Storyboard thumbnails: generate one still per scene using a user-selected image provider.

### Production editor
7. Visual timeline with draggable scenes, duration trim, transitions and track lanes.
8. Per-scene camera controls: wide, medium, close-up, pan, tilt, zoom and tracking.
9. Character and prop library with reusable references.
10. Preview render for a selected scene before rendering the full film.
11. Project import/export as a portable JSON bundle, including images, audio and subtitle assets.

### Audio and post-production
12. Multi-speaker voice casting, emotion and speaking rate controls.
13. Per-line subtitle timing editor, dual-language subtitles and subtitle style presets.
14. Upload custom music/SFX, volume automation, ducking and fade controls.
15. Lip-sync alignment from generated speech to character mouth animation.
16. Loudness normalization, noise reduction and final audio QC.

### Cloud queue and delivery
17. Render queue with pause/cancel, estimated frame count and detailed per-segment progress.
18. Segment-level artifact persistence and resume after runner loss.
19. Automatic render profiles for YouTube 16:9, Shorts/Reels/TikTok 9:16 and square 1:1.
20. Thumbnail, chapter markers, metadata and social caption generation.
21. Automatic quality checks: duration, resolution, audio stream, subtitle stream and MP4 integrity.

## AI integration choices
- OpenAI-compatible chat API: scriptwriting, translation, continuity checks and prompt enhancement.
- Image generation API: scene keyframes and consistent character reference images.
- Video generation API: image-to-video or text-to-video clips; provider-specific costs, quotas and access rules apply.
- Edge TTS: current online EN/MY voice prototype; network-dependent and subject to service changes.

## Priority order
1. Make the current APK build and 1-minute 480p end-to-end render pass.
2. Verify bilingual TTS and Myanmar subtitle playback on Android.
3. Add visual timeline editing and project import/export.
4. Add image-provider integration for actual scene artwork.
5. Add per-scene video-generation provider integration and continuity references.

Do not advertise image/video generation, lip-sync, or fully featured timeline editing as implemented until those modules have working end-to-end tests.
