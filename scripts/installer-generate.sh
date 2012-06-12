#!/bin/bash

## RUN from main tigase-server directory

IZPACK_DIR="installer/izpack.patched"
#IZPACK_DIR="/Applications/IzPack"

# create packages directory
if [ ! -e packages ] ; then
	mkdir packages || exit -1
fi

# create logs directory
if [ ! -e logs ] ; then
	mkdir logs || exit -1
fi

# insert appropriate version information
export TIGVER=`grep -m 1 "Tigase-Version:" MANIFEST.MF | sed -e "s/Tigase-Version: \(.*\)/\\1/"`
sed -e "s/<appversion>\([^<]*\)<\/appversion>/<appversion>$TIGVER<\/appversion>/" \
    src/main/izpack/install.xml > src/main/izpack/install_copy.xml

#ant -verbose -f src/main/izpack/build.xml -Dinstaller.path=$IZPACK_DIR

# generate javadocs
export WINDOWTITLE=`grep -m 1 "javadoc-windowtitle=" build.properties | sed -e "s/javadoc-windowtitle=\(.*\)/\\1/"`
export COPYRIGHT=`grep -m 1 "javadoc-copyright=" build.properties | sed -e "s/javadoc-copyright=\(.*\)/\\1/"`
javadoc -d docs -sourcepath src/main/java/ -subpackages tigase -windowtitle "$WINDOWTITLE" -overview package.html -bottom "$COPYRIGHT" -use -author -version -protected

# compile installer
$IZPACK_DIR/bin/compile \
     src/main/izpack/install_copy.xml \
     -h $IZPACK_DIR/ \
     -b . -o ./packages/tigase-server-$TIGVER.jar

python $IZPACK_DIR/utils/wrappers/izpack2exe/izpack2exe.py \
     --file=./packages/tigase-server-$TIGVER.jar --no-upx \
     --output=./packages/tigase-server-$TIGVER.exe

#rm -f src/main/izpack/install_copy.xml
