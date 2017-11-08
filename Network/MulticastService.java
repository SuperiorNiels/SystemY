package Network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MulticastService extends Thread {
    private String multicast_ip;
    private String interface_ip;
    private int multicast_port;
    private MulticastSocket socket;
    private Boolean running;
    private String received;
    private MulticastObserver observer;

    public MulticastService(String multicast_ip,String ip,int port) throws IOException {
        this.multicast_ip = multicast_ip;
        this.interface_ip = ip;
        this.multicast_port = port;
        received = "";
        observer = new MulticastObserver();
    }

    /**
     * Sets the socket on the specified port
     * @return true when succeeded, false when socket not available
     */
    public Boolean setupService(){
        try {
            this.socket = new MulticastSocket(multicast_port);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * This method send a multicast packet out on the ip specified in the constructor
     * @param message the message of the packet
     * @return true when success, false when not send
     */
    public Boolean sendMulticast(String message){
        if(socket!=null) {
            if (!this.setupService()) {
                return false;
            }
        }
        try {
            byte[] buf;
            buf = message.getBytes();
            InetAddress groupAddress = InetAddress.getByName(multicast_ip);
            DatagramPacket packet = new DatagramPacket(buf,buf.length,groupAddress,multicast_port);
            socket.send(packet);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * closes the socket and stop the receiving thread
     */
    public void stopService(){
        running = false;
        if(socket!=null){
            socket.close();
        }

    }

    /**
     * constantly check for receiving multicast packets
     */
    @Override
    public void run() {

        if(socket!=null) {
            if (!this.setupService()) {
                return;
            }
        }
        running = true;
        InetAddress groupAddress = null;
        try {
            groupAddress = InetAddress.getByName(multicast_ip);
            //Set the interface where to listen to the multicast packets
            socket.setInterface(InetAddress.getByAddress(interface_ip.getBytes()));
            socket.joinGroup(groupAddress);
            DatagramPacket packet;
            while(running) {
                byte[] buf = new byte[256];
                packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                received = new String(packet.getData());
                observer.setChanged();
                observer.notifyObservers(received);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
