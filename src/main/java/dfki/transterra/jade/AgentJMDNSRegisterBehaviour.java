/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dfki.transterra.jade;

import jade.domain.introspection.AMSSubscriber;
import jade.domain.introspection.BornAgent;
import jade.domain.introspection.DeadAgent;
import jade.domain.introspection.Event;
import jade.domain.introspection.IntrospectionVocabulary;
import java.util.Map;

/**
 * Every time an agent gets "born" it is registered via JMDNS, and when it
 * "dies" it is unregistered.
 *
 * @author Satia Herfert
 */
public class AgentJMDNSRegisterBehaviour extends AMSSubscriber {
    
    JMDNSManager jmdnsManager;

    public AgentJMDNSRegisterBehaviour(JMDNSManager jmdnsManager) {
        this.jmdnsManager = jmdnsManager;
    }

    protected void installHandlers(Map handlers) {

        AMSSubscriber.EventHandler addAgentEH = new AMSSubscriber.EventHandler() {
            public void handle(Event event) {
                BornAgent ba = (BornAgent) event;
                // We must only register JADE agents, not RockDummyAgents
                if (!ba.getClassName().equals(RockDummyAgent.class.getName())) {
                    jmdnsManager.registerJadeAgent(ba.getAgent().getLocalName());
                }
            }
        };

        AMSSubscriber.EventHandler removeAgentEH = new AMSSubscriber.EventHandler() {
            public void handle(Event event) {
                DeadAgent da = (DeadAgent) event;
                // We unregister in any case:
                // It was a RockDummyAgent: the JMDNS entry will already be deleted
                // It was a JADE Agent: we delete the entry
                jmdnsManager.unregisterJadeAgent(da.getAgent().getLocalName());
            }
        };

        handlers.put(IntrospectionVocabulary.BORNAGENT, addAgentEH);
        handlers.put(IntrospectionVocabulary.DEADAGENT, removeAgentEH);
//          handlers.put(IntrospectionVocabulary.SUSPENDEDAGENT, jpa);
//          handlers.put(IntrospectionVocabulary.RESUMEDAGENT, jpa);
//          handlers.put(IntrospectionVocabulary.FROZENAGENT, jpa);
//          handlers.put(IntrospectionVocabulary.THAWEDAGENT, jpa);
//          handlers.put(IntrospectionVocabulary.MOVEDAGENT, jpa);
    }
}
