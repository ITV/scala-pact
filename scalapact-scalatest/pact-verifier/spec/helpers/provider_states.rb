require 'pact'

Pact.provider_states_for 'ExampleConsumer' do
  provider_state 'Provider service is available' do
    set_up do
    end
  end
end
