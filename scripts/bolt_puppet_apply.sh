#!/bin/bash -xe


#default to a worker if not set
PUPPET_MANIFEST=$1

LOCAL_REPO=$2

mkdir -p /etc/docker

cp "${LOCAL_REPO}/files/subscription.lic" /etc/docker/subscription.lic
mkdir -p /etc/docker/certs
cp -R "/tmp/certs" /etc/docker/certs/

#https://tickets.puppetlabs.com/browse/PUP-5808
cd /tmp

/usr/local/bin/puppet apply --debug --modulepath=${LOCAL_REPO}/modules ${LOCAL_REPO}/manifests/${PUPPET_MANIFEST}
