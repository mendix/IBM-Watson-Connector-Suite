#!/bin/bash
set -exu

cf login -a $API_URL -u $USERNAME -p $PASSWORD
cf delete-service-key my-travis-conv-service-blab keyasstring -f
cf ds my-travis-conv-service-blab -f

exit $?
