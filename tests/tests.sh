#!/bin/bash

set -exu

COMMIT=`git rev-parse --short HEAD`
function exportCredentials {
  RAW_CREDENTIALS=$1
  export $2_USERNAME=`echo $RAW_CREDENTIALS | grep username | sed "s/[\",:]//g" | awk '{print $2}'`
  export $2_PASSWORD=`echo $RAW_CREDENTIALS | grep password | sed "s/[\",:]//g" | awk '{print $2}'`
}

env

RAW_CONVERSATION=`cf service-key travis-$COMMIT-conv-service conv-$COMMIT-cred`
exportCredentials $RAW_CONVERSATION CONVERSATION

java -cp .:userlib/junit-4.12.jar org.junit.runner.JUnitCore  ConversationServiceTest
exit 0
