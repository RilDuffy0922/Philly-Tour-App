#!/bin/bash
# Prompts for your API keys (input is hidden) and writes them into WaterwayTours/Resources/Secrets.plist.
set -e
cd "$(dirname "$0")/.."
FILE=WaterwayTours/Resources/Secrets.plist
[ -f "$FILE" ] || cp Secrets.example.plist "$FILE"

read -r -s -p "Paste your Gemini API key: " GEMINI; echo
read -r -s -p "Paste your ElevenLabs API key: " ELEVEN; echo
plutil -replace GeminiAPIKey -string "$GEMINI" "$FILE"
plutil -replace ElevenLabsAPIKey -string "$ELEVEN" "$FILE"
echo "Saved to $FILE (git-ignored)."
