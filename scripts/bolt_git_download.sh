#!/bin/bash -xe

PUPPET_REPO=$1

LOCAL_REPO=$2

BRANCH=$3

echo "Deploying to ${LOCAL_REPO} Puppet repo ${PUPPET_REPO} "

if [ -d "${LOCAL_REPO}" ];
then
	rm -rf ${LOCAL_REPO}
fi

echo "Git Clone Puppet Repo"
git clone "${PUPPET_REPO}" "${LOCAL_REPO}"
cd ${LOCAL_REPO}
echo "GIT Checking out branch"
git checkout ${BRANCH}

