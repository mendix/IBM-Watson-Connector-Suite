#!/bin/bash
set -exu

cf login -a $API_URL -u $USERNAME -p $PASSWORD -o $ORG -s $SPACE
cf cs conversation free my-travis-conv-service-blab
cf create-service-key my-travis-conv-service-blab keyasstring
cf service-key my-travis-conv-service-blab keyasstring

exit $?
