#!/bin/bash

mydir=$1

if [ "$mydir" == "" ] ; then
  echo "Directory name is missing..."
  exit 1
fi

for i in $mydir/* ; do
  newuser=`basename $i`
  echo "Adding new user: "$newuser
  /usr/sbin/useradd $newuser -m
  rm -f $i
  /usr/bin/mail -s "Welcome to Tigase LiveCD" -t ${newuser}@livecd.tigase.org <<EOFMAIL
Hi,

This is an automated message sent to every new user registered on the LiveCD.

Please enjoy the system and send me your comments.

Artur Hefczyc
.
EOFMAIL
done
