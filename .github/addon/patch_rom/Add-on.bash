# Add-on
id=patch_rom
name="Patch ROM"
author=Kakathic
version=3.6
versionCode=361
shortcut=true
process=true
root=false
description="Includes many ROM patching features and APK patches"
transai=true

# other languages
case "$LANGUAGE-$COUNTRY" in
  vi*)
    name="Bản vá ROM"
    description="Gồm rất nhiều tính năng vá rom và các bản vá apk"
    ;;
  hu*)
    description="Számos ROM-javító és APK-javító funkciót tartalmaz"
    ;;
  es*)
    description="Incluye muchas funciones de parcheo de ROM y parches de APK"
    ;;
  *)
    ;;
esac
