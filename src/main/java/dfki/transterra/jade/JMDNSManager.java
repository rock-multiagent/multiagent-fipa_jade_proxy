package dfki.transterra.jade;

import java.io.IOException;
import java.net.InetAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

/**
 * Manages JMDNS/avahi registrations. JADE agents must register themselves here
 * so that ROCK agents can contact them.
 *
 * @author Satia Herfert
 */
public class JMDNSManager {

    /**
     * The JMDNS type
     */
    public static final String JMDNS_TYPE = "_fipa_service_directory._udp.local.";
    /**
     * ROCKs DateFormat
     */
    public static final DateFormat JMDNS_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd-HH:mm:ss:SSS000");

    public static final String JMDNS_DESCRIPTION = "Message proxy";

    /**
     * The Logger.
     */
    private static final Logger logger = Logger.getLogger(JMDNSManager.class.getName());

    private JmDNS jmdns;
    private InetAddress inetAddress;
    private int socketProxyAgentPort;

    /**
     * @param inetAddress the IPv4 address to bind to. Must point to the ROCK
     * proxy MTS uses!
     * @param socketProxyAgentPort the port the SocketProxyAgent listens on.
     * @param listener a listener that performs JADE relevant actions when
     * services are added or removed.
     */
    public JMDNSManager(InetAddress inetAddress, int socketProxyAgentPort, ServiceListener listener) {
        this.inetAddress = inetAddress;
        this.socketProxyAgentPort = socketProxyAgentPort;
        try {
            jmdns = JmDNS.create(inetAddress);
            jmdns.addServiceListener(JMDNS_TYPE, listener);
            logger.log(Level.INFO, "JMDNS created for IP {0}", inetAddress.getHostAddress());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "JMDNS could not be started", e);
        }
    }

    /**
     * Registers a local JADE agent
     *
     * @param localname its name
     */
    public void registerJadeAgent(String localname) {
        try {
            HashMap<String, String> properties = new HashMap<String, String>();
            properties.put("DESCRIPTION", JMDNS_DESCRIPTION);
            properties.put("TYPE", "mts_client");
            properties.put("LOCATOR", "udt://" + inetAddress.getHostAddress() + ":"
                    + socketProxyAgentPort + " fipa_services::MessageTransportTask");
            properties.put("TIMESTAMP", JMDNS_DATE_FORMAT.format(GregorianCalendar.getInstance().getTime()));

            jmdns.registerService(ServiceInfo.create(JMDNS_TYPE, localname, socketProxyAgentPort, 1, 1, true, properties));
            logger.log(Level.INFO, "JMDNS registerered {0}", localname);
        } catch (IOException e) {
            logger.log(Level.WARNING, "JMDNS register failed", e);
        }
    }

    /**
     * Unregisters a local JADE agent
     *
     * @param localname its name
     */
    public void unregisterJadeAgent(String localname) {
        ServiceInfo si = jmdns.getServiceInfo(JMDNS_TYPE, localname);
        if (si != null) {
            jmdns.unregisterService(si);
            logger.log(Level.INFO, "JMDNS unregisterered {0}", localname);
        } else {
            logger.log(Level.INFO, "JMDNS already unregistered: {0}", localname);
        }
    }

    /**
     * Closes the JMDNSManager and unregisters all JADE agents.
     */
    public void close() {
        try {
            jmdns.close();
            logger.log(Level.INFO, "JMDNS closed");
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Could not close the JMDNS", ex);
        }
    }
}
