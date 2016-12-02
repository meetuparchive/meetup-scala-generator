#!/bin/bash

# Killing 0 sends the signal to all processes in the current process group.
# Without this you must wrestle with ctrl+c to get at this process and
# those it spawns.
trap "kill 0" SIGINT

while [ 1 ]; do fswatch -o src/ | `pwd`/test.sh; done