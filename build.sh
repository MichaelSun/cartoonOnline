#!/bin/sh

makeChannelApk()
{
echo "current dir : `pwd`"

FILE=res/values/strings.xml
sed 's/umeng_channel\">.*/umeng_channel">'$1'<\/string>/' res/values/strings.xml > string.xml_tmp
mv string.xml_tmp res/values/strings.xml
sed 's/umeng_params_channel\">.*/umeng_params_channel">'$2'<\/string>/' res/values/strings.xml > string.xml_tmp
mv string.xml_tmp res/values/strings.xml
sed 's/youmi_channel\">.*/youmi_channel">'$3'<\/string>/' res/values/strings.xml > string.xml_tmp
mv string.xml_tmp res/values/strings.xml

#sed 's/UMENG_CHANNEL\"\ android:value=\"[a-z]*\"/UMENG_CHANNEL" android:value="'$1'"/' AndroidManifest.xml > Android.tmp

#mv Android.tmp AndroidManifest.xml

ant clean ; ant release

echo
echo
echo
echo
echo
echo "finish build channel : $1 >>>>>>>>>"

echo "cp -rf bin/cartoonOnline-release.apk $4/cartoonOnline_xiee-release_$5_$2.apk"
cp -rf bin/cartoonOnline-release.apk $4/cartoonOnline_xiee-release_$5_$2.apk

}

VERSION=`grep "android:versionName" AndroidManifest.xml | awk -F "\"" '{print $2}'`
TARGET="/Users/michael/Dropbox/apk_backup/manhuaOnline_$VERSION"
echo $TARGET
rm -rf $TARGET
cd ~/Dropbox/apk_backup/
mkdir manhuaOnline_$VERSION
cd -

makeChannelApk hiapk_邪恶小漫画 hiapk 10030 $TARGET $VERSION
makeChannelApk google_邪恶小漫画 google 10000 $TARGET $VERSION
makeChannelApk appchina_邪恶小漫画 appchina 10050 $TARGET $VERSION
makeChannelApk gfan_邪恶小漫画 gfan 10020 $TARGET $VERSION
makeChannelApk mumayi_邪恶小漫画 mumayi 10070 $TARGET $VERSION
makeChannelApk baidu_邪恶小漫画 baidu 10010 $TARGET $VERSION
makeChannelApk anzhi_邪恶小漫画 anzhi 10010 $TARGET $VERSION
