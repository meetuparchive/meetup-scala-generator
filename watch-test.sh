#!/bin/bash

# Allow continuous testing for generator development. When a source file
# changes the generator package is rebuilt, the given generator used to
# generate source, and then those sources are built and reported on.

# Killing 0 sends the signal to all processes in the current process group.
# Without this you must wrestle with ctrl+c to get at this process and
# those it spawns.
trap "kill 0" SIGINT

if [ "$1" != "server" -a "$1" != "client" ]; then
  echo "Please specify 'server' or 'client'!"
  exit 1
fi

while [ 1 ]; do fswatch -o src/ | `pwd`/test.sh "$@"; done