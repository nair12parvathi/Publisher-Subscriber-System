package edu.rit.CSCI652.impl;

import edu.rit.CSCI652.demo.Subscriber;
import edu.rit.CSCI652.demo.Topic;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Sub implements Subscriber{
    ArrayList<Topic> subscriptionList = new ArrayList<>();

    private Scanner scanner;

    public Sub() {
        scanner = new Scanner(System.in);
    }

    public void initSubscriber(PubSubAgent pubSubAgent){
        boolean isActive = true;

        while (isActive) {
            System.out.println("What do you want to do?\n"
                    + "Enter\n"
                    + "1 to subscribe from a topic \n"
                    + "2 to subscribe using keywords\n"
                    + "3 to unsubscribe from a topic\n"
                    + "4 to delete all subscriptions\n"
                    + "5 to show your subscriptions\n"
                    + "6 to Go Back to Main Menu\n ");

            int choice = Integer.parseInt(scanner.nextLine());
            int topicIndex ;
            Topic topic;

            switch (choice) {
                case 1:
                    ArrayList<Topic> topicList = pubSubAgent.getTopicList();
                    pubSubAgent.displayList(topicList, "Below is the list of Topics");
                    System.out.println("Enter the index of topic you want to subscribe \n");
                    topicIndex = Integer.parseInt(scanner.nextLine());
                    topic = topicList.get(topicIndex - 1);
                    subscribe(topic, pubSubAgent);
                    break;

                case 2:
                    System.out.println("Enter keyword");
                    String keyword = scanner.nextLine();
                    subscribe(keyword, pubSubAgent);
                    break;

                case 3:
                    listSubscribedTopics(pubSubAgent);
                    System.out.println("Enter the index of the topic you want to unsubscribe from");
                    topicIndex = Integer.parseInt(scanner.nextLine())-1;
                    topic = subscriptionList.get(topicIndex);
                    unsubscribe(topic, pubSubAgent);
                    break;

                case 4:
                    unsubscribe(pubSubAgent);
                    break;

                case 5:
                    listSubscribedTopics(pubSubAgent);
                    break;

                case 6:
                    isActive = false;
                    break;

                default:
                    System.out.println("Invalid choice. Please enter a choice again");
                    break;
            }
        }
    }

    /*
    * Subscribe to a topic
    * */
    @Override
    public void subscribe(Topic topic, PubSubAgent pubSubAgent) {
        Socket socket = null;

        int [] availablePorts = pubSubAgent.getAvailablePorts();
        InetAddress serverAddress=pubSubAgent.serverAddress;

        while(socket == null){
            int listenerPort = pubSubAgent.getRandomAvailablePort(availablePorts);
            socket = pubSubAgent.startConnection(serverAddress, listenerPort);
        }
        String nodeID = pubSubAgent.myIP.getHostAddress()+":"+pubSubAgent.notificationListenerPort;
        Message msg = new Message(Message.MessageType.SubscribeTopic, nodeID);
        msg.setTopic(topic);
        pubSubAgent.sendObject(msg,socket);
        addToSubscriptionList(topic);
        pubSubAgent.closeConnection(socket);
    }

    /*
    * add new topic to the subscription list
    * */
    private void addToSubscriptionList(Topic topic) {
        subscriptionList.add(topic);
    }

    /*
    * Subscribe to a topic using keyword
    * */
    @Override
    public void subscribe(String keyword, PubSubAgent pubSubAgent) {
        Socket socket = null;

        int [] availablePorts = pubSubAgent.getAvailablePorts();
        InetAddress serverAddress=pubSubAgent.serverAddress;

        while(socket == null){
            int listenerPort = pubSubAgent.getRandomAvailablePort(availablePorts);
            socket = pubSubAgent.startConnection(serverAddress, listenerPort);
        }

        String nodeID = pubSubAgent.myIP.getHostAddress()+":"+pubSubAgent.notificationListenerPort;
        Message msg = new Message(Message.MessageType.SubscribeKeyword, nodeID);
        msg.setKeyword(keyword);
        pubSubAgent.sendObject(msg,socket);

        Message message =pubSubAgent.receiveObject(socket);//Event Manager sends back the topic in case of keyword
        if (message!=null) {
            addToSubscriptionList(message.getTopic());
        }
        else
        {
            System.out.println("Keyword not present in any topic");
        }
        pubSubAgent.closeConnection(socket);
    }

    /*
    * Unsubscribe to a topic
    * */
    @Override
    public void unsubscribe(Topic topic, PubSubAgent pubSubAgent) {
        Socket socket = null;

        int [] availablePorts = pubSubAgent.getAvailablePorts();
        InetAddress serverAddress=pubSubAgent.serverAddress;

        while(socket == null){
            int listenerPort = pubSubAgent.getRandomAvailablePort(availablePorts);
            socket = pubSubAgent.startConnection(serverAddress, listenerPort);
        }

        String nodeID = pubSubAgent.myIP.getHostAddress()+":"+pubSubAgent.notificationListenerPort;
        Message msg = new Message(Message.MessageType.UnsubscribeTopic, nodeID);
        msg.setTopic(topic);
        pubSubAgent.sendObject(msg,socket);
        updateSubscriptionList(topic);
        pubSubAgent.closeConnection(socket);
    }

    /*
    * Update subscription list
    * */
    private void updateSubscriptionList(Topic topic) {
        subscriptionList.remove(topic);
    }

    /*
    * Unsubscribe from all subscriptions
    * */
    @Override
    public void unsubscribe(PubSubAgent pubSubAgent) {
        Socket socket = null;

        int [] availablePorts = pubSubAgent.getAvailablePorts();
        InetAddress serverAddress=pubSubAgent.serverAddress;

        while(socket == null){
            int listenerPort = pubSubAgent.getRandomAvailablePort(availablePorts);
            socket = pubSubAgent.startConnection(serverAddress, listenerPort);
        }

        String nodeID = pubSubAgent.myIP.getHostAddress()+":"+pubSubAgent.notificationListenerPort;
        Message msg = new Message(Message.MessageType.UnsubscribeAll, nodeID);
        pubSubAgent.sendObject(msg,socket);
        subscriptionList.clear();
        pubSubAgent.closeConnection(socket);
    }

    /*
    * List all the subscriptions
    * */
    @Override
    public void listSubscribedTopics(PubSubAgent pubSubAgent) {
        if(subscriptionList.isEmpty()){
            System.out.println("There are no subscriptions to show");
            return;
        }

        pubSubAgent.displayList(subscriptionList, "Below is the list of your subscriptions");
    }

}
