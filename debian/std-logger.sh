#!/bin/sh
LOG=$1
shift
exec $* >>$LOG 2>&1 </dev/null
