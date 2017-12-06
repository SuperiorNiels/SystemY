package Agents;

import java.io.Serializable;
import java.util.TreeMap;
import Node.Node;

public class FileAgent implements Runnable, Serializable{
    private TreeMap<String, Boolean> files = new TreeMap<String, Boolean>();


    @Override
    public void run() {
    }
}
