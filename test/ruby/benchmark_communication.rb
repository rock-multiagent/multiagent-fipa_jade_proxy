require 'orocos'
require 'descriptive_statistics'

include Orocos
Orocos.initialize

# Test producing a message delivery failure message
Orocos.run "fipa_services::MessageTransportTask" => "mts", :valgrind => false  do
    puts "JADE communication test"

    # Start a mts for the communication
    begin
        mts_module = TaskContext.get "mts"
    rescue Orocos::NotFound
        print 'Deployment not found.'
        raise
    end

    # important to have tcp here, udt can be skipped
    mts_module.transports = ["tcp"]

    mts_module.configure
    mts_module.start

    this_agent = "rock_agent"

    mts_module.addReceiver(this_agent, true)

    letter_writer = mts_module.letters.writer
    letter_reader = mts_module.port(this_agent).reader

    other_agent = Readline::readline("Type JADE agent identifier:")

    require 'fipa-message'
    msg = FIPA::ACLMessage.new
    content = ""
    for i in 0..99
        content += "a"
    end
    msg.setContent("#{content}")
    msg.addReceiver(FIPA::AgentId.new(other_agent))

    msg.setSender(FIPA::AgentId.new(this_agent))
    msg.setConversationID("rock_agent_cid")

    env = FIPA::ACLEnvelope.new
    env.insert(msg, FIPARepresentation::BITEFFICIENT)

    deltaTime = Array.new
    numberOfSamples = 10000
    for i in 0..numberOfSamples
        startTime = Time.new
        letter_writer.write(env)
        while true
            if letter = letter_reader.read_new
                endTime = Time.new
                deltaTime << (endTime - startTime)
                break
            end
            sleep 0.001
        end
    end

    puts "Number of samples: #{numberOfSamples}"
    puts "Mean: #{deltaTime.mean}"
    puts "Stdev: #{deltaTime.standard_deviation}"
end
