#!/bin/bash

CURDIR=`pwd`
TARGET=/usr/share/tigase
ETCDIR=/etc/tigase
LOGDIR=/var/log/tigase
VARDIR=/var/lib/tigase

if ! getent passwd tigase >/dev/null; then
	adduser --disabled-password  --quiet --system \
	--home $VARDIR \
	--gecos "Tigase XMPP server" --group tigase
fi
if ! getent group tigase >/dev/null; then
	adduser --system tigase
fi

mkdir -p $TARGET
mkdir -p $ETCDIR
mkdir -p $LOGDIR
mkdir -p $VARDIR/tigase-derbydb
mkdir -p $TARGET/jars/
mkdir -p $TARGET/libs/

cp -f $CURDIR/jars/*.jar $TARGET/jars/
cp -f $CURDIR/libs/*.jar $TARGET/libs/
cp -fr $CURDIR/certs/ $TARGET/
cp -fr $CURDIR/database/ $TARGET/
cp -fr $CURDIR/scripts/ $TARGET/

chown -R tigase:tigase $TARGET
chmod -R o-rwx $TARGET

cp -f $CURDIR/scripts/debian/init-debian.properties $ETCDIR/init.properties
cp -f $CURDIR/scripts/debian/tigase-debian.conf /etc/default/tigase
cp -f $CURDIR/scripts/debian/tigase.init.d /etc/init.d/tigase

chown -R tigase:tigase $ETCDIR
chown -R tigase:tigase $LOGDIR
chown -R tigase:tigase $VARDIR
#chmod -R o-rwx $ETCDIR
#chmod -R o-rwx $LOGDIR
#chmod -R o-rwx $VARDIR
chmod 755 /etc/init.d/tigase

update-rc.d tigase defaults
