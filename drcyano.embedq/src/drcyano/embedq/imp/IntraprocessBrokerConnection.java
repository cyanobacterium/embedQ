package drcyano.embedq.imp;

import drcyano.embedq.broker.Broker;
import drcyano.embedq.client.Subscriber;
import drcyano.embedq.connection.BrokerConnection;
import drcyano.embedq.data.Message;
import drcyano.embedq.data.Topic;

/**
 * An implementation of <code>BrokerConnection</code> for simple intra-process communication
 */
public class IntraprocessBrokerConnection extends BrokerConnection {
	private final Broker broker;
	
	/**
	 * Wraps a Broker
	 * @param host The Broker
	 */
	public IntraprocessBrokerConnection(Broker host){
		this.broker = host;
	}
	/**
	 * Subscribe a subscriber to a given topic.
	 * @param sub Message handler that wants to receive messages on the subscribed topic
	 * @param topic The topic to subscribe to
	 */
	@Override public void subscribe(Subscriber sub, Topic topic) {
		//Payload p = PayloadManager.encodeSubscribeEvent(sub, topic);
		broker.addSubscription(new IntraprocessSourceConnection(sub), topic);
	}
	/**
	 * This method publishes the given message to the Broker
	 * @param m a message to publish
	 */
	@Override public void publish(Message m){
		//Payload p = PayloadManager.encodePublishEvent(m, topic);
		broker.publishMessageReliable(m);
	}
	
	/**
	 * Unsubscribe a subscriber from a specific topic
	 * @param sub Message handler that no longer wants to receive messages on the subscribed topic
	 * @param topic The topic to unsubscribe from
	 */
	@Override
	public void unsubscribe(Subscriber sub, Topic topic) {
		broker.removeSubscription(new IntraprocessSourceConnection(sub), topic);
	
	}
	/**
	 * Unsubscribe from all topics
	 * @param sub Message handler that no longer wants to receive messages
	 */
	@Override
	public void unsubscribeAll(Subscriber sub) {
		broker.removeSubscriber(new IntraprocessSourceConnection(sub));
	}
}
