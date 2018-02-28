include '::docker_ee_common'

$docker_url = lookup('docker_ee_common::repo_url')
$docker_key = lookup('docker_ee_common::repo_key')
$ucp_user = lookup('docker_ee_common::ucp_user')
$ucp_password = lookup('docker_ee_common::ucp_password')

class { 'docker':
  tcp_bind        => ['tcp://127.0.0.1:2376'],
  docker_ee                 => true,
  docker_ee_source_location => $docker_url,
  docker_ee_key_source      => $docker_key,
  ip_forward                => true,
  extra_parameters => ['--insecure-registry=vite.associatesys.local:5000'],
}

docker::swarm { 'swarm' :
  init           => true,
  advertise_addr => $ipaddress_eth0,
  listen_addr    => $ipaddress_eth0,
}

class { 'docker_ucp':
  version                   => '2.2.4',

  listen_address            => $ipaddress_eth0,
  advertise_address         => $ipaddress_eth0,
  ucp_manager               => $ipaddress_eth0,
  ucp_url                   => "https://${ipaddress_eth0}/:443",
  username                  => $ucp_user,
  password                  => $ucp_password,
  controller                => true,
  host_address              => $ipaddress_eth0,
  usage                     => false,
  tracking                  => false,
  subject_alternative_names => $ipaddress_eth0,
  external_ca               => false,
  swarm_scheduler           => 'binpack',
  swarm_port                => 19001,
  controller_port           => 19002,
  preserve_certs            => true,
  docker_socket_path        => '/var/run/docker.sock',
  license_file              => '/etc/docker/subscription.lic',
}
