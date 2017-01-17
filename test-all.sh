#!/bin/bash

# Test both the client and server generators regardless if one fails. If either
# fails, exit with that exit code.

SWAGGER_SPEC=$1

./test.sh client $SWAGGER_SPEC
R_CLIENT=$?

./test.sh server $SWAGGER_SPEC
R_SERVER=$?

if [ $R_CLIENT -ne 0 ]; then
  exit $R_CLIENT
fi

if [ $R_SERVER -ne 0 ]; then
  exit $R_SERVER
fi

