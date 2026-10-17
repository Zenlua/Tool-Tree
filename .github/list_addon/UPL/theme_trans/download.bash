# Add-on

id=theme_trans
name="Theme Trans"
author=Kakathic
description="Effective widget translation tool using Gemini"
version=1.0
versionCode=103
root=true
url=https://github.com/Kakathic/Tool-Tree/releases/download/V1.0.2/${id}_$versionCode.add

# other languages
case "$LANGUAGE-$COUNTRY" in
  vi*)
name="Việt hóa widget"
description="Công cụ dịch widget hiệu quả bằng Gemini"
    ;;
  *)
    ;;
esac
