# Add-on
id=apk_features
name="Utility"
author=Kakathic
description="Small utilities such as speed testing, block cleanup help the device work faster"
version=1.0
versionCode=100
root=false
transai=true

# other languages
case "$LANGUAGE-$COUNTRY" in
  vi*)
    name="Tiện ích nhỏ"
    description="Các tiện ích nhỏ như kiểm tra tốc độ, dọn dẹp block giúp thiết bị hoạt động nhanh hơn"
    ;;
  hu*)
    description="A kisebb segédeszközök, mint a sebességtesztelés, a blokk tisztítása gyorsabban segítik az eszköz működését"
    ;;
  es*)
    description="Pequeñas empresas como pruebas de velocidad y limpieza de bloques ayudan a que el dispositivo funcione más rápido"
    ;;
  *)
    ;;
esac
