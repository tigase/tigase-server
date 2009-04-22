#!/bin/bash

INSTALLER_DIR="installer"
SVN_URL="http://svn.codehaus.org/izpack/izpack-src/trunk/"
REVISION=2739
ORIGINAL_IZPACK_DIR="izpack.original"
PATCHED_IZPACK_DIR="izpack.patched"

# create installer directory
#if [ ! -e $INSTALLER_DIR ] ; then
#	mkdir $INSTALLER_DIR || exit -1
#fi

# create original izpack dir
#if [ -e $INSTALLER_DIR/$ORIGINAL_IZPACK_DIR ] ; then
#	rm -rf $INSTALLER_DIR/$ORIGINAL_IZPACK_DIR || exit -1
#fi

# checkout izpack from svn repository
#svn checkout $SVN_URL --revision $REVISION $INSTALLER_DIR/$ORIGINAL_IZPACK_DIR || exit -1

# create patched directory
if [ -e $INSTALLER_DIR/$PATCHED_IZPACK_DIR ] ; then
	rm -rf $INSTALLER_DIR/$PATCHED_IZPACK_DIR || exit -1
fi
cp -r $INSTALLER_DIR/$ORIGINAL_IZPACK_DIR $INSTALLER_DIR/$PATCHED_IZPACK_DIR || exit -1

# apply patch
patch -d $INSTALLER_DIR/$PATCHED_IZPACK_DIR -p 1 < src/main/izpack/changes.patch || exit -1 

# add custom TigasePanels
cp src/main/izpack/java/*.java $INSTALLER_DIR/$PATCHED_IZPACK_DIR/src/lib/com/izforge/izpack/panels

# make the new installer compiler
cd $INSTALLER_DIR/$PATCHED_IZPACK_DIR/src
ant dist || exit -1

cd ..
cp src/dist-files/compile bin 
