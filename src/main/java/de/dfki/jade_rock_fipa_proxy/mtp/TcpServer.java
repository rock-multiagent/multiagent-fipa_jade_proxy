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
import jade.domain.FIPANames;
import jade.lang.acl.ACLCodec;
import jade.lang.acl.ACLCodec.CodecException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.ACLParser;
import jade.lang.acl.ParseException;
import jade.mtp.InChannel;
import jade.mtp.MTPException;
import jade.mtp.http.XMLCodec;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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
                acceptConnectionsAndForward();
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
    private void acceptConnectionsAndForward() {
        try {
            while (true) {
                Socket socket = serverSocket.accept();
                logger.log(Level.FINE, "Accepted a new connection");
                
                ACLMessage msg;
                Envelope env;
                String input;
                
                // XXX This can destroy bit-efficient encoding. One should read
                // into a byte[]
                try {
                    try {
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(socket.getInputStream()));
                        StringBuilder builder = new StringBuilder();
                        
                        String line;
                        while((line = reader.readLine()) != null) {
                            builder.append(line);
                        }
                        input = builder.toString();
                    } finally {
                        socket.close();
                        logger.log(Level.FINE, "Closed connection");
                    }
                    
                    logger.log(Level.INFO, "Got: {0}", input);
                    
                    // XXX this is not robust!
                    // Find end of the envelope ("</envelope>")
                    String splitter = "</envelope>";
                    String [] parts = input.split(splitter);
                    StringReader envReader = new StringReader(parts[0] + splitter);
                    StringReader msgReader = new StringReader(parts[1]);
                    
                    // Parse envelope
                    XMLCodec xmlCodec = new XMLCodec(XML_PARSER_CLASS);
                    env = xmlCodec.parse(envReader);
                    logger.log(Level.INFO, "Decoded env: {0}", env);

                    // Optional message parsing:
                    //ACLParser aclParser = new ACLParser(msgReader);
                    //msg = aclParser.parse(msgReader);
                    //logger.log(Level.INFO, "Decoded msg: {0}", msg);
                    
                    // Modify receivers
                    // FIXME triple intended receivers from Rock
                    ArrayList<AID> newRecvs = new ArrayList<AID>();
                    Iterator<AID> i = env.getAllIntendedReceiver();
                    while (i.hasNext()) {
                        AID aid = i.next();
                        newRecvs.add(new AID(aid.getName().replaceAll("-dot-", "."), true));
                    }
                    env.clearAllIntendedReceiver();
                    for (AID aid : newRecvs) {
                        env.addIntendedReceiver(aid);
                    }

                    // dispatch envelope
                    dispatcher.dispatchMessage(env, parts[1].getBytes());

                } catch (IOException e) {
                    logger.log(Level.WARNING, "Socket io threw: ", e);
                } catch (MTPException e) {
                    logger.log(Level.WARNING, "Envelope parser threw: ", e);
                } catch(ArrayIndexOutOfBoundsException e) {
                    logger.log(Level.WARNING, "No proper plitting of envelope and message possible. ");
                }
            }
        } catch (IOException e) {
            // accept threw an exception. Could also be due to takeDown
            logger.log(Level.SEVERE, "Socket accept threw (could be due to deactivate): ", e);
        }
    }

}
