require 'orocos'

include Orocos
Orocos.initialize

# Test producing a message delivery failure message
Orocos.run "fipa_services_test"  do
    puts "JADE communication test"
    
    # Start a mts for the communication
    begin
        mts_module = TaskContext.get "mts_0"
        mts_proxy_module = TaskContext.get "mts_1"
    rescue Orocos::NotFound
        print 'Deployment not found.'
        raise
    end
    mts_proxy_module.configure
    mts_module.configure
    mts_proxy_module.start
    mts_module.start
    
    this_agent = "rock_agent"
    dummy_proxy_agent = "dummy_proxy_agent"
    
    mts_module.addReceiver(this_agent, true)
    
    # make the mts socket capable, and add a dummy to be abel to see the IP and Port in avahi-discover
    mts_proxy_module.addReceiver(dummy_proxy_agent, true)
    mts_proxy_module.addSocketTransport()
    
    letter_writer = mts_module.letters.writer
    letter_reader = mts_module.port(this_agent).reader
    
    other_agent = Readline::readline("Type JADE agent identifier:")
    
    while true
        Readline::readline("Press ENTER to send a msg.")
        
        require 'fipa-message'
        msg = FIPA::ACLMessage.new
        msg.setContent("test-content from #{this_agent} to #{other_agent} #{Time.now}")
        msg.addReceiver(FIPA::AgentId.new(other_agent))
        # comment next line out
        #msg.addReceiver(FIPA::AgentId.new(this_agent))
        
        msg.setSender(FIPA::AgentId.new(this_agent))
        msg.setConversationID("conversation-id")

        env = FIPA::ACLEnvelope.new
        env.insert(msg, FIPARepresentation::BITEFFICIENT)
        
        letter_writer.write(env)
        
        Readline::readline("Press ENTER when a response has been sent or to continue.")

        while letter = letter_reader.read_new
            puts "#{this_agent}: received letter: #{letter.getACLMessage.getContent}"
            Readline::readline("Press ENTER when a response has been sent or to continue.")
        end
        sleep 1
    end
end