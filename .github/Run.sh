
date(){ busybox date "$@"; }

FORCE_DEPTH=1

wget -q -O PIXEL_VERSIONS_HTML --no-check-certificate https://developer.android.com/about/versions 2>&1 || exit 1;
wget -q -O PIXEL_LATEST_HTML --no-check-certificate $(grep -o 'https://developer.android.com/about/versions/.*[0-9]"' PIXEL_VERSIONS_HTML | sort -ru | cut -d\" -f1 | head -n1) 2>&1 || exit 1;

if grep -qE 'Developer Preview|tooltip>.*preview program' PIXEL_LATEST_HTML && [ ! "$FORCE_PREVIEW" ]; then
  wget -q -O PIXEL_BETA_HTML --no-check-certificate $(grep -o 'https://developer.android.com/about/versions/.*[0-9]"' PIXEL_VERSIONS_HTML | sort -ru | cut -d\" -f1 | head -n2 | tail -n1) 2>&1 || exit 1;
else
  TITLE="Preview";
  mv -f PIXEL_LATEST_HTML PIXEL_BETA_HTML;
fi;

wget -q -O PIXEL_OTA_HTML --no-check-certificate https://developer.android.com$(grep -o 'href=".*download-ota.*"' PIXEL_BETA_HTML | cut -d\" -f2 | head -n$FORCE_DEPTH | tail -n1) 2>&1 || exit 1;
echo "$(grep -m1 -oE 'tooltip>Android .*[0-9]' PIXEL_OTA_HTML | cut -d\> -f2) $TITLE$(grep -oE 'tooltip>QPR.* Beta' PIXEL_OTA_HTML | cut -d\> -f2 | head -n$FORCE_DEPTH | tail -n1)";


BETA_REL_DATE="$(date -D '%B %e, %Y' -d "$(grep -m1 -A1 'Release date' PIXEL_OTA_HTML | tail -n1 | sed 's;.*<td>\(.*\)</td>.*;\1;')" '+%Y-%m-%d')";
BETA_EXP_DATE="$(date -D '%s' -d "$(($(date -D '%Y-%m-%d' -d "$BETA_REL_DATE" '+%s') + 60 * 60 * 24 * 7 * 6))" '+%Y-%m-%d')";
echo -e "Beta Released: $BETA_REL_DATE \
  \nEstimated Expiry: $BETA_EXP_DATE";

MODEL_LIST="$(grep -A1 'tr id=' PIXEL_OTA_HTML | grep 'td' | sed 's;.*<td>\(.*\)</td>;\1;')";
PRODUCT_LIST="$(grep -o 'ota/.*_beta' PIXEL_OTA_HTML | cut -d\/ -f2)";
OTA_LIST="$(grep 'ota/.*_beta' PIXEL_OTA_HTML | cut -d\" -f2)";


if [ -z "$PRODUCT" ]; then
  set_random_beta() {
    local list_count="$(echo "$MODEL_LIST" | wc -l)";
    local list_rand="$((RANDOM % $list_count + 1))";
    local IFS=$'\n';
    set -- $MODEL_LIST;
    MODEL="$(eval echo \${$list_rand})";
    set -- $PRODUCT_LIST;
    PRODUCT="$(eval echo \${$list_rand})";
    set -- $OTA_LIST;
    OTA="$(eval echo \${$list_rand})";
    DEVICE="$(echo "$PRODUCT" | sed 's/_beta//')";
  }
  set_random_beta;
fi;
echo "$MODEL ($PRODUCT)";

#(wget -q -O PIXEL_ZIP_METADATA --no-check-certificate $OTA) 2>/dev/null;
curl -s -L -k "$OTA" | head -c 8048 > PIXEL_ZIP_METADATA 2>/dev/null

FINGERPRINT="$(grep -am1 'post-build=' PIXEL_ZIP_METADATA 2>/dev/null | cut -d= -f2)";
SECURITY_PATCH="$(grep -am1 'security-patch-level=' PIXEL_ZIP_METADATA 2>/dev/null | cut -d= -f2)";
if [ -z "$FINGERPRINT" -o -z "$SECURITY_PATCH" ]; then
  echo "\nError: Failed to extract information from metadata!";
  exit 1;
fi;

if [[ "$FINGERPRINT" ]] && [[ "$MODEL" ]];then
echo '{
  "com.google.android.gms": {
  "FINGERPRINT": "'$FINGERPRINT'",
  "MANUFACTURER": "Google",
  "BRAND": "google",
  "MODEL": "'$MODEL'",
  "PRODUCT": "'$PRODUCT'",
  "DEVICE": "'$DEVICE'",
  "BOARD": "'$DEVICE'",
  "DEVICE_INITIAL_SDK_INT": "32",
  "SECURITY_PATCH": "'$SECURITY_PATCH'"
  }
}' | tee test.json
else
echo error9 >&2
exit 1
fi

wget -q -O PIF_METADATA --no-check-certificate https://github.com/Zenlua/Tool-Tree/releases/download/V1/PIF.json 2>/dev/null || curl -s -L -k -o PIF_METADATA https://github.com/Zenlua/Tool-Tree/releases/download/V1/PIF.json 2>/dev/null

if [[ "$(cat PIF_METADATA | grep -cm1 spoof_config)" == 1 ]];then

echo
cat PIF_METADATA

if [[ -f test.json ]];then
cat PIF_METADATA | jq '. + {
  "gms_config": "'$(cat test.json | base64 -w0)'"
}' > PIF_METADATA2
mv PIF_METADATA2 PIF_METADATA
fi

if [[ "$LIT" ]];then
cat PIF_METADATA | jq '. + {
  "spoof_config": "'$LIT'"
}' > PIF_METADATA2
mv PIF_METADATA2 PIF_METADATA
fi

if [[ "$LUN" ]];then
cat PIF_METADATA | jq '. + {
  "shell": "'$LUN'"
}' > PIF_METADATA2
mv PIF_METADATA2 PIF_METADATA
fi

if [[ "$DEL" ]];then
cat PIF_METADATA | jq 'del(.["'$DEL'"])' > PIF_METADATA2
mv PIF_METADATA2 PIF_METADATA
fi

cat PIF_METADATA | jq -r .spoof_config | base64 -d > chplay.json
jq '. + {
  "com.android.vending": {
  "FINGERPRINT": "'$FINGERPRINT'",
  "MANUFACTURER": "Google",
  "BRAND": "google",
  "MODEL": "'$MODEL'",
  "PRODUCT": "'$PRODUCT'",
  "DEVICE": "'$DEVICE'",
  "BOARD": "'$DEVICE'",
  "DEVICE_INITIAL_SDK_INT": "32",
  "SECURITY_PATCH": "'$SECURITY_PATCH'"
  }
}' chplay.json > chplay2.json

cat PIF_METADATA | jq '. + {
  "spoof_config": "'$(cat chplay2.json | base64 -w0)'"
}' > PIF_METADATA

mv PIF_METADATA PIF.json

echo
cat PIF.json
echo
cat PIF.json | jq -r .gms_config | base64 -d
echo
cat PIF.json | jq -r .spoof_config | base64 -d
echo
[ "$(jq -r .shell PIF.json)" == 'null' ] || jq -r .shell PIF.json | base64 -d

else
echo "Lỗi tải pif"
exit 1
fi
