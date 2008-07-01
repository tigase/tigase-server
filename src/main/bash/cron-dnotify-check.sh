#!/bin/bash

monitored_dir="/home/webapp/drupal-sites/users-list"
mydir=`dirname $0`

cron_check=`ps axw | grep -c "[d]notify -C"`

if [ $cron_check -eq 0 ] ; then 
#  echo "dnotify monitor is not running, starting a new one...."
  /usr/bin/dnotify -C -b $monitored_dir -e $mydir/users-list-moitor.sh '{}'
fi
