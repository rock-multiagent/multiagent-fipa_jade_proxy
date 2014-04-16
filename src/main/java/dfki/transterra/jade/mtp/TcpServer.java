/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dfki.transterra.jade.mtp;

import dfki.transterra.jade.JMDNSManager;
import jade.core.Profile;
import jade.mtp.InChannel;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Satia Herfert
 */
public class TcpServer {
    
    public static final int DEFAULT_PORT = 6789;
    
    private InChannel.Dispatcher d;
    private Profile prfl;
    private TcpAddress address;

    public TcpServer(InChannel.Dispatcher d, Profile prfl) throws SocketException {
        this.d = d;
        this.prfl = prfl;
        
        this.address = new TcpAddress(JMDNSManager.getLocalIPv4Address(), DEFAULT_PORT);
    }

    public TcpServer(InChannel.Dispatcher d, Profile prfl, TcpAddress address) {
        this.d = d;
        this.prfl = prfl;
        this.address = address;
    }

    public TcpAddress getAddress() {
        return address;
    }
    
    public void deactivate() {
        
    }

}
