require_relative './provider_states'

Pact.configure do |config|
  config.log_dir     = 'spec/pact/log'
  config.doc_dir     = 'spec/pact/doc'
  config.reports_dir = 'spec/pact/reports'
  config.tmp_dir     = 'spec/pact/tmp'
end
