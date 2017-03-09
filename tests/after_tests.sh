#!/bin/bash
set -exu

COMMIT=`git rev-parse --short HEAD`
cf login -a $API_URL -u $USERNAME -p $PASSWORD
cf delete-service-key travis-$COMMIT-conv-service conv-$COMMIT-cred -f
cf ds travis-travis-$COMMIT-conv-service -f

exit $?
