Vagrant.configure("2") do |config|

  config.vm.define "node-1" do |node|
   node.vm.box = "generic/rhel7"
   node.vm.network "private_network", ip: "192.168.35.10"

   #node.vm.provision "puppet"

   node.vm.provision "shell", path: "scripts/install_puppet.sh"
   #node.vm.synced_folder "file", source: "/Users/strongbad/Documents/code/contino/docker-ee-puppet/puppet/", destination: "/home/vagrant/puppet", create: true

   node.vm.provider "virtualbox" do |v|
       v.memory = "4096"

       # Change the network adapter type and promiscuous mode
       v.customize ['modifyvm', :id, '--nictype1', '82540em']
       v.customize ['modifyvm', :id, '--nicpromisc1', 'allow-all']
       v.customize ['modifyvm', :id, '--nictype2', '82540em']
       v.customize ['modifyvm', :id, '--nicpromisc2', 'allow-all']
     end

  end

end
