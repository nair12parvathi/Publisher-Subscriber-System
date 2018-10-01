package edu.rit.CSCI652.demo;

import java.io.Serializable;

public class Event implements Serializable{
	private int id;
	private Topic topic;
	private String title;
	private String content;

	public Event(Topic topic, String title, String content){
		this.topic = topic;
		this.title = title;
		this.content = content;
	}

	/*
	* Returns the topic ID
	* */
	public int getTopicId() {
		return this.topic.getTopicId();
	}

	/*
	* Returns the topic
	* */
	public Topic getTopic() {
		return this.topic;
	}

	/*
	* Returns the title of the event
	* */
	public String getTitle(){
		return this.title;
	}

	/*
	* Returns the content of the event
	* */
	public String getContent(){
		return this.content;
	}
}
