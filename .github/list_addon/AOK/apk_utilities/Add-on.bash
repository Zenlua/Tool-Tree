# Add-on
id=apk_utilities
name="Apk patch"
author=Kakathic
description="Patches after decrypting the apk, filtering out the language"
version=1.0
versionCode=100
root=false

case "$LANGUAGE-$COUNTRY" in
  vi*)
    name="Bản vá Apk"
    description="Bản vá sau khi giải mã apk, lọc bỏ ngôn ngữ"
    ;;
  *)
    ;;
esac
