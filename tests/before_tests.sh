#!/bin/bash
set -exu

COMMIT=`git rev-parse --short HEAD`
cf login -a $API_URL -u $USERNAME -p $PASSWORD
cf cs conversation free travis-$COMMIT-conv-service
cf create-service-key travis-$COMMIT-conv-service conv-$COMMIT-cred

exit $?
