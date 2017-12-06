package Agents;

import java.io.Serializable;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

import Node.Node;

public class FileAgent extends Agent {

    private TreeMap<String, Semaphore> files = new TreeMap<String, Semaphore>();

    @Override
    public void run() {
        
    }

    public void getFiles() {

    }
}
