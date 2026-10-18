# Feature coverage and completion roadmap

Reference: https://play.google.com/store/apps/details?id=com.nexstreaming.app.kinemasterfree (reviewed 18 October 2026).

| Area | This implementation | Required next work |
|---|---|---|
| Video import / clip ordering | Implemented | Large-library browsing, thumbnails and waveform cache |
| Trim / split / duplicate / delete | Implemented | Drag handles, snapping, multi-select |
| Preview | Selected clip, effects applied | Full composition preview, accurate playhead, dropped-frame tests |
| Text | Static centered per-clip title | Fonts, placement, timing, rich text, subtitles |
| Color | Grayscale | Color controls, LUT import, effects browser |
| Rotation | 90-degree increments | Crop, mirror, transform handles and aspect ratios |
| Audio | Per-clip mute | Music track, voice recording, mixer, fades, EQ, ducking |
| Export | Sequential 720/1080 H.264/AAC MP4 | 4K/60 hardware matrix, bitrate/FPS controls, durable foreground jobs |
| Projects | One atomic auto-save | Project gallery, portable archives, relink, migrations, thumbnails |
| Undo/redo | Session history | Durable history, memory budgeting |
| Video/image/GIF layers | Not implemented | Multitrack model, compositing, layer z-order, timing and transforms |
| Photos / stickers / assets | Not implemented | Image import, bundled licensed packs, asset management |
| Transitions | Not implemented | Overlap timing and crossfades with matching preview/export |
| Keyframes / easing | Not implemented | Property curves and time-based renderer |
| Chroma key | Not implemented | Shader, spill suppression, alpha compositing |
| Speed / reverse | Not implemented | Speed-aware audio/video mapping, reverse proxy generation |
| AI captions / TTS / voice | Not implemented | Model/provider, language coverage, subtitle editing, explicit costs |
| AI remove background / noise / vocals | Not implemented | Models, GPU/device benchmarks and model licenses |
| AI tracking / upscale / style | Not implemented | Models and temporally consistent rendering |
| AI music/template recommendations | Not implemented | Licensed catalog and ranking service |
| Template sharing / asset store | Not implemented | Original licensed catalog, accounts, moderation and services |
| Cloud backup / handoff | Not implemented | Auth, storage, resumable media upload and conflict handling |
| Transparent video | Not implemented | Alpha-capable formats, encoder/player compatibility |
| Tablet/Chromebook polish | Basic adaptive vertical layout | Responsive editor, keyboard/mouse and accessibility testing |

## Implementation order toward a full product

1. Validate the current core on real devices and stabilize export/lifecycle behavior.
2. Introduce a versioned multitrack timeline with frame-based rational timestamps and asset relinking.
3. Add full composition preview, thumbnail/waveform caches and background foreground-service rendering.
4. Add independent audio/text/image/video tracks and transform controls.
5. Add transitions, keyframes, speed/reverse, chroma key, color and audio tools, each with export golden tests.
6. Add project gallery, original templates and licensed assets.
7. Add each AI feature behind capability checks; implement actual inference before exposing buttons.
8. Add optional account/cloud services and conflict-safe sync.
9. Test across low/mid/high-tier devices, accessibility, thermal/storage failure and long projects before public release.

All stages require engineering and verification. This package does not claim they are already completed.
