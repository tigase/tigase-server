#!/bin/bash

## RUN from main tigase-server directory

INSTALLER_DIR="installer"
ORIGINAL_IZPACK_DIR="izpack.original"
PATCHED_IZPACK_DIR="izpack.patched"

GIT_URL="git://github.com/izpack/izpack.git"

# create installer directory
if [ !  -e $INSTALLER_DIR ] ; then
	mkdir $INSTALLER_DIR || exit -1
fi

# create original izpack dir
if [ -e $INSTALLER_DIR/$ORIGINAL_IZPACK_DIR ] ; then
	rm -rf $INSTALLER_DIR/$ORIGINAL_IZPACK_DIR || exit -1
fi

# clone IzPack git repository
git clone $GIT_URL $INSTALLER_DIR/$ORIGINAL_IZPACK_DIR || exit -1
cd $INSTALLER_DIR/$ORIGINAL_IZPACK_DIR/
	git checkout -b original fde79de81836dbf4c594d6a6f184e27d756ae009 || exit -1
cd ../../

# create patched directory
if [ -e $INSTALLER_DIR/$PATCHED_IZPACK_DIR ] ; then
	rm -rf $INSTALLER_DIR/$PATCHED_IZPACK_DIR || exit -1
fi
cp -r $INSTALLER_DIR/$ORIGINAL_IZPACK_DIR $INSTALLER_DIR/$PATCHED_IZPACK_DIR || exit -1

#checkout correct revision of the sources from local repository
cd $INSTALLER_DIR/$PATCHED_IZPACK_DIR/
git checkout -b v4 v4.3.0
cd ../../

# apply patch
patch -d $INSTALLER_DIR/$PATCHED_IZPACK_DIR -p 1 < src/main/izpack/changes.patch || exit -1

# add custom TigasePanels
cp src/main/izpack/java/*.java $INSTALLER_DIR/$PATCHED_IZPACK_DIR/src/lib/com/izforge/izpack/panels

#copy utilities from newer sources
if [ ! -e $INSTALLER_DIR/$PATCHED_IZPACK_DIR/utils/wrappers/ ] ; then
	mkdir -p $INSTALLER_DIR/$PATCHED_IZPACK_DIR/utils/wrappers || exit -1
	cp -r $INSTALLER_DIR/$ORIGINAL_IZPACK_DIR/izpack-utils/src/main/resources/utils/wrappers $INSTALLER_DIR/$PATCHED_IZPACK_DIR/utils || exit -1
fi

# get dependencies
mvn -f modules/distribution/pom.xml dependency:copy-dependencies -DoverWriteReleases=true -DoverWriteSnapshots=true -DoutputDirectory=${PWD}/jars -Dmdep.stripVersion=true

# add tigase classes to installer build path
mkdir $INSTALLER_DIR/$PATCHED_IZPACK_DIR/tigaseLib
for tigase_lib in jars/tigase-server.jar jars/tigase-utils.jar jars/tigase-xmltools.jar
do
	cp $tigase_lib $INSTALLER_DIR/$PATCHED_IZPACK_DIR/tigaseLib/`basename $tigase_lib`
done


# make the new installer compiler
cd $INSTALLER_DIR/$PATCHED_IZPACK_DIR/src
ant dist || exit -1

cd ..
cp src/dist-files/compile bin
