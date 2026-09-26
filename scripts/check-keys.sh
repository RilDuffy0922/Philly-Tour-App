#!/bin/bash
# Checks the keys in WaterwayTours/Resources/Secrets.plist and says which one (if any) is wrong.
# Never prints the keys themselves.
cd "$(dirname "$0")/.."
FILE=WaterwayTours/Resources/Secrets.plist
[ -f "$FILE" ] || { echo "No $FILE. Copy Secrets.example.plist there and fill in your keys (or run scripts/set-keys.sh)."; exit 1; }

get() { plutil -extract "$1" raw "$FILE" 2>/dev/null; }
G=$(get GeminiAPIKey); E=$(get ElevenLabsAPIKey); V=$(get ElevenLabsVoiceID); M=$(get GeminiModel)
[ -n "$M" ] || M=gemini-3.8-flash
[ -n "$V" ] || V=cgSgspJ2msm6clMCkdW9

problem=0

echo "== Gemini"
if [ -z "$G" ] || [[ "$G" == YOUR_* ]]; then
  echo "FAIL: still the placeholder. Paste your real Gemini key (from aistudio.google.com/apikey)."; problem=1
else
  [[ "$G" == sk_* ]] && echo "WARN: this looks like an ElevenLabs key (starts with sk_). Did you paste it in the wrong field?"
  [[ "$G" != "${G//[[:space:]\"\']/}" ]] && echo "WARN: the key has spaces or quote marks in it. Remove them."
  echo "Key length: ${#G} (Gemini keys are usually 39 characters starting with AIza, or start with AQ.)"
  ok=0
  for i in 1 2 3 4; do
    code=$(curl -s -o /tmp/check-gemini.json -w "%{http_code}" -H "Content-Type: application/json" \
      "https://generativelanguage.googleapis.com/v1beta/models/$M:generateContent?key=$G" \
      -d '{"contents":[{"parts":[{"text":"Say hi"}]}]}')
    case $code in
      200) echo "OK: Gemini key works."; ok=1; break;;
      429|503|404) sleep 2;;   # busy or rate limited; the key itself is fine, try again
      *) break;;
    esac
  done
  if [ $ok = 0 ]; then
    problem=1
    echo "FAIL: Gemini answered HTTP $code."
    python3 -c "import json;print(json.load(open('/tmp/check-gemini.json'))['error']['message'])" 2>/dev/null
    case $code in
      400) echo "-> The key isn't valid. Make a new one at aistudio.google.com/apikey and paste the whole thing.";;
      403) echo "-> The key is blocked or was deleted. Make a new one at aistudio.google.com/apikey.";;
      429|503|404) echo "-> Google is busy or rate limiting this key right now. The key is probably fine; wait a minute and run this again.";;
    esac
  fi
fi

echo "== ElevenLabs"
if [ -z "$E" ] || [[ "$E" == YOUR_* ]]; then
  echo "FAIL: still the placeholder. Paste your real ElevenLabs key."; problem=1
else
  [[ "$E" != sk_* ]] && echo "WARN: ElevenLabs keys start with sk_. This one doesn't."
  code=$(curl -s -o /tmp/check-eleven.mp3 -w "%{http_code}" -H "xi-api-key: $E" -H "Content-Type: application/json" \
    "https://api.elevenlabs.io/v1/text-to-speech/$V?output_format=mp3_22050_32" -d '{"text":"Hi","model_id":"eleven_flash_v2_5"}')
  case $code in
    200) echo "OK: ElevenLabs key and voice work.";;
    401) echo "FAIL: ElevenLabs rejected the key (HTTP 401). It is wrong, deleted, or missing Text to Speech access."; problem=1;;
    402|404) echo "FAIL: HTTP $code. The voice ID isn't available on this account. Pick another voice."; problem=1;;
    *) echo "FAIL: ElevenLabs answered HTTP $code."; problem=1;;
  esac
fi

exit $problem
