#!/bin/bash

set -exu

function exportCredentials {
  RAW_CREDENTIALS=$1
  export $2_USERNAME=`echo $RAW_CREDENTIALS | grep username | sed "s/[\",:]//g" | awk '{print $2}'`
  export $2_PASSWORD=`echo $RAW_CREDENTIALS | grep password | sed "s/[\",:]//g" | awk '{print $2}'`
}

RAW_CONVERSATION=`cf service-key my-travis-conv-service travis-conv-cred`
exportCredentials $RAW_CONVERSATION CONVERSATION

java -cp .:userlib/junit-4.12.jar org.junit.runner.JUnitCore  ConversationServiceTest
exit 0
