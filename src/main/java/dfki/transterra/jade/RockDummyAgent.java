/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dfki.transterra.jade;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
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
     * TODO also change sender, so that responses go through the SocketProxyAgent?
     */
    private class RockForwardBehaviour extends CyclicBehaviour {
        
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null) {
                // TODO process message: Send to ROCK proxy MTS via socket
                System.out.println("We got a msg: " + msg);
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
