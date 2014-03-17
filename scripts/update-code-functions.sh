#!/bin/bash

export TMP_DIR="/tmp"

function build_xmltools() {
   mvn -q clean package install &> ${TMP_DIR}/xmltools-build.txt
   ant clean jar-dist &> ${TMP_DIR}/xmltools-build.txt
   echo jars/tigase-xmltools.jar
 }

 function build_utils() {
   mvn -q clean package install &> ${TMP_DIR}/utils-build.txt
   cp -f ../xmltools/jars/tigase-xmltools.jar jars/
   ant clean jar-dist &> ${TMP_DIR}/utils-build.txt
   echo jars/tigase-utils.jar
 }

 function build_server() {
   mvn -q clean package install &> ${TMP_DIR}/server-build.txt
   ant clean jar-dist &> ${TMP_DIR}/server-build.txt
   echo ""
 }

 function build_maven() {
   mvn clean package &> ${TMP_DIR}/$1-build.txt
   echo `find target -name "tigase-*.jar"`
 }

 function build_extras() {
   build_maven extras
 }

 function build_muc() {
   build_maven muc
 }

 function build_pubsub() {
   build_maven pubsub
 }

function build_archiving() {
	build_maven archiving
}

function build_socks5() {
	build_maven socks5
}

function build_stun() {
	build_maven stun
}

function build_tigase_http_api() {
  build_maven "tigase-http-api"
}
