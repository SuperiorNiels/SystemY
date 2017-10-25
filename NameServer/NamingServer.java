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
import java.util.TreeMap;
import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class NamingServer {

    private TreeMap<Integer, String> map = new TreeMap<>();
    private TreeMap<Integer, String> names = new TreeMap<>();
    private TreeMap<Integer,Integer> files = new TreeMap<>();
    private ArrayList<Integer> hashes = new ArrayList<>();


    public NamingServer() {}

    public Boolean updateMap(Integer hash, String ip, String name) {
        Boolean error = true;
        if(hashes.contains(hash)) {
            System.out.println("Hash already exists.");
            error = false;
        } else if(map.containsValue(ip)) {
            System.out.println("IP already exists.");
            error = false;
        } else if(names.containsValue(name)) {
            System.out.println("Name already exists.");
            error = false;
        } else {
            hashes.add(hash);
            map.put(hash, ip);
            names.put(hash, name);
        }
        return error;
    }

    /**
     * @return String (path of xml file)
     */
    public String createXML() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document xml = builder.newDocument();
            Element root = xml.createElement("nodes");
            xml.appendChild(root);

            for(Integer hash : hashes) {
                Element host = xml.createElement("host");
                host.setAttribute("id", Integer.toString(hash)); // ID can be changed to "hash"
                root.appendChild(host);
                // Add IP address
                Element address = xml.createElement("address");
                address.appendChild(xml.createTextNode(map.get(hash)));
                host.appendChild(address);
                // Add hostname
                Element hostname = xml.createElement("hostname");
                hostname.appendChild(xml.createTextNode(names.get(hash)));
                host.appendChild(hostname);
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(xml);
            StreamResult result = new StreamResult(new File("./data/output.xml"));

            transformer.transform(source, result);
        }
        catch(ParserConfigurationException e) {
            e.printStackTrace();
        }
        catch(TransformerConfigurationException e) {
            e.printStackTrace();
        }
        catch(TransformerException e) {
            e.printStackTrace();
        }
        return "../data/output.xml";
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
     * @return the IP address of the owner node
     */
    public String getOwner(String fileName){
        int hash = getHash(fileName);
        return map.get(files.get(hash));
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
            for(int h : hashes){
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
                for(int h : hashes){
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
