/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dfki.transterra.jade.mtp;

import cascom.fipa.envelope.BitEfficientEnvelopeCodec;
import dfki.transterra.jade.JMDNSManager;
import jade.core.Profile;
import jade.domain.FIPAAgentManagement.Envelope;
import jade.mtp.InChannel;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A TcpServer that listens for incoming envelopes and forwards them accordingly.
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
                try { // TODO correct exception catches
                    Envelope env;
                    try {
                        BufferedReader br = new BufferedReader(new InputStreamReader( socket.getInputStream()));
                        String str;
                        StringBuilder b = new StringBuilder();
                        while((str = br.readLine()) != null) {
                            b.append(str);
                        }
                        logger.log(Level.INFO, "Received on socket: {0}", b.toString());
                        
                        // Parse envelope
                        BitEfficientEnvelopeCodec codec = new BitEfficientEnvelopeCodec();
                        env = codec.decode(b.toString().getBytes());
                        
                        logger.log(Level.INFO, "Decoded env: {0}", env.toString());
                        
                    } finally {
                        socket.close();
                        logger.log(Level.FINE, "Closed connection");
                    }
                    // dispatch envelope
                    

                } catch (Exception e) {
                    logger.log(Level.WARNING, "Socket parsing threw: ", e);
                }
            }
        } catch (IOException e) {
            // accept threw an exception. Could also be due to takeDown
            logger.log(Level.SEVERE, "Socket accept threw (could be due to deactivate): ", e);
        }
    }

}
