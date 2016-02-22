require 'bundler/setup'
require 'pact/provider/proxy/tasks'

# Wrapper for invoking one or more verify tasks
namespace :pact do
  task :verify do
    Rake::Task['pact:verify:microservice'].invoke
  end
end

# Runs given pact file against given endpoint e.g.
# endpoint="https://auth.qa.user.itv.com" spec_url="spec/pacts/mywebconsumer-mywebservice.json" bundle exec rake pact:verify
Pact::ProxyVerificationTask.new(:microservice) do |task|
  task.pact_url ENV['spec_url'], pact_helper: 'helpers/pact_helper.rb'
  task.provider_base_url ENV['endpoint']
end

# Runs the test user-reg pact file against the auth QA environment, run this to make sure dev env works ok
#Pact::ProxyVerificationTask.new(:mockwebservice) do |task|
#  task.pact_url 'spec/pacts/mywebconsumer-mywebservice.json', pact_helper: 'helpers/pact_helper.rb'
#  task.provider_base_url 'https://auth.qa.user.itv.com'
#end
