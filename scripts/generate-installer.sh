#!/bin/bash

IZPACK_DIR="installer/izpack.patched"
#IZPACK_DIR="/Applications/IzPack"

# create packages directory
if [ ! -e packages ] ; then
	mkdir packages || exit -1
fi

export TIGVER=`grep -m 1 "Tigase-Version:" MANIFEST.MF | sed -e "s/Tigase-Version: \(.*\)/\\1/"`
sed -e "s/<appversion>\([^<]*\)<\/appversion>/<appversion>$TIGVER<\/appversion>/" \
    src/main/izpack/install.xml > src/main/izpack/install_copy.xml

#ant -verbose -f src/main/izpack/build.xml -Dinstaller.path=$IZPACK_DIR

$IZPACK_DIR/bin/compile \
     src/main/izpack/install_copy.xml \
     -h $IZPACK_DIR/ \
     -b . -o ./packages/tigase-server-$TIGVER.jar

python $IZPACK_DIR/utils/wrappers/izpack2exe/izpack2exe.py \
     --file=./packages/tigase-server-$TIGVER.jar --no-upx \
     --output=./packages/tigase-server-$TIGVER.exe

rm -f src/main/izpack/install_copy.xml