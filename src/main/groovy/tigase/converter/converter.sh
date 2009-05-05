#!/bin/bash
LIBS="/usr/share/tigase-server/lib /usr/share/openfire/lib"
for DIR in $LIBS; do CLASSPATH="`ls -d $DIR/*.jar 2>/dev/null | grep -v groovy | tr '\n' :`$CLASSPATH"; done
export CLASSPATH
#echo $CLASSPATH
exec ${0/%.sh/.groovy}
