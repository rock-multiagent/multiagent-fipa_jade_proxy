/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.dfki.jade_rock_fipa_proxy.mtp;

//import cascom.fipa.acl.BitEffACLCodec;
//import cascom.fipa.envelope.BitEfficientEnvelopeCodec;
import de.dfki.jade_rock_fipa_proxy.RockDummyAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.core.Profile;
import jade.domain.FIPAAgentManagement.Envelope;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ReceivedObject;
import jade.lang.acl.ACLCodec;
import jade.lang.acl.ACLMessage;
import jade.mtp.MTP;
import jade.mtp.MTPException;
import jade.mtp.TransportAddress;
import jade.mtp.http.XMLCodec;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

/**
 *
 * @author Satia Herfert
 */
public class TcpMtp implements MTP {

    /**
     * The Logger.
     */
    private static final Logger logger = Logger.getLogger(TcpMtp.class.getName());
    /**
     * The MTP name.
     */
    private static final String FIPA_NAME = "fipa.mts.mtp.tcp.std";
    /**
     * The supported protocols.
     */
    private static final String[] PROTOCOLS = {"tcp"};
    /**
     * Maps the addresses (TcpAddress.toString()) to the TcpServers.
     */
    private final Map<String, TcpServer> servers = new HashMap<String, TcpServer>();

    public TransportAddress strToAddr(String string) throws MTPException {
        try {
            return new TcpAddress(string);
        } catch (UnknownHostException ex) {
            throw new MTPException("Malformed tcp address", ex);
        }
    }

    public String addrToStr(TransportAddress ta) throws MTPException {
        if (ta instanceof TcpAddress) {
            return ((TcpAddress) ta).toString();
        }
        throw new MTPException("Not a TCPAddress");
    }

    public String getName() {
        return FIPA_NAME;
    }

    public String[] getSupportedProtocols() {
        return PROTOCOLS;
    }

    public TransportAddress activate(Dispatcher d, Profile prfl) throws MTPException {
        logger.log(Level.INFO, "Activating Tcp server.");
        // Create
        TcpServer server;
        try {
            server = new TcpServer(d, prfl);
        } catch (SocketException ex) {
            logger.log(Level.SEVERE, "Activating Tcp server failed: ", ex);
            throw new MTPException("Activating Tcp server failed: ", ex);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Activating Tcp server failed: ", ex);
            throw new MTPException("Activating Tcp server failed: ", ex);
        }
        // Save
        servers.put(server.getAddress().toString(), server);
        // Return address
        return server.getAddress();
    }

    public void activate(Dispatcher d, TransportAddress ta, Profile prfl) throws MTPException {
        if (!(ta instanceof TcpAddress)) {
            throw new MTPException("Not a TCPAddress");
        }
        logger.log(Level.INFO, "Activating Tcp server.");
        // Create
        TcpServer server;
        try {
            server = new TcpServer(d, prfl, (TcpAddress) ta);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Activating Tcp server failed: ", ex);
            throw new MTPException("Activating Tcp server failed: ", ex);
        }
        // Save
        servers.put(server.getAddress().toString(), server);
    }

    public void deactivate(TransportAddress ta) throws MTPException {
        if (!(ta instanceof TcpAddress)) {
            throw new MTPException("Not a TCPAddress");
        }
        logger.log(Level.INFO, "Deactivating Tcp server.");
        // Deactivate
        TcpServer server = servers.get(ta.toString());
        server.deactivate();
        // Remove from map
        servers.remove(ta.toString());
    }

    public void deactivate() throws MTPException {
        logger.log(Level.INFO, "Activating all tcp servers.");
        // Deactivate all servers
        for (TcpServer server : servers.values()) {
            server.deactivate();
        }
        // Clear map
        servers.clear();
    }

    public void deliver(String string, Envelope envlp, byte[] bytes) throws MTPException {
        logger.log(Level.INFO, "Sending envelope to {0}", string);

        // Extract tcp address
        String[] addParts = string.substring("tcp://".length()).split(":");

        try {
            Socket socket = new Socket(addParts[0], Integer.parseInt(addParts[1]));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                        
            // First send envelope in XML encoding
            String xmlEnv = XMLCodec.encodeXML(envlp);
            writer.println(xmlEnv);
            System.out.println(xmlEnv);
            
            // And now message (bytes) XXX use output stream directly, not string!
            writer.println(new String(bytes));
            System.out.println(new String(bytes));
            
            // Include EOF
            //writer.print(-1);
            socket.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Forwarding envelope to Rock failed: ", e);
            throw new MTPException("Forwarding envelope to Rock failed: ", e);
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.log(Level.WARNING, "Forwarding envelope to Rock failed: ", e);
            throw new MTPException("Forwarding envelope to Rock failed: ", e);
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "Forwarding envelope to Rock failed: ", e);
            throw new MTPException("Forwarding envelope to Rock failed: ", e);
        }
    }
}

