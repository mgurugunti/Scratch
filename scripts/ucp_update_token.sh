#!/bin/bash
SWARMTOKEN=$1
HIERADATAFILE=$2
UCP_ROLE=$3
MANAGER_IP=$4

if [[ ${SWARMTOKEN} != *SWMTKN* ]]; then
 
 echo "${SWARMTOKEN} is invalid, exiting!"
 exit 2
 
fi

if [ -e ${HIERADATAFILE} ]
then

  sed -i 's/'${UCP_ROLE}'_token: ".*"/'${UCP_ROLE}'_token: "'${SWARMTOKEN}'"/g' "${HIERADATAFILE}"
  sed -i 's/docker_ee_common::manager: ".*"/docker_ee_common::manager: "'${MANAGER_IP}'"/g' "${HIERADATAFILE}"

  cat ${HIERADATAFILE}

else
 echo "Data file does not exist"
 exit 1
fi
