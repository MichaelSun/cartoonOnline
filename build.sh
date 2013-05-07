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

echo "cp -rf bin/cartoonOnline-release.apk $4/cartoonOnline-release_$5_$2.apk"
cp -rf bin/cartoonOnline-release.apk $4/cartoonOnline-release_$5_$2.apk

}

VERSION=`grep "android:versionName" AndroidManifest.xml | awk -F "\"" '{print $2}'`
TARGET="/Users/michael/Dropbox/apk_backup/cartoon_$VERSION"
echo $TARGET
rm -rf $TARGET
cd ~/Dropbox/apk_backup/
mkdir cartoon_$VERSION
cd -

makeChannelApk hiapk_懂你画报 hiapk 10030 $TARGET $VERSION
makeChannelApk google_懂你画报 google 10000 $TARGET $VERSION
makeChannelApk appchina_懂你画报 appchina 10050 $TARGET $VERSION
makeChannelApk gfan_懂你画报 gfan 10020 $TARGET $VERSION
makeChannelApk mumayi_懂你画报 mumayi 10070 $TARGET $VERSION
makeChannelApk baidu_懂你画报 baidu 10010 $TARGET $VERSION
makeChannelApk anzhi_懂你画报 anzhi 10010 $TARGET $VERSION
