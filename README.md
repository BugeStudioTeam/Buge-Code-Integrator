# 📦 Buge Code Integrator

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-blue.svg)](https://kotlinlang.org/)
[![Material3](https://img.shields.io/badge/Material%203-1.11.0-purple.svg)](https://m3.material.io/)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)](https://android-arsenal.com/api?level=21)
[![License](https://img.shields.io/badge/License-GPL3.0-red.svg)](LICENSE)

**Code Integrator** is an Android app that merges source code files from any project into a single text file for easy analysis, backup, and sharing.

## Features

- Merge all source files into one text file
- Project tree structure viewer
- Custom file type filters (exclude images, audio, video, archives, binaries)
- Material Design 3 UI with light/dark theme
- Auto-exclude build and .gradle folders
- File size ranking

## Supported File Types

**Included:** .java, .kt, .xml, .gradle, .txt, .md, .json, .sh, .bat, .gitignore

**Excluded by default:** .jpg, .png, .mp3, .mp4, .zip, .exe, .pdf, .doc, .class, and more

## Usage

1. Enter your project folder path
2. Click "Filter File Types" to customize excluded extensions (optional)
3. Click "Start Integration"
4. Find `SourceCodeIntegration.txt` in your selected folder

## Output Format

The generated file includes:
- Project tree structure with 📁/📄 icons
- Complete content of each text file
- File size ranking

## Requirements

- Android 5.0 (API 21) or higher
- Storage permission

## Package

`com.buge.codeintegrator`

## License

GNU General Public License v3.0

© 2026 BugeStudioTeam
