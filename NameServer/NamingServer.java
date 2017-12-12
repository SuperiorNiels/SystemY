package NameServer;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import GUI.ServerController;
import Network.MulticastService;
import javafx.fxml.FXMLLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import Node.NodeInterface;
import Node.Neighbour;

public class NamingServer implements NamingInterface, Observer {

    private TreeMap<Integer, Neighbour> map = new TreeMap<>();
    private String ip = null;
    MulticastService multicast;
    private ServerController controller;
    private NameServerOutputHandler handler;

    public NamingServer() { }

    public void start(){
        handler = new NameServerOutputHandler();
        try {
            multicast = new MulticastService("224.0.0.1", 4446);
            ip = multicast.getIpAddress();
        } catch (IOException e) {
            System.err.println("Multicast fail");
        }

        multicast.addObserver(this);
        multicast.start();
        startRMI();

        System.out.println("Nameserver started. IP: "+ip);
            Scanner input = new Scanner(System.in);
            while(true) {
                String command = input.nextLine();
                String parts[] = command.split(" ");
                if(parts[0].toLowerCase().equals("save")) {
                    try {
                        createXML("./data/output.xml");
                    }
                    catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                } else if(parts[0].toLowerCase().equals("print")) {
                    System.out.println("# nodes in network: "+map.size());
                } else if(parts[0].toLowerCase().equals("reload")) {
                    map = new TreeMap<Integer, Neighbour>();
                    System.out.println("Nameserver reload complete.");
                } else if(parts[0].toLowerCase().equals("help")) {
                    System.out.println("Commands: ");
                    System.out.println("\t- save : saves the current map.");
                    System.out.println("\t- print: print the current map size.");
                    System.out.println("\t- reload: reload the nameserver, empty map.");
                } else {
                    System.out.println("Command not found, run 'help' to get a list of available commands.");
                }
            }
    }

    public void start(ServerController controller) {
        handler = new NameServerOutputHandler(controller);
        try {
            multicast = new MulticastService("224.0.0.1", 4446);
            ip = multicast.getIpAddress();
            multicast.addObserver(this);
            multicast.start();
            startRMI();
            this.controller = controller;
            controller.update("Nameserver started. IP: "+ip);

        }
        catch (IOException e) {
            System.out.println("IOException: multicast failed.");
        }
    }

    /**
     * New node sends multicast starting with 00, the name and ip of the new node are send in this message
     * The nameserver checks if the new node can join, if the node is accepted the nameserver will send a multicast message:
     * 01;size_of_map-1;name_of_new_node;ip_of_new_node;nameserver_ip
     * When a node recieves this last message, the nessecary steps can be taken for the new node to join.
     * @param observable
     * @param o
     */
    @Override
    public void update(Observable observable, Object o) {
        String message = o.toString();
        String parts[] = message.split(";");
        if(controller == null) {

        }
        if(parts[0].equals("00")) {
            controller.update("New node detected. Name: "+parts[1]+" IP: "+parts[2]);
            System.out.println("New node detected.");
            System.out.println("Name: "+parts[1]+" IP: "+parts[2]);
            try {
                addNode(parts[2],parts[1]);
                multicast.sendMulticast("01;"+(map.size()-1)+";"+parts[1]+";"+parts[2]+";"+ip);
            }
            catch (AlreadyExistsException e) {
                controller.update("Node name already in use, won't add the node");
                System.out.println("Node name already in use, won't add the node");
                try {
                    NodeInterface wrongNode = (NodeInterface) Naming.lookup("//" + parts[2] + "/Node");
                    wrongNode.failedToAddNode();
                    controller.update("Succesfully notified and shutdown the wrong node");
                    System.out.println("Succesfully notified and shutdown the wrong node");
                } catch (NotBoundException e1) {
                    e1.printStackTrace();
                } catch (MalformedURLException e1) {
                    e1.printStackTrace();
                } catch (RemoteException e1) {
                    controller.update("Succesfully notified and shutdown the wrong node");
                    System.out.println("Succesfully notified and shutdown the wrong node");
                }

            }
        }
    }

    /**
     * Create RMI registry
     */
    private void startRMI() {
        try {
            System.setProperty("java.rmi.server.hostname",ip);
            //Start the RMI-server
            NamingServer server = this;
            NamingInterface stub = (NamingInterface) UnicastRemoteObject.exportObject(server,0);
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.bind("NamingServer", stub);
            //System.out.println("Server ready!");
        } catch (RemoteException e) {
            controller.updateError("Remote exception: "+e.getMessage());
            System.err.println("Remote exception: "+e.getMessage());
        } catch (AlreadyBoundException e) {
            controller.updateError("Port already bound");
            System.err.println("Port already bound");
        }
    }

    /**
     * @param ip String, ip address of node
     * @param name String, hostname of node
     * @throws AlreadyExistsException
     *
     */
    public void addNode(String ip, String name) throws AlreadyExistsException {
        Integer hash = getHash(name);
        if (map.containsKey(hash)) {
            controller.updateError("Hash already exists.");
            System.err.println("Hash already exists.");
            throw new AlreadyExistsException();
        } else {
            Neighbour newNeighbour = new Neighbour(name,ip);
            if(!map.containsValue(newNeighbour)) {
                map.put(hash, newNeighbour);
            }
        }
    }

    /**
     * Remove a Node from the map
     * If there is no such node in the map throw an exception
     * @param name, String, hostname of node
     */
    public void removeNode(String name) throws NullPointerException {
        if(map.remove(getHash(name))== null){
            throw new NullPointerException();
        }
        controller.update("Node: "+name+" successfully removed.");
        System.out.println("Node: "+name+" successfully removed.");
    }

    /**
     * Create XML for map, and save it in the given file
     * @param path String, path of file to write xml to
     */
    public void createXML(String path) throws Exception {
        if(map.size() != 0) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();

                Document xml = builder.newDocument();
                Element root = xml.createElement("nodes");
                xml.appendChild(root);

                for (Integer hash : map.keySet()) {
                    Element host = xml.createElement("host");
                    host.setAttribute("id", Integer.toString(hash)); // ID can be changed to "hash"
                    root.appendChild(host);
                    // Add IP address
                    Element address = xml.createElement("address");
                    address.appendChild(xml.createTextNode(map.get(hash).getIp()));
                    host.appendChild(address);
                    // Add hostname
                    Element hostname = xml.createElement("hostname");
                    hostname.appendChild(xml.createTextNode(map.get(hash).getName()));
                    host.appendChild(hostname);
                }

                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource source = new DOMSource(xml);
                File file = new File(path);
                StreamResult result = new StreamResult(file);

                transformer.transform(source, result);
            } catch (ParserConfigurationException e) {
                throw e;
            } catch (TransformerConfigurationException e) {
                throw e;
            } catch (TransformerException e) {
                throw e;
            }
            controller.update("Map saved to ./data/output.xml");
            System.out.println("Map saved to ./data/output.xml");
        } else {
            controller.updateError("Empty map (no nodes in network), map not saved.");
            System.err.println("Empty map (no nodes in network), map not saved.");
        }
    }

    /**
     * @return name and ip of owner (Neighbour object)
     * returns 0 if map is empty
     * @param fileName = name of the file
     */
    public Neighbour getOwner(String fileName) {
        int ownerHash = 0;
        int fileHash = getHash(fileName);
        if(!map.isEmpty()) {
            ArrayList<Integer> lowerHashes = new ArrayList<>();
            //Find the lower hashes first
            for(int h : map.keySet()){
                if(h<fileHash){
                    lowerHashes.add(h);
                }
            }
            //Check if lower hashes are found. If not take largest hash
            if(!lowerHashes.isEmpty()){
                //Look for the hash closest to the filehash
                int diff = Integer.MAX_VALUE;
                int owner = 0;
                for(int h : lowerHashes){
                    int newDiff = fileHash - h;
                    if(newDiff<diff){
                        diff = newDiff;
                        owner = h;
                    }
                    ownerHash = owner;
                }
            }else{
                int big= -1;
                for(int h : map.keySet()){
                    if(h>big){
                        big = h;
                    }
                }
                ownerHash = big;
            }
        }
        return (ownerHash!=0) ? map.get(ownerHash): null;
    }

    /**
     * @param name
     * @return hash of the input string
     */
    public int getHash(String name){
        return Math.abs(name.hashCode() % 32768);
    }

    public Neighbour findPreviousNode(String nameFailedNode){
        int key;
        try{
            key = map.lowerKey(getHash(nameFailedNode));
        }catch (NullPointerException e){
            key = map.lastKey();
        }
        Neighbour previous = new Neighbour(map.get(key).getName(),map.get(key).getIp());
        return previous;
    }

    /**
     * finds the next node from another node n by chechking the hashes
     * the node with a highest hash (closest by node n) is the nextNode
     * @param nameFailedNode
     * @return
     */
    public Neighbour findNextNode(String nameFailedNode) {
        int key;
        try{
            key = map.higherKey(getHash(nameFailedNode));
        }catch (NullPointerException e){
            key = map.firstKey();
        }
        Neighbour next = new Neighbour(map.get(key).getName(),map.get(key).getIp());
        return next;
    }

    /**
     * this method asks for the number of nodes.
     * different from sendNumberOfNodes() because in this method a node is pulling data
     * @return map.size()
     */
    public int getNumberOfNodes() {return map.size();}
}
