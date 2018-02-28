include '::docker_ee_common'

$docker_url = lookup('docker_ee_common::repo_url')
$docker_key = lookup('docker_ee_common::repo_key')

class { 'docker':
  tcp_bind        => ['tcp://0.0.0.0:2376'],
  docker_ee                 => true,
  docker_ee_source_location => $docker_url,
  docker_ee_key_source      => $docker_key,
  ip_forward                => true,
  tls_enable      => true,
  tls_cacert      => '/etc/docker/certs/ca.pem',
  tls_cert        => '/etc/docker/certs/cert.pem',
  tls_key         => '/etc/docker/certs/key.pem',
  extra_parameters => ['--insecure-registry=vite.associatesys.local:5000'],
}

docker::swarm { 'swarm' :
  init           => true,
  advertise_addr => $ipaddress_eth0,
  listen_addr    => $ipaddress_eth0,
}
