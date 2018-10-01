package edu.rit.CSCI652.impl;

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

public class PubSubAgent {
    Thread notificationListener;
    Pub pub;
    Sub sub;
    InetAddress serverAddress = null;
    InetAddress myIP = null;
    int notificationListenerPort = 0;
    String availablePorts;
    private Scanner scanner;

    /*
    * Constructor to initialize the PubSubAgent
    *
    */
    public PubSubAgent() {
        this.scanner = new Scanner(System.in);
    }

    /*
    * Main method to turn on a notification listener in the background and provide list of options to user
    *
    */
    public static void main(String[] args) throws UnknownHostException {

        PubSubAgent pubSubAgent = new PubSubAgent();
        pubSubAgent.pub = new Pub();
        pubSubAgent.sub = new Sub();
        Scanner sc = pubSubAgent.scanner;

        //get and assign the system's IP
        try {
            pubSubAgent.myIP = InetAddress.getLocalHost();
            // pubSubAgent.myIP = InetAddress.getByName("localhost"); //for localhost
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        pubSubAgent.serverAddress = InetAddress.getByName(args[0]); // if not localhost
        pubSubAgent.notificationListenerPort = Integer.parseInt(args[1]); // if not localhost
        pubSubAgent.loadProperties();
        pubSubAgent.turnOnNotificationListener();
        pubSubAgent.communicateListenerPortToEM();

        boolean isActive = true;

        while (isActive) {
            System.out.println("\n\nSelect an Option from below choices\n "
                    + "1. Advertise mode\n "
                    + "2. Publish mode\n "
                    + "3. Subscribe mode\n "
                    + "4. Go offline\n"
                    + "5. Exit\n");

            int choice = Integer.parseInt(sc.nextLine());
            switch (choice) {
                case 1:
                    pubSubAgent.pub.initAdvertiser(pubSubAgent);
                    break;
                case 2:
                    pubSubAgent.pub.initPublisher(pubSubAgent);
                    break;
                case 3:
                    pubSubAgent.sub.initSubscriber(pubSubAgent);
                    break;
                case 4:
                    pubSubAgent.goOffline();
                    break;
                case 5:
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a choice again");
                    break;
            }
        }
    }

    /*
    * Returns a pool of available ports at the Event Manager in an array
    * */
    public int[] getAvailablePorts() {
        return (Arrays.stream(availablePorts.split(",")).mapToInt(Integer::parseInt).toArray());
    }

    /*
    * Allows user to go offline and receive no notifications until it goes back online
    * */
    public void goOffline() {
        Socket socket = null;
        int[] availablePorts = getAvailablePorts();

        while (socket == null) {
            int listenerPort = getRandomAvailablePort(availablePorts);
            socket = startConnection(serverAddress, listenerPort);
        }
        String nodeID = myIP.getHostAddress() + ":" + notificationListenerPort;
        Message msg = new Message(Message.MessageType.Availability, nodeID);
        msg.setAvailability("OFF");
        sendObject(msg, socket);
        closeConnection(socket);
        while (true) {
            System.out.println("########################");
            System.out.println("# Your status: OFFLINE #");
            System.out.println("#   Go Online ? (Y/N)  #");
            System.out.println("########################");
            String input = scanner.nextLine();
            if (input.equals("Y") || input.equals("y")) {
                goOnline();
                break;
            }
        }

    }

    /*
    * Allows user to go back online from offline status
    * */
    public void goOnline() {
        System.out.println("########################");
        System.out.println("# Your status: ONLINE  #");
        System.out.println("########################");
        Socket socket = null;
        int[] availablePorts = getAvailablePorts();

        while (socket == null) {
            int listenerPort = getRandomAvailablePort(availablePorts);
            socket = startConnection(serverAddress, listenerPort);
        }
        String nodeID = myIP.getHostAddress() + ":" + notificationListenerPort;
        Message msg = new Message(Message.MessageType.Availability, nodeID);
        msg.setAvailability("ON");
        sendObject(msg, socket);
        closeConnection(socket);
    }

    /*
    * Create a notification listener thread
    * */
    public void turnOnNotificationListener() {
        notificationListener = new Thread() {
            public void run() {
                ServerSocket notificationListenSocket = null;
                try {
                    System.out.println("\r\nRunning Notification Listener: " +
                            "Host=" + myIP +
                            "Port=" + notificationListenerPort);
                    notificationListenSocket = new ServerSocket(notificationListenerPort, 1, myIP);

                } catch (Exception E) {
                    System.out.println("Notification listener is down");
                }

                while (true) {
                    Socket eventManager = null;
                    try {
                        eventManager = notificationListenSocket.accept();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    receiveMessage(eventManager);
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        notificationListener.start();
    }

    /*
    * Communicates the notification listener port number to the Event Manager and also notifies the availability
    * status of the node (ON)
    * */
    public void communicateListenerPortToEM() {
        boolean isConnected = false;
        int[] availablePorts = getAvailablePorts();
        Socket socket = null;
        while (!isConnected) {
            int listenerPort = getRandomAvailablePort(availablePorts);
            try {
                socket = new Socket(serverAddress, listenerPort);
                    if (socket != null) {
                    isConnected = true;
                }
            } catch (IOException e) {
            }
        }

        String nodeID = myIP.getHostAddress() + ":" + notificationListenerPort;
        Message msg = new Message(Message.MessageType.Availability, nodeID);
        msg.setAvailability("ON");
        sendObject(msg, socket);
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /*
    * Receives notifications from Event Manager and displays the contents of the notification
    * */
    public void receiveMessage(Socket socket) {
        Message msg = receiveObject(socket);
        if (msg.getMessageType() == Message.MessageType.Topic) {

            System.out.println("**********************NEW NOTIFICATION**********************\n\n"
                    + "Topic Name: " + msg.getTopic().getTopicName()
                    + "\nTopic Keywords: " + msg.getTopic().getTopicKeywords()
                    + "\n\n************************************************************");
        }
        if (msg.getMessageType() == Message.MessageType.Event) {

            System.out.println("**********************NEW NOTIFICATION**********************\n\n"
                    + "Topic Name:" + msg.getEvent().getTopic().getTopicName()
                    + "\nEvent Title: " + msg.getEvent().getTitle()
                    + "\nEvent Content: " + msg.getEvent().getContent()
                    + "\n\n************************************************************");
        }
    }

    /*
    * Receive messages from Event Manager
    * */
    public Message receiveObject(Socket socket) {
        Message msg = null;
        try {
            ObjectInputStream eventManagerInputStream = new
                    ObjectInputStream(socket.getInputStream());
            msg = (Message) eventManagerInputStream.readObject();

        } catch (Exception E) {
            E.printStackTrace();
            System.out.println("IO Exception while receiving data from Event Manager");
        }
        return msg;
    }

    /*
    * Receives the list of topics from Event Manager
    * */
    public ArrayList<Topic> getTopicList() {
        ArrayList<Topic> topicList = null;
        int[] availablePorts = getAvailablePorts();
        Socket socket = null;

        while (socket == null) {
            int listenerPort = getRandomAvailablePort(availablePorts);
            socket = startConnection(serverAddress, listenerPort);
        }

        Message msg = new Message(Message.MessageType.ListTopic, null);
        sendObject(msg, socket);
        Message received_msg = receiveObject(socket);
        topicList = received_msg.getTopicList();
        closeConnection(socket);
        return topicList;
    }

    /*
    * Displays any array list passed as arguments with the display message corresponding to it
    * */
    public void displayList(ArrayList<Topic> topicList, String displayMsg) {
        Topic topic = null;

        System.out.println("\n\n" + displayMsg + "\n");
        for (int i = 0; i < topicList.size(); i++) {
            topic = topicList.get(i);
            System.out.println((i + 1) + ". " + topic.getTopicName());
        }
    }

    /*
    * Sends messages
    * */
    public void sendObject(Message msg, Socket socket) {
        ObjectOutputStream clientOutputStream = null;
        try {
            clientOutputStream = new
                    ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {

            clientOutputStream.writeObject(msg);
            clientOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    * Returns a random port number from the array of available ports
    * */
    public int getRandomAvailablePort(int[] array) {
        int randomIndex = new Random().nextInt(array.length);
        return array[randomIndex];
    }

    /*
    * Socket code for establishing connection
    * */
    public Socket startConnection(InetAddress serverAddress, int listenerPort) {
        Socket socket = null;
        try {
            socket = connectionHandler(serverAddress, listenerPort);
        } catch (Exception e) {
            System.out.println("Server is down");
            e.printStackTrace();
            return null;
        }
        return socket;
    }

    /*
    * Socket code for closing connection
    * */
    public void closeConnection(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    * Create a new socket and returns the socket
    * */
    public Socket connectionHandler(InetAddress ip, int portno) throws Exception {
        Socket socket = new Socket(ip, portno);
        return socket;
    }

    /*
    * Reads and loads properties from the configuration file
    * */
    public void loadProperties() {

        Properties confProps = new Properties();
        try {
            confProps.load(PubSubAgent.class.getResourceAsStream("Conf.Properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

            availablePorts = confProps.getProperty("ports-available");


            System.out.println(serverAddress.getHostAddress());
    }

    //to locally run this
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
            availablePorts = confProps.getProperty("ports-available");
            notificationListenerPort = Integer.parseInt(confProps.getProperty("notification-listener-port"));
            serverAddress = InetAddress.getByName(confProps.getProperty("event-manager-ip"));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }*/

}