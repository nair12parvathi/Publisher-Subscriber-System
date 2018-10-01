package edu.rit.CSCI652.demo;

import edu.rit.CSCI652.impl.PubSubAgent;


public interface Publisher {
	/*
	 * publish an event of a specific topic with title and content
	 */
	public void publish(Event event, PubSubAgent pubSubAgent);

	/*
	 * advertise new topic
	 */
	public void advertise(Topic newTopic, PubSubAgent pubSubAgent);
}
