#!/bin/bash

TIGVER=`grep -m 1 "Tigase-Version:" MANIFEST.MF | sed -e "s/Tigase-Version: \(.*\)/\\1/"`

/Applications/IzPack/bin/compile \
     src/main/izpack/install.xml \
     -h /Applications/IzPack/ \
     -b . -o ./packages/tigase-server-$TIGVER.jar

python /Applications/IzPack/utils/izpack2exe/izpack2exe.py \
     --file=./packages/tigase-server-$TIGVER.jar --no-upx \
     --output=./packages/tigase-server-$TIGVER.exe
