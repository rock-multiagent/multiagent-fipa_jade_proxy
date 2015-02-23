java -jar ./target/fipa_services-0.0.1-jar-with-dependencies.jar -gui -agents tcpMtpAgent:multiagent.fipa_services.TcpMtpAgent;echoAgent:multiagent.fipa_services.example.EchoAgent
# Note: starting multiple agents seems not to work via -agents: start from GUI
# open nodes of AgentPlatforms-><IP>->MainContainer
# right-click on MainContainer -> Start New Agent, browse and select the EchoAgent and give it a
# proper name
