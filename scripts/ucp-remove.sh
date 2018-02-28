#!/bin/bash

#Undoes all the puppet code and resets the target for testing purposes

docker kill $(docker ps -q)

docker swarm leave --force

rpm -evv docker-ee

rm -rf /etc/docker

rm -rf /var/lib/docker

rm -rf /tmp/puppet
