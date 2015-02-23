package multiagent.fipa_services.example;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is an example of how to communicate between Jade and, e.g. Rock.
 * In Jade, own agents need to extend the Agent class.
 *
 * @author Satia Herfert
 */
public class EchoAgent extends Agent {

    /**
     * Agent's behaviours are usually implemented in own classes, extending
     * Behaviour. CyclicBehaviour is active as long as the agent is active,
     * which is appropriate for an Echo agents.
     *
     * Other behaviours do their work and become inactive afterwards.
     */
    private class EchoBehaviour extends CyclicBehaviour {

        /**
         * When extending CyclicBehaviour, the action() method needs to be
         * implemented.
         *
         * {@inheritDoc}
         */
        @Override
        public void action() {
            /**
             * The protected field Behaviour.myAgent refers to the
             * agent which this behaviour is attached to. The receive
             * message, tries to obtain the next method from the incoming
             * message's queue. This method is non-blocking and returns
             * null, if there is no new message.
             */
            ACLMessage msg = myAgent.receive();
            if (msg != null) {
                /**
                 * We create a reply message. createReply() automatically
                 * sets sender, receiver, conversation-id, etc. accordingly.
                 */
                ACLMessage response = msg.createReply();

                StringBuilder b = new StringBuilder();
                b.append("Reply to ")
                        .append(msg.getSender().getName())
                        .append(" by ")
                        .append(new Date());
                /**
                 * We set the content.
                 */
                response.setContent(b.toString());

                /**
                 * Finally, the message is dispatched using the send() method.
                 */
                send(response);
            } else {
                /**
                 * If there was no new message, we block(), which waits until
                 * a message arrives. This happens in the very beginning and
                 * can also happen if other behaviours 'ate' the new message
                 * using receiver, before we had the chance.
                 */
                block();
            }
        }
    }

    /**
     * This method can be used to initiate a conversation with a foreign agent,
     * instead of replying.
     * @param receiver the receiver's global name, e.g. "rock_agent".
     */
    private void initiateConversation(String receiver) {

        /**
         * Here, a new message is created using the constructor and not using
         * the createReply method. The chosen performative is INFORM here.
         */
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);

        /**
         * When sending to foreign agents, we must know their addresses.
         * Therefore, we ask the AMS (using AMSService.search()).
         */
        AID aid = new AID(receiver, true);
        AMSAgentDescription ad = new AMSAgentDescription();
        ad.setName(aid);
        try {
            AMSAgentDescription[] res = AMSService.search(this, ad);
            if (res.length > 0) {
                aid = res[0].getName();
            } else {
                throw new FIPAException("Foreign agent not found.");
            }
        } catch (FIPAException ex) {
            /**
             * When the foreign agent could not be found, we cannot send the
             * message and abort.
             */
            Logger.getLogger(EchoAgent.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        /**
         * When a valid AID has been created, we use that as a receiver,
         * set the conversation-id and content, and send the message.
         */
        msg.addReceiver(aid);
        msg.setConversationId("echoAgent0");

        StringBuilder b = new StringBuilder();
        b.append("Message to ")
                .append(receiver)
                .append(" by ")
                .append(new Date());

        msg.setContent(b.toString());

        send(msg);
    }

    /**
     * This method must be overwritten and does all initializations. In our
     * case this simply means to add the EchoBehaviour. If your class contains
     * fields, they usually would be initialized here, not in the constructor.
     * {@inheritDoc}
     */
    @Override
    protected void setup() {
        this.addBehaviour(new EchoBehaviour());
    }

    /**
     * This method must be overwritten and does all cleanup. In our case,
     * there is nothing to do.
     * {@inheritDoc}
     */
    @Override
    protected void takeDown() {
    }
}
