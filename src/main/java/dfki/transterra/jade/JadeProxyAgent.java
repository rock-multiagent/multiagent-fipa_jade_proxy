package dfki.transterra.jade;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.introspection.AMSSubscriber;
import jade.domain.introspection.BornAgent;
import jade.domain.introspection.DeadAgent;
import jade.domain.introspection.Event;
import jade.domain.introspection.IntrospectionVocabulary;
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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

/**
 * Handles the JMDNS/avahi subscriptions and creates/deletes RockDummyAgents.
 *
 * @author Satia Herfert
 */
public class JadeProxyAgent extends Agent {

    /**
     * Every time an agent gets "born" it is registered via JMDNS, and when it
     * "dies" it is unregistered.
     */
    private class AgentJMDNSRegisterBehaviour extends AMSSubscriber {

        protected void installHandlers(Map handlers) {

            EventHandler addAgentEH = new EventHandler() {
                public void handle(Event event) {
                    BornAgent ba = (BornAgent) event;
                    // We must only register JADE agents, not RockDummyAgents
                    if (!ba.getClassName().equals(RockDummyAgent.class.getName())) {
                        jmdnsManager.registerJadeAgent(ba.getAgent().getLocalName());
                    }
                }
            };

            EventHandler removeAgentEH = new EventHandler() {
                public void handle(Event event) {
                    DeadAgent da = (DeadAgent) event;
                    // We unregister in any case:
                    // It was a RockDummyAgent: the JMDNS entry will already be deleted
                    // It was a JADE Agent: we delete the entry
                    jmdnsManager.unregisterJadeAgent(da.getAgent().getLocalName());
                }
            };

            handlers.put(IntrospectionVocabulary.BORNAGENT, addAgentEH);
            handlers.put(IntrospectionVocabulary.DEADAGENT, removeAgentEH);
//          handlers.put(IntrospectionVocabulary.SUSPENDEDAGENT, jpa);
//          handlers.put(IntrospectionVocabulary.RESUMEDAGENT, jpa);
//          handlers.put(IntrospectionVocabulary.FROZENAGENT, jpa);
//          handlers.put(IntrospectionVocabulary.THAWEDAGENT, jpa);
//          handlers.put(IntrospectionVocabulary.MOVEDAGENT, jpa);
        }
    }

    /**
     * Listens for added/removed JMDNS services. Adds/removes corresponding
     * RockDummyAgents, unless it was a JADE agent that we registered ourselves.
     */
    public class JadeProxyServiceListener implements ServiceListener {

        // TODO this code must probably go to resolved!?
        public void serviceAdded(ServiceEvent event) {
            // If it is a ROCK agent, we must create a RockDummyAgent,
            // if it is a JADE agent, we must not do anything.
            try {
                getContainerController().getAgent(event.getName());
                // The agent was found, it is a JADE agent
            } catch (ControllerException ex) {
                // This means it's a ROCK agent
                logger.log(Level.INFO, "Rock agent appeared: {0}",
                        new String[]{event.getName()});
                
                // TODO parse the rockSocketPort from the event TXT LOCATOR
                
                String locator = event.getInfo().getPropertyString("LOCATOR");
                logger.log(Level.INFO, "Locator is {0}", locator);
                
                Agent rockDummyAgent = new RockDummyAgent();
                try {
                    Object[] args = new Object[] { 7890/*rockSocketPort*/ };
                    rockDummyAgent.setArguments(args);
                    
                    AgentController dummyControl = getContainerController().
                            acceptNewAgent(event.getName(), rockDummyAgent);
                    dummyControl.start();
                } catch (StaleProxyException ex0) {
                    logger.log(Level.WARNING, "Could not add RockProxyAgent to the container.", ex0);
                }
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

        public void serviceResolved(ServiceEvent event) {
            String locator = event.getInfo().getPropertyString("LOCATOR");
            logger.log(Level.INFO, "Locator is {0}", locator);
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
            if(args == null || args.length < 1) {
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

        this.addBehaviour(new AgentJMDNSRegisterBehaviour());

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
                        logger.log(Level.INFO, "Received on socket: {0}", msg.toString());
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
