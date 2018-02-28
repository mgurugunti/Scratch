#!/bin/bash

USER=$1
PASS=$2
UCP_URL=$3
UCP_PORT=$4

cd /tmp

curl -X GET -L https://github.com/stedolan/jq/releases/download/jq-1.5/jq-linux64 --silent -o jq

chmod u+x,o+x,g+x jq

echo "{\"username\":\"${USER}\",\"password\":\"${PASS}\"}" >> auth.json

AUTHTOKEN=$(curl -sk -d @auth.json https://${UCP_URL}:${UCP_PORT}/auth/login | ./jq -r .auth_token)

rm -rf auth.json

echo "${AUTHTOKEN}"

