package de.dfki.jade_rock_fipa_proxy;

import de.dfki.jade_rock_fipa_proxy.mtp.TcpMtp;
import de.dfki.jade_rock_fipa_proxy.mtp.TcpServer;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAException;
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
     * Adds/removes corresponding RockDummyAgents.
     */
    public class TcpMtpProxyServiceListener extends AbstractJadeJMDNSServiceListener {

        public TcpMtpProxyServiceListener() {
            super(TcpMtpAgent.this);
        }

        @Override
        public void handleAddedForeignAgent(String address, String agentName) {
            try {
                logger.log(Level.INFO, "Registering {0} ({1}) with the AMS.",
                        new Object[]{agentName, address});

                AID aid = new AID(agentName, true);
                aid.addAddresses(address);
                AMSAgentDescription amsad = new AMSAgentDescription();
                amsad.setName(aid);
                amsad.setState(AMSAgentDescription.ACTIVE);

                AMSService.register(TcpMtpAgent.this, amsad);
            } catch (FIPAException ex) {
                logger.log(Level.WARNING, "Could not register " + agentName, ex);
            }
        }

        @Override
        public void handleRemovedForeignAgent(String agentName) {
            try {
                logger.log(Level.INFO, "Deregistering {0} with the AMS.",
                        agentName);

                AID aid = new AID(agentName, true);
                AMSAgentDescription amsad = new AMSAgentDescription();
                amsad.setName(aid);
                amsad.setState(AMSAgentDescription.ACTIVE);

                AMSService.deregister(TcpMtpAgent.this, amsad);
            } catch (FIPAException ex) {
                logger.log(Level.WARNING, "Could not register " + agentName, ex);
            }
        }
    }

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
            jmdnsManager = new JMDNSManager(TcpServer.DEFAULT_PORT,
                    new TcpMtpProxyServiceListener());
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
