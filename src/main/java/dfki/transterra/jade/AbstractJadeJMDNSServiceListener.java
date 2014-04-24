/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dfki.transterra.jade;

import jade.core.Agent;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
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
    private Agent agent;

    /**
     *
     * @param agent the associated agent.
     */
    public AbstractJadeJMDNSServiceListener(Agent agent) {
        this.agent = agent;
    }

    /**
     * Handle this.
     *
     * @param address the full address.
     * @param agentName the agent's name.
     */
    public abstract void handleAddedRockAgent(String address, String agentName);

    /**
     * From a semicolon-separated list of locators, extracts the first TCP
     * endpoint's address and port in the format "IP:port".
     *
     * @param locators the locators
     * @return the endpoint.
     */
    private String getTCPResolverAddress(String locators) {
        if (locators == null) {
            return null;
        }
        final String prefix = "tcp://";

        String[] locatorsArray = locators.split(";");
        for (String locator : locatorsArray) {
            // XXX also check for "fipa::services::message_transport::SocketTransport" ?
            if (locator.startsWith(prefix)) {
                // Cut last part, cut "tcp://"
                String[] locatorParts = locator.split(" ");
                if (locatorParts.length < 2) {
                    return null;
                }

                return locatorParts[0].substring(prefix.length());
            }
        }
        return null;
    }

    public void serviceAdded(ServiceEvent event) {

        // If it is a ROCK agent, we must create a RockDummyAgent,
        // if it is a JADE agent, we must not do anything.
        try {
            agent.getContainerController().getAgent(event.getName());
            // The agent was found, it is a JADE agent
        } catch (ControllerException ex) {
            // This means it's a ROCK agent
            logger.log(Level.INFO, "Foreign agent appeared: {0}",
                    new String[]{event.getName()});

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

            handleAddedRockAgent(endpoint, event.getName());
        }
    }

    public void serviceResolved(ServiceEvent event) {
    }
}
