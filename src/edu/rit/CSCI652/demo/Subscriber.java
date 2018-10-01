package edu.rit.CSCI652.demo;


import edu.rit.CSCI652.impl.PubSubAgent;

public interface Subscriber {
	/*
	 * subscribe to a topic
	 */
	public void subscribe(Topic topic, PubSubAgent pubSubAgent);

	/*
	 * subscribe to a topic with matching keywords
	 */
	public void subscribe(String keyword, PubSubAgent pubSubAgent);

	/*
	 * unsubscribe from a topic
	 */
	public void unsubscribe(Topic topic, PubSubAgent pubSubAgent);

	/*
	 * unsubscribe to all subscribed topics
	 */
	public void unsubscribe(PubSubAgent pubSubAgent);

	/*
	 * show the list of topics current subscribed to
	 */
	public void listSubscribedTopics(PubSubAgent pubSubAgent);

}
