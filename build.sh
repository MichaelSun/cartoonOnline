#!/bin/sh

makeChannelApk()
{
echo "current dir : `pwd`"

sed 's/UMENG_CHANNEL\"\ android:value=\"[a-z]*\"/UMENG_CHANNEL" android:value="'$1'"/' AndroidManifest.xml > Android.tmp

mv Android.tmp AndroidManifest.xml

ant clean ; ant release

echo
echo
echo
echo
echo
echo "finish build channel : $1 >>>>>>>>>"

echo "cp -rf bin/cartoonOnline-release.apk $2/cartoonOnline-release_$3_$1.apk"
cp -rf bin/cartoonOnline-release.apk $2/cartoonOnline-release_$3_$1.apk

}

VERSION=`grep "android:versionName" AndroidManifest.xml | awk -F "\"" '{print $2}'`
TARGET="/Users/michael/Dropbox/apk_backup/cartoon_$VERSION"
echo $TARGET
rm -rf $TARGET
cd ~/Dropbox/apk_backup/
mkdir cartoon_$VERSION
cd -

makeChannelApk hiapk $TARGET $VERSION
makeChannelApk google $TARGET $VERSION
makeChannelApk appchina $TARGET $VERSION
makeChannelApk gfan $TARGET $VERSION
makeChannelApk mumayi $TARGET $VERSION
makeChannelApk baidu $TARGET $VERSION
makeChannelApk anzhi $TARGET $VERSION
