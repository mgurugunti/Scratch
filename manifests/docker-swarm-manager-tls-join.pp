include 'docker_ee_common'

$docker_url = lookup('docker_ee_common::repo_url')
$docker_key = lookup('docker_ee_common::repo_key')
$swarm_manager =  looku`p('docker_ee_common::manager')
$swarm_token =  lookup('docker_ee_common::manager_token')

class { 'docker':
  docker_ee                 => true,
  docker_ee_source_location => $docker_url,
  docker_ee_key_source      => $docker_key,
  ip_forward                => true,
  tls_enable      => true,
  tls_cacert      => '/tmp/ca.pem',
  tls_cert        => '/tmp/cert.pem',
  tls_key         => '/tmp/key.pem',
  extra_parameters => ['--insecure-registry=vite.associatesys.local:5000'],
}

docker::swarm { 'swarm' :
  join => true,
  manager_ip => $swarm_manager,
  advertise_addr => $ipaddress_eth0,
  token => $swarm_token,
  listen_addr    => $ipaddress_eth0,
}

