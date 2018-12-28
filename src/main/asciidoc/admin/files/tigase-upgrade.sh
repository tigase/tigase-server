#!/bin/bash
##
##  Tigase Jabber/XMPP Server
##  Copyright (C) 2004-2018 "Tigase, Inc." <office@tigase.com>
##
##  This program is free software: you can redistribute it and/or modify
##  it under the terms of the GNU General Public License as published by
##  the Free Software Foundation, either version 3 of the License.
##
##  This program is distributed in the hope that it will be useful,
##  but WITHOUT ANY WARRANTY; without even the implied warranty of
##  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
##  GNU General Public License for more details.
##
##  You should have received a copy of the GNU General Public License
##  along with this program. Look for COPYING file in the top folder.
##  If not, see http://www.gnu.org/licenses/.
##
##  $Rev: $
##  Last modified by $Author: $
##  $Date: $
##

PWD=`pwd`
RED="\033[0;31m"
GREEN="\033[0;32m"
NO_COLOR="\033[0m"
LEFT_OFFSET="80"

function usage() {
  echo "Usage: $0 {upgrade|rollback} install_package install_directory [tar|dir]"
  exit 1
}

function backup_installation() {
  local installationDir=$1;
  local backupPath=$2;
  local backupFormat=$3 || "dir";

  case "${backupFormat}" in
    dir)
    backup_installation_dir $installationDir $backupPath
    ;;
    tar)
    backup_installation_tar $installationDir "${backupPath}.tar.gz"
    ;;
    *)
    backup_installation_dir $installationDir $backupPath
    ;;
  esac
}

function backup_installation_dir() {
  local installationDir=$1;
  local backupDir=$2;
  local tmp=

  echo "Backuping installation from $installationDir to $backupDir ..."
  tmp=$(cp -pR $installationDir $backupDir 2>&1)
  if [ $? -ne 0 ]; then
    printf "${RED}%${LEFT_OFFSET}s${NO_COLOR}\n" "FAIL"
    echo -e "${RED}Failed to move backup to $backupDir${NO_COLOR}\n${tmp}"
    exit 1
  fi
  printf "${GREEN}%${LEFT_OFFSET}s${NO_COLOR}\n" "DONE";
}

function backup_installation_tar() {
  local installationDir=$1;
  local backupFile=$2;
  local tarResult=

  echo "Backuping installation from $installationDir to $backupFile ..."
  tarResult=$(COPYFILE_DISABLE=1 tar -cz -C `dirname $installationDir` -f $backupFile `basename $installationDir` 2>&1)
  if [ $? -ne 0 ]; then
    printf "${RED}%${LEFT_OFFSET}s${NO_COLOR}\n" "FAIL"
    echo -e "${RED}Failed to create archive with backup $backupFile${NO_COLOR}\n${tarResult}"
    exit 1
  fi
  printf "${GREEN}%${LEFT_OFFSET}s${NO_COLOR}\n" "DONE";
}

function download_package() {
  local url=$1;
  local result=$2;
  local tmp=`basename "$url"`;
  local curlResult=
  echo "Downloading archive $archiveFile ..."
  curlResult=$(curl --fail -O -J -L $url 2>&1)
  if [ $? -ne 0 ]; then
    printf "${RED}%${LEFT_OFFSET}s${NO_COLOR}\n" "FAIL"
    echo -e "${RED}Failed to download archive from $url${NO_COLOR}\n${curlResult}"
    exit 1
  else
    eval "$result=`pwd`/$tmp"
  fi
  printf "${GREEN}%${LEFT_OFFSET}s${NO_COLOR}\n" "DONE";
}

function unpack_archive() {
  local archive=$1;
  local homeDir=$2;
  local result=$3;
  local tarResult=
  echo "Unpacking archive $archive ..."
  tarResult=$(tar -xC $homeDir -f $archive 2>&1)
  if [ $? -ne 0 ]; then
    printf "${RED}%${LEFT_OFFSET}s${NO_COLOR}\n" "FAIL"
    echo -e "${RED}Failed to unpack archive $archive!${NO_COLOR}\n${tarResult}"
    exit 1
  fi
  local firstFile=$(tar -tf $archive | head -1 | cut -f1 -d"/");
  eval "$result=$homeDir/`dirname $firstFile/.`"
  printf "${GREEN}%${LEFT_OFFSET}s${NO_COLOR}\n" "DONE";
}

function copy_data_to_new_installation() {
  local oldDir=$1;
  local newDir=$2;
  local tmp=
  echo "Removing default config file ..."
  tmp=$(rm "$newDir/etc/config.tdsl" 2>&1)
  if [ $? -ne 0 ]; then
    printf "${RED}%${LEFT_OFFSET}s${NO_COLOR}\n" "FAIL"
    echo -e "${RED}Failed to remove default config file!${NO_COLOR}\n${tmp}"
    exit 1
  fi
  printf "${GREEN}%${LEFT_OFFSET}s${NO_COLOR}\n" "DONE";
  echo "Copying configuration files ..."
  tmp=$(cp -pR "$oldDir/etc/" "$newDir/etc/" 2>&1)
  if [ $? -ne 0 ]; then
    printf "${RED}%${LEFT_OFFSET}s${NO_COLOR}\n" "FAIL"
    echo -e "${RED}Failed to copy configuration files!${NO_COLOR}\n${tmp}";
    exit 1
  fi
  printf "${GREEN}%${LEFT_OFFSET}s${NO_COLOR}\n" "DONE";
  echo "Copying SSL certificates ..."
  tmp=$(cp -pR "$oldDir/certs/" "$newDir/certs/" 2>&1)
  if [ $? -ne 0 ]; then
    printf "${RED}%${LEFT_OFFSET}s${NO_COLOR}\n" "FAIL"
    echo -e "${RED}Failed to copy SSL certificates!${NO_COLOR}\n${tmp}";
    exit 1
  fi
  printf "${GREEN}%${LEFT_OFFSET}s${NO_COLOR}\n" "DONE";
}

function upgrade_installation() {
  local archiveFile=$1;
  local installationDir=$2;
  local backupFormat=$3;
  local installationLink=$4;
  local unpackedArchive=
  local wasDownloaded=0
  local tmp=

  if [[ $archiveFile == http\:* ]] || [[ $archiveFile == https\:* ]]; then
    download_package $archiveFile archiveFile
    wasDownloaded=1
  fi

  local homeDir=$(dirname $installationDir)
  local backupDir=$homeDir
  local backupPath="$backupDir/`basename $installationDir`_backup-`date +%y-%m-%d_%H%M`"

  backup_installation $installationDir $backupPath $backupFormat;
  unpack_archive $archiveFile $homeDir unpackedArchive

  echo "Unpacked archive to $unpackedArchive"
  if [ $wasDownloaded -ne 0 ]; then
    rm $archiveFile
  fi

  copy_data_to_new_installation $installationDir $unpackedArchive

  echo "Removing old installation directory ..."
  tmp=$(rm -rf $installationDir 2>&1)
  if [ $? -ne 0 ]; then
    printf "${RED}%${LEFT_OFFSET}s${NO_COLOR}\n" "FAIL"
    echo -e "${RED}Failed to remove old installation directory!${NO_COLOR}\n${tmp}";
    exit 1
  fi

  printf "${GREEN}%${LEFT_OFFSET}s${NO_COLOR}\n" "DONE";

  if [ -z $installationLink ] ; then
    echo "Replacing installation directories ..."
    tmp=$(mv $unpackedArchive $installationDir 2>&1)
    if [ $? -ne 0 ]; then
      printf "${RED}%${LEFT_OFFSET}s${NO_COLOR}\n" "FAIL"
      echo -e "${RED}Failed to move unpacked archive to installation directory!${NO_COLOR}\n${tmp}";
      exit 1
    fi
    printf "${GREEN}%${LEFT_OFFSET}s${NO_COLOR}\n" "DONE";
  else
    echo "Updating symlink $installationLink ..."
    tmp=$(unlink $installationLink 2>&1)
    if [ $? -ne 0 ]; then
      printf "${RED}%${LEFT_OFFSET}s${NO_COLOR}\n" "FAIL"
      echo -e "${RED}Failed to remove symlink!${NO_COLOR}\n${tmp}";
      exit 1
    fi
    printf "${GREEN}%${LEFT_OFFSET}s${NO_COLOR}\n" "DONE";
    tmp=$(ln -s $unpackedArchive $installationLink 2>&1)
    if [ $? -ne 0 ]; then
      printf "${RED}%${LEFT_OFFSET}s${NO_COLOR}\n" "FAIL"
      echo -e "${RED}Failed to create new symlink!${NO_COLOR}\n${tmp}";
      exit 1
    fi
    printf "${GREEN}%${LEFT_OFFSET}s${NO_COLOR}\n" "DONE";
  fi

  echo "Starting Tigase XMPP Server schema upgrade ..."
  if [ -z $INSTALLATION_LINK ] ; then
    cd $INSTALLATION_DIR
  else
    cd $INSTALLATION_LINK
  fi
  scripts/tigase.sh upgrade-schema etc/tigase.conf
  cd $PWD
  if [ $? -ne 0 ]; then
    echo -e "${RED}Failed to upgrade database schema!${NO_COLOR}";
    exit 1
  fi

  echo -e "\n${GREEN}Upgrade of Tigase XMPP Server finished!${NO_COLOR}";
}

function rollback_installation() {
  local archiveFile=$1;
  local installationDir=$2;
  local backupFormat=$3;
  local installationLink=$4;
  local wasDownloaded=0
  local unpackedArchive
  local tmp=

  if [[ $archiveFile == http\:* ]] || [[ $archiveFile == https\:* ]]; then
    download_package $archiveFile archiveFile
    wasDownloaded=1
  fi

  local homeDir=$(dirname $installationDir)
  local backupDir=$homeDir
  local backupPath="$backupDir/`basename $installationDir`_rollback-`date +%y-%m-%d_%H%M`"

  backup_installation $installationDir $backupPath $backupFormat;

  echo "Removing installation directory ..."
  tmp=$(rm -rf $installationDir 2>&1)
  if [ $? -ne 0 ]; then
    printf "${RED}%${LEFT_OFFSET}s${NO_COLOR}\n" "FAIL"
    echo -e "${RED}Failed to remove installation directory!${NO_COLOR}\n${tmp}";
    exit 1
  fi

  if [ $wasDownloaded -eq 0 ] && [[ $archiveFile != *.tar.gz ]] ; then
    echo "Restoring data from backup location $archiveFile ..."
    unpackedArchive=$(echo $archiveFile | sed "s/\(.*\)_backup.*/\1/")
    tmp=$(mv $archiveFile $unpackedArchive 2>&1)
    if [ $? -ne 0 ]; then
      printf "${RED}%${LEFT_OFFSET}s${NO_COLOR}\n" "FAIL"
      echo -e "${RED}Failed to copy backup data to installation directory!${NO_COLOR}\n${tmp}";
      exit 1
    fi
  else
    unpack_archive $archiveFile $homeDir $unpackedArchive
  fi

  if [ $wasDownloaded -ne 0 ]; then
    rm $archiveFile
  fi

  if ! [ -z $installationLink ] ; then
    echo "Updating symlink $installationLink ..."
    tmp=$(unlink $installationLink 2>&1)
    if [ $? -ne 0 ]; then
      printf "${RED}%${LEFT_OFFSET}s${NO_COLOR}\n" "FAIL"
      echo -e "${RED}Failed to remove symlink!${NO_COLOR}\n${tmp}";
      exit 1
    fi
    printf "${GREEN}%${LEFT_OFFSET}s${NO_COLOR}\n" "DONE";
    tmp=$(ln -s $unpackedArchive $installationLink 2>&1)
    if [ $? -ne 0 ]; then
      printf "${RED}%${LEFT_OFFSET}s${NO_COLOR}\n" "FAIL"
      echo -e "${RED}Failed to create new symlink!${NO_COLOR}\n${tmp}";
      exit 1
    fi
    printf "${GREEN}%${LEFT_OFFSET}s${NO_COLOR}\n" "DONE";
  fi

  echo "Starting Tigase XMPP Server schema upgrade ..."
  if [ -z $INSTALLATION_LINK ] ; then
    cd $INSTALLATION_DIR
  else
    cd $INSTALLATION_LINK
  fi
  scripts/tigase.sh upgrade-schema etc/tigase.conf
  cd $PWD
  if [ $? -ne 0 ]; then
    echo -e "${RED}Failed to upgrade database schema!${NO_COLOR}";
    exit 1
  fi

  echo -e "\n${GREEN}Rollback of Tigase XMPP Server finished!${NO_COLOR}";
}

if [ $# -lt 3 ] ; then
  usage
fi

INSTALLATION_DIR=$3
INSTALLATION_LINK=

if [ $# -gt 2 ] ; then
  INSTALLATION_DIR=`readlink $3`
  if [ -z $INSTALLATION_DIR ] ; then
    INSTALLATION_DIR=$3
  else
    INSTALLATION_LINK=$3;
    echo -e "Passed installation directory is symlink to $INSTALLATION_DIR"
  fi
  if [ "${INSTALLATION_DIR:0:1}" != '/' ] ; then
    INSTALLATION_DIR="$PWD/$INSTALLATION_DIR"
  fi
fi
INSTALLATION_DIR="`dirname ${INSTALLATION_DIR}//.`"

BACKUP_FORMAT="dir"
if [ $# -gt 3 ] ; then
  BACKUP_FORMAT=$4
fi

ARCHIVE_FILE=$2

case "${1}" in
  upgrade)
  upgrade_installation $ARCHIVE_FILE $INSTALLATION_DIR $BACKUP_FORMAT $INSTALLATION_LINK
  ;;
  rollback)
  rollback_installation $ARCHIVE_FILE $INSTALLATION_DIR $BACKUP_FORMAT $INSTALLATION_LINK
  ;;
  *)
  usage
  ;;
esac
