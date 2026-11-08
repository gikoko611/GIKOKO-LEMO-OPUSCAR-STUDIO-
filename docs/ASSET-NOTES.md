# Studio asset notes

## Bundled interface artwork
The ten files under `app/src/main/res/drawable/ic_tool_*.xml` are original, hand-authored Android Vector Drawable artwork created for G.I KO KO ViD Gen. They are small XML vector paths, recolourable, resolution-independent and bundled locally. No network icon request is needed at runtime.

## Starter content
The ten editable starter templates are built into `MainActivity.java` and load directly into the project editor. They are production starting points, not finished footage or promises of AI video generation. Users can replace every prompt, character description, scene and dialogue line.

## Design references
- Google Material Symbols documentation: https://developers.google.com/fonts/docs/material_symbols
- Android touch-target guidance: https://support.google.com/accessibility/android/answer/7101858

Google Material Symbols are documented under Apache License 2.0. This project uses the documentation as a design reference; the bundled tool icons are original project vectors, not copied Material Symbols files.

## Language
The application interface is English-first. Burmese text is used where users author dialogue and in the bilingual English + Myanmar subtitle stream.
