#!/data/data/com.tool.tree/files/home/bin/bash
# Kakathic

export EXECUTOR_PATH="$({EXECUTOR_PATH})"
export START_DIR="$({START_DIR})"
export DARK_MODE="$({DARK_MODE})"
export NO_BACKUP="$({NO_BACKUP_DIR})"
export ANDROID_ID="$({android_id})"
export ANDROID_DIR="$({ANDROID_DIR})"
export ANDROID_UID="$({ANDROID_UID})"
export ANDROID_MANUFACTURER="$({ANDROID_MANUFACTURER})"
export ANDROID_MODEL="$({ANDROID_MODEL})"
export ANDROID_BRAND="$({ANDROID_BRAND})"
export ANDROID_DEVICE="$({ANDROID_DEVICE})"
export ANDROID_CPU_ABI="$({ANDROID_CPU_ABI})"
export ANDROID_FINGERPRINT="$({ANDROID_FINGERPRINT})"
export ANDROID_RELEASE="$({ANDROID_RELEASE})"
export PATH_APK_APP="$({PATH_APK_APP})"
export API="$({ANDROID_SDK})"
export LANGUAGE="$({LANGUAGE})"
export COUNTRY="$({COUNTRY})"
export SDCARD_PATH="$({SDCARD_PATH})"
export APP_ID="$({APP_USER_ID})"
export ROOT=$({ROOT_PERMISSION})
export TEMP="$({TEMP_DIR})"
export TIMEZONE="$({TIMEZONE})"
export HOME="$({BIN})"
export PACKAGE_NAME="$({PACKAGE_NAME})"
export PACKAGE_VERSION_NAME="$({PACKAGE_VERSION_NAME})"
export PACKAGE_VERSION_CODE="$({PACKAGE_VERSION_CODE})"
export TERMUX="$HOME/termux"
export ETC="$HOME/etc"
export BIN="$HOME/bin"
export LOG="$HOME/log"
export JAVA_HOME="$TERMUX"
export PYTHONHOME="$TERMUX"
export SDH="$HOME/TREE"
export APK="$HOME/TOOL"
export AON="$HOME/ZADD"
export AOK="$HOME/ZAPK"
export TMPDIR="$HOME/tmp"
export LIB="$HOME/lib"
export TMP="$TMPDIR"
export SDC="$SDCARD_PATH/TREE"
export START_TIME=$(date +%s)
export PIP_ROOT_USER_ACTION=ignore
# export LD_LIBRARY_PATH="$LIB"

if [ ! -f $LOG/PATH ]; then
export PATH="$BIN:$TERMUX/bin:$TERMUX/py:$PATH"
else
source $LOG/PATH
fi

export PTSD="$(glog PTSD $SDC/ROM 2>/dev/null)"; # $PTSD
export PTSH="$(glog PTSH ROM 2>/dev/null)"; # $SDH/$PTSH
export PTAD="$(glog PTAD $SDC/APK 2>/dev/null)"; # $PTAD
export PTAH="$(glog PTAH APK 2>/dev/null)"; # $APK/$PTAH

[[ -z "$PAGE_CONFIG_DIR" ]] && unset PAGE_CONFIG_DIR
[[ -z "$PAGE_WORK_DIR" ]] && unset PAGE_WORK_DIR
[[ -z "$PAGE_CONFIG_FILE" ]] && unset PAGE_CONFIG_FILE
[[ -z "$PAGE_WORK_FILE" ]] && unset PAGE_WORK_FILE

export WEBS="User-Agent: Mozilla/5.0 (Linux; Android $ANDROID_RELEASE; $ANDROID_MANUFACTURER $ANDROID_MODEL) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"

if [ "$ROOT" == 'true' ];then
export ROT=1
else
export ROT=0
export xu=xu # fake root proot
fi

if [ "$ANDROID_CPU_ABI" != 'arm64-v8a' ];then
echo "Only arm64-v8a devices supported"
sleep 10
exitapp exit
exit 1
else
export ARCH=arm64
fi

if [ -f "$1" ];then
export shell_progres="$2";
cd "$HOME";
source "$1";
rm -f "$1";
else
echo "Error file" 1>&2
sleep 5
exitapp exit
fi
