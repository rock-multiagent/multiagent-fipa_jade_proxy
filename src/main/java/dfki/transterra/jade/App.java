package dfki.transterra.jade;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.io.PrintWriter;
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
            Socket socket = new Socket("127.0.0.1", 6789);

            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            ACLMessage m = new ACLMessage(ACLMessage.REQUEST);
            m.addReceiver(new AID("da0", true));
            m.addReceiver(new AID("da1", true));
            m.setContent("hallo");
            System.out.println("Sending: " + m.toString());
            writer.println(m.toString());

            socket.close();
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}
