# Add-on
id=overlay_create
name="Create overlay"
author=Kakathic
description="Extract the language from the apk and create an overlay, or create an overlay yourself"
version=1.0
versionCode=100
root=false
transai=true

# other languages
case "$LANGUAGE-$COUNTRY" in
  vi*)
    name="Tạo Overlay"
    description="Trích xuất ngôn ngữ từ apk và tạo overlay, hoặc tự tạo overlay"
    ;;
  hu*)
    description="Húzd ki a nyelvet az apk-ból, és hozz létre egy overlay-t, vagy magad hozz létre egy overlay-t"
    ;;
  es*)
    description="Extrae el lenguaje del apk y crea una superposición, o crea una superposición tú mismo"
    ;;
  *)
    ;;
esac
