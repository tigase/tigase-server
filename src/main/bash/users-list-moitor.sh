#!/bin/bash

mydir=$1

if [ "$mydir" == "" ] ; then
  echo "Directory name is missing..."
  exit 1
fi

for i in $mydir/* ; do
  newuser=`basename $i`
  echo "Adding new user: "$newuser
  useradd $newuser -m
  rm -f $i
done
