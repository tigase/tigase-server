#!/bin/bash

function usage() {
  echo "The script has to be run with following parameters:"
  echo "$0 hostname username [vhost]"
  echo "--------------------"
  echo "  hostname - is the machine hostname which might be also used as the cluster node name"
  echo "  username - is a user account from which the Tigase server will be run"
  echo "  vhost    - is a virtual hostname used for the service if different than the hostname"
}

function check_dns() {
  if host $1 | grep "not found" > /dev/null ; then
    echo "WARNING - the $1 does NOT resolve to a valid IP address"
  else
    if host $1 | grep "127.0.0.1" > /dev/null ; then
      echo "WARNING - DNS points to the localhost for the domain: $1"
    else
      echo "OK, DNS settings for $1"
    fi
  fi
  if host -t SRV _xmpp-server._tcp.$1 | grep SRV > /dev/null ; then
    echo "OK, SRV record found _xmpp-server._tcp.$1"
  else
    echo "WARNING - no SRV record _xmpp-server._tcp.$1"
  fi
  if host -t SRV _xmpp-client._tcp.$1 | grep SRV > /dev/null ; then
    echo "OK, SRV record found _xmpp-client._tcp.$1"
  else
    echo "WARNING - no SRV record _xmpp-client._tcp.$1"
  fi
}

function check_net() {
  if ping -q -c 2 -W 100 $1 &> /dev/null ; then
    echo "OK, The $1 host accessible through the network"
  else
    echo "WARNING - the $1 host seems to be unaccessible through the network!"
  fi
}

if [ "$1" = "" ] || [ "$2" = "" ] ; then
  echo "Missing hostname or username as a parameter, please run the script with correct parameters"
  usage
  exit
fi

HST=$1
USR=$2
VHST=$3

if [ "$VHST" = "" ] ; then
  VHST=$HST
fi

# Checking network configuration

check_dns $HST
check_dns $VHST

check_net $HST
check_net $VHST

# Checking system configuration

if echo $OSTYPE | grep -i linux > /dev/null ; then

  if [ "$USR" = "root" ] ; then
    echo "WARNING - you should not run the Tigase server from the root account."
  fi

  if [ "$UID" == "0" ] ; then
    # Checking ulimits
    RES=`su -c - $USR "ulimit -n"`
    if echo $RES | grep -i "user $USR does not exist" > /dev/null ; then
      echo "WARNING - the $USR user given does not exist, run 'adduser -m $USR' to create the account"
    else
      if [ $RES -lt 300000 ] ; then
        echo "WARNING - maximum open files for the $USR user is too low: $RES"
        echo "          To fix this, add following lines to file: /etc/security/limits.conf"
        echo "$USR               soft    nofile         500000"
        echo "$USR               hard    nofile         500000"
        echo "-------"
        echo "Add these lines? (yes/no)"
        read ans
        if [ "$ans" = "yes" ] ; then
          echo "$USR               soft    nofile         500000" >> /etc/security/limits.conf
          echo "$USR               hard    nofile         500000" >> /etc/security/limits.conf
        fi
      else
        echo "OK, Max open files set to: $RES"
      fi
    fi
  else
    echo "I am running not within root account, can't check limits for the $USR"
  fi

  # Checking kernel settings
  RES=`/sbin/sysctl fs.file-max | sed -e "s/fs.file-max *= *\([0-9]\+\)/\\1/"`
  if [ $RES -lt 600000 ] ; then
    echo "WARNING - system wide fs.file-max is too low: $RES"
    echo "          To fix this, add following line to file: /etc/sysctl.conf"
    echo "fs.file-max=1000000"
    echo "-------"
    echo "Add the line and adjust sysctl for running system? (yes/no)"
    read ans
    if [ "$ans" = "yes" ] ; then
      echo "fs.file-max=1000000" >> /etc/sysctl.conf
      /sbin/sysctl -p > /dev/null
    fi
  else
    echo "OK, system wide fs.file-max set to: $RES"
  fi

  LO_RES=`/sbin/sysctl net.ipv4.ip_local_port_range | sed -e "s/[^=]*= *\([0-9]\+\)[^0-9]*\([0-9]\+\)/\\1/"`
  HI_RES=`/sbin/sysctl net.ipv4.ip_local_port_range | sed -e "s/[^=]*= *\([0-9]\+\)[^0-9]*\([0-9]\+\)/\\2/"`
  if [ $LO_RES -gt 5000 ] || [ $HI_RES -lt 64000 ] ; then
    echo "WARNING - IP port range is not optimal: $LO_RES   $HI_RES"
    echo "          Recommended: 1024   65530"
    echo "          To fix this, add following line to file: /etc/sysctl.conf"
    echo "net.ipv4.ip_local_port_range=1024 65530"
    echo "-------"
    echo "Add the line and adjust sysctl for running system? (yes/no)"
    read ans     
    if [ "$ans" = "yes" ] ; then
      echo "net.ipv4.ip_local_port_range=1024 65530" >> /etc/sysctl.conf
      /sbin/sysctl -p > /dev/null
    fi
  else
    echo "OK, IP port range set to: $LO_RES   $HI_RES"
  fi

else
  echo "The $OSTYPE is not supported for further checks yet"
fi

