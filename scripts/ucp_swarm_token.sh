#!/bin/bash

AUTHTOKEN=$1
UCP_URL=$2
UCP_PORT=$3
TOKEN=$4

cd /tmp

#Once JQ is in Artifactory we can move to a yum install
curl -X GET -L https://github.com/stedolan/jq/releases/download/jq-1.5/jq-linux64 --silent -o jq

chmod u+x jq

./jq --version > /dev/null
if [ $? != 0 ]; then
  echo "ISSUES with JQ, exiting"
  exit 1
fi

#curl the for the swarm manager token
#/https://docs.docker.com/datacenter/ucp/2.2/reference/api/#!/Swarm/SwarmInspect
UCPTOKEN=$(curl -sk -H "Authorization: Bearer $AUTHTOKEN" https://${UCP_URL}:${UCP_PORT}/swarm | ./jq -r .JoinTokens.${TOKEN})

echo $UCPTOKEN
