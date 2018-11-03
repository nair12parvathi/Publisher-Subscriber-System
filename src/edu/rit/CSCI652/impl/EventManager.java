package edu.rit.CSCI652.impl;

import edu.rit.CSCI652.demo.Event;
import edu.rit.CSCI652.demo.Topic;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

public class EventManager {

    InetAddress serverAddress = null;
    String[] availablePorts;

    // Key = topicId , Value = List of Subscribers
    private HashMap<Integer, HashSet<String>> subscribersList = new HashMap<>();

    //key = subscriber ID,  value = events to be notified stored in a Queue
    private HashMap<String, Queue<Event>> messageQueue = new HashMap<>();

    private ArrayList<Topic> topicsList = new ArrayList<>();

    //key = topic, value = hashSet of keywords
    private HashMap<Topic, HashSet<String>> topicKeywordMap = new HashMap<>();

    private Thread[] threads;

    // key = node id, value = ON or OFF
    private HashMap<String, String> listOfNodeAvailabilityStatus = new HashMap<>();


    /**
     * Main method for Event Manager to start the service at all available ports and provide an option to view all the Subscribers
     */
    public static void main(String[] args) {
        EventManager eventManager = new EventManager();
        eventManager.loadProperties();

        eventManager.startServiceAtAllAvailablePorts();

        while (true) {
            System.out.println("Select an operation to be performed\n " +
                    "1. Display list of Subscribers based on Topic\n " +
                    "2. Display all Subscribers with availability status\n");

            Scanner scanner = new Scanner(System.in);

            int choice = Integer.parseInt(scanner.nextLine());

            switch (choice) {
                case 1:
                    if (eventManager.topicsList.isEmpty()) {
                        System.out.println("There are no topics in the topic list yet!");
                    }
                    else {
                        eventManager.listAllTopics();
                        System.out.println("Enter the Topic ID");
                        int topicID = Integer.parseInt(scanner.nextLine());
                        eventManager.showSubscribers(eventManager.topicsList.get(topicID - 1));
                    }
                    break;
                case 2:
                    if(eventManager.listOfNodeAvailabilityStatus.isEmpty()){
                        System.out.println("There are no subscribers to show");
                    }
                    else{
                        for (Map.Entry nodeID : eventManager.listOfNodeAvailabilityStatus.entrySet()) {
                            System.out.println(nodeID.getKey() + " : " + nodeID.getValue());
                        }
                    }
                    break;

                default:
                    System.out.println("Invalid choice. Please enter a choice again");
                    break;
            }
            try {
                Thread.currentThread().sleep(1000); // Sleep added to help switch between the threads
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    /*
    * Display the list of all topics advertised sofar
    * */
    private void listAllTopics() {
        if (!topicsList.isEmpty()) {
            System.out.println("****Here is the list of Topics****");
            for (int i = 0; i < topicsList.size(); i++) {
                System.out.println((i + 1) + ". " + topicsList.get(i).getTopicName());
            }
        }
    }

    /*
    * Accept Connection from PubSubAgents on available ports
    */
    private void startServiceAtAllAvailablePorts() {
        threads = new Thread[availablePorts.length];
        for (int i = 0; i < availablePorts.length; i++) {

            int finalI = i;
            threads[i] = new Thread() {
                public void run() {

                    String publisher = null;
                    ServerSocket serverPubSubAgent = null;
                    try {
                        System.out.println("\r\nRunning Server: " +
                                "Host=" + serverAddress +
                                "Port=" + availablePorts[finalI]);
                        serverPubSubAgent = new ServerSocket(Integer.parseInt(availablePorts[finalI]), 1, serverAddress);

                    } catch (Exception E) {
                        E.printStackTrace();
                        System.out.println("Server is down");
                    }
                    while (true) {
                        Socket pubSubAgent = null;
                        try {
                            pubSubAgent = serverPubSubAgent.accept();
                        } catch (Exception E) {
                            System.out.println("pubSubAgent not accepted");
                        }
                        String pubSubAgentAddress = pubSubAgent.getInetAddress().getHostAddress();
                        System.out.println("\r\nNew connection from " + pubSubAgentAddress + " " + pubSubAgent.getPort());
                        publisher = pubSubAgent.getInetAddress().getHostAddress() + ":" + pubSubAgent.getPort();
                        receiveObject(pubSubAgent, publisher);
                        closeConnection(pubSubAgent);
                    }
                }
            };
            threads[i].start();
        }
    }

    /*
    * Socket code to close connection
    * */
    private void closeConnection(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    * Receive a message from PubSubAgent and perform appropriate actions according to the message type
    * */
    private void receiveObject(Socket socket, String publisher) {
        Message message = null;
        try {
            ObjectInputStream pubSubAgentInputStream = new
                    ObjectInputStream(socket.getInputStream());
            message = (Message) pubSubAgentInputStream.readObject();

            switch (message.getMessageType()) {
                case Topic: // New Topic is created
                    addTopic(message.getTopic());
                    notifyPubSubOfNewTopic(message.getTopic(), publisher);
                    break;

                case Event: // New Event is created
                    notifySubscribers(message);
                    break;

                case ListTopic: //send list of all topics
                    Message msg = new Message(Message.MessageType.ListTopic, "");
                    msg.setTopicList(topicsList);
                    sendObject(msg, socket);
                    break;

                case SubscribeTopic: // Subscribe to a topic
                    updateSubscribersListUsingTopic(message);
                    break;

                case SubscribeKeyword: // Subscribe using a keyword
                    updateSubscribersListUsingKeyword(message, socket);
                    break;

                case UnsubscribeTopic: // Unsubscribe a topic
                    removeTopicSubscription(message, socket);
                    break;

                case UnsubscribeAll: // Unsubscribe from all topics previously subscribed
                    removeAllSubscriptions(message);
                    break;

                case Availability: // Update availability of a pubsubAgent
                    updateSubscriberStatus(message.getNodeID(), message.getAvailability());
                    break;

                default:
                    System.out.println("Invalid choice. Please enter a choice again");
                    break;
            }

        } catch (Exception E) {
            System.out.println("IO Exception thrown");
        }
    }

    /*
    * Delete all subscriptions
    * */
    private void removeAllSubscriptions(Message msg) {
        for (Map.Entry topics : subscribersList.entrySet()) {
            HashSet<String> subscribers = (HashSet<String>) topics.getValue();
            if (subscribers.contains(msg.getNodeID())) {
                subscribers.remove(msg.getNodeID());
            }
        }
    }

    /*
    * Delete subscriptions from a topic
    * */
    private void removeTopicSubscription(Message msg, Socket socket) {
        Topic topic = msg.getTopic();
        if (subscribersList.containsKey(topic.getTopicId())) {
            subscribersList.get(topic.getTopicId()).remove(msg.getNodeID());
        }
    }

    /*
    * Update subscribers list with subscribers subscribed using keyword
    * */
    private void updateSubscribersListUsingKeyword(Message msg, Socket socket) {
        boolean isPresent = false;
        String keyword = msg.getKeyword();
        for (Map.Entry entry : topicKeywordMap.entrySet()) {
            HashSet<String> keywordslist = (HashSet<String>) entry.getValue();
            if (keywordslist.contains(keyword)) {
                int topicID = ((Topic) entry.getKey()).getTopicId();
                if (subscribersList.containsKey(topicID)) {
                    subscribersList.get(topicID).add(msg.getNodeID());
                } else {
                    subscribersList.put(topicID, new HashSet<>());
                    subscribersList.get(topicID).add(msg.getNodeID());
                }
                isPresent = true;
                Message message = new Message(Message.MessageType.Topic, "");
                message.setTopic((Topic) entry.getKey());
                sendObject(message, socket);
                break;
            }
        }

        if (!isPresent) {
            System.out.println("Keyword is not present");
            sendObject(null, socket);
        }

    }

    /*
     * Update subscribers list with subscribers subscribed using topic
     * */
    private void updateSubscribersListUsingTopic(Message msg) {
        Topic topic = msg.getTopic();
        if (subscribersList.containsKey(topic.getTopicId())) {
            subscribersList.get(topic.getTopicId()).add(msg.getNodeID());
        } else {
            if (topicsList.get(topic.getTopicId()).getTopicName().equals(topic.getTopicName())) {
                subscribersList.put(topic.getTopicId(), new HashSet<>());
                subscribersList.get(topic.getTopicId()).add(msg.getNodeID());
            }
        }
    }

    /*
     * notify all subscribers of new event
     */
    private void notifySubscribers(Message message) {
        Event event = message.getEvent();
        int topicId = event.getTopicId();
        Message msg = new Message(Message.MessageType.Event, "");
        msg.setEvent(event);
        Socket socket = null;
        if (subscribersList.containsKey(topicId)) {
            HashSet<String> subscribers = subscribersList.get(topicId);
            for (String sub : subscribers) {
                if (listOfNodeAvailabilityStatus.containsKey(sub) &&
                        listOfNodeAvailabilityStatus.get(sub).equals("ON")) {
                    try {
                        String arr[] = sub.split(":");
                        socket = connectionHandler(arr[0], arr[1]);
                        sendObject(msg, socket);
                        closeConnection(socket);
                    } catch (Exception e) {
                        listOfNodeAvailabilityStatus.put(sub,"OFF");
                        if (messageQueue.containsKey(sub)) {
                            messageQueue.get(sub).add(event);
                        } else {
                            messageQueue.put(sub, new LinkedList<>());
                            messageQueue.get(sub).add(event);
                        }
                    }
                } else {
                    if (messageQueue.containsKey(sub)) {
                        messageQueue.get(sub).add(event);
                    } else {
                        messageQueue.put(sub, new LinkedList<>());
                        messageQueue.get(sub).add(event);
                    }
                }
            }
        }
    }

    /*
     * Add new topic on receipt of an advertisement
     */
    private void addTopic(Topic newTopic) {
        String newTopicName = newTopic.getTopicName();
        boolean isTopicPresent = false;

        for (Map.Entry entry : topicKeywordMap.entrySet()) {
            String topicName = ((Topic) entry.getKey()).getTopicName();
            if (topicName.equals(newTopicName)) {
                // Topic already present, add keywords in the HashSet
                HashSet<String> keywords = (HashSet<String>) entry.getValue();
                List<String> newTopicKeywords = newTopic.getTopicKeywords();
                for (int i = 0; i < newTopicKeywords.size(); i++) {
                    keywords.add(newTopicKeywords.get(i));
                }
                isTopicPresent = true;
                break;
            }
        }
        if (!isTopicPresent) {
            //New Topic is to be created
            int topicId = generateTopicID(newTopic); // create TopicId
            // set topic Id
            newTopic.setTopicId(topicId);
            // Add new topic to topicsList
            topicsList.add(newTopic);
            // Add the topic in topicKeywords Map
            List<String> topicKeywords = newTopic.getTopicKeywords();
            topicKeywordMap.put(newTopic, new HashSet<>());
            HashSet<String> keywords = topicKeywordMap.get(newTopic);
            for (int i = 0; i < topicKeywords.size(); i++) {
                keywords.add(topicKeywords.get(i));
            }
        }
    }

    /*
    * Send instances of Message
    * */
    private void sendObject(Message msg, Socket socket) {
        ObjectOutputStream clientOutputStream = null;
        try {
            clientOutputStream = new
                    ObjectOutputStream(socket.getOutputStream());
            clientOutputStream.writeObject(msg);
            clientOutputStream.flush();
        } catch (IOException e) {
            System.out.println("Failed to send object to PubSubAgent");
        }
    }

    /*
    * Notify PubSubAgent about new topic on receipt of advertisement
    * */
    private void notifyPubSubOfNewTopic(Topic topic, String publisher) {
        Iterator iter = listOfNodeAvailabilityStatus.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry pair = (Map.Entry) iter.next();
            String serverToNotify = (String) pair.getKey();
            try {
                if (!serverToNotify.equals(publisher) && pair.getValue().equals("ON")) {

                    String[] addr = serverToNotify.split(":");
                    Socket socket = connectionHandler(addr[0], addr[1]);

                    Message msg = new Message(Message.MessageType.Topic, "");
                    msg.setTopic(topic);
                    sendObject(msg, socket);
                    closeConnection(socket);
                }
            } catch (Exception e) {
                pair.setValue("OFF");
            }
        }
    }

    /*
    * Generate unique topic ID
    * */
    private int generateTopicID(Topic topic) {

        return topicsList.size();
    }

    /*
     * Add subscriber to the listOfNodeAvailabilityStatus with status "ON"
     */
    private void updateSubscriberStatus(String nodeID, String status) {
        listOfNodeAvailabilityStatus.put(nodeID, status);
        String[] addr = nodeID.split(":");
        Socket socket = null;
        if (status.equals("ON")) {
            try {
                if (messageQueue.containsKey(nodeID)) {

                    Queue<Event> cachedMessages = messageQueue.get(nodeID);
                    for (Event e : cachedMessages) {
                        socket = connectionHandler(addr[0], addr[1]);
                        Message msg = new Message(Message.MessageType.Event, "");
                        msg.setEvent(e);
                        sendObject(msg, socket);
                        closeConnection(socket);
                    }
                    cachedMessages.clear();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * Show the list of subscriber for a specified topic
     */
    private void showSubscribers(Topic topic) {
        if (subscribersList.isEmpty()) {
            return;
        }

        if (subscribersList.containsKey(topic.getTopicId())) {
            HashSet<String> subscribers = subscribersList.get(topic.getTopicId());
            System.out.println("List of Subscribers");
            int i = 1;
            for (String sub : subscribers) {
                System.out.println(i + ". " + sub);
                i++;
            }
        } else {
            System.out.println("No Subscribers");
        }
    }

    /*
    * Socket code to start connection
    * */
    public Socket connectionHandler(String ip, String portno) throws Exception {
        InetAddress inetAddress = InetAddress.getByName(ip);
        int port = Integer.parseInt(portno);
        Socket socket = new Socket(inetAddress, port);
        return socket;
    }

    /*
     * Reads and loads properties from the configuration file
     * */


    // to run in localhost
    /*public void loadProperties() {
        String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        String configPath = rootPath + "Conf.Properties";
        Properties confProps = new Properties();
        try {
            confProps.load(new FileInputStream(configPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            serverAddress = InetAddress.getByName(confProps.getProperty("event-manager-ip"));
            availablePorts = confProps.getProperty("ports-available").split(",");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }*/
    //to run in docker
    public void loadProperties() {
        Properties confProps = new Properties();
        try {
            confProps.load(EventManager.class.getResourceAsStream("Conf.Properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            serverAddress = InetAddress.getLocalHost();
            availablePorts = confProps.getProperty("ports-available").split(",");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}