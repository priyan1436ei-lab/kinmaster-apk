# PRIYAN CUT — Android video editor, v0.1.0

An original local-first Android editor inspired by common mobile editing workflows. This is an early core editor implementation, not a feature-complete KineMaster replacement. It uses no KineMaster code, branding, assets, subscriptions or services.

## Included

- Import multiple videos through Android's document picker; source files stay untouched.
- A clip-card timeline: select, reorder, duplicate or delete clips.
- Selected-clip playback and scrubbing through Media3 ExoPlayer.
- Non-destructive trim using source timestamps; split at the selected clip's current preview playhead.
- Per-clip rotation, grayscale, mute and centered static text.
- Shared effect definitions for preview and rendered output.
- Automatic atomic save/restore of one current project; in-session undo/redo (50 snapshots).
- Sequential MP4 export through Media3 Transformer, H.264/AAC, 720p or 1080p, 16:9 letterboxed canvas.
- Export progress, cancellation, errors and a document picker to choose the output location.
- Save-last-render retry while the app remains alive, if saving was cancelled or failed.
- Original mint/navy UI and vector logo.

## Build and run

1. Install Android Studio with JDK 17 and Android SDK platform 35.
2. Open this directory (the one containing settings.gradle).
3. Let Gradle sync. Internet access is needed for the first dependency download.
4. Connect an Android 8.0+ phone with USB debugging, or create an emulator.
5. Run the `app` configuration. For command-line builds use `./gradlew assembleDebug` on macOS/Linux or `gradlew.bat assembleDebug` on Windows.
6. The debug APK appears at `app/build/outputs/apk/debug/app-debug.apk`.

The source pins Gradle 8.9, Android Gradle Plugin 8.7.3 and Media3 1.4.1. These are deliberate reproducible baseline versions, not claims about the newest available versions.

## Use

1. Tap **Import videos** and choose one or more videos. Download cloud-only media first.
2. Tap a timeline card. The preview plays only that selected clip.
3. Trim by entering start/end seconds relative to the original source. Leave at least 0.1 seconds.
4. Scrub within the clip and tap **Split at playhead**. Both resulting clips retain the original effects.
5. Add text, rotate, mute, switch grayscale, or rearrange clips.
6. Tap **Export MP4**, choose resolution and keep the app open until rendering finishes.
7. Choose a destination and save. Export includes all clips in timeline order.

There is one auto-saved current project. Start a new project only after exporting your previous edit. Undo history is session-only. Selected media must remain at its original provider URI; moving/deleting it or revoking access prevents playback/export. Project JSON is not a portable media archive.

## Important current limits

Preview is per selected clip; it is not continuous composition playback. Output is a fixed 16:9 canvas and source-dependent frame rate. Hardware codecs determine supported inputs and export performance. Export is Activity-bound: keep the app visible; process termination cancels the render. Partial destination files may remain after a storage/write failure; delete them before retrying. Render cache is private and may be cleared by Android. This build has no background export service, no public gallery indexing and no account or Internet permission.

See FEATURE_MATRIX.md for explicitly missing features and DEVICE_TESTS.md for release acceptance. A successful build is not proof of codec compatibility, smooth playback or complete feature parity.

## Files

- MainActivity.java: UI, input, history, import/export orchestration.
- Clip.java: pure Java edit model and validation.
- EditPipeline.java: shared effects and sequential composition.
- ProjectStore.java: versioned atomic local project persistence.
- tests/ClipTest.java: pure model regression checks.

## Run model tests

```sh
mkdir -p /tmp/cut-tests
javac -d /tmp/cut-tests app/src/main/java/dev/priyan/cut/Clip.java tests/ClipTest.java
java -cp /tmp/cut-tests dev.priyan.cut.ClipTest
```

## Privacy and licensing

The app has no analytics and no Internet permission. Android media libraries have their own upstream licenses; review dependencies and notices before distribution. Imported content and fonts must be licensed by the person distributing their output. The debug APK is for development/testing; Play publication needs your release signing key, application identity, privacy disclosures and completed device QA.
"# kinmaster-apk" 
