<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run in AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/586166b0-5507-4c9d-bb50-aafe4b16cb81

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device

## Web App

This repo also includes a Vite/React rebuild for browser deployment.

```bash
npm install
npm run dev
npm run build
```

Deploy to Vercel or Netlify with:

- Build command: `npm run build`
- Publish/output directory: `dist`

## Android APK

The debug APK is built at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install to a connected Android device with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
