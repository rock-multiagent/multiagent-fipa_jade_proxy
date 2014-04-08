package dfki.transterra.jade;

import com.barchart.udt.SocketUDT;
import com.barchart.udt.TypeUDT;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

public class UDPProxy implements Runnable {

    private int port;
    
    public static final String msg = "(INFORM\n"
                + ":receiver  (set ( agent-identifier :name ams@134.102.71.27:1099/JADE) )\n"
                + ":content  \"Hello\"\n"
                + ")";

    public UDPProxy(int port) {
        this.port = port;
    }

    public void run() {
        try {
            SocketUDT socket = new SocketUDT(TypeUDT.DATAGRAM);
            socket.bind(new InetSocketAddress("134.102.7.57"/*InetAddress.getLocalHost()*/, port));
            socket.listen(1);

            System.out.printf("Listening on udt:%s:%d%n", socket.getLocalInetAddress().getHostAddress(), port);
            System.out.println("Open:" + socket.isOpen());
            System.out.println("Connected:" + socket.isConnected());
            System.out.println("Non-Blocking:" + socket.isNonBlocking());
            
            sendSocketString("");

            while (true) {
                SocketUDT readS = socket.accept();
                System.out.println("Accepted");
                while (true) {
                    byte[] buffer = new byte[1000];
                    int read;
                    System.out.println("Connected:" + readS.isConnected());
                    while ((read = readS.receive(buffer)) > 0) {
                        System.out.println("Read:" + read);
                        String sentence = new String(buffer, 0,
                                buffer.length);
                        System.out.println("RECEIVED: " + sentence);

                        sendSocketString(sentence);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    /**
     *
     */
    public void sendUDT() throws Exception {
        try {
            SocketUDT socket = new SocketUDT(TypeUDT.DATAGRAM);
            socket.bind(new InetSocketAddress(InetAddress.getLocalHost(), port + 1));

            System.out.println("Sender binded");

            socket.connect(new InetSocketAddress("134.102.7.57", port));
            System.out.println("Sender conneced");
            String txt = "abc";
            int sent = socket.send(txt.getBytes());
            System.out.println("Sender sent:" + sent);

            Thread.sleep(500);
            socket.close();
            System.out.println("Sender closed");

        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public void sendSocketString(String str) {
        try {
            Socket socket = new Socket("134.102.7.57", 6789);
            
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            
            ACLMessage m = new ACLMessage(ACLMessage.REQUEST);
            m.addReceiver(new AID("da0", true));
            m.setContent("hallo");
            System.out.println("Sending: " + m.toString());
            writer.println(m.toString());
            
            socket.close();
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}
