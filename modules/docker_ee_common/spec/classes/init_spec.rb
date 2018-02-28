require 'spec_helper'
describe 'docker_ee_common' do
  context 'with default values for all parameters' do
    it { should contain_class('docker_ee_common') }
  end
end
