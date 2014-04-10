/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dfki.transterra.jade;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * For each ROCK agent, one RockDummyAgent is created, that forwards incoming
 * messages to him via the Rock proxy MTS (sockets).
 *
 * @author Satia Herfert
 */
public class RockDummyAgent extends Agent {

    /**
     * Forwards all received messages to the ROCK proxy MTS via sockets.
     *
     * XXX also change sender to just local?
     */
    private class RockForwardBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null) {
                // Only forward, if it's not a ROCK message
                if (!JadeProxyAgent.ROCK_MSG_USER_DEF_PARAM_VALUE.equals(
                        msg.getUserDefinedParameter(
                                JadeProxyAgent.ROCK_MSG_USER_DEF_PARAM_KEY))) {

                    // Change the sender & receiver names to local names
                    AID oldSender = msg.getSender();

                    if (oldSender != null) {
                        msg.setSender(new AID(oldSender.getLocalName(), true));
                    }

                    ArrayList<AID> newRecvs = new ArrayList<AID>();
                    Iterator<AID> i = msg.getAllReceiver();
                    while (i.hasNext()) {
                        AID aid = i.next();
                        newRecvs.add(new AID(aid.getLocalName(), true));
                    }
                    msg.clearAllReceiver();
                    for (AID aid : newRecvs) {
                        msg.addReceiver(aid);
                    }

                    logger.log(Level.INFO, "Forwarding msg to Rock: {0}", msg);

                    try {
                        Socket socket = new Socket("127.0.0.1", 7890); // TODO Port
                        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                        writer.println(msg.toString());
                        // Include EOF
                        writer.print(-1);
                        socket.close();
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Fowarding message to Rock failed: ", e);
                        System.err.println(e);
                    }
                }
            } else {
                block();
            }
        }
    }

    /**
     * The Logger.
     */
    private static final Logger logger = Logger.getLogger(RockDummyAgent.class.getName());

    @Override
    protected void setup() {
        logger.log(Level.INFO, "RockDummyAgent {0}: starting", getLocalName());
        this.addBehaviour(new RockForwardBehaviour());
    }

    @Override
    protected void takeDown() {
        logger.log(Level.INFO, "RockDummyAgent {0}: terminating", getLocalName());
    }
}
