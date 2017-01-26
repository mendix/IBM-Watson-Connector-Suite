#!/bin/bash
set -exu

cf login -a $API_URL -u $USERNAME -p $PASSWORD
cf delete-service-key my-travis-conv-service travis-conv-cred -f
cf ds my-travis-conv-service -f


exit $?
