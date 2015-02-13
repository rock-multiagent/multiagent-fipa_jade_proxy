/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.dfki.jade_rock_fipa_proxy.mtp;

//import cascom.fipa.acl.BitEffACLCodec;
//import cascom.fipa.envelope.BitEfficientEnvelopeCodec;
import jade.core.AID;
import jade.core.Profile;
import jade.domain.FIPAAgentManagement.Envelope;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.ACLParser;
import jade.lang.acl.ParseException;
import jade.mtp.MTP;
import jade.mtp.MTPException;
import jade.mtp.TransportAddress;
import jade.mtp.http.XMLCodec;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    /**
     * All currently open connections. Only outgoing.
     */
    private final Map<String, Socket> openConnections = new HashMap<String, Socket>();

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
        logger.log(Level.INFO, "Deactivating all tcp servers.");
        // Deactivate all servers
        for (TcpServer server : servers.values()) {
            server.deactivate();
        }
        // Clear map
        servers.clear();
        
        // Close all outgoing connections
        for(Socket socket : openConnections.values()) {
            try {
                socket.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Closing socket failed: ", e);
                throw new MTPException("Closing socket failed: ", e);
            }
        }
        openConnections.clear();
    }

    public void deliver(String string, Envelope envlp, byte[] bytes) throws MTPException {
        // Check if there is already an open socket.
        Socket socket = null;
        if (openConnections.containsKey(string)) {
            logger.log(Level.INFO, "Reusing connection to {0}. Checking if still open.", string);
            socket = openConnections.get(string);
            // Check if the socket is still open
            if (socket.isClosed()) {
                logger.log(Level.INFO, "Removing connection to {0}, it has been closed.", string);
                // Remove from open connections
                openConnections.remove(string);
                socket = null;
            }
        }
        // Create a new socket if we didn't find any open
        if (socket == null) {
            logger.log(Level.INFO, "Opening a new connection to {0}", string);
            try {
                // Extract tcp address
                String[] addParts = string.substring("tcp://".length()).split(":");
                socket = new Socket(addParts[0], Integer.parseInt(addParts[1]));
                openConnections.put(string, socket);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Creating connection failed: ", e);
                throw new MTPException("Creating connection failed: ", e);
            } catch (ArrayIndexOutOfBoundsException e) {
                logger.log(Level.WARNING, "Creating connection failed: ", e);
                throw new MTPException("Creating connection failed: ", e);
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "Creating connection failed: ", e);
                throw new MTPException("Creating connection failed: ", e);
            }
        }

        // Finally send
        send(socket, envlp, bytes);
    }

    /**
     * Actually sends an envelope through an open socket.
     *
     * @param socket the socket to use. Must be open.
     * @param envlp the envelope.
     * @param bytes the message bytes.
     */
    private void send(Socket socket, Envelope envlp, byte[] bytes) throws MTPException {
        logger.log(Level.INFO, "Sending envelope to {0}:{1}", 
                new Object[] { socket.getInetAddress(), socket.getPort() });

        try {
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            // XXX Modify envelope from
            envlp.setFrom(new AID(envlp.getFrom().getName(), true));
            // ... and message sender
            try {
                ACLMessage msg = ACLParser.create().parse(new StringReader(new String(bytes)));
                msg.setSender(new AID(msg.getSender().getName(), true));
                bytes = msg.toString().getBytes();
            } catch (ParseException e) {
                logger.log(Level.WARNING, "Could not modify message sender: ", e);
            }
            // IMPORTANT: This changes the payload length, so remember to adapt that
            envlp.setPayloadLength((long)bytes.length);

            // First send envelope in XML encoding
            // No line break after the envelope, so that the payload
            // starts immediately after.
            String xmlEnv = XMLCodec.encodeXML(envlp);
            writer.print(xmlEnv);
            // And now message (bytes) XXX use output stream directly, not string!
            writer.print(new String(bytes));
            writer.flush();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Forwarding envelope failed: ", e);
            throw new MTPException("Forwarding envelope failed: ", e);
        }
    }
}
