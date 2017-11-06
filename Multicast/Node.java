package Multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Node extends Thread{
    String nodeName;
    public Node(String name){
        this.nodeName = name;
    }
    public void run(){
        try {
            MulticastSocket socket = new MulticastSocket(4446);
            InetAddress groupAddress = InetAddress.getByName("224.0.0.1");
            socket.joinGroup(groupAddress);

            DatagramPacket packet;
            byte[] buf = new byte[256];
            packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);

            String received = new String(packet.getData());
            System.out.println("Node "+nodeName +" RECEIVED PACKET: " + received);

            socket.leaveGroup(groupAddress);
            socket.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class NodeMain {

    public static void main(String[] args) throws IOException {
        new Node("client 1").run();
    }

}
