require 'orocos'

include Orocos
Orocos.initialize

# Test producing a message delivery failure message
Orocos.run "fipa_services_test"  do
    puts "JADE communication test"
    
    # Start a mts for the communication
    begin
        mts_module = TaskContext.get "mts_0"
    rescue Orocos::NotFound
        print 'Deployment not found.'
        raise
    end
    
    # important to have tcp here, udt can be skipped
    mts_module.protocols = ["tcp"]
    
    mts_module.configure
    mts_module.start
    
    this_agent = "rock_agent"
    
    mts_module.addReceiver(this_agent, true)
    
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
        msg.setConversationID("rock_agent_cid")

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