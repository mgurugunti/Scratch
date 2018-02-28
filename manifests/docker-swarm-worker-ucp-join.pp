include 'docker_ee_common'

$docker_url = lookup('docker_ee_common::repo_url')
$docker_key = lookup('docker_ee_common::repo_key')
$swarm_manager =  lookup('docker_ee_common::manager')
$swarm_token =  lookup('docker_ee_common::worker_token')

class { 'docker':
  docker_ee                 => true,
  docker_ee_source_location => $docker_url,
  docker_ee_key_source      => $docker_key,
  ip_forward                => true,
  extra_parameters => ['--insecure-registry=vite.associatesys.local:5000'],
}

docker::swarm { 'swarm' :
  manager_ip => $swarm_manager,
  token => $swarm_token,
  join => true,
  advertise_addr => $ipaddress_eth0,
  listen_addr    => $ipaddress_eth0,
}



