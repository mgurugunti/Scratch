   properties([
     parameters([
        string(defaultValue: "", description: 'Address of Target machine for puppet', name: 'IP'), 		
        string(defaultValue: "manager",  description: 'Role the target will be in swarm cluster either manager or worker', name: 'SWARM_ROLE'),
		string(defaultValue: "", description: 'Cluster to Join', name: 'CLUSTER_LEADER'),
        string(defaultValue: "19002", description: 'UCP cluster port to join', name: 'CLUSTER_PORT'),
		choice(choices: 'ucp\ntls', description: 'Deploying UCP or Docker Swarm with TLS', name: 'DEPLOYMENT'),
		string(defaultValue: "h0001017", description: 'Build Cluster to run jenkins slave', name: 'BUILD_UCP_CLUSTER'),
        string(defaultValue: "19002", description: 'BUILD UCP cluster port', name: 'BUILD_UCP_PORT'),
        string(defaultValue: "https://artifactory.associatesys.local", description: 'Registry to pull jenkins slave image from', name: 'REGISTRY'),
        string(defaultValue: "/tmp/puppet", description: 'remote directory to place pupept repo', name: 'REPO_DIR'),
        string(defaultValue: "ssh://git@bitbucket.associatesys.local/vite/docker-ee-puppet.git", description: 'Git URL for puppet repo', name: 'PUPPET_URL_REPO'),
        string(defaultValue: "bitbucket.associatesys.local", description: 'Git URL for ssh config access', name: 'GIT_URL_SERVER'),     
		string(defaultValue: "master", description: 'Working Branch for puppet code', name: 'BRANCH')   ,
		string(defaultValue: "modules/docker_ee_common/hieradata/docker-ucp.yaml", description: 'puppet manifest to run', name: 'HIERADATAFILE')   
		
    ])
])
node{
  cleanWs()
  env.DOCKER_TLS_VERIFY = "0"
  
  stage('Checkout'){
	env.BRANCH = "${params.BRANCH}" 
	
    checkout([$class: 'GitSCM', branches: [[name: '*/${BRANCH}']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'bitbucket_access', url: "${params.PUPPET_URL_REPO}"]]])
    
    //for some reason the mode is not consistent with git so forcing it	
	sh 'chmod u+rw,g+r,o+r ${HIERADATAFILE}'
	sh 'chmod u+x,o+x,g+x scripts/*.sh'
	
  }
  stage('UCPAUTH'){
		withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'new_ucp_default_creds', usernameVariable: 'UCP_USER', passwordVariable: 'UCP_PASS']]){
				
				env.CLUSTER_LEADER = "${params.CLUSTER_LEADER}"
				env.CLUSTER_PORT = "${params.CLUSTER_PORT}"     
				
				def ret = sh(
					script: 'scripts/ucp_auth_token.sh ${UCP_USER} ${UCP_PASS} ${CLUSTER_LEADER} ${CLUSTER_PORT}',
					returnStdout: true
				)
				println ret

        env.AUTHTOKEN=ret
		}    
	}
  stage('UCPTOKEN'){

      env.CLUSTER_LEADER = "${params.CLUSTER_LEADER}"
      env.CLUSTER_PORT = "${params.CLUSTER_PORT}"     
      env.SWARM_ROLE = "${params.SWARM_ROLE}"     
      
	  def token = sh(
        script: 'scripts/ucp_swarm_token.sh ${AUTHTOKEN} ${CLUSTER_LEADER} ${CLUSTER_PORT} ${SWARM_ROLE^}',
        returnStdout: true
      )
      
      println token

      env.SWARMTOKEN=token
    }    
  stage('Secrets') {
    withCredentials(bindings: [sshUserPrivateKey(credentialsId: 'jenkins_access', keyFileVariable: 'SSH_KEY', usernameVariable: 'USERNAME')]) {
      withCredentials([file(credentialsId: 'jenkins_deployment', variable: 'DEPLOYMENT')]) {
        sh 'chmod u+rw,g+r,o+r $DEPLOYMENT'

		env.IP= "${params.IP}"
		
        //Deploy git ssh key to pull puppet repo
        sh 'scp -o StrictHostKeyChecking=no -o "UserKnownHostsFile /dev/null" -i $SSH_KEY $DEPLOYMENT $USERNAME@$IP:~/jenkins_deployment'
      }

      //Add key to config for GIT_SERVER
	   env.GIT_URL_SERVER = "${params.GIT_URL_SERVER}"  
	   
       sh 'ssh -o StrictHostKeyChecking=no -o "UserKnownHostsFile /dev/null" -i $SSH_KEY $USERNAME@$IP \'bash -s\' < scripts/bolt_deploy_git_secret.sh ${GIT_URL_SERVER}'

    }
  }

  stage('Download'){
    withCredentials(bindings: [sshUserPrivateKey(credentialsId: 'jenkins_access', keyFileVariable: 'SSH_KEY', usernameVariable: 'USERNAME')]) {
     	
		env.PUPPET_URL_REPO = "${params.PUPPET_URL_REPO}"
		env.REPO_DIR = "${params.REPO_DIR}"
		
        sh 'ssh -o StrictHostKeyChecking=no -o "UserKnownHostsFile /dev/null" -i $SSH_KEY $USERNAME@$IP \'bash -s\' < scripts/bolt_git_download.sh ${PUPPET_URL_REPO} ${REPO_DIR} ${BRANCH}'
    }  
  }
  stage('TOKENUPDATE'){
  
    //Update the Hiera Data with new manager token
	env.REPO_DIR = "${params.REPO_DIR}"
	env.HIERADATAFILE= "${params.HIERADATAFILE}"
	   
	def output = sh(
      script: 'scripts/ucp_update_token.sh ${SWARMTOKEN} ${HIERADATAFILE} ${SWARM_ROLE} ${CLUSTER_LEADER}',
      returnStdout: true
    )
    
	//Copy the heira file to the target
    withCredentials(bindings: [sshUserPrivateKey(credentialsId: 'jenkins_access', keyFileVariable: 'SSH_KEY', usernameVariable: 'USERNAME')]) {
      sh 'scp -o StrictHostKeyChecking=no -o "UserKnownHostsFile /dev/null" -i $SSH_KEY ${HIERADATAFILE} $USERNAME@$IP:${REPO_DIR}/${HIERADATAFILE}'
    }

  }
  stage('Puppet'){

    env.PUPPET_MANIFEST= "${params.PUPPET_MANIFEST}"
	env.DEPLOYMENT = "${params.DEPLOYMENT}"   
	 		
    
	withCredentials(bindings: [sshUserPrivateKey(credentialsId: 'jenkins_access', keyFileVariable: 'SSH_KEY', usernameVariable: 'USERNAME')]) {
      
      withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'jenkins_sudo', usernameVariable: 'SUDOUSER', passwordVariable: 'SUDOPASS']]){
     
        withCredentials([dockerCert(credentialsId: 'ucp-cert-chain', variable: 'DOCKER_CERT_PATH')]) {
          
          docker.withServer("tcp://${params.BUILD_UCP_CLUSTER}:${params.BUILD_UCP_PORT}", "$DOCKER_CERT_PATH") {
        
            docker.withRegistry("${params.REGISTRY}") {
                
              jenkins = docker.image('hce_jenkins_slave:latest') 
                    
              jenkins.inside{  
                sh 'bolt script run scripts/bolt_puppet_apply.sh docker-swarm-${SWARM_ROLE}-${DEPLOYMENT}-join.pp ${REPO_DIR} --tty --insecure --private-key $SSH_KEY --run-as root --sudo-password $SUDOPASS -u $SUDOUSER --nodes ${IP}'
              }
            }
          }
        }
      }
    }
  }
}
 