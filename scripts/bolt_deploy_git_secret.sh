#!/bin/bash -xe

GIT_SERVER=$1

#get the git deploy key from somewhere
mkdir -p ${HOME}/.ssh/

echo "Copying Jenkins Deployment Key to SSH dir"
mv ${HOME}/jenkins_deployment ${HOME}/.ssh/jenkins_deployment

echo "Fixing permissions on SSH Deployment key"
chmod 600 ${HOME}/.ssh/jenkins_deployment

echo "Updating Git config for ssh config"

if grep  "jenkins_deployment" ~/.ssh/config > /dev/null
then
 echo "Config up to date"
else
 echo "
 Host *
 StrictHostKeyChecking no
 Host ${GIT_SERVER}
   IdentityFile ${HOME}/.ssh/jenkins_deployment" >> ${HOME}/.ssh/config
fi

chmod 755 ${HOME}/.ssh/config

echo ${HOME}/.ssh/config
