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

public class Mapper {

    TreeMap<Integer, String> map = new TreeMap<>();
    TreeMap<Integer, String> names = new TreeMap<>();
    ArrayList<Integer> hashes = new ArrayList<>();

    public Mapper() {}

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
}
