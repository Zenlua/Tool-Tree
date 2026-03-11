#!/data/data/com.tool.tree/files/home/bin/bash
# Kakathic

set -o pipefail

vlua="$2"
clsu="$3"

# Thư mục hiện tại
MPAT="${0%/*}"

MIUIGallery(){
ii="$(find $pproduct -type f -name "MIUIGallery.apk" -print -quit)"
if [ "$(echo "$ii" | grep -cm1 'data-app')" == 1 ];then
mv "${ii%/*}" "$pproduct/app"
ii="$(find $pproduct -type f -name "MIUIGallery.apk" -print -quit)"
fi
oi="$MPAT/apk/$(basename "$ii" .apk)"
[ "$(check_props MIUIGallery)" == "fix_mapcn×1" ] && { echo "$patch_text ${ii##*/} ✓"; exit; }
echo -e "$patch_text1 ${ii##*/}..."
apkeditor_d -i "$ii" -o "${oi%/*}" -t raw &>$TMP/apk_patch_ximi.log || killtree "Error decompile $ii\n\n$(cat $TMP/apk_patch_ximi.log)"
# Patch smali
patch_smali "$oi/smali/classes/miuix/os/xBuild.smali"

if [ "$fix_mapcn" == 1 ] && [ "$(check_props fix_mapcn)" != 1 ];then
Thayvc 0 'method public static checkMapAvailable()Z' $oi/smali/classes*/com/miui/gallery/map/utils/MapInitializerImpl.smali
Thaythe 'Lcom/miui/gallery/util/BuildUtil;->isGlobal()Z' 'Lmiuix/os/xBuild;->isOne()Z' $oi/smali/classes*/com/miui/gallery/ui/featured/type/ItemTypeSortManager.smali
fi

# End patch smali
apkeditor_b -i "$oi" -o "${ii%/*}" -d 1 -x false &>$TMP/apk_patch_ximi.log || killtree "Error build ${ii%/*}\n\n$(cat $TMP/apk_patch_ximi.log)"
sprop "#MIUIGallery" "fix_mapcn×$fix_mapcn" "$psystem/build.prop"
}

Joyose(){
ii="$(find $pproduct/app $psystem/app $pproduct/pangu -type f -name "Joyose.apk" -print -quit)"
oi="$MPAT/apk/$(basename "$ii" .apk)"
[ "$(check_props Joyose)" == "fix_joyose×1" ] && { echo "$patch_text ${ii##*/} ✓"; exit; }
echo -e "$patch_text1 ${ii##*/}..."
apkeditor_d -i "$ii" -o "${oi%/*}" -t raw &>$TMP/apk_patch_ximi.log || killtree "Error decompile $ii\n\n$(cat $TMP/apk_patch_ximi.log)"
# Patch smali

if [ "$fix_joyose" == 1 ] && [ "$(check_props fix_joyose)" != 1 ];then
patgpu="$(Timkiem GPUTUNER_SWITCH $oi/smali/classes)"
sed -i "`grep -nA2 GPUTUNER_SWITCH $patgpu | grep -m1 getString | cut -d- -f1`i\ const/4 v0, 0x1 \n return v0" $patgpu || about "Error: GPUTUNER_SWITCH"
sed -i "`grep -nA2 SUPPORT_UGD $patgpu | grep -m1 getString | cut -d- -f1`i\ const/4 v0, 0x1 \n return v0" $patgpu || about "Error: SUPPORT_UGD"
Thayvc -v 'method public run()V' $(Timkiem "job exist, sync local" $oi/smali/classes)
fi

# End patch smali
apkeditor_b -i "$oi" -o "${ii%/*}" -d 1 -x false &>$TMP/apk_patch_ximi.log || killtree "Error build ${ii%/*}\n\n$(cat $TMP/apk_patch_ximi.log)"
sprop "#Joyose" "fix_joyose×$fix_joyose" "$psystem/build.prop"
}

Weather(){
ii="$(find $pproduct -type f -name "*Weather.apk" -print -quit)"
if [ "$(echo "$ii" | grep -cm1 'data-app')" == 1 ];then
mv "${ii%/*}" "$pproduct/app"
ii="$(find $pproduct -type f -name "*Weather.apk" -print -quit)"
fi
oi="$MPAT/apk/$(basename "$ii" .apk)"
[ "$(check_props Weather)" == "fix_thoit×1" ] && { echo "$patch_text ${ii##*/} ✓"; exit; }
echo -e "$patch_text1 ${ii##*/}..."
apkeditor_d -i "$ii" -o "${oi%/*}" -t raw &>$TMP/apk_patch_ximi.log || killtree "Error decompile $ii\n\n$(cat $TMP/apk_patch_ximi.log)"
# Patch smali
patch_smali "$oi/smali/classes/miuix/os/xBuild.smali"

if [ "$fix_thoit" == 1 ] && [ "$(check_props fix_thoit)" != 1 ];then
Thaythe 'Lmiui/os/Build;->IS_INTERNATIONAL_BUILD:Z' 'Lmiuix/os/xBuild;->isOne:Z' "$oi/smali/classes*/com/miui/weather2/view/onOnePage/DailyForecastTable.smali
$oi/smali/classes*/com/miui/weather2/structures/AQIData.smali
$oi/smali/classes*/com/miui/weather2/view/onOnePage/RealTimeDetailCard.smali
$oi/smali/classes*/com/miui/weather2/view/WeatherScrollView.smali
$(Timkiem 'https://weatherapi.market.xiaomi.com/' $oi/smali)"
fi

# End patch smali
apkeditor_b -i "$oi" -o "${ii%/*}" -d 1 -x false &>$TMP/apk_patch_ximi.log || killtree "Error build ${ii%/*}\n\n$(cat $TMP/apk_patch_ximi.log)"
sprop "#Weather" "fix_thoit×$fix_thoit" "$psystem/build.prop"
}

ThemeManager(){
IFS=$'\n'
ii="$(find $pproduct/app -type f -name "*ThemeManager.apk" -print -quit)"
oi="$MPAT/apk/$(basename "$ii" .apk)"
[ "$(check_props ThemeManager)" == "fix_themes×1" ] && { echo "$patch_text ${ii##*/} ✓"; exit; }
echo -e "$patch_text1 ${ii##*/}..."
apkeditor_d -i "$ii" -o "${oi%/*}" -t raw &>$TMP/apk_patch_ximi.log || killtree "Error decompile $ii\n\n$(cat $TMP/apk_patch_ximi.log)"
# Patch smali

if [ "$fix_themes" == 1 ] && [ "$(check_props fix_themes)" != 1 ];then
Tmtong="$(Timkiem '$onlineThemeDetail' $oi/smali)"
if [ "$Tmtong" ]; then
for vv in $(grep 'Lcom/android/thememanager/detail/theme/model/OnlineResourceDetail;->bought:Z' $Tmtong); do
luv="$(echo "$vv" | awk '{print $2}')"
Thaythe "$vv" "$vv \n const/4 $luv 0x1" $Tmtong
done
fi
Tmtong2="$(Timkiem 'OnlineResourceDetailPresenter_code_changed' $oi/smali)"
if [ "$Tmtong2" ]; then
for vv2 in $(grep 'Lcom/android/thememanager/detail/theme/model/OnlineResourceDetail;->bought:Z' $Tmtong2); do
luv2="$(echo "$vv2" | awk '{print $2}')"
Thaythe "$vv2" "$vv2 \n const/4 $luv2 0x1" $Tmtong2
done
fi
Tmtong3="$(Timkiem 'miui.systemui.plugin.theme_adapter_os_version' $oi/smali)"
if [ "$Tmtong3" ]; then
for vv3 in $(grep 'Lcom/android/thememanager/activity/ai/.*.:Lcom/android/thememanager/activity/ai/.*.;' $Tmtong3); do
luv3="$(echo "$vv3" | awk '{print $2}')"
Thaythe "$vv3" "const/4 $luv3 0x1 \n return ${luv3/,/} \n $vv3" $Tmtong3
done
fi
pathanhk="$(Timkiem 'Apply Failed for rightPath is null or rightPath file not exist' $oi/smali)"
if [ "$pathanhk" ]; then
for vv4 in $(grep -B 2 'const-string .*., "Apply Failed for rightPath is null or rightPath file not exist"' $pathanhk | grep ':' | awk '{print $1}'); do
sed -i "/$vv4/d" $pathanhk || about "Error: pathanhk"
done
fi
Thayvc 0 '.method public isVideoAd()Z' $oi/smali/classes*/com/android/thememanager/basemodule/ad/model/AdInfo.smali
Thayvc 0 '.method private static isAdValid(Lcom/android/thememanager/basemodule/ad/model/AdInfo;)Z' $oi/smali/classes*/com/android/thememanager/basemodule/ad/model/AdInfoResponse.smali
Thayvc 0 '.method public isAuthorizedResource()Z' $oi/smali/classes*/com/android/thememanager/basemodule/resource/model/Resource.smali
Thayvc 0 '.method public static final themeManagerSupportPaidWidget(Landroid/content/Context;)Z' $oi/smali/classes*/com/miui/maml/widget/edit/MamlutilKt.smali
Thaythe '>DRM_ERROR_UNKNOWN' '>DRM_SUCCESS' "$(Timkiem DRM_ERROR_UNKNOWN $oi/smali)"
fi
# End patch smali
apkeditor_b -i "$oi" -o "${ii%/*}" -d 1 -x false &>$TMP/apk_patch_ximi.log || killtree "Error build ${ii%/*}\n\n$(cat $TMP/apk_patch_ximi.log)"
sprop "#ThemeManager" "fix_themes×$fix_themes" "$psystem/build.prop"
}

PersonalAssistant(){
ii="$(find $pproduct/priv-app -type f -name "*PersonalAssistant*.apk" -print -quit)"
oi="$MPAT/apk/$(basename "$ii" .apk)"
[ "$(check_props PersonalAssistant)" == "fix_appvault×1" ] && { echo "$patch_text ${ii##*/} ✓"; exit; }
echo -e "$patch_text1 ${ii##*/}..."
apkeditor_d -i "$ii" -o "${oi%/*}" -t raw &>$TMP/apk_patch_ximi.log || killtree "Error decompile $ii\n\n$(cat $TMP/apk_patch_ximi.log)"
# Patch smali

if [ "$fix_appvault" == 1 ] && [ "$(check_props fix_appvault)" != 1 ];then
Thayvc 0 '.method public static final themeManagerSupportPaidWidget(Landroid/content/Context;)Z' $oi/smali/classes*/com/miui/maml/widget/edit/MamlutilKt.smali
Thayvc 0 '.method public final isPay()Z' $oi/smali/classes*/com/miui/personalassistant/picker/business/detail/bean/PickerDetailResponse.smali
Thayvc 1 '.method public final isBought()Z' $oi/smali/classes*/com/miui/personalassistant/picker/business/detail/bean/PickerDetailResponse.smali
Thayvc 0 '.method public final isPay()Z' $oi/smali/classes*/com/miui/personalassistant/picker/business/detail/bean/PickerDetailResponseWrapper.smali
Thayvc 1 '.method public final isBought()Z' $oi/smali/classes*/com/miui/personalassistant/picker/business/detail/bean/PickerDetailResponseWrapper.smali
Thayvc 1 '.method private final isCanDownload(Lcom/miui/personalassistant/picker/business/detail/bean/PickerDetailResponse;)Z' $oi/smali/classes*/com/miui/personalassistant/picker/business/detail/utils/PickerDetailDownloadManager\$Companion.smali
Thayvc 1 '.method public static final isCanAutoDownloadMaMl()Z' $oi/smali/classes*/com/miui/personalassistant/picker/business/detail/utils/PickerDetailUtil.smali
Thayvc 0 '.method private final isTargetPositionMamlPayAndDownloading(I)Z' $oi/smali/classes*/com/miui/personalassistant/picker/business/detail/PickerDetailViewModel.smali
Thaythe 'MM:dd' 'dd:MM' "$(Timkiem '"MM:dd"' $oi/smali)"
fi

# End patch smali
apkeditor_b -i "$oi" -o "${ii%/*}" -d 1 -x false &>$TMP/apk_patch_ximi.log || killtree "Error build ${ii%/*}\n\n$(cat $TMP/apk_patch_ximi.log)"
sprop "#PersonalAssistant" "fix_appvault×$fix_appvault" "$psystem/build.prop"
}

Settings(){
IFS=$'\n'
ii="$psystem_ext/priv-app/Settings/Settings.apk"
oi="$MPAT/apk/Settings"
[ "$(check_props Settings)" == "fix_global×1 fix_ime×1" ] && { echo "$patch_text ${ii##*/} ✓"; exit; }
echo -e "$patch_text1 ${ii##*/}..."
apkeditor_d -i "$ii" -o "${oi%/*}" -t reso &>$TMP/apk_patch_ximi.log || killtree "Error decompile $ii\n\n$(cat $TMP/apk_patch_ximi.log)"
# Patch smali
patch_smali "$oi/smali/classes/miuix/os/xBuild.smali"

if [ "$fix_ime" == 1 ] && [ "$(check_props fix_ime)" != 1 ];then
Thaythe com.iflytek.inputmethod.miui "$(glog ime_app)" "$(Timkiem com.iflytek.inputmethod.miui "$oi/smali" | sed '/MecBoardInputController/d')"
Thayvc 1 '.method public static isMiuiImeBottomSupport()Z' $oi/smali/classes*/com/android/settings/inputmethod/InputMethodFunctionSelectUtils.smali
fi

if [ "$fix_global" == 1 ] && [ "$(check_props fix_global)" != 1 ];then
Thaycn '.method public static isNeedRemoveKidSpaceInternal(Landroid/content/Context;)Z' 'Lmiui/os/Build;->IS_INTERNATIONAL_BUILD:Z' \
'Lmiuix/os/xBuild;->isOne:Z' $oi/smali/classes*/com/android/settings/utils/SettingsFeatures.smali
Thaythe 'sget-boolean v0, Lmiui/os/Build;->IS_GLOBAL_BUILD:Z' 'sget-boolean v0, Lmiuix/os/xBuild;->isOne:Z' $oi/smali/classes*/com/android/settings/MiuiSettings.smali
Thayvc 1 '.method public static isLocationNeeded(Landroid/content/Context;)Z' $oi/smali/classes*/com/android/settings/utils/SettingsFeatures.smali
Thayvc 1 '.method public static isPrivacyNeeded(Landroid/content/Context;)Z' $oi/smali/classes*/com/android/settings/utils/SettingsFeatures.smali

# Thêm gr telegram
Thayvc 1 '.method public static isVisible(Landroid/content/Context;)Z' $oi/smali/classes*/com/android/settings/device/MiuiGuaranteeCard.smali
Thayme '.method public onClick(Landroid/view/View;)V
    .registers 5
    invoke-virtual {p0}, Lcom/android/settings/device/MiuiGuaranteeCard;->getContext()Landroid/content/Context;
    move-result-object v0
    if-eqz v0, :cond_0
    :try_start_0
    new-instance v1, Landroid/content/Intent;
    const-string v2, "android.intent.action.VIEW"
    invoke-direct {v1, v2}, Landroid/content/Intent;-><init>(Ljava/lang/String;)V
    const-string v2, "tg://resolve?domain=tooltree"
    invoke-static {v2}, Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;
    move-result-object v2
    invoke-virtual {v1, v2}, Landroid/content/Intent;->setData(Landroid/net/Uri;)Landroid/content/Intent;
    const/high16 v2, 0x10000000
    invoke-virtual {v1, v2}, Landroid/content/Intent;->setFlags(I)Landroid/content/Intent;
    invoke-virtual {v0, v1}, Landroid/content/Context;->startActivity(Landroid/content/Intent;)V
    goto :goto_0
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0
    :catch_0
    move-exception v1
    invoke-virtual {v1}, Ljava/lang/Exception;->printStackTrace()V
    :cond_0
    :goto_0
    return-void
.end method' $oi/smali/classes*/com/android/settings/device/MiuiGuaranteeCard.smali

Thayme '.method private initView()V
    .registers 4
    iget-object v0, p0, Landroid/widget/FrameLayout;->mContext:Landroid/content/Context;
    invoke-static {v0}, Lcom/android/settings/device/MiuiGuaranteeCard;->isVisible(Landroid/content/Context;)Z
    move-result v0
    if-nez v0, :cond_e
    const/16 v0, 0x8
    invoke-virtual {p0, v0}, Landroid/widget/FrameLayout;->setVisibility(I)V
    return-void
    :cond_e
    iget-object v0, p0, Landroid/widget/FrameLayout;->mContext:Landroid/content/Context;
    const-string/jumbo v1, "micare_expiry_time"
    invoke-static {v0, v1}, Lcom/android/settings/device/MiCareUtils;->getMiCareInfoWithPrefKey(Landroid/content/Context;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v0
    iput-object v0, p0, Lcom/android/settings/device/MiuiGuaranteeCard;->mMiCareExpiryTime:Ljava/lang/String;
    iget-object v0, p0, Landroid/widget/FrameLayout;->mContext:Landroid/content/Context;
    const-string/jumbo v1, "micare_link"
    invoke-static {v0, v1}, Lcom/android/settings/device/MiCareUtils;->getMiCareInfoWithPrefKey(Landroid/content/Context;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v0
    iput-object v0, p0, Lcom/android/settings/device/MiuiGuaranteeCard;->mMiCareLink:Ljava/lang/String;
    iget-object v0, p0, Landroid/widget/FrameLayout;->mContext:Landroid/content/Context;
    invoke-static {v0}, Landroid/view/LayoutInflater;->from(Landroid/content/Context;)Landroid/view/LayoutInflater;
    move-result-object v0
    sget v1, Lcom/android/settings/R$layout;->my_device_info_item:I
    const/4 v2, 0x1
    invoke-virtual {v0, v1, p0, v2}, Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;Z)Landroid/view/View;
    invoke-virtual {p0, p0}, Landroid/widget/FrameLayout;->setOnClickListener(Landroid/view/View$OnClickListener;)V
    sget v0, Lcom/android/settings/R$id;->title:I
    invoke-virtual {p0, v0}, Landroid/widget/FrameLayout;->findViewById(I)Landroid/view/View;
    move-result-object v0
    check-cast v0, Landroid/widget/TextView;
    new-instance v1, Ljava/lang/StringBuilder;
    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V
    const-string/jumbo v2, "Kakathic"
    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v1
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
    sget v0, Lcom/android/settings/R$id;->summary:I
    invoke-virtual {p0, v0}, Landroid/widget/FrameLayout;->findViewById(I)Landroid/view/View;
    move-result-object v0
    check-cast v0, Landroid/widget/TextView;
    new-instance v1, Ljava/lang/StringBuilder;
    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V
    const-string/jumbo v2, "@tooltree"
    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object p0
    invoke-virtual {v0, p0}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
    return-void
.end method' $oi/smali/classes*/com/android/settings/device/MiuiGuaranteeCard.smali

# thông tin
if [ "$(grep -cm1 device_description_cpu "$oi/resources/package_1/res/values/strings.xml")" == 0 ];then
smurl1="$(find $oi/smali/classes*/com/android/settings/device/DeviceParamsInitHelper.smali -type f)"
if [ -f "$smurl1" ];then
for vnl in $(grep -B 2 -n "langType" "$smurl1" | tac | grep 'move-result-object'); do
sed -i "$(echo "$vnl" | cut -d- -f1)a\ const-string $(echo "$vnl" | awk '{print $3}'), \"enUS\"" "$smurl1"
done
fi

Timueelnd="$(find $oi/smali/classes*/com/android/settings/device/DeviceBasicInfoPresenter.smali -type f)"
if [ -f "$Timueelnd" ];then
    thdbdbi1="$(grep -A 2 'Lcom/android/settings/device/ParseMiShopDataUtils;->getItemTitle(Lorg/json/JSONObject;)Ljava/lang/String;' "$Timueelnd" | grep move-result-object | awk '{print $2}')"
    thdbdbi2="$(grep 'Lcom/android/settings/device/ParseMiShopDataUtils;->getItemTitle(Lorg/json/JSONObject;)Ljava/lang/String;' "$Timueelnd" | awk '{print $2}' | sed "s|{|{$thdbdbi1, |")"
    if [ "$thdbdbi1" ];then
    Thaythe "$(grep 'Lcom/android/settings/device/ParseMiShopDataUtils;->getItemTitle(Lorg/json/JSONObject;)Ljava/lang/String;' "$Timueelnd")" "
    iget-object $thdbdbi1, p0, Lcom/android/settings/device/DeviceBasicInfoPresenter;->mContext:Landroid/content/Context;
    invoke-static $thdbdbi2 Lcom/android/settings/device/ParseMiShopDataUtils;->getItemTitle(Landroid/content/Context;Lorg/json/JSONObject;)Ljava/lang/String;" "$Timueelnd"
    fi
    idkfjf1="$(grep -nA 20 'sget .*., Lcom/android/settings/R$string;->camera_front:I' "$Timueelnd" | grep 'const-string .*., ""')"
    if [ "$idkfjf1" ];then
    sed -i "$(echo "$idkfjf1" | cut -d- -f1)c\ const-string $(echo "$idkfjf1" | awk '{print $3}') \" \"" "$Timueelnd"
    fi
fi

Thayme '.method public static getItemTitle(Landroid/content/Context;Lorg/json/JSONObject;)Ljava/lang/String;
    .registers 4
    invoke-static {p1}, Lcom/android/settings/device/ParseMiShopDataUtils;->getItemIndex(Lorg/json/JSONObject;)I
    move-result v0
    const/4 v1, 0x0
    if-eq v0, v1, :cond_17
    const/4 v1, 0x1
    if-eq v0, v1, :cond_22
    const/4 v1, 0x3
    if-eq v0, v1, :cond_2d
    const/4 v1, 0x4
    if-eq v0, v1, :cond_38
    const-string v1, "Title"
    invoke-static {p1, v1}, Lcom/android/settings/device/JSONUtils;->getString(Lorg/json/JSONObject;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v0
    return-object v0
    :cond_17
    invoke-virtual {p0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;
    move-result-object v1
    sget p0, Lcom/android/settings/R$string;->device_description_cpu:I
    invoke-virtual {v1, p0}, Landroid/content/res/Resources;->getString(I)Ljava/lang/String;
    move-result-object v0
    return-object v0
    :cond_22
    invoke-virtual {p0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;
    move-result-object v1
    sget p0, Lcom/android/settings/R$string;->device_description_battery:I
    invoke-virtual {v1, p0}, Landroid/content/res/Resources;->getString(I)Ljava/lang/String;
    move-result-object v0
    return-object v0
    :cond_2d
    invoke-virtual {p0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;
    move-result-object v1
    sget p0, Lcom/android/settings/R$string;->device_description_screen:I
    invoke-virtual {v1, p0}, Landroid/content/res/Resources;->getString(I)Ljava/lang/String;
    move-result-object v0
    return-object v0
    :cond_38
    invoke-virtual {p0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;
    move-result-object v1
    sget p0, Lcom/android/settings/R$string;->device_description_resolution:I
    invoke-virtual {v1, p0}, Landroid/content/res/Resources;->getString(I)Ljava/lang/String;
    move-result-object v0
    return-object v0
.end method' $oi/smali/classes*/com/android/settings/device/ParseMiShopDataUtils.smali
    Thaythe '</resources>' '    <string name="device_description_cpu" public="false">CPU</string>
    <string name="device_description_resolution" public="false">Resolution</string>
    <string name="device_description_screen" public="false">Display</string>
    </resources>' "$oi/resources/package_1/res/values/strings.xml"
    fi
fi
# End patch smali
apkeditor_b -i "$oi" -o "${ii%/*}" -d 1 &>$TMP/apk_patch_ximi.log || killtree "Error build ${ii%/*}\n\n$(cat $TMP/apk_patch_ximi.log)"
sprop "#Settings" "fix_global×$fix_global fix_ime×$fix_ime" "$psystem/build.prop"
}

MiuiSystemUI(){
ii="$psystem_ext/priv-app/MiuiSystemUI/MiuiSystemUI.apk"
oi="$MPAT/apk/MiuiSystemUI"
[ "$(check_props MiuiSystemUI)" == "fix_noti×1 fix_ime×1" ] && { echo "$patch_text ${ii##*/} ✓"; exit; }
echo -e "$patch_text1 ${ii##*/}..."
apkeditor_d -i "$ii" -o "${oi%/*}" -t raw &>$TMP/apk_patch_ximi.log || killtree "Error decompile $ii\n\n$(cat $TMP/apk_patch_ximi.log)"
# Patch smali
patch_smali "$oi/smali/classes/miuix/os/xBuild.smali"

if [ "$fix_ime" == 1 ] && [ "$(check_props fix_ime)" != 1 ];then
Thaythe 'Lmiuix/os/Build;->IS_INTERNATIONAL_BUILD:Z' 'Lmiuix/os/xBuild;->isZero()Z' "$oi/smali/classes*/com/android/systemui/navigationbar/NavigationBar.smali"
fi

if [ "$fix_noti" == 1 ] && [ "$(check_props fix_noti)" != 1 ];then
Thaythe 'Lcom/miui/utils/configs/MiuiConfigs;->IS_INTERNATIONAL_BUILD:Z' 'Lmiuix/os/xBuild;->isOne:Z' \
"$oi/smali/classes*/com/android/systemui/statusbar/notification/utils/NotificationUtil.smali
$oi/smali/classes*/com/miui/systemui/notification/MiuiBaseNotifUtil.smali
$oi/smali/classes*/com/miui/systemui/notification/NotificationSettingsManager.smali
$oi/smali/classes*/com/android/systemui/statusbar/notification/interruption/MiuiNotificationInterruptStateProviderImpl.smali"
Thaythe 'Lmiui/os/Build;->IS_INTERNATIONAL_BUILD:Z' 'Lmiuix/os/xBuild;->isOne:Z' "$oi/smali/classes*/com/miui/keyguard/biometrics/fod/MiuiGxzwQuickOpenUtil.smali
$oi/smali/classes*/com/android/systemui/assist/PhoneStateMonitor.smali"
fi

# End patch smali
apkeditor_b -i "$oi" -o "${ii%/*}" -d 1 &>$TMP/apk_patch_ximi.log || killtree "Error build ${ii%/*}\n\n$(cat $TMP/apk_patch_ximi.log)"
sprop "#MiuiSystemUI" "fix_noti×$fix_noti fix_ime×$fix_ime" "$psystem/build.prop"
}

services(){
ii="$psystem/framework/services.jar"
oi="$MPAT/apk/services"
[ "$(check_props services)" == "fix_apksign×1 fix_show_error×1 fix_screen×1" ] && { echo "$patch_text ${ii##*/} ✓"; exit; }
echo -e "$patch_text1 ${ii##*/}..."
apkeditor_d -i "$ii" -o "${oi%/*}" -t raw &>$TMP/apk_patch_ximi.log || killtree "Error decompile $ii\n\n$(cat $TMP/apk_patch_ximi.log)"
# Patch smali

if [ "$fix_screen" == 1 ] && [ "$(check_props fix_screen)" != 1 ];then
Thayvc 0 '.method isSecureLocked()Z' $oi/smali/classes*/com/android/server/wm/WindowState.smali
fi

if [ "$fix_show_error" == 1 ] && [ "$(check_props fix_show_error)" != 1 ];then
Thayvc -v '.method public showSystemReadyErrorDialogsIfNeeded()V' $oi/smali/classes*/com/android/server/wm/ActivityTaskManagerService\$LocalService.smali
fi

if [ "$fix_apksign" == 1 ] && [ "$(check_props fix_apksign)" != 1 ];then
Thayme '.method static constructor <clinit>()V
    .locals 0
    const/4 v0, 0x1
    sput-boolean v0, Lcom/android/server/pm/ReconcilePackageUtils;->ALLOW_NON_PRELOADS_SYSTEM_SHAREDUIDS:Z
    return-void
.end method' $oi/smali/classes*/com/android/server/pm/ReconcilePackageUtils.smali

url_jsdhdh="$(find $oi/smali/classes*/com/android/server/pm/InstallPackageHelper.smali -type f)"
if [ -f "$url_jsdhdh" ];then
kk_tssjjd="$(grep -n 'Lcom/android/server/pm/pkg/AndroidPackage;->isLeavingSharedUser()Z' "$url_jsdhdh" | tail -n1 | cut -d: -f1)"
ss_sjjdhx="$(nl -ba "$url_jsdhdh" | sed -n ''$((kk_tssjjd - 5))','$kk_tssjjd'p' | grep -m1 'move-result')"
sed -i "$(echo "$ss_sjjdhx" | awk '{print $1}')a\ const/4 $(echo "$ss_sjjdhx" | awk '{print $3}'), 0x1" "$url_jsdhdh" || about "Error patch: $url_jsdhdh"
fi

Thayvc -v '.method public static checkDowngrade(Lcom/android/server/pm/pkg/AndroidPackage;Landroid/content/pm/PackageInfoLite;)V' $oi/smali/classes*/com/android/server/pm/PackageManagerServiceUtils.smali
Thayvc 0 '.method public static verifySignatures(Lcom/android/server/pm/PackageSetting;Lcom/android/server/pm/SharedUserSetting;Lcom/android/server/pm/PackageSetting;Landroid/content/pm/SigningDetails;ZZZ)Z' $oi/smali/classes*/com/android/server/pm/PackageManagerServiceUtils.smali
Thayvc 1 '.method private static matchSignaturesCompat(Ljava/lang/String;Lcom/android/server/pm/PackageSignatures;Landroid/content/pm/SigningDetails;)Z' $oi/smali/classes*/com/android/server/pm/PackageManagerServiceUtils.smali
Thayvc 0 '.method public shouldCheckUpgradeKeySetLocked(Lcom/android/server/pm/pkg/PackageStateInternal;Lcom/android/server/pm/pkg/SharedUserApi;I)Z' $oi/smali/classes*/com/android/server/pm/KeySetManagerService.smali
fi

# End patch smali
apkeditor_b -i "$oi" -o "${ii%/*}" -d 1 &>$TMP/apk_patch_ximi.log || killtree "Error build ${ii%/*}\n\n$(cat $TMP/apk_patch_ximi.log)"
sprop "#services" "fix_apksign×$fix_apksign fix_show_error×$fix_show_error fix_screen×$fix_screen" "$psystem/build.prop"
}

miui-framework(){
ii="$psystem_ext/framework/miui-framework.jar"
oi="$MPAT/apk/miui-framework"
[ "$(check_props miui-framework)" == "fix_global×1 fix_reset_theme×1 fix_ime×1" ] && { echo "$patch_text ${ii##*/} ✓"; exit; }
echo -e "$patch_text1 ${ii##*/}..."
apkeditor_d -i "$ii" -o "${oi%/*}" -t raw &>$TMP/apk_patch_ximi.log || killtree "Error decompile $ii\n\n$(cat $TMP/apk_patch_ximi.log)"
# Patch smali
patch_smali "$oi/smali/classes/miuix/os/xBuild.smali"

if [ "$fix_ime" == 1 ] && [ "$(check_props fix_ime)" != 1 ];then
Thaythe com.iflytek.inputmethod.miui "$(glog ime_app)" "$(Timkiem com.iflytek.inputmethod.miui "$oi/smali")"
if [ "$(gprop ro.miui.support_miui_ime_bottom "$psystem/build.prop")" != 1 ];then
sprop "ro.miui.support_miui_ime_bottom" 1 "$psystem/build.prop"
cp -rf "$MPAT/mod/GestureLineOverlay.apk" "$pproduct/overlay"
fi
fi

if [ "$fix_reset_theme" == 1 ] && [ "$(check_props fix_reset_theme)" != 1 ];then
Thayvc -v '.method private.*.validateTheme(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;Ljava/util/Map;)V' $oi/smali/classes/miui/drm/ThemeReceiver.smali
fi

if [ "$fix_global" == 1 ] && [ "$(check_props fix_global)" != 1 ];then
cp -rf "$MPAT/mod/eu" "$oi/smali/classes"
Thaythe 'Lmiui/os/Build;->IS_INTERNATIONAL_BUILD:Z' 'Lmiuix/os/xBuild;->isOne:Z' "$oi/smali/classes*/miui/util/font/MultiLangHelper.smali
$oi/smali/classes*/com/android/internal/app/ChooserActivityStubImpl.smali
$oi/smali/classes*/miui/util/font/SymlinkUtils.smali
$oi/smali/classes*/android/app/AppOpsManagerInjector.smali"
fi
# End patch smali
apkeditor_b -i "$oi" -o "${ii%/*}" -d 1 &>$TMP/apk_patch_ximi.log || killtree "Error build ${ii%/*}\n\n$(cat $TMP/apk_patch_ximi.log)"
sprop "#miui-framework" "fix_global×$fix_global fix_reset_theme×$fix_reset_theme fix_ime×$fix_ime" "$psystem/build.prop"
}

miui-services(){
ii="$psystem_ext/framework/miui-services.jar"
oi="$MPAT/apk/miui-services"
[ "$(check_props miui-services)" == "fix_global×1 fix_noti×1 fix_window×1 fix_ime×1 fix_screen×1 fix_apksign×1" ] && { echo "$patch_text ${ii##*/} ✓"; exit; }
echo -e "$patch_text1 ${ii##*/}..."
apkeditor_d -i "$ii" -o "${oi%/*}" -t raw &>$TMP/apk_patch_ximi.log || killtree "Error decompile $ii\n\n$(cat $TMP/apk_patch_ximi.log)"
# Patch smali
patch_smali "$oi/smali/classes/miuix/os/xBuild.smali"

if [ "$fix_apksign" == 1 ] && [ "$(check_props fix_apksign)" != 1 ];then
[ "$APIs" -ge 35 ] && Thayvc -v '.method private verifyIsolationViolation(Lcom/android/internal/pm/parsing/pkg/ParsedPackage;Lcom/android/server/pm/InstallSource;)V' $oi/smali/classes*/com/android/server/pm/PackageManagerServiceImpl.smali
Thayvc -v '.method public canBeUpdate(Ljava/lang/String;)V' $oi/smali/classes*/com/android/server/pm/PackageManagerServiceImpl.smali
fi

if [ "$fix_screen" == 1 ] && [ "$(check_props fix_screen)" != 1 ];then
Thayvc 0 '.method public notAllowCaptureDisplay(Lcom/android/server/wm/RootWindowContainer;I)Z' $oi/smali/classes*/com/android/server/wm/WindowManagerServiceImpl.smali
fi

if [ "$fix_ime" == 1 ] && [ "$(check_props fix_ime)" != 1 ];then
Thaythe com.iflytek.inputmethod.miui "$(glog ime_app)" "$(Timkiem com.iflytek.inputmethod.miui "$oi/smali" | grep -i "Input")"
fi

if [ "$fix_window" == 1 ] && [ "$(check_props fix_window)" != 1 ];then
Thayvc 6 '.method public getMaxMiuiFreeFormStackCount(Ljava/lang/String;Lcom/android/server/wm/MiuiFreeFormActivityStack;)I' $oi/smali/classes*/com/android/server/wm/MiuiFreeFormStackDisplayStrategy.smali
fi

if [ "$fix_noti" == 1 ] && [ "$(check_props fix_noti)" != 1 ];then
Thaythe 'Lmiui/os/Build;->IS_INTERNATIONAL_BUILD:Z' 'Lmiuix/os/xBuild;->isOne:Z' "$oi/smali/classes*/com/android/server/notification/NotificationManagerServiceImpl.smali
$oi/smali/classes*/com/android/server/am/ProcessSceneCleaner.smali
$oi/smali/classes*/com/android/server/am/ProcessManagerService.smali
$oi/smali/classes*/com/android/server/am/BroadcastQueueModernStubImpl.smali"
fi

if [ "$fix_global" == 1 ] && [ "$(check_props fix_global)" != 1 ];then
Thayvc 1 '.method public static isCTS()Z' $oi/smali/classes*/com/android/server/pm/PackageManagerServiceImpl.smali
Thaythe 'Lmiui/os/Build;->IS_INTERNATIONAL_BUILD:Z' 'Lmiuix/os/xBuild;->isOne:Z' "$oi/smali/classes/com/android/server/ForceDarkAppListManager.smali"
fi
# End patch smali
apkeditor_b -i "$oi" -o "${ii%/*}" -d 1 &>$TMP/apk_patch_ximi.log || killtree "Error build ${ii%/*}\n\n$(cat $TMP/apk_patch_ximi.log)"
sprop "#miui-services" "fix_global×$fix_global fix_noti×$fix_noti fix_window×$fix_window fix_ime×$fix_ime fix_screen×$fix_screen fix_apksign×$fix_apksign" "$psystem/build.prop"
}

PowerKeeper(){
ii="$psystem/app/PowerKeeper/PowerKeeper.apk"
oi="$MPAT/apk/PowerKeeper"
[ "$(check_props PowerKeeper)" == "fix_noti×1 fix_fps×1" ] && { echo "$patch_text ${ii##*/} ✓"; exit; }
echo -e "$patch_text1 ${ii##*/}..."
apkeditor_d -i "$ii" -o "${oi%/*}" -t raw &>$TMP/apk_patch_ximi.log || killtree "Error decompile $ii\n\n$(cat $TMP/apk_patch_ximi.log)"
# Patch smali
patch_smali "$oi/smali/classes/miuix/os/xBuild.smali"

if [ "$fix_fps" == 1 ] && [ "$(check_props fix_fps)" != 1 ];then
Thayvc 0 '.method public getDisplayCtrlCode()I' $oi/smali/classes*/com/miui/powerkeeper/feedbackcontrol/ThermalManager.smali
Thayvc -v '.method public displayControl(I)V' $oi/smali/classes*/com/miui/powerkeeper/feedbackcontrol/ThermalManager.smali
Thayvc -v '.method public setScreenEffect(II)V' $oi/smali/classes*/com/miui/powerkeeper/statemachine/DisplayFrameSetting.smali
Thayvc -v '.method private setScreenEffectInternal(IILjava/lang/String;)V' $oi/smali/classes*/com/miui/powerkeeper/statemachine/DisplayFrameSetting.smali
Thayvc -v '.method private setScreenEffect(Ljava/lang/String;II)V' $oi/smali/classes*/com/miui/powerkeeper/statemachine/DisplayFrameSetting.smali
fi

#$oi/smali/classes*/com/miui/powerkeeper/utils/GmsObserver.smali
if [ "$fix_noti" == 1 ] && [ "$(check_props fix_noti)" != 1 ];then
Thaythe 'Lmiui/os/Build;->IS_INTERNATIONAL_BUILD:Z' 'Lmiuix/os/xBuild;->isOne:Z' "$oi/smali/classes*/com/miui/powerkeeper/statemachine/PhoneSleepModeController\$SleepHandler.smali
$oi/smali/classes*/com/miui/powerkeeper/statemachine/PhoneSleepModeController.smali
$oi/smali/classes*/com/miui/powerkeeper/statemachine/PadSleepModeController\$SleepHandler.smali
$oi/smali/classes*/com/miui/powerkeeper/statemachine/PadSleepModeController.smali"
fi

# End patch smali
apkeditor_b -i "$oi" -o "${ii%/*}" -d 1 &>$TMP/apk_patch_ximi.log || killtree "Error build ${ii%/*}\n\n$(cat $TMP/apk_patch_ximi.log)"
sprop "#PowerKeeper" "fix_noti×$fix_noti fix_fps×$fix_fps" "$psystem/build.prop"
}

FrequentPhrase(){
ii="$(find $pproduct/app -type f -name "*FrequentPhrase.apk" -print -quit)"
oi="$MPAT/apk/$(basename "$ii" .apk)"
[ "$(check_props FrequentPhrase)" == "fix_ime×1" ] && { echo "$patch_text ${ii##*/} ✓"; exit; }
echo -e "$patch_text1 ${ii##*/}..."
apkeditor_d -i "$ii" -o "${oi%/*}" -t reso &>$TMP/apk_patch_ximi.log || killtree "Error decompile $ii\n\n$(cat $TMP/apk_patch_ximi.log)"
# Patch smali

if [ "$fix_ime" == 1 ] && [ "$(check_props fix_ime)" != 1 ];then
Thaythe com.iflytek.inputmethod.miui "$(glog ime_app)" "$(Timkiem com.iflytek.inputmethod.miui "$oi/smali")"
Thaythe "</resources>" '<color name="input_bottom_background_color">'"$(glog ime_color_dark)"'</color>
</resources>' "$oi/resources/package_1/res/values-night/colors.xml"
Thaythe "</resources>" '<color name="input_bottom_background_color">'"$(glog ime_color)"'</color>
</resources>' "$oi/resources/package_1/res/values/colors.xml"
Thaythe "</resources>" ''"$(glog ime_dimen)"'
</resources>' "$oi/resources/package_1/res/values/dimens.xml"
fi

# End patch smali
apkeditor_b -i "$oi" -o "${ii%/*}" -d 1 &>$TMP/apk_patch_ximi.log || killtree "Error build ${ii%/*}\n\n$(cat $TMP/apk_patch_ximi.log)"
sprop "#FrequentPhrase" "fix_ime×$fix_ime" "$psystem/build.prop"
}

framework(){
ii="$psystem/framework/framework.jar"
oi="$MPAT/apk/framework"
[ "$(check_props framework)" == "fix_fwko×1 fix_apksign×1" ] && { echo "$patch_text ${ii##*/} ✓"; exit; }
echo -e "$patch_text1 ${ii##*/}..."
apkeditor_d -i "$ii" -o "${oi%/*}" -t raw &>$TMP/apk_patch_ximi.log || killtree "Error decompile $ii\n\n$(cat $TMP/apk_patch_ximi.log)"
# Patch smali

if [ "$fix_apksign" == 1 ] && [ "$(check_props fix_apksign)" != 1 ];then
gjsg_jsdhdh="$(find $oi/smali/classes*/com/android/internal/pm/pkg/parsing/ParsingPackageUtils.smali -type f 2>/dev/null)"
if [ -f "$gjsg_jsdhdh" ];then
ccbb="$(grep -n 'Landroid/content/pm/parsing/FrameworkParsingPackageUtils;->validateName(Landroid/content/pm/parsing/result/ParseInput;Ljava/lang/String;ZZ)Landroid/content/pm/parsing/result/ParseResult;' "$gjsg_jsdhdh" | cut -d: -f1)"
ht22jhh="$(nl -ba "$gjsg_jsdhdh" | sed -n "$ccbb,$((ccbb + 9))p" | grep -m1 'move-result ')"
sed -i "$(echo "$ht22jhh" | awk '{print $1}')a\ const/4 $(echo "$ht22jhh" | awk '{print $3}'), 0x0" "$gjsg_jsdhdh" || about "Error patch: $gjsg_jsdhdh"
fi
Thayvc 1 '.method private static.*.verifyMessageDigest([B[B)Z' $oi/smali/classes*/android/util/jar/StrictJarVerifier.smali
file_fjsjsf="$(find $oi/smali/classes*/android/util/jar/StrictJarFile.smali -type f)"
if [ -f "$file_fjsjsf" ];then
gggansg="$(grep -n -A 6 'Landroid/util/jar/StrictJarFile;->findEntry(Ljava/lang/String;)Ljava/util/zip/ZipEntry;' "$file_fjsjsf" | grep -m1 'if-eqz' | cut -d- -f1)"
sed -i "${gggansg}d" "$file_fjsjsf" || about "Error patch: $file_fjsjsf"
fi
url_cccccsdg="$(find $oi/smali/classes*/android/content/pm/PackageParser.smali -type f)"
if [ -f "$url_cccccsdg" ];then
testhhhhf="$(grep -n -B 9 '<manifest> specifies bad sharedUserId name' "$url_cccccsdg" | grep 'if-nez')"
sed -i "$(echo "$testhhhhf" | cut -d- -f1)i\ const/4 $(echo "$testhhhhf" | awk '{print $3}') 0x1" "$url_cccccsdg" || about "Error patch: $url_cccccsdg"
fi
Thayvc 0 '.method public static.*.getMinimumSignatureSchemeVersionForTargetSdk(I)I' $oi/smali/classes*/android/util/apk/ApkSignatureVerifier.smali
sed -i '/invoke-static {[^}]*}, Landroid\/util\/apk\/ApkSignatureVerifier;->verifyV1Signature(Landroid\/content\/pm\/parsing\/result\/ParseInput;Ljava\/lang\/String;Z)Landroid\/content\/pm\/parsing\/result\/ParseResult;/s/invoke-static {\([^,]*\), \([^,]*\), \([vp][0-9]\+\)}/const\/4 \3, 0x0\n    invoke-static {\1, \2, \3}/' $oi/smali/classes*/android/util/apk/ApkSignatureVerifier.smali || about "Error patch: ApkSignatureVerifier.smali"
sed -i '/Ljava\/security\/MessageDigest;->isEqual(\[B\[B)Z/{n;s/move-result \([vp][0-9]\+\)/&\n    const\/4 \1, 0x1/}' $oi/smali/classes*/android/util/apk/ApkSigningBlockUtils.smali || about "Error patch: ApkSigningBlockUtils.smali"
sed -i '/Ljava\/security\/MessageDigest;->isEqual(\[B\[B)Z/{n;s/move-result \([vp][0-9]\+\)/&\n    const\/4 \1, 0x1/}' $oi/smali/classes*/android/util/apk/ApkSignatureSchemeV3Verifier.smali || about "Error patch: ApkSignatureSchemeV3Verifier.smali"
sed -i '/Ljava\/security\/MessageDigest;->isEqual(\[B\[B)Z/{n;s/move-result \([vp][0-9]\+\)/&\n    const\/4 \1, 0x1/}' $oi/smali/classes*/android/util/apk/ApkSignatureSchemeV2Verifier.smali || about "Error patch: ApkSignatureSchemeV2Verifier.smali"
Thayvc 1 '.method public.*.checkCapability(Landroid/content/pm/SigningDetails;I)Z' $oi/smali/classes*/android/content/pm/SigningDetails.smali
Thayvc 1 '.method public.*.checkCapability(Ljava/lang/String;I)Z' $oi/smali/classes*/android/content/pm/SigningDetails.smali
Thayvc 1 '.method public.*.checkCapabilityRecover(Landroid/content/pm/SigningDetails;I)Z' $oi/smali/classes*/android/content/pm/SigningDetails.smali
Thayvc 1 '.method public.*.hasAncestorOrSelf(Landroid/content/pm/SigningDetails;)Z' $oi/smali/classes*/android/content/pm/SigningDetails.smali
Thayvc 1 '.method public.*.checkCapability(Landroid/content/pm/PackageParser$SigningDetails;I)Z' $oi/smali/classes*/android/content/pm/PackageParser\$SigningDetails.smali
Thayvc 1 '.method public.*.checkCapability(Ljava/lang/String;I)Z' $oi/smali/classes*/android/content/pm/PackageParser\$SigningDetails.smali
Thayvc 1 '.method public.*.checkCapabilityRecover(Landroid/content/pm/PackageParser$SigningDetails;I)Z' $oi/smali/classes*/android/content/pm/PackageParser\$SigningDetails.smali
sed -i 's/iput \([vp][0-9]\+\), .*PackageParser\$PackageParserException;->error:I/const\/4 \1, 0x0\n&/' $oi/smali/classes*/android/content/pm/PackageParser\$PackageParserException.smali || about "Error patch: PackageParser\$PackageParserException.smali"
fi

if [ "$fix_fwko" == 1 ] && [ "$(check_props fix_fwko)" != 1 ];then

if ! ls "$oi"/smali/classes*/com/android/internal/util/kaorios &>/dev/null; then
mkdir -p "$psystem/priv-app/KaoriosToolbox/lib/arm64"
cp -rf "$MPAT/mod/KaoriosToolbox.apk" "$psystem/priv-app/KaoriosToolbox"
unzip -qj "$MPAT/mod/KaoriosToolbox.apk" lib/arm64-v8a/* -d "$psystem/priv-app/KaoriosToolbox/lib/arm64"
cp -rf "$MPAT/mod/com.kousei.kaorios.xml" "$psystem/etc/permissions"
kkklast=$(ls -1d "$oi"/smali/classes* 2>/dev/null | sort | tail -n1)
cp -rf "$MPAT/mod/classes.dex" "$oi/dex/classes$(( ${kkklast##*classes} + 1 )).dex"
fi

path_smali_4="$(find $oi/smali/classes*/android/security/KeyStore2.smali -type f)"
[ -f "$path_smali_4" ] && sed -i '/\.method public .*getKeyEntry/,/\.end method/ s/check-cast \([vp][0-9][0-9]*\), Landroid\/system\/keystore2\/KeyEntryResponse;/check-cast \1, Landroid\/system\/keystore2\/KeyEntryResponse;\
invoke-static {\1}, Lcom\/android\/internal\/util\/kaorios\/KaoriKeyboxHooks;->KaoriGetKeyEntry(Landroid\/system\/keystore2\/KeyEntryResponse;)Landroid\/system\/keystore2\/KeyEntryResponse;\
move-result-object \1/' "$path_smali_4" || about "Error method public .*getKeyEntry"
path_smali_3="$(find $oi/smali/classes*/android/security/keystore2/AndroidKeyStoreSpi.smali -type f)"
if [ -f "$path_smali_3" ];then
sed -i '/\.method public .*engineGetCertificateChain/,/\.end method/{
/\.locals/a\ invoke-static {}, Lcom/android/internal/util/kaorios/KaoriPropsUtils;->KaoriGetCertificateChain()V
}' "$path_smali_3" || about "Error method public .*engineGetCertificateChain"
so_dong_1="$(nl -ba "$path_smali_3" | sed -n '/\.method public .*engineGetCertificateChain/,/\.end method/p' | grep -A2 'aput-object' | grep -B2 'return-object' | awk '/return-object/{print $1;exit}')"
sed -i "${so_dong_1}s/return-object \([vp][0-9]\+\)/invoke-static {\1}, Lcom\/android\/internal\/util\/kaorios\/KaoriKeyboxHooks;->KaoriGetCertificateChain([Ljava\/security\/cert\/Certificate;)[Ljava\/security\/cert\/Certificate;\nmove-result-object \1\nreturn-object \1/" "$path_smali_3" || about "Error method public .*engineGetCertificateChain 2"
fi
path_smali_1="$(find $oi/smali/classes*/android/app/ApplicationPackageManager.smali -type f)"
if [ -f "$path_smali_1" ];then
sed -i '/\.method public .*hasSystemFeature(Ljava\/lang\/String;)Z/,/\.end method/{
s/\(move-result \([vp][0-9]\+\)\)/&\
:try_start\
invoke-static {\2, p1}, Lcom\/android\/internal\/util\/kaorios\/KaoriPropsUtils;->KaoriFeatureBlock(ZLjava\/lang\/String;)Z\
move-result \2\
:try_end\
.catchall {:try_start .. :try_end} :after_appinj\
:after_appinj/
}' "$path_smali_1" || about "Error method public .*hasSystemFeature"
sed -i '/\.method public .*hasSystemFeature(Ljava\/lang\/String;I)Z/,/\.end method/{
/\.locals [0-9]\+/a\
invoke-static {}, Landroid/app/ActivityThread;->currentPackageName()Ljava/lang/String;\
move-result-object v0\
:try_start_kaori\
iget-object v1, p0, Landroid/app/ApplicationPackageManager;->mContext:Landroid/app/ContextImpl;\
invoke-static {v1, p1, v0}, Lcom/android/internal/util/kaorios/KaoriFeatureOverrides;->getOverride(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Boolean;\
move-result-object v0\
:try_end_kaori\
.catchall {:try_start_kaori .. :try_end_kaori} :catch_kaori\
if-eqz v0, :cond_kaori\
invoke-virtual {v0}, Ljava/lang/Boolean;->booleanValue()Z\
move-result v0\
return v0\
:catch_kaori\
:cond_kaori
}' "$path_smali_1" || about "Error method public .*hasSystemFeature 2"
fi
path_smali_2="$(find $oi/smali/classes*/android/app/Application.smali -type f)"
[ -f "$path_smali_2" ] && sed -i '/iput-object .* Landroid\/app\/Application;->mLoadedApk:Landroid\/app\/LoadedApk;/a\
:try_start\
invoke-static {p0}, Lcom/android/internal/util/kaorios/KaoriPropsUtils;->KaoriProps(Landroid/content/Context;)V\
:try_end\
.catchall {:try_start .. :try_end} :after_appinj\
:after_appinj' "$path_smali_2" || about "Error method iput-object mLoadedApk"
fi

# End patch smali
apkeditor_b -i "$oi" -o "${ii%/*}" -d 1 &>$TMP/apk_patch_ximi.log || killtree "Error build ${ii%/*}\n\n$(cat $TMP/apk_patch_ximi.log)"
sprop "#framework" "fix_fwko×$fix_fwko fix_apksign×$fix_apksign" "$psystem/build.prop"
}

home_poco(){
if [ "$vlua" == "check" ];then
[ -f "$psystem/build.prop" ] && grep -cm1 'ro.miui.product.home=com.miui.home' "$psystem/build.prop"
exit
fi
file_ext_ss="$psystem_ext/etc/init/init.miui.ext.rc"
if [ -f "$file_ext_ss" ];then
sed -i "s|com.mi.android.globallauncher|com.miui.home|g" "$file_ext_ss"
fi
sprop "ro.miui.product.home" "com.miui.home" "$psystem/build.prop"
}

del_app(){
echo "$SEARCHING_AND_DELETING"
echo
for vv in $del_app; do
find $psystem $pproduct $psystem_ext -type d -name "$vv" -print -exec rm -rf {} +
done
}

rw_rom(){
[ -d "$pvendor" ] || killtree "$NOT_FOUND_TEXT vendor"
[ -d "$pvendor_boot" ] || echo "$RW_ROM_TEXT_1" >&2
[ -d "$pmi_ext" ] || echo "$RW_ROM_TEXT_2" >&2
for vv in $pvendor/etc/fstab.* $pvendor_boot/ramdisk/first_stage_ramdisk/fstab.*; do
if [ -f "$vv" ];then
[ "$(grep '/system .*.discard' "$vv" | grep -cm1 ext4)" == 1 ] || echo "$RW_ROM_TEXT_3 $vv" >&2
sed -i '/camera/!s/^\(overlay.*\)/#\1/' "$vv"
fi

done
# di chuyển pangu và dọn dẹp
if [ -d $pproduct/pangu/system ];then
cp -rf $pproduct/pangu/system/* $psystem 2>/dev/null
rm -fr $pproduct/pangu/system
fi
# copy mi_ext
if [ -d "$pmi_ext" ];then
cp -rf $pmi_ext/product/* $pproduct 2>/dev/null
cp -rf $pmi_ext/system_ext/* $psystem_ext 2>/dev/null
cp -rf $pmi_ext/system/* $psystem 2>/dev/null
rm -fr $pmi_ext/product/* $pmi_ext/system_ext/* $pmi_ext/system/*
fi
sprop "#rw_rom" 1 "$psystem/build.prop"
}

crypto_prop(){
if [ "$vlua" == "check" ];then
[ -f "$psystem/build.prop" ] && grep -cm1 'ro.crypto.state=encrypted' "$psystem/build.prop"
exit
fi
[ -f "$psystem/build.prop" ] && sprop ro.crypto.state encrypted "$psystem/build.prop"
}

device_features(){
if [ "$vlua" == "check" ];then
ffhnhg="$(find $pproduct/etc/device_features/*.xml -type f -print -quit)"
[ -f "$ffhnhg" ] && grep -cm1 'support_ota_validate">false' "$ffhnhg"
exit
fi
for vv in $pproduct/etc/device_features/*.xml; do
[ -f "$vv" ] && sed -i "s|support_ota_validate\">true|support_ota_validate\">false|" "$vv"
done
}

patch_prop(){
# Check ro.control_privapp_permissions
if [ "$vlua" == "check" ];then
[ -f "$psystem/build.prop" ] && gprop ro.control_privapp_permissions "$psystem/build.prop"
exit
fi
[ -d "$pvendor" ] || killtree "$NOT_FOUND_TEXT vendor"
for vv in $pvendor/build.prop $psystem_ext/etc/build.prop $pproduct/etc/build.prop; do
[ -f "$vv" ] && sed -i "/ro.control_privapp_permissions/d" "$vv"
done
[ -f "$psystem/build.prop" ] && sprop ro.control_privapp_permissions $patch_prop "$psystem/build.prop"
}

check_prop(){ [ -f "$psystem/build.prop" ] && gprop "#$vlua" "$psystem/build.prop"; }
set_prop(){ [ -f "$psystem/build.prop" ] && sprop "#$vlua" 1 "$psystem/build.prop"; }
check_props(){ [ -f "$psystem/build.prop" ] && gprop "#$1" "$psystem/build.prop"; }
check_nums(){ [ -f "$psystem/build.prop" ] && grep -c "$vlua×1" "$psystem/build.prop"; }

Timkiem(){ grep -rl --include="*.*" "$1" $2; }
about(){ echo -e "$1" >&2; }

patch_smali(){
if [ ! -f "$1" ];then
mkdir -p "${1%/*}"
echo '.class public Lmiuix/os/xBuild;
.super Ljava/lang/Object;

# static fields
.field public static final isOne:Z = true
.field public static final isZero:Z

# invoke-static {}, Lmiuix/os/xBuild;->isOne()Z
# move-result v0
.method public static isOne()Z
    .registers 1
    const/4 v0, 0x1
    return v0
.end method
# invoke-static {}, Lmiuix/os/xBuild;->isZero()Z
# move-result v0
.method public static isZero()Z
    .registers 1
    const/4 v0, 0x0
    return v0
.end method' > "$1";
fi
}

Thaycn(){
urlsmali="$(find $4 -type f -print -quit 2>/dev/null)"
vcv="$(echo "$1" | head -n1 | sed 's|/|\\/|g')"
[ -f "$urlsmali" ] && sed -i "/$vcv/,/\.end method/ s|sget-boolean \(.*\), $2|sget-boolean \1, $3|" "$urlsmali" || about "Error: $urlsmali"
}

Thayme(){
urlsmali="$(find $2 -type f 2>/dev/null)"
for vv in $urlsmali; do
if [ -f "$vv" ];then
vcv="$(echo "$1" | head -n1 | sed -e 's|/|\\/|g' -e 's|\[|\\[|g')"
sed -i "/$vcv/,/.end method/d" "$vv" && echo "$1" >> "$vv" || about "Error: $vv"
fi
done
}

Thaythe(){
if [ "$3" ];then
for vv in $(find $3 -type f 2>/dev/null); do
[ -f "$vv" ] && toybox sed -i "s!$1!$2!" "$vv" || about "Error: $vv"
done
fi
}

Thayvc(){
urlsmali="$(find $3 -type f 2>/dev/null)"
[ -f "$urlsmali" ] || about "File not found: $3"
if [ "$(grep -cm1 "${2//\[/\\[}" "$urlsmali")" == 1 ];then
if [ "$1" == "-v" ];then
Thayme "$(grep -m1 "${2//\[/\\[}" "$urlsmali")
    .locals 1
    return-void
.end method" "$urlsmali"
else
[ $1 -ge 8 ] && ui=16 || ui=4
Thayme "$(grep -m1 "${2//\[/\\[}" "$urlsmali")
    .locals 1
    const/$ui v0, 0x$1
    return v0
.end method" "$urlsmali"
fi
else
about "Error not found: $2 - $urlsmali"
fi
}

tao_oat(){
export features_oat="-a53,-crc,-lse,-fp16,-dotprod,-sve"
export PTSH="${PTROM##*/}"
if [ "$oat_fw_at" == 1 ];then
echo -e "$CREATING_OAT_TEXT framework, service...\n"
export services_switch=1
export framework_switch=1
$AON/add_features/bin/dex2oat
fi
if [ "$list_oat_tex" ];then
export services_switch=0
export framework_switch=0
echo -e "$CREATING_OAT_TEXT app...\n"
for vv in $list_oat_tex; do
if [ "${vv##*/}" == 'MiuiSystemUI.apk' ];then
export secontex="PCL[]{PCL[/system_ext/framework/extphonelib.jar]#PCL[/system_ext/app/miuisystem/miuisystem.apk]}"
elif [ "${vv##*/}" == 'Settings.apk' ];then
export secontex="PCL[]{PCL[/system/framework/org.apache.http.legacy.jar]#PCL[/system_ext/framework/com.xiaomi.slalib.jar]#PCL[/system_ext/priv-app/RtMiCloudSDK/RtMiCloudSDK.apk]{PCL[/system_ext/app/miuisystem/miuisystem.apk]}#PCL[/system_ext/framework/gson.jar]#PCL[/system_ext/framework/MiuiSettingsSearchLib.jar]#PCL[/system_ext/app/miuisystem/miuisystem.apk]#PCL[/system/system_ext/framework/com.xiaomi.nfc.jar]}"
else
    if [ "$mi_secontex" ];then
    export secontex="$mi_secontex"
    else
    export secontex=''
    fi
fi
export apps_apk_oat="$vv"
$AON/add_features/bin/dex2oat
done
fi
}

# Ngôn ngữ mặc định
eval "$(grep '="' "$MPAT/addon.prop")"
# Google dịch
if [ "$(glog "auto_trans_text_${MPAT##*/}")" == 1 ];then
[ -f "$MPAT/auto.sh" ] && source "$MPAT/auto.sh"
fi

# Tìm path
PTROM="$(glog patch_rom_path)"
# system
psystem="$(ls -1d $PTROM/*/system/build.prop 2>/dev/null | grep -m1 'system' | sed 's|\/build.prop||')"
pvendor="$(ls -1d $PTROM/*endo*/build.prop 2>/dev/null | grep -m1 'vendor' | sed 's|\/build.prop||')"
#system_ext
psystem_ext="$(ls -1d $PTROM/*ystem_ex*/etc/build.prop 2>/dev/null | sed 's|\/etc/build.prop||' | grep -m1 'system_ext')"
[ -z "$psystem_ext" ] && psystem_ext="$(ls -1d $PTROM/*/*ystem_ex*/etc/build.prop 2>/dev/null | grep -m1 'system_ext' | sed 's|\/etc/build.prop||')"
#product
pproduct="$(ls -1d $PTROM/*roduc*/etc/build.prop 2>/dev/null | grep -m1 'product' | sed 's|\/etc/build.prop||')"
[ -z "$pproduct" ] && pproduct="$(ls -1d $PTROM/*/*roduc*/etc/build.prop 2>/dev/null | grep -m1 'product' | sed 's|\/etc/build.prop||')"
pvendor_boot="$(ls -1d $PTROM/*/vendor_boot.img 2>/dev/null | grep -m1 'vendor_boot' | sed 's|\/vendor_boot.img||')"
pmi_ext="$(ls -1d $PTROM/*i_ex*/etc/build.prop 2>/dev/null | grep -m1 'mi_ext' | sed 's|\/etc/build.prop||')"

# check
[ -d "$psystem" ] || killtree "$NOT_FOUND_TEXT system"
[ -d "$psystem_ext" ] || killtree "$NOT_FOUND_TEXT system_ext"
[ -d "$pproduct" ] || killtree "$NOT_FOUND_TEXT product"

# Lấy api sdk
APIs="$(gprop "ro.system.build.version.sdk" "$psystem/build.prop")"

# index
if [ "$(type -t "$1")" = "function" ];then
"$@"
else
killtree "$NO_VALUE_TEXT"
fi

