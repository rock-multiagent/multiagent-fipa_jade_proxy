package dfki.transterra.jade;

import jade.core.Agent;
import jade.domain.introspection.AMSSubscriber;
import jade.domain.introspection.BornAgent;
import jade.domain.introspection.DeadAgent;
import jade.domain.introspection.Event;
import jade.domain.introspection.IntrospectionVocabulary;
import jade.tools.SocketProxyAgent.SocketProxyAgent;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
import java.net.InetAddress;
import java.net.UnknownHostException;
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

        // TODO more handlers?
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
                    // It was a JADE Agent: we delte the entry
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
                Agent rockDummyAgent = new RockDummyAgent();
                try {
                    // TODO use paramteres as soon as he expects some
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
        }
    }

    /**
     * The Logger.
     */
    private static final Logger logger = Logger.getLogger(JadeProxyAgent.class.getName());

    /**
     * The JMDNS Manager
     */
    private JMDNSManager jmdnsManager;

    /**
     * Creates the JMDNS Manager and adds the behavior registering all JADE
     * agents there. Also, starts an SocketProxyAgent.
     */
    @Override
    protected void setup() {
        logger.log(Level.INFO, "JadeProxyAgent {0}: starting", getLocalName());
        try {
            jmdnsManager = new JMDNSManager(InetAddress.getByName("134.102.232.209"),
                    1099, new JadeProxyServiceListener()); // FIXME IP and Port
        } catch (UnknownHostException ex) {
            logger.log(Level.SEVERE, "Could not create the JMDNS Manager.", ex);
        }

        Agent spa = new SocketProxyAgent();
        try {
            // Default params. Port: 6789 IP: JADE IP
            AgentController spaControl = getContainerController().
                    acceptNewAgent("jadeSocketProxyAgent", spa);
            spaControl.start();
        } catch (StaleProxyException ex0) {
            logger.log(Level.SEVERE, "Could not start the SocketProxyAgent.", ex0);
        }

        this.addBehaviour(new AgentJMDNSRegisterBehaviour());
    }

    /**
     * Closes also the JMDNS Manager
     */
    @Override
    protected void takeDown() {
        logger.log(Level.INFO, "JadeProxyAgent {0}: terminating", getLocalName());
        jmdnsManager.close();
    }
}
