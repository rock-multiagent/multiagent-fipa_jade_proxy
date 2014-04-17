package dfki.transterra.jade;

import dfki.transterra.jade.mtp.TcpMtp;
import dfki.transterra.jade.mtp.TcpServer;
import jade.core.Agent;
import jade.mtp.MTPException;
import jade.wrapper.StaleProxyException;
import java.io.IOException;
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
            logger.log(Level.SEVERE, "Could not install mtp", ex);
            // In the case of an exception here, we cannot function properly
            doDelete();
            return;
        } catch (StaleProxyException ex) {
            logger.log(Level.SEVERE, "Could not install mtp", ex);
            // In the case of an exception here, we cannot function properly
            doDelete();
            return;
        }
        
        try {
            jmdnsManager = new JMDNSManager(TcpServer.DEFAULT_PORT, null);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error initializing JMDNS", ex);
            // In the case of an exception here, we cannot function properly
            doDelete();
            return;
        }
        
        logger.log(Level.INFO, "TcpMtpAgent {0}: starting", getLocalName());
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
}
