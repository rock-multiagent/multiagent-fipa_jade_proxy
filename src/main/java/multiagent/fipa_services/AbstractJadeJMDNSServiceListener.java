package multiagent.fipa_services;

import jade.core.AID;
import jade.core.Agent;
import jade.wrapper.ControllerException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

/**
 * Listens for added/removed JMDNS services. Differentiate between Jade and
 * non-Jade agents and ignores Jade agents.
 *
 * @author Satia Herfert
 */
public abstract class AbstractJadeJMDNSServiceListener implements ServiceListener {

    /**
     * The Logger.
     */
    private static final Logger logger = Logger.getLogger(AbstractJadeJMDNSServiceListener.class.getName());

    /**
     * This listener needs an associated agent.
     */
    private final Agent agent;

    /**
     *
     * @param agent the associated agent.
     */
    public AbstractJadeJMDNSServiceListener(Agent agent) {
        this.agent = agent;
    }

    /**
     * Handle that a foreign agent appeared.
     *
     * @param address the full address.
     * @param agentName the agent's name.
     */
    public abstract void handleAddedForeignAgent(String address, String agentName);

    /**
     * Handle that a foreign agent disappeared.
     *
     * @param agentName the agent's name.
     */
    public abstract void handleRemovedForeignAgent(String agentName);

    /**
     * From a semicolon-separated list of locators, extracts the first TCP
     * endpoint's address and port in the format "tcp://IP:port".
     *
     * @param locators the locators
     * @return the endpoint or null on error
     */
    private String getTCPResolverAddress(String locators) {
        if (locators == null) {
            return null;
        }
        final String prefix = "tcp://";

        String[] locatorsArray = locators.split(";");
        for (String locator : locatorsArray) {
            // TODO (optional) : check for valid service signatures
            if (locator.startsWith(prefix)) {
                // Cut last part, cut "tcp://"
                String[] locatorParts = locator.split(" ");
                if (locatorParts.length < 2) {
                    return null;
                }

                return locatorParts[0];
            }
        }
        return null;
    }

    /**
     * This method is only necessary, as JADE's agents are published without
     * dots -- since that is not properly understood by 
     * the mdsn implementation.
     * To distinguish jade from foreign agents, this has to be reverted.
     *
     * Also, the local name is extracted, which is necessary in any case.
     *
     * @param name the given name.
     * @return the actual name.
     */
    private String getActualLocalName(String name) {
        AID aid = new AID(name.replaceAll("?", "."), true);
        return aid.getLocalName();
    }

    /**
     * Handler when services are added
     */
    public void serviceAdded(ServiceEvent event) {
        String name = getActualLocalName(event.getName());
        // If it is a foreign agent, we must handle the event
        // if it is a JADE agent, we must not do anything.
        try {
            agent.getContainerController().getAgent(name);
            // The agent was found, it is a JADE agent
        } catch (ControllerException ex) {
            // This means it's a foreign agent
            logger.log(Level.INFO, "Foreign agent appeared: {0}", name);

            // event.getInfo() does not suffice, service must be resolved
            ServiceInfo info = event.getDNS().getServiceInfo(event.getType(),
                    event.getName());

            // Get the TXT LOCATOR
            String locators = info.getPropertyString("LOCATOR");
            // And parse the tcp address, if any
            String endpoint = getTCPResolverAddress(locators);
            if (endpoint == null) {
                logger.log(Level.INFO, "Unknown agent without (valid) tcp locator: {0}",
                        event.getName());
                return;
            }

            handleAddedForeignAgent(endpoint, event.getName());
        }
    }

    public void serviceRemoved(ServiceEvent event) {
        logger.log(Level.INFO, "Agent disappeared: {0}", event.getName());
        handleRemovedForeignAgent(event.getName());
    }

    public void serviceResolved(ServiceEvent event) {
    }
}
