/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dfki.transterra.jade.mtp;

import jade.mtp.TransportAddress;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

/**
 *
 * @author Satia Herfert
 */
public class TcpAddress implements TransportAddress {
    
    /**
     * The Logger.
     */
    private static final Logger logger = Logger.getLogger(TcpAddress.class.getName());

    private InetAddress address;
    private int port;

    /**
     * Create a TCPAddress by an address string.
     * @param str "tcp:[IP]/[Port]"
     * @throws UnknownHostException if str is malformed or the Host is unknown.
     */
    public TcpAddress(String str) throws UnknownHostException {
        // TODO
        String[] parts = str.split("/");
        if (parts.length != 2) {
            throw new UnknownHostException("Malformed TCP address: " + str);
        }

        address = InetAddress.getByName(parts[0].substring("tcp:".length()));
        port = Integer.parseInt(parts[1]);
    }
    
    /**
     * Create a TCPAddress by IP address and port.
     * @param address the IP address.
     * @param port the port. 
     */
    public TcpAddress(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    public String getProto() {
        return "tcp";
    }

    public String getHost() {
        return address.getHostAddress();
    }

    public String getPort() {
        return "" + port;
    }

    public String getFile() {
        throw new UnsupportedOperationException("Not supported.");
    }

    public String getAnchor() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public String toString() {
        return "tcp:/" + address + ":" + port;
    }

    
}
