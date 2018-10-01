package edu.rit.CSCI652.impl;


import edu.rit.CSCI652.demo.Event;
import edu.rit.CSCI652.demo.Publisher;
import edu.rit.CSCI652.demo.Topic;
import java.net.*;
import java.util.*;

public class Pub implements Publisher {
    private Scanner scanner;

    public Pub(){
        scanner = new Scanner(System.in);
    }

    /*
    * Publishes an event
    * */
    @Override
    public void publish(Event event, PubSubAgent pubSubAgent) {
        Socket socket = null;
        int [] availablePorts = pubSubAgent.getAvailablePorts();
        InetAddress myIP=pubSubAgent.serverAddress;
        while(socket == null){
            int listenerPort = pubSubAgent.getRandomAvailablePort(availablePorts);
            socket = pubSubAgent.startConnection(myIP, listenerPort);
        }
        Message msg = new Message(Message.MessageType.Event, null);
        msg.setEvent(event);
        pubSubAgent.sendObject(msg,socket);
        pubSubAgent.closeConnection(socket);
    }

    /*
    * Advertises a new topic
    * */
    @Override
    public void advertise(Topic newTopic, PubSubAgent pubSubAgent){
        int [] availablePorts = pubSubAgent.getAvailablePorts();
        InetAddress myIP=pubSubAgent.serverAddress;
        Socket socket = null;

        while(socket == null){
            int listenerPort = pubSubAgent.getRandomAvailablePort(availablePorts);
            socket = pubSubAgent.startConnection(myIP, listenerPort);
        }
        Message msg = new Message(Message.MessageType.Topic, null);
        msg.setTopic(newTopic);
        pubSubAgent.sendObject(msg,socket);
        pubSubAgent.closeConnection(socket);
    }

    /*
    * Collect topic details from user and pass details to the advertise()
    * */
    public void initAdvertiser(PubSubAgent pubSubAgent){
        System.out.println("Enter the topic name\n");
        String topicName = scanner.nextLine();
        System.out.println("Enter the keywords separated by space\n");
        String keywordsList = scanner.nextLine().toLowerCase();
        ArrayList<String> keywords = new ArrayList<String>(Arrays.asList(keywordsList.split(" ")));
        Topic topic = new Topic(topicName,keywords);
        advertise(topic, pubSubAgent);
    }

    /*
     * Collect event details from user and pass details to the publish()
     * */
    public void initPublisher(PubSubAgent pubSubAgent) {
        Topic topic = null;
        ArrayList<Topic> topicList = pubSubAgent.getTopicList();
        pubSubAgent.displayList(topicList, "Below is the list of topics");
        System.out.println("Enter the topic number under which you want to publish the event\n");
        int topicIndex = Integer.parseInt(scanner.nextLine());
        topic = topicList.get(topicIndex-1);
        System.out.println("Enter the event title\n");
        String eventTitle=scanner.nextLine();
        System.out.println("Enter the event content\n");
        String eventContent=scanner.nextLine();
        Event event = new Event(topic,eventTitle,eventContent);
        publish(event,pubSubAgent);
    }

}


