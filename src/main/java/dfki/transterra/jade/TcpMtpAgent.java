package dfki.transterra.jade;

import dfki.transterra.jade.mtp.TcpMtp;
import dfki.transterra.jade.mtp.TcpServer;
import jade.core.Agent;
import jade.domain.introspection.AMSSubscriber;
import jade.domain.introspection.BornAgent;
import jade.domain.introspection.DeadAgent;
import jade.domain.introspection.Event;
import jade.domain.introspection.IntrospectionVocabulary;
import jade.mtp.MTPException;
import jade.wrapper.StaleProxyException;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles the JMDNS/avahi subscriptions and installs the TcpMtp.
 *
 * @author Satia Herfert
 */
public class TcpMtpAgent extends Agent {

    /**
     * The Logger.
     */
    private static final Logger logger = Logger.getLogger(TcpMtpAgent.class.getName());

    /**
     * The JMDNS Manager
     */
    private JMDNSManager jmdnsManager;

    /**
     * Creates the JMDNS Manager and adds the behavior registering all JADE
     * agents there. Also, listens for incoming serverSocket messages.
     */
    @Override
    protected void setup() {
        
        try {
            // Install the TcpMtp
            getContainerController().installMTP(null, TcpMtp.class.getName());
        } catch (MTPException ex) {
            Logger.getLogger(TcpMtpAgent.class.getName()).log(Level.SEVERE, null, ex);
        } catch (StaleProxyException ex) {
            Logger.getLogger(TcpMtpAgent.class.getName()).log(Level.SEVERE, null, ex);
        }

        logger.log(Level.INFO, "TcpMtpAgent {0}: starting", getLocalName());
        try {
            jmdnsManager = new JMDNSManager(TcpServer.DEFAULT_PORT, null);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error initializing JMDNS", ex);
            // In the case of an exception here, we cannot function properly
            doDelete();
            return;
        }

        this.addBehaviour(new AgentJMDNSRegisterBehaviour(jmdnsManager));
        
        
    }

    /**
     * Closes also the JMDNS Manager
     */
    @Override
    protected void takeDown() {
        logger.log(Level.INFO, "TcpMtpAgent {0}: terminating", getLocalName());
        if (jmdnsManager != null) {
            jmdnsManager.close();
        }
    }

    
    
    /**
     * From a semicolon-separated list of locators, extracts the first
 TCP endpoint's address and port in the format "IP:port".
     * @param locators the locators
     * @return the endpoint.
     */
    private String getTCPResolverAddress(String locators) {
        if(locators == null) {
            return null;
        }
        final String prefix = "tcp://";
        
        String[] locatorsArray = locators.split(";");
        for(String locator : locatorsArray) {
            // XXX also check for "fipa::services::message_transport::SocketTransport" ?
            if(locator.startsWith(prefix)) {
                // Cut last part, cut "tcp://"
                String[] locatorParts = locator.split(" ");
                if(locatorParts.length < 2) {
                    return null;
                }
                
                return locatorParts[0].substring(prefix.length());
            }
        }
        return null;
    }
}
