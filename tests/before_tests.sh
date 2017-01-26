#!/bin/bash
set -exu

cf login -a $API_URL -u $USERNAME -p $PASSWORD
cf cs conversation free my-travis-conv-service
cf create-service-key my-travis-conv-service travis-conv-cred

exit $?
