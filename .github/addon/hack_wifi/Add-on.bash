# Add-on

id=hack_wifi
name="Hack Wi-Fi"
author=Kakathic
description="Wi-Fi hacking tool"
version=1.0
versionCode=101
root=true

# default
google_text="Currently using a translation tool"
wifi_text_1="List of networks"
wifi_text_2="Choose any WIFI network to try to find the password, Prioritize Wi-Fi networks with the letters WPS because they are easier to hack."
wifi_text_3="Start connecting"
wifi_text_4="No Wi-Fi network found."
wifi_text_5="Saved password"
wifi_text_6="Saved password not found."
STR_ADVANCED_CUSTOM="Advanced customization"
STR_OPTION="Option"
STR_ALL_ATTACK_METHODS="All attack methods combined"
STR_BASIC_ATTACK="Basic attack"
STR_PIN_DATABASE_ATTACK="PIN database attack"
STR_WPS_PUSH_ATTACK="Push button attack (WPS)"
STR_PIN_WARNING="Entering the correct PIN will increase the chances of a successful connection"
STR_INPUT_PIN="Enter PIN code"
STR_PIN_CODE="PIN code"

# other languages
case "$LANGUAGE-$COUNTRY" in
  vi*)
    description="Công cụ hack Wi-Fi"
    wifi_text_1="Danh sách mạng"
    wifi_text_2="Chọn mạng Wi-Fi bất kỳ để thử tìm mật khẩu, ưu tiên WI-FI có chữ WPS vì dễ hack"
    wifi_text_3="Bắt đầu kết nối"
    wifi_text_4="Không tìm thấy mạng Wi-Fi nào."
    wifi_text_5="Mật khẩu đã lưu"
    wifi_text_6="Không tìm thấy mật khẩu đã lưu."
    STR_ADVANCED_CUSTOM="Tùy chỉnh nâng cao"
    STR_OPTION="Tùy chọn"
    STR_ALL_ATTACK_METHODS="Kết hợp tất cả các phương thức"
    STR_BASIC_ATTACK="Tấn công cơ bản"
    STR_PIN_DATABASE_ATTACK="Tấn công bằng cơ sở dữ liệu PIN"
    STR_WPS_PUSH_ATTACK="Tấn công bằng nút nhấn (WPS)"
    STR_PIN_WARNING="Nhập đúng mã PIN sẽ tăng cơ hội kết nối thành công"
    STR_INPUT_PIN="Nhập mã PIN"
    STR_PIN_CODE="Mã PIN"
    ;;
  hu*)
    description="Wi-Fi feltörő eszköz"
    wifi_text_1="Hálózatlista"
    wifi_text_2="Válasszon ki egy tetszőleges Wi-Fi hálózatot a jelszó megkereséséhez, lehetőleg olyat, amelyiken van „WPS”, mivel azt könnyebb feltörni."
    wifi_text_3="Kezdje el a kapcsolatteremtést"
    wifi_text_4="Nem található Wi-Fi-hálózat."
    wifi_text_5="Mentett jelszavak"
    wifi_text_6="Nem található mentett jelszó."
    STR_ADVANCED_CUSTOM="Speciális testreszabás"
    STR_OPTION="Beállítások"
    STR_ALL_ATTACK_METHODS="Kombinálja az összes módszert"
    STR_BASIC_ATTACK="Alaptámadás"
    STR_PIN_DATABASE_ATTACK="PIN-adatbázis elleni támadás"
    STR_WPS_PUSH_ATTACK="Gombnyomásos támadás (WPS)"
    STR_PIN_WARNING="A helyes PIN-kód megadása növeli a sikeres csatlakozás esélyét"
    STR_INPUT_PIN="Adja meg a PIN-kódot"
    STR_PIN_CODE="PIN kód"
    ;;
  es*)
    description="Herramienta para hackear Wi-Fi"
    wifi_text_1="Lista de redes"
    wifi_text_2="Elige cualquier red Wi-Fi para intentar averiguar la contraseña; se recomiendan las que tienen WPS, ya que son más fáciles de acceder"
    wifi_text_3="Iniciar conexión"
    wifi_text_4="No se ha encontrado ninguna red Wi-Fi."
    wifi_text_5="Contraseñas guardadas"
    wifi_text_6="No se ha encontrado ninguna contraseña guardada."
    STR_ADVANCED_CUSTOM="Personalización avanzada"
    STR_OPTION="Opción"
    STR_ALL_ATTACK_METHODS="Combinar todos los métodos"
    STR_BASIC_ATTACK="Método básico"
    STR_PIN_DATABASE_ATTACK="Método con base de datos de PIN"
    STR_WPS_PUSH_ATTACK="Método por botón (WPS)"
    STR_PIN_WARNING="Introducir el PIN correcto aumenta las probabilidades de conectar con éxito"
    STR_INPUT_PIN="Introduce el código PIN"
    STR_PIN_CODE="Código PIN"
    ;;
  *)
    ;;
esac
