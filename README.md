# G.I KO KO LEMO OPUSCAR PROFESSIONAL STUDIO

**Powered By I KO KO** — phone-managed Cartoon and Cinematic video studio.

## Current implementation
- Android controller app source, minSdk 23, ARMv7-compatible build.
- APK build runs in GitHub Actions; no local Android Studio or OpenJDK installation is required.
- Cloud render presets: Cartoon 2D, Cartoon 3D, Cinematic.
- Duration presets: 1, 5, 10, and 30 minutes.
- Output presets: 480p and 720p, 24 FPS, MP4.
- APK opens the GitHub Actions workflow page and checks the latest public workflow run status.
- No GitHub personal access token is embedded in the APK.

## Build the APK
1. Open the repository Actions tab.
2. Select **Build G.I KO KO Studio APK**.
3. Press **Run workflow** (or wait for the push-triggered build).
4. Wait for success, open the run, and download the artifact **GIKOKO-LEMO-OPUSCAR-STUDIO-APK**.
5. Install app-debug.apk on Android. Allow installation from the browser or file manager if prompted.

## Render a video
1. Open the APK and choose a style, duration, and resolution.
2. Tap **OPEN GITHUB CLOUD RENDER**.
3. Sign in to GitHub if required.
4. Select the same options on the workflow page, then press **Run workflow**.
5. When complete, download the MP4 artifact from the workflow run page.

The first APK milestone opens the workflow page and checks the latest public run. It does not silently trigger Actions because that requires a secure GitHub authorization flow; never hard-code a personal access token into an APK.

## Renderer milestone and limits
The current demo is procedural motion graphics, not yet a complete storyboard editor or prompt-driven AI video generator. It accepts style and duration query parameters. A 30-minute video at 24 FPS contains 43,200 frames and must be benchmarked. If render time exceeds practical runner limits, the next milestone is resumable segmented rendering.

GitHub Actions runner time, account quotas, and artifact retention policies apply. This is not an unlimited free GPU service. Start with 1 minute at 480p.

## Project structure
- app/ — Android controller app source.
- .github/workflows/android-apk.yml — cloud APK build.
- .github/workflows/cloud-render.yml — cloud video render.
- films/demo/index.html — starter procedural scene.
- assets/icons/icon-512.png — app icon source asset.
