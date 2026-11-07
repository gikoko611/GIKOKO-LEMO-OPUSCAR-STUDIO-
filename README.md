# G.I KO KO LEMO OPUSCAR PROFESSIONAL STUDIO

**Powered By I KO KO** — Android controller for Cartoon and Cinematic cloud rendering.

## APK features
- Cartoon 2D, Cartoon 3D, and Cinematic presets.
- 1, 5, 10, and 30-minute duration presets.
- 480p and 720p, 24 FPS MP4 render configuration.
- Start GitHub Actions directly from the APK using the GitHub REST API.
- Poll the exact render request using a unique request ID.
- Download the workflow artifact ZIP and extract the MP4 into the phone's Download folder.
- Fine-grained token encrypted using Android Keystore AES-GCM; the token is not hard-coded in the APK.
- Android minSdk 23; GitHub Actions builds the APK (no local Android Studio/JDK required).

## One-time GitHub token setup
1. Open GitHub in a browser and go to Settings → Developer settings → Personal access tokens → Fine-grained tokens.
2. Select Generate new token.
3. Set Resource owner to the account that owns this repository.
4. Under Repository access choose Only select repositories and select GIKOKO-LEMO-OPUSCAR-STUDIO-.
5. Under Repository permissions set Actions: Read and write. No Contents write permission is required by the APK render controller.
6. Choose an expiry date, generate the token, and copy it. Do not share it in chats or commit it to this repository.
7. Open the APK, paste the token into the token field, and tap SAVE TOKEN. The app validates the workflow permission and encrypts the token with Android Keystore. The input field is cleared after saving.

GitHub documents that workflow dispatch requires Actions write permission, while run/artifact listing and artifact download require Actions read permission:
- https://docs.github.com/en/rest/actions/workflows
- https://docs.github.com/en/rest/actions/artifacts
- https://docs.github.com/en/rest/authentication/permissions-required-for-fine-grained-personal-access-tokens

## Render from inside the APK
1. Save the token once.
2. Choose Cartoon 2D, Cartoon 3D, or Cinematic.
3. Choose 1, 5, 10, or 30 minutes and 480p or 720p.
4. Tap START RENDER. The app sends the workflow dispatch request directly; no workflow page is required.
5. Keep the phone online. The app checks the run and downloads the MP4 automatically after success.
6. Find the video in the phone's Download folder. You can also use CHECK RENDER STATUS and DOWNLOAD LATEST MP4 later.

If the app is closed while rendering, GitHub Actions continues in the cloud. Reopen the app and use CHECK RENDER STATUS, then DOWNLOAD LATEST MP4.

## Cloud renderer and limitations
The render workflow uses the upstream Lemo-Opuscar code renderer, Node, Chromium, and FFmpeg. The current demo scene is procedural motion graphics; it is a pipeline sample, not a complete storyboard editor or a prompt-driven generative AI video engine. Full production features such as multi-scene storyboards, character continuity, voice-over, subtitles, music/SFX mixing and a visual timeline remain planned development work.

A 30-minute video at 24 FPS contains 43,200 frames. Long renders must be benchmarked; GitHub runner time, account policies, queueing, and storage limits apply. Artifacts are retained for 7 days. Start with 1 minute at 480p to verify the end-to-end pipeline.

## Build APK
Open the repository Actions tab → Build G.I KO KO Studio APK → Run workflow. Download the GIKOKO-LEMO-OPUSCAR-STUDIO-APK artifact and install app-debug.apk on Android 10 / ARMv7.
