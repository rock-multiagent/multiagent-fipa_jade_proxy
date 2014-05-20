/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.dfki.jade_rock_fipa_proxy.mtp;

//import cascom.fipa.acl.ACLDecoder;
//import cascom.fipa.acl.ACLEncoder;
//import cascom.fipa.acl.BEParseException;
//import cascom.fipa.acl.BitEffACLCodec;
//import cascom.fipa.envelope.BitEfficientEnvelopeCodec;
//import cascom.fipa.envelope.EnvelopeDecoder;
//import cascom.fipa.envelope.EnvelopeEncoder;
import de.dfki.jade_rock_fipa_proxy.JMDNSManager;
import jade.core.AID;
import jade.core.Profile;
import jade.domain.FIPAAgentManagement.Envelope;
import jade.lang.acl.ACLMessage;
import jade.mtp.InChannel;
import jade.mtp.MTPException;
import jade.mtp.http.XMLCodec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A TcpServer that listens for incoming envelopes and forwards them
 * accordingly.
 *
 * @author Satia Herfert
 */
public class TcpServer {

    /**
     * The default port.
     */
    public static final int DEFAULT_PORT = 6789;

    /**
     * The XML parser implementation class.
     */
    public static final String XML_PARSER_CLASS = "org.apache.crimson.parser.XMLReaderImpl";
    /**
     * The Logger.
     */
    private static final Logger logger = Logger.getLogger(TcpServer.class.getName());

    /**
     * The ServerSocket to accept connections.
     */
    private ServerSocket serverSocket;

    private final InChannel.Dispatcher dispatcher;
    private final Profile prfl;
    private final TcpAddress address;

    /**
     * Creates a TcpServer with the default port on the local IPv4 network.
     *
     * @param dispatcher the dispatcher
     * @param prfl the dispatcher
     * @throws SocketException when there is no local IPv4 network
     * @throws IOException if activation fails
     */
    public TcpServer(InChannel.Dispatcher dispatcher, Profile prfl) throws SocketException, IOException {
        this.dispatcher = dispatcher;
        this.prfl = prfl;
        this.address = new TcpAddress(JMDNSManager.getLocalIPv4Address(), DEFAULT_PORT);

        activate();
    }

    /**
     * Creates a TcpServer.
     *
     * @param dispatcher the dispatcher
     * @param prfl the dispatcher
     * @param address the address to use
     * @throws IOException if activation fails
     */
    public TcpServer(InChannel.Dispatcher dispatcher, Profile prfl, TcpAddress address) throws IOException {
        this.dispatcher = dispatcher;
        this.prfl = prfl;
        this.address = address;

        activate();
    }

    public TcpAddress getAddress() {
        return address;
    }

    /**
     * Activates this server, opens the socket. Called from the constructors.
     */
    private void activate() throws IOException {
        logger.log(Level.INFO, "TcpServer activating");
        try {
            serverSocket = new ServerSocket(address.getPortNo());
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error initializing ServerSocket:", ex);
            // In the case of an exception here, we cannot function properly
            throw ex;
        }
        // Start thread accepting connections
        new Thread() {
            @Override
            public void run() {
                acceptConnections();
            }
        }.start();
    }

    /**
     * Deactivates this server, closes the socket.
     */
    public void deactivate() {
        logger.log(Level.INFO, "TcpServer terminating");
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Could not close server socket.", ex);
            }
        }
    }

    /**
     * Accepts connections, de-serializes the envelopes and dispatches them.
     */
    private void acceptConnections() {
        try {
            while (true) {
                final Socket socket = serverSocket.accept();
                logger.log(Level.INFO, "Accepted a new connection");
                // Start thread accepting connections
                new Thread() {
                    @Override
                    public void run() {
                        forward(socket);
                    }
                }.start();
            }
        } catch (IOException e) {
            // accept threw an exception. Could also be due to takeDown
            logger.log(Level.SEVERE, "Socket accept threw (could be due to deactivate): ", e);
        }
    }

    /**
     * De-serializes the envelopes and dispatches them.
     */
    private void forward(Socket socket) {
        ACLMessage msg;
        Envelope env;
        String input;

        // XXX this is not robust!
        final String splitter = "</envelope>";

        // XXX This can destroy bit-efficient encoding. One should read
        // into a byte[] instead of char[]
        try {
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                StringBuilder buffer = new StringBuilder();

                char[] segment = new char[4096];
                int charsRead;
                while ((charsRead = reader.read(segment, 0, segment.length)) != -1) {
                    buffer.append(segment, 0, charsRead);
                    
                    int index;
                    // As long as a (new) whole envelope is included...
                    while ((index = buffer.indexOf(splitter)) != -1) {
                        // Include the end tag
                        index += splitter.length();
                        // At least the envelope is included, the message may
                        // still be missing
                        StringReader envReader = new StringReader(buffer.substring(0, index));

                        // Parse envelope
                        XMLCodec xmlCodec = new XMLCodec(XML_PARSER_CLASS);
                        env = xmlCodec.parse(envReader);
                        logger.log(Level.INFO, "Decoded env: {0}", env);

                        // Modify receivers
                        // Using a set removes duplicates that occur,
                        // as Rock and Jade interpret FIPA differently.
                        // dispatch envelope
                        Set<AID> newRecvs = new HashSet<AID>();
                        Iterator<AID> i = env.getAllIntendedReceiver();
                        while (i.hasNext()) {
                            AID aid = i.next();
                            newRecvs.add(new AID(aid.getName().replaceAll("-dot-", "."), true));
                        }
                        env.clearAllIntendedReceiver();
                        for (AID aid : newRecvs) {
                            env.addIntendedReceiver(aid);
                        }

                        int msgLen = env.getPayloadLength().intValue();
                        // Keep reading until message is fully included,
                        // or end of stream is reached
                        while (buffer.length() < index + msgLen
                                && (charsRead = reader.read(segment, 0, segment.length)) != -1) {
                            buffer.append(segment, 0, charsRead);
                        }

                        // Optional message parsing:
                        //StringReader msgReader = new StringReader(parts[1]);
                        //ACLParser aclParser = new ACLParser(msgReader);
                        //msg = aclParser.parse(msgReader);
                        //logger.log(Level.INFO, "Decoded msg: {0}", msg);

                        if(buffer.length() < index + msgLen) {
                            logger.log(Level.WARNING, "Stream only contains {0} chars. Expected {1}",
                                    new Object[]{buffer.length(), index + msgLen});

                        } else {
                            logger.log(Level.INFO, "Dispatching message string: {0}", buffer.substring(index, index + msgLen));
                            dispatcher.dispatchMessage(env, buffer.substring(index, index + msgLen).getBytes());
                        }

                        // Remove envelope and message from the buffer
                        buffer.delete(0, index + msgLen);
                    }
                }
            } finally {
                socket.close();
                logger.log(Level.INFO, "Closed connection");
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Socket io threw: ", e);
        } catch (MTPException e) {
            logger.log(Level.WARNING, "Envelope parser threw: ", e);
        }
    }
}
