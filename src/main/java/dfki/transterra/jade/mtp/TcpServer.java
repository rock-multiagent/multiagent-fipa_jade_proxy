/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dfki.transterra.jade.mtp;

import cascom.fipa.acl.ACLDecoder;
import cascom.fipa.acl.ACLEncoder;
import cascom.fipa.acl.BEParseException;
import cascom.fipa.acl.BitEffACLCodec;
import cascom.fipa.envelope.BitEfficientEnvelopeCodec;
import cascom.fipa.envelope.EnvelopeDecoder;
import cascom.fipa.envelope.EnvelopeEncoder;
import dfki.transterra.jade.JMDNSManager;
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
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
     * 
     * TODO Workarounds:
     * 
     * msg + env with msg sent => msg sent twice
     * 
     * payload length not set by rock 
     */
    private void acceptConnectionsAndForward() {
        try {
            while (true) {
                Socket socket = serverSocket.accept();
                logger.log(Level.FINE, "Accepted a new connection");
                ACLMessage msg;
                try {
                    byte[] dataIn;
                    try {
                        // Convert the input to a byte array.
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        int nRead;
                        byte[] data = new byte[16384];
                        while ((nRead = socket.getInputStream().read(data, 0, data.length)) != -1) {
                            buffer.write(data, 0, nRead);
                        }
                        buffer.flush();
                        dataIn = buffer.toByteArray();
                    } finally {
                        socket.close();
                        logger.log(Level.FINE, "Closed connection");
                    }

                    // Parse envelope
                    EnvelopeDecoder decoder = new EnvelopeDecoder();
                    EnvelopeEncoder encoder = new EnvelopeEncoder();
                    Envelope env = decoder.getEnvelope(dataIn);

                    logger.log(Level.INFO, "Decoded env: {0}", env);

                    // Modify envelope
                    env.setAclRepresentation(FIPANames.ACLCodec.STRING);
                    int envByteLen = encoder.encode(env).length();
//                    byte[] msgData = msg.toString().getBytes();
//                    env.setPayloadLength((long)msgData.length);
                    
                    env.setPayloadLength((long) dataIn.length - 341);
                    System.err.println("len: " + envByteLen);
                    // FIXME no works!
                    byte[] msgData = Arrays.copyOfRange(dataIn, 341, dataIn.length);
                    // Modify 2nd byte (version number 1.0)
                    msgData[1] = 0x10;

                    File outFile = new File("/home/satia/rockjade/rock");
                    OutputStream outStream = new FileOutputStream(outFile);
                    outStream.write(msgData);
                    outStream.close();

                    // Re-encode message as in String encoding
                    BitEffACLCodec mCodBE = new BitEffACLCodec();
                    msg = mCodBE.decode(msgData, "ASCII");
                    logger.log(Level.INFO, "Decoded msg: {0}", msg);
                    
                    // FIXME
                    env.addIntendedReceiver(new AID("da0", false));
                    msg.addReceiver(new AID("da0", false));
                    //
                    
                    msgData = msg.toString().getBytes();

                    // dispatch envelope
                    dispatcher.dispatchMessage(env, msgData);

                } catch (IOException e) {
                    logger.log(Level.WARNING, "Socket io threw: ", e);
                } catch (BEParseException e) {
                    logger.log(Level.WARNING, "Socket parsing threw: ", e);
                } catch (CodecException ex) {
                    Logger.getLogger(TcpServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (IOException e) {
            // accept threw an exception. Could also be due to takeDown
            logger.log(Level.SEVERE, "Socket accept threw (could be due to deactivate): ", e);
        }
    }

}
