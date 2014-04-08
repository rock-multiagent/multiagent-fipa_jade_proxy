package dfki.transterra.jade;

import jade.core.AID;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.HashMap;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Hello world!
 *
 */
public class App {

    public static void main(String[] args) throws Exception {
        // Listen UDP port 1099
        //UDPProxy proxy = new UDPProxy(1099);
        //new Thread(proxy).start();

        //new JMDNSManager(InetAddress.getByName("134.102.7.57") , 1099).registerJadeAgent("hans");
        sendSocketString();
    }

    public static void sendSocketString() {
        try {
            Socket socket = new Socket("134.102.232.209", 6789);

            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            ACLMessage m = new ACLMessage(ACLMessage.REQUEST);
            m.addReceiver(new AID("hans", true));
            m.setContent("hallo");
            System.out.println("Sending: " + m.toString());
            writer.println(m.toString());

            socket.close();
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}
