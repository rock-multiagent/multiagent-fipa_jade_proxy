/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dfki.transterra.jade;

import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;

/**
 *
 * @author satia
 */
public class BlockingReceiveAgent extends Agent {

    protected void setup() {
        while (true) {
            System.out.println("Agent " + getLocalName() + ": waiting for message...");
            ACLMessage msg = blockingReceive();
            System.out.println("Agent " + getLocalName() + ": message received. Reply:");
            ACLMessage reply = msg.createReply();
            reply.addReceiver(new AID("da0", false));
            reply.addReceiver(new AID("da1", false));
            
            reply.setContent("recvd");
            System.out.println(reply.toString());
            send(reply);
            // send twice
            //reply.setContent("recvd2");
            //send(reply);
        }
    }

    protected void takeDown() {
        System.out.println("Agent " + getLocalName() + ": terminating");
    }

}
