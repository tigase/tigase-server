#!/bin/bash

#IZPACK_DIR="/usr/local/IzPack421"
IZPACK_DIR="/Applications/IzPack"

export TIGVER=`grep -m 1 "Tigase-Version:" MANIFEST.MF | sed -e "s/Tigase-Version: \(.*\)/\\1/"`
sed -e "s/<appversion>\([^<]*\)<\/appversion>/<appversion>$TIGVER<\/appversion>/" \
    src/main/izpack/install.xml > src/main/izpack/install_copy.xml

ant -f src/main/izpack/build.xml -Dinstaller.path=$IZPACK_DIR

$IZPACK_DIR/bin/compile \
     src/main/izpack/install_copy.xml \
     -h $IZPACK_DIR/ \
     -b . -o ./packages/tigase-server-$TIGVER.jar

python $IZPACK_DIR/utils/wrappers/izpack2exe/izpack2exe.py \
     --file=./packages/tigase-server-$TIGVER.jar --no-upx \
     --output=./packages/tigase-server-$TIGVER.exe

rm -f src/main/izpack/install_copy.xml
