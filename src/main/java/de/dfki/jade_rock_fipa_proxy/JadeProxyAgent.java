package de.dfki.jade_rock_fipa_proxy;

import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.ACLParser;
import jade.lang.acl.ParseException;
import jade.lang.acl.TokenMgrError;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jmdns.ServiceEvent;

/**
 * Handles the JMDNS/avahi subscriptions and creates/deletes RockDummyAgents.
 *
 * @author Satia Herfert
 */
public class JadeProxyAgent extends Agent {

    /**
     * Adds/removes corresponding RockDummyAgents.
     */
    public class JadeProxyServiceListener extends AbstractJadeJMDNSServiceListener {

        public JadeProxyServiceListener() {
            super(JadeProxyAgent.this);
        }

        @Override
        public void handleAddedRockAgent(String address, String agentName) {
            // now extract IP and port
            String[] parts = address.split(":");
            if (parts.length != 2) {
                logger.log(Level.INFO, "Malformed tcp endpoint: {0}",
                        address);
                return;
            }

            Agent rockDummyAgent = new RockDummyAgent();
            try {
                rockDummyAgent.setArguments(parts);

                AgentController dummyControl = getContainerController().
                        acceptNewAgent(agentName, rockDummyAgent);
                dummyControl.start();
            } catch (StaleProxyException ex0) {
                logger.log(Level.WARNING, "Could not add RockProxyAgent to the container.", ex0);
            }
        }

        public void serviceRemoved(ServiceEvent event) {
        // If it was a ROCK agent, we must delete the RockDummyAgent,
            // if it was a JADE agent, we must not do anything.
            try {
                getContainerController().getAgent(event.getName());
                // The agent was found, it was a ROCK agent
                logger.log(Level.INFO, "Rock agent disappeared: {0}",
                        new String[]{event.getName()});

                try {
                    getContainerController().getAgent(event.getName()).kill();
                } catch (StaleProxyException ex) {
                    logger.log(Level.WARNING, "Could not kill RockProxyAgent.", ex);
                } catch (ControllerException ex) {
                    logger.log(Level.WARNING, "Could not kill RockProxyAgent.", ex);
                }
            } catch (ControllerException ex) {
                // The agent is already removed, it was a JADE agent
            }
        }
    }

    // These are to identify rock messages
    public static final String ROCK_MSG_USER_DEF_PARAM_KEY = "isRockMessage";
    public static final String ROCK_MSG_USER_DEF_PARAM_VALUE = "true";

    /**
     * The Logger.
     */
    private static final Logger logger = Logger.getLogger(JadeProxyAgent.class.getName());

    /**
     * The JMDNS Manager
     */
    private JMDNSManager jmdnsManager;

    /**
     * The ServerSocket to accept connections.
     */
    private ServerSocket serverSocket;

    /**
     * Jade's Socket port.
     */
    private int jadeSocketPort;

    /**
     * Creates the JMDNS Manager and adds the behavior registering all JADE
     * agents there. Also, listens for incoming serverSocket messages.
     */
    @Override
    protected void setup() {
        try {
            Object[] args = getArguments();
            if (args == null || args.length < 1) {
                throw new Exception();
            }

            jadeSocketPort = Integer.parseInt(args[0].toString());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Bad/no port argument provided", e);
            // In the case of an exception here, we cannot function properly
            doDelete();
            return;
        }

        logger.log(Level.INFO, "JadeProxyAgent {0}: starting", getLocalName());
        try {
            jmdnsManager = new JMDNSManager(jadeSocketPort, new JadeProxyServiceListener());
            serverSocket = new ServerSocket(jadeSocketPort);
            
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error initializing JMDNS or ServerSocket:", ex);
            // In the case of an exception here, we cannot function properly
            doDelete();
            return;
        }

        this.addBehaviour(new AgentJMDNSRegisterBehaviour(jmdnsManager));

        // Start thread accepting connections
        new Thread() {
            @Override
            public void run() {
                acceptConnectionsAndForward();
            }
        }.start();
    }

    /**
     * Closes also the JMDNS Manager
     */
    @Override
    protected void takeDown() {
        logger.log(Level.INFO, "JadeProxyAgent {0}: terminating", getLocalName());
        if (jmdnsManager != null) {
            jmdnsManager.close();
        }
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Could not close server socket.", ex);
            }
        }
    }

    /**
     * Accepts connections, de-serializes the messages and forwards them.
     */
    private void acceptConnectionsAndForward() {
        try {
            while (true) {
                Socket socket = serverSocket.accept();
                logger.log(Level.FINE, "Accepted a new connection");
                try {
                    ACLMessage msg;
                    try {
                        // Parse message
                        ACLParser parser = new ACLParser(socket.getInputStream());
                        msg = parser.Message();
                        logger.log(Level.INFO, "Message received on socket: {0}", msg.toString());
                    } finally {
                        socket.close();
                        logger.log(Level.FINE, "Closed connection");
                    }
                    // Sender & every receiver must be set to NOT local
                    AID oldSender = msg.getSender();
                    if (oldSender != null) {
                        msg.setSender(new AID(oldSender.getLocalName(), false));
                    }

                    ArrayList<AID> newRecvs = new ArrayList<AID>();
                    Iterator<AID> i = msg.getAllReceiver();
                    while (i.hasNext()) {
                        AID aid = i.next();
                        newRecvs.add(new AID(aid.getLocalName(), false));
                    }
                    msg.clearAllReceiver();
                    for (AID aid : newRecvs) {
                        msg.addReceiver(aid);
                    }

                    // Set a custom field, to mark it as a ROCK message
                    msg.addUserDefinedParameter(ROCK_MSG_USER_DEF_PARAM_KEY, ROCK_MSG_USER_DEF_PARAM_VALUE);

                    // Now send message
                    send(msg);
                    //msg.setDefaultEnvelope();

                } catch (TokenMgrError e) {
                    logger.log(Level.WARNING, "Socket parsing threw: ", e);
                } catch (ParseException e) {
                    logger.log(Level.WARNING, "Socket parsing threw: ", e);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Socket reading/closing threw: ", e);
                }
            }
        } catch (IOException e) {
            // accept threw an exception. Could also be due to takeDown
            logger.log(Level.SEVERE, "Socket accept threw (could be due to takeDown): ", e);
        }
    }
}
