#!/bin/bash
LIBS="/usr/share/tigase/lib /usr/share/openfire/lib"

# Locale check
if ! locale | grep -iq '^LC_CTYPE=.*\.utf8.\?$'; then
	echo "You need to set UTF8 based locale first" >&2; exit 1
fi

for DIR in $LIBS; do CLASSPATH="`ls -d $DIR/*.jar 2>/dev/null | grep -v groovy | tr '\n' :`$CLASSPATH"; done
export CLASSPATH
#echo $CLASSPATH
exec ${0/%.sh/.groovy}
