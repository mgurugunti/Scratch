import groovy.json.JsonSlurperClassic;

properties([
  parameters([ 
        string(defaultValue: "", description: 'Address of Target machine for puppet', name: 'IP'),
		choice(choices: 'ucp\ntls', description: 'Deploying UCP or Docker Swarm with TLS', name: 'DEPLOY'),
        string(defaultValue: "h0001017", description: 'Cluster to run jenkins slave', name: 'BUILD_UCP_CLUSTER'),
        string(defaultValue: "19002", description: 'UCP cluster port', name: 'BUILD_UCP_PORT'),
        string(defaultValue: "https://artifactory.associatesys.local", description: 'Registry to pull jenkins slave image from', name: 'REGISTRY'),
        string(defaultValue: "/tmp/puppet", description: 'remote directory to place pupept repo', name: 'REPO_DIR'),
        string(defaultValue: "ssh://git@bitbucket.associatesys.local/vite/docker-ee-puppet.git", description: 'Git URL for puppet repo', name: 'PUPPET_URL_REPO'), 
        string(defaultValue: "bitbucket.associatesys.local", description: 'Git URL for ssh config access', name: 'GIT_URL_SERVER'),
		    string(defaultValue: "master", description: 'Working Branch for puppet code', name: 'BRANCH'),
        string(defaultValue: "http://h0001016.associatesys.local:8200/v1/pki/issue/server", description: 'Vault endpoint to generate server cert', name: 'VAULT_SERVER_CERT_ENDPOINT'),
      ])
])

node{

  stage('Checkout'){
    env.BRANCH = "${params.BRANCH}"  
    checkout([$class: 'GitSCM', branches: [[name: '*/${BRANCH}']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'bitbucket_access', url: "$PUPPET_URL_REPO"]]])
  }
  stage('Secrets') {
    withCredentials(bindings: [sshUserPrivateKey(credentialsId: 'jenkins_access', keyFileVariable: 'SSH_KEY', usernameVariable: 'USERNAME')]) {
      withCredentials([file(credentialsId: 'jenkins_deployment', variable: 'DEPLOYMENT')]) {
        sh 'chmod u+rw,g+r,o+r $DEPLOYMENT'
		
		env.IP = "${params.IP}" 
        
		//Deploy git ssh key to pull puppet repo
        sh 'scp -o StrictHostKeyChecking=no -o "UserKnownHostsFile /dev/null" -i $SSH_KEY $DEPLOYMENT $USERNAME@$IP:~/jenkins_deployment'
      }

      //Add key to config for GIT_SERVER
	  env.GIT_URL_SERVER = "${params.GIT_URL_SERVER}"  
      sh 'ssh -o StrictHostKeyChecking=no -o "UserKnownHostsFile /dev/null" -i $SSH_KEY $USERNAME@$IP \'bash -s\' < scripts/bolt_deploy_git_secret.sh $GIT_URL_SERVER'

    }
  }

  stage('FetchCerts') {
    withCredentials([string(credentialsId: 'vault-secret-token-text', variable: 'VAULT_TOKEN')]) {      
      env.VAULT_SERVER_CERT_ENDPOINT = "${params.VAULT_SERVER_CERT_ENDPOINT}" 
      env.TARGET_HOST = "${params.IP}.associatesys.local"
      env.DATA_JSON = "{\"common_name\": \"$TARGET_HOST\",\"ttl\": \"360h\"}"      
      
      cert_json = sh (
          script: 'curl -X POST $VAULT_SERVER_CERT_ENDPOINT -H "Cache-Control: no-cache" -H "X-Vault-Token: $VAULT_TOKEN" -d "$DATA_JSON"',
          returnStdout: true
      ).trim()

      def cert = new JsonSlurperClassic().parseText(cert_json)

      dir ('certs') {
        writeFile file: 'cert.pem', text: cert.data.certificate
        writeFile file: 'ca.pem', text: cert.data.issuing_ca
        writeFile file: 'key.pem', text: cert.data.private_key
      }
    }
  }

  stage('CopyCerts') {
    withCredentials(bindings: [sshUserPrivateKey(credentialsId: 'jenkins_access', keyFileVariable: 'SSH_KEY', usernameVariable: 'USERNAME')]) {        
        sh 'chmod -R u+rw,g+r,o+r certs'

        sh 'scp -r -o StrictHostKeyChecking=no -o "UserKnownHostsFile /dev/null" -i $SSH_KEY certs/ $USERNAME@$IP:/tmp/'
    }
  }

  stage('Download'){
    withCredentials(bindings: [sshUserPrivateKey(credentialsId: 'jenkins_access', keyFileVariable: 'SSH_KEY', usernameVariable: 'USERNAME')]) {
	  
		env.PUPPET_URL_REPO = "${params.PUPPET_URL_REPO}" 
    env.REPO_DIR = "${params.REPO_DIR}"  
	 		
		sh 'ssh -o StrictHostKeyChecking=no -o "UserKnownHostsFile /dev/null" -i $SSH_KEY $USERNAME@$IP \'bash -s\' < scripts/bolt_git_download.sh $PUPPET_URL_REPO $REPO_DIR $BRANCH'
    }  
  }

  stage('Puppet'){
    env.DOCKER_TLS_VERIFY = "0"
	
	env.DEPLOY = "${params.DEPLOY}"   
	 		
    withCredentials(bindings: [sshUserPrivateKey(credentialsId: 'jenkins_access', keyFileVariable: 'SSH_KEY', usernameVariable: 'USERNAME')]) {
      
      withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'jenkins_sudo', usernameVariable: 'SUDOUSER', passwordVariable: 'SUDOPASS']]){
     
        withCredentials([dockerCert(credentialsId: 'ucp-cert-chain', variable: 'DOCKER_CERT_PATH')]) {
          
          docker.withServer("tcp://$BUILD_UCP_CLUSTER:$BUILD_UCP_PORT", "$DOCKER_CERT_PATH") {
        
            docker.withRegistry("$REGISTRY", 'jenkins_sudo' ) {
                
              jenkins = docker.image('hce_jenkins_slave:latest') 
                 
              jenkins.inside{  
                sh 'bolt script run scripts/bolt_puppet_apply.sh docker-swarm-manager-${DEPLOY}-init.pp $REPO_DIR --tty --insecure --private-key $SSH_KEY --run-as root --sudo-password $SUDOPASS -u $SUDOUSER --nodes $IP'
                    
              }
            }
          }
        }
      }
    }
  }
  cleanWs()
}
 
