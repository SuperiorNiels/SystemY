package NameServer;

import java.io.File;

import javax.management.openmbean.KeyAlreadyExistsException;
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

    private TreeMap<Integer, Node> map = new TreeMap<>();

    public NamingServer() {}

    /**
     * @param hash Integer, calculated hash for node
     * @param ip String, ip address of node
     * @param name String, hostname of node
     * @throws AlreadyExistsException
     *
     */
    public void addNode(Integer hash, String ip, String name) throws AlreadyExistsException {
        if (map.containsKey(hash)) {
            System.out.println("Hash already exists.");
            throw new AlreadyExistsException();
        } else {
            Node node = new Node(ip, name);
            for (Node n : map.values()) {
                if (node.equals(n)) {
                    throw new AlreadyExistsException();
                }
            }
            map.put(hash, node);
        }
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
}
