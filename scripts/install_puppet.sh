#!/bin/bash

yum -y update

rpm -ivh https://yum.puppetlabs.com/puppetlabs-release-el-7.noarch.rpm

yum -y install ruby puppet git vim rubygem-json net-tools

echo "Fixing Hostname " #for vagrant
IPADDR=`hostname -I | awk '{print $2}'`
NAME=${IPADDR//./-}.localdomain
echo ${NAME}
echo ${NAME} >> /etc/hostname
hostname ${NAME}

puppet module install thias-sysctl --version 1.0.6
puppet module install puppetlabs-docker_ucp --version 1.0.0
puppet module install puppetlabs-docker --version 1.0.4
puppet module install jsok-vault --version 1.2.8