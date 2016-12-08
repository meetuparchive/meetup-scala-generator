#!/bin/bash

./test.sh client
R_CLIENT=$?

./test.sh server
R_SERVER=$?

if [ $R_CLIENT -ne 0 ]; then
  exit $R_CLIENT
fi

if [ $R_SERVER -ne 0]; then
  exit $R_SERVER
fi

