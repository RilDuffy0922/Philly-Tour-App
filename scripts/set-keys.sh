#!/bin/bash
# Prompts for your API keys (input is hidden) and writes them into the root .env file.
set -e
cd "$(dirname "$0")/.."
FILE=.env
[ -f "$FILE" ] || cp .env.example "$FILE"

set_key() {
    local key="$1" value="$2"
    if grep -q "^${key}=" "$FILE"; then
        sed -i '' "s|^${key}=.*|${key}=${value}|" "$FILE"
    else
        echo "${key}=${value}" >> "$FILE"
    fi
}

read -r -s -p "Paste your Google Maps API key (blank to skip): " MAPS; echo
read -r -s -p "Paste your Gemini API key (blank to skip): " GEMINI; echo
read -r -s -p "Paste your ElevenLabs API key (blank to skip): " ELEVEN; echo

[ -n "$MAPS" ] && set_key MAPS_API_KEY "$MAPS"
[ -n "$GEMINI" ] && set_key GEMINI_API_KEY "$GEMINI"
[ -n "$ELEVEN" ] && set_key ELEVEN_LABS_API_KEY "$ELEVEN"

echo "Saved to $FILE (git-ignored)."
