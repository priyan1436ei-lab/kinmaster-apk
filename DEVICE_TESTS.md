# Device acceptance — required before release

Status: model tests run in the development environment. See BUILD_STATUS.md for compilation results. Phone/emulator media-flow testing has not been performed unless explicitly recorded there.

1. Install fresh on API 26 and a current Android phone. Verify safe-area insets and readable controls in portrait/landscape.
2. Import an H.264/AAC MP4, a video with no audio, HEVC, rotated portrait media and a cloud document URI. Unsupported inputs must produce an error.
3. Trim a known ten-second test video to 2–7 seconds; verify preview duration and exported duration/frame content.
4. Split at two seconds into the trimmed clip. Verify no missing/repeated content outside normal encoder/frame boundaries.
5. Reorder, duplicate and delete clips. Undo and redo; close and reopen. Confirm edit decisions persist.
6. Apply text, grayscale, rotation and mute. Export and compare to preview on another video player; verify actual audio silence.
7. Combine mixed resolutions/orientations and audio/no-audio clips. Check a uniform 16:9 canvas, continuous A/V timestamps and no frozen sections.
8. Export at 720p and 1080p. Use ffprobe to inspect codec, dimensions, duration and audio; play the entire file.
9. Cancel export, cancel the save picker, retry Save last render, deny provider access, delete source media, fill storage and write to removable storage.
10. Test backgrounding, rotation, screen lock, process kill and repeated exports. Activity destruction cancels rendering by design in this version.
11. Test 20+ clips, large files, a 30-minute composition, Unicode/Tamil titles and repeated undo/redo. Watch memory, heat and cancellation cleanup.
12. Do not publish until all supported device/input cases pass, unsupported cases fail clearly, and no corrupted output is reported as successful.
