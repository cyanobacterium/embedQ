package drcyano.embedq.util;

import drcyano.embedq.client.Publisher;
import drcyano.embedq.client.Subscriber;
import drcyano.embedq.connection.BrokerConnection;
import drcyano.embedq.data.Message;
import drcyano.embedq.data.Topic;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * A <code>Worker</code> automatically runs the implementation of {@link Worker#doWork(Message, Publisher)}
 * asynchronously whenever a relevant message is published to the relevant <code>Topic</code>.
 */
public abstract class Worker implements Subscriber {
	
	private final ExecutorService scheduler;
	private final Publisher publisher;
	private final Consumer<Exception> exceptionHandler;
	private final Topic listenTopic;
	
	/**
	 * This constructor automaticall subscribes this instance to the provided <code>BrokerConnection</code>.
	 * @param listenTopic Subscribe topic
	 * @param brokerConnection Connection to the {@link drcyano.embedq.broker.Broker}
	 * @param threadPool An <code>ExecutorService</code> to serve as a thread pool for concurrent execution.
	 * @param exceptionHandler A function to handling exceptions generated by
	 * {@link Worker#doWork(Message, Publisher)}. Can be <code>null</code> if you wish to simply
	 *                         ignore errors or handle them by alternative means (e.g. publishing
	 *                         a message on an error topic).
	 */
	public Worker(Topic listenTopic, BrokerConnection brokerConnection, ExecutorService threadPool, Consumer<Exception> exceptionHandler) {
		this.scheduler = threadPool;
		this.publisher = new PublisherWrapper(brokerConnection);
		this.exceptionHandler = exceptionHandler;
		this.listenTopic = listenTopic;
		
		//
		brokerConnection.subscribe(this, listenTopic);
	}
	
	/**
	 * Returns a String useful for debugging
	 * @return A String descrribing some features of this Worker
	 */
	@Override public String toString(){
		return String.format("%s#%s << %s ", getClass().getName(), hashCode(), listenTopic.toString());
	}
	
	/**
	 * Override this method to implement the worker task.
	 * <p>This method will be invoked on a worker thread (via the provided <code>ExecutorService</code>
	 * whenever a message is published to the subscribed topic. A <code>Publisher</code> instance
	 * wrapping the <code>BrokerConnection</code> allows you to publish one or more messages to
	 * communicate the results of this method.</p>
	 * @param input The <code>Message</code> to process
	 * @param publisher A <code>Publisher</code> instance so that you can publish new messages to the
	 *                  Broker
	 * @throws Exception Any Exceptions thrown will be passed to the exception handler provided in
	 * the constructor (or silently ignored if no exception handler is provided)
	 */
	protected abstract void doWork(Message input, Publisher publisher) throws Exception;
	
	private void workHandler(Message m) {
		try {
			doWork(m, publisher);
		} catch (Exception e) {
			if (exceptionHandler != null) exceptionHandler.accept(e);
		}
	}
	
	/**
	 * This method submits the {@link #doWork(Message, Publisher)} method as a <code>Runnable</code>
	 * to the <code>ExecutorService</code>
	 * @param m The message received
	 */
	@Override
	public final void receiveMessage(Message m) {
		scheduler.submit(() -> workHandler(m));
	}
	
	private class PublisherWrapper implements Publisher {
		private final Publisher target;
		
		public PublisherWrapper(Publisher target) {
			this.target = target;
		}
		
		
		@Override
		public void publish(Message m) {
			target.publish(m);
		}
		
	}
}
