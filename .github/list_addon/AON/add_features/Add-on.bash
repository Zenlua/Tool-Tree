# Add-on
id=add_features
name="ROM utility"
author=Kakathic
version=1.2
versionCode=102
root=false
description="Includes many features such as dex2oat, partition merging"
transai=true

# other languages
case "$LANGUAGE-$COUNTRY" in
  vi*)
    name="Tiện ích rom"
    description="Gồm nhiều tính năng như dex2oat, hợp nhất phân vùng"
    ;;
  hu*)
    description="Partíciók egyesítése, dex2oat és egyéb funkciók"
    ;;
  es*)
    description="Combinar particiones, dex2oat y otras funciones"
    ;;
  *)
    ;;
esac
