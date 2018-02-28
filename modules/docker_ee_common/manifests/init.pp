class docker_ee_common {
  package { 'docker-common':
  ensure => 'absent',
}

package { 'docker-engine-selinux':
  ensure => 'absent',
}

package { 'docker-selinux':
  ensure => 'absent',
}

package { 'docker-engine':
  ensure => 'absent',
}

package { 'yum-utils':
  ensure => 'present',
}

package { 'device-mapper-persistent-data':
  ensure => 'present',
}

package { 'lvm2':
  ensure => 'present',
}

sysctl { 'net.ipv4.ip_forward': value => '1' }

yumrepo { 'rhel7-extras':

 name => "rhel7-extras",
 gpgcheck => true,
 gpgkey => "http://prdctlvyumas01.associateaux.local/yumrepos/mrepo/rhel-7-x86_64/RPM-GPG-KEY/RPM-GPG-KEY-redhat-release",
 enabled => true,
 baseurl => "http://prdctlvyumas01.associateaux.local/yumrepos/mrepo/rhel-7-x86_64/RPMS.extra/",

}

package { 'container-selinux':
  
  ensure => 'present'
}

}
