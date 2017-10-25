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
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.TreeMap;
import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class NamingServer implements NamingInterface{

    private TreeMap<Integer, Node> map = new TreeMap<>();
    private TreeMap<Integer,Integer> files = new TreeMap<>();

    public NamingServer() {
        try {
            //Start the RMI-server
            NamingServer server = this;
            NamingInterface stub = (NamingInterface) UnicastRemoteObject.exportObject(server,0);
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.bind("NamingServer", stub);
            System.out.println("Server ready!");
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (AlreadyBoundException e) {
            e.printStackTrace();
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
            System.out.println("Hash already exists.");
            throw new AlreadyExistsException();
        } else {
            Node node = new Node(ip, name);
            if(!map.containsValue(node)) {
                map.put(hash, node);
            }
        }
    }

    /**
     * Remove a Node from the map
     * @param name, String, hostname of node
     */
    public void removeNode(String name) {
        map.remove(getHash(name));
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
                address.appendChild(xml.createTextNode(map.get(hash).ip));
                host.appendChild(address);
                // Add hostname
                Element hostname = xml.createElement("hostname");
                hostname.appendChild(xml.createTextNode(map.get(hash).name));
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
     * @param fileName
     * @return the hash of the owner node
     * This method adds a file to the files treemap
     */
    public int addFile(String fileName){
        int hash = getHash(fileName);
        int owner = calculateOwner(hash);
        files.put(hash,owner);
        return owner;
    }

    /**
     * @param fileName
     * Removes the files hash from the files treemap
     */
    public void removeFile(String fileName){
        files.remove(getHash(fileName));
    }

    /**
     *
     * @param fileName
     * @return a node object
     */
    public String getOwner(String fileName){
        int hash = getHash(fileName);
        return map.get(files.get(hash)).getIp();
    }

    /**
     * @return int (hash of new file owner)
     * returns 0 if map is empty
     * @param fileHash = the hash of the filename
     */
    private int calculateOwner(int fileHash) {
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
                    return owner;
                }
            }else{
                int big= -1;
                for(int h : map.keySet()){
                    if(h>big){
                        big = h;
                    }
                }
                return big;
            }
        }
        return 0;
    }

    /**
     * @param name
     * @return hash of the input string
     */
    public int getHash(String name){
        return Math.abs(name.hashCode() % 32768);
    }
}
