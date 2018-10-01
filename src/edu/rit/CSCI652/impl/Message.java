package edu.rit.CSCI652.impl;

import edu.rit.CSCI652.demo.Event;
import edu.rit.CSCI652.demo.Topic;

import java.io.Serializable;
import java.util.ArrayList;

public class Message implements Serializable{
    public enum MessageType
    {
       Event, Topic, ListTopic, SubscribeTopic, UnsubscribeTopic, SubscribeKeyword, UnsubscribeAll, Availability
    }

    private MessageType messageType;
    private Topic topic=null;
    private Event event = null;
    private String nodeID;
    private String keyword;
    private ArrayList<Topic> TopicList;
    private String availability = "ON";

    public String getAvailability() {
        return this.availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public Message(MessageType messageType, String nodeID){
        this.messageType = messageType;
        this.nodeID = nodeID;
    }
    public String getNodeID() {
        return this.nodeID;
    }
    
    public void setKeyword(String keyword){
        this.keyword = keyword;
    }

    public String getKeyword(){
        return this.keyword;
    }
    public MessageType getMessageType() {
        return messageType;
    }


    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public void setTopicList(ArrayList<Topic> topicList){
        this.TopicList = topicList;
    }

    public ArrayList<Topic> getTopicList(){
        return this.TopicList;
    }
}

