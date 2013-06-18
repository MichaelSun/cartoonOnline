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

echo "cp -rf bin/cartoonOnline-release.apk $4/cartoonOnline_rosi-release_$5_$2.apk"
cp -rf bin/cartoonOnline-release.apk $4/cartoonOnline_rosi-release_$5_$2.apk

}

VERSION=`grep "android:versionName" AndroidManifest.xml | awk -F "\"" '{print $2}'`
TARGET="/Users/michael/Dropbox/apk_backup/rosi_$VERSION"
echo $TARGET
rm -rf $TARGET
cd ~/Dropbox/apk_backup/
mkdir rosi_$VERSION
cd -

makeChannelApk google_rosi google 10000 $TARGET $VERSION
