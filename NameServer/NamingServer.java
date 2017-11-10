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
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import Network.MulticastObserverable;
import Network.MulticastService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import Node.Node;
import Node.NodeInterface;

public class NamingServer implements NamingInterface, Observer {

    private TreeMap<Integer, Node> map = new TreeMap<>();
    private String ip = null;
    MulticastService multicast;

    public NamingServer() { }

    public void start() {
        try {
            multicast = new MulticastService("224.0.0.1", 4446);
            ip = multicast.getIpAddress();
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
                } else {
                    System.out.println(command);
                }
            }
        }
        catch (IOException e) {
            System.out.println("IOException: multicast failed.");
        }
    }

    @Override
    public void update(Observable observable, Object o) {
        String message = o.toString();
        String parts[] = message.split(";");
        if(parts[0].equals("00")) {
            System.out.println("New node detected.");
            System.out.println("Name: "+parts[1]+" IP: "+parts[2]);
            try {
                addNode(parts[2],parts[1]);
                multicast.sendMulticast("01;"+(map.size()-1)+";"+parts[1]+";"+parts[2]+";"+ip);
            }
            catch (AlreadyExistsException e) {
                System.out.println("Node name taken, node rejected.");
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
            System.err.println("Remote exception: "+e.getMessage());
        } catch (AlreadyBoundException e) {
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
            System.err.println("Hash already exists.");
            throw new AlreadyExistsException();
        } else {
            Node node = new Node(name);
            if(!map.containsValue(node)) {
                map.put(hash, node);
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
    }

    /**
     * Create XML for map, and save it in the given file
     * @param path String, path of file to write xml to
     */
    public void createXML(String path) throws Exception {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document xml = builder.newDocument();
            Element root = xml.createElement("nodes");
            xml.appendChild(root);

            for(Integer hash : map.keySet()) {
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
        }
        catch(ParserConfigurationException e) {
            throw e;
        }
        catch(TransformerConfigurationException e) {
            throw e;
        }
        catch(TransformerException e) {
            throw e;
        }
    }

    /**
     * @return hash of owner
     * returns 0 if map is empty
     * @param fileName = name of the file
     */
    public String getOwner(String fileName) {
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
        return (ownerHash!=0) ? map.get(ownerHash).getIp(): null;
    }

    /**
     * Send number of nodes in the network to the ip (paramater) via RMI
     * @param node_ip, ip address of node (RMI server)
     */
    public void sendNumberOfNodes(String node_ip) {
        try {
            NodeInterface stub = (NodeInterface) Naming.lookup("//"+node_ip+"/Node");
            stub.setNumberOfNodesInNetwork(map.size());
            stub.setNameServerIp(ip);
        }
        catch (Exception e) {
            System.err.println("RMI to node failed.");
        }
    }

    /**
     * @param name
     * @return hash of the input string
     */
    public int getHash(String name){
        return Math.abs(name.hashCode() % 32768);
    }

    public Node findPreviousNode(String nameFailedNode){
        int hashFailedNode      = getHash(nameFailedNode);
        int hashPreviousNode    = map.lowerKey(hashFailedNode);
        return map.get(hashPreviousNode);
    }

    /**
     * finds the next node from another node n by chechking the hashes
     * the node with a highest hash (closest by node n) is the nextNode
     * @param nameFailedNode
     * @return
     */
    public Node findNextNode(String nameFailedNode) {
        int hashFailedNode = getHash(nameFailedNode);
        int hashNextNode = map.higherKey(hashFailedNode);
        return map.get(hashNextNode);

    }
}
