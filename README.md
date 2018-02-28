
#Mac install of Puppet
`ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)" < /dev/null 2> /dev/null ; brew install caskroom/cask/brew-cask 2> /dev/null`

brew cask install puppet-agent

#Vagrant

https://www.vagrantup.com/docs/provisioning/puppet_apply.html


#Obtain a trial license of Docker EE

https://store.docker.com/editions/enterprise/docker-ee-server-centos


#Puppet and docker

Docker
https://forge.puppet.com/puppetlabs/docker/readme

UCP
https://forge.puppet.com/puppetlabs/docker_ucp

Bolt
https://puppet.com/docs/bolt/0.x/bolt.html

Tested with version 0.15
There is a bug with ffi and bolt, you need to force version 1.9.8 of ffi and then install gem install bolt -v 0.15

Jenkins
The Main Jenkins pipeline, JenkinsFile, takes the docker hosts IP addresses or hostnames, how many workers and managers you want, then runs ucp-swarm-join or ucp-swarm-init to bootstrap the cluster.


Credentials required in Jenkins Credential Store

1. jenkins_access - SSH key with username and password - SSH Access to the target servers

2. jenkins_sudo - Useranme and password - Sudo privilages on the target machines

3. jenkins_deployment  - Secret Key File - Could be the same as 4 but as a secret file to copy to target server ssh config

4. ucp-cert-chain - Docker CA Certs - if using UCP the CA chain file for building Jenkins slave to run bolt on. 

5. bitbucket_access -  SSH key with username and password - SSH key for git server access. 
