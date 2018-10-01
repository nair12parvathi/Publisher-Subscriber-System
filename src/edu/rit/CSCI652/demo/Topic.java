package edu.rit.CSCI652.demo;

import java.io.Serializable;
import java.util.List;

public class Topic implements Serializable{
	private int id;
	private List<String> keywords;
	private String name;

	public Topic(String name, List<String> keywords  ) {
		this.name = name;
		this.keywords = keywords;
	}

	/*
	* Sets the topic ID
	* */
	public void setTopicId(int id){
		this.id=id;
	}

	/*
	* Returns the topic ID
	* */
	public int getTopicId()	{
		return this.id;
	}

	/*
	* Returns the topic name
	* */
	public String getTopicName(){
		return this.name;
	}

	/*
	* Returns the keywords of a topic
	* */
	public List<String> getTopicKeywords(){
		return this.keywords;
	}
}
