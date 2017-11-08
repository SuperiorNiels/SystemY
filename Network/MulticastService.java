package Network;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.Observer;

public class MulticastService extends Thread {
    private String multicast_ip;
    private String interface_ip;
    private int multicast_port;
    private MulticastSocket socket;
    private Boolean running;
    private String received;
    private MulticastObserverable observer;

    public MulticastService(String multicast_ip,int port) throws IOException {
        this.multicast_ip = multicast_ip;
        this.interface_ip = getIpAddress();
        this.multicast_port = port;
        received = "";
        observer = new MulticastObserverable();
    }

    /**
     * Helper class to get ip address of the interface
     * This method tries to find the ethernet interface and not the wlans
     * @return the host ethernet interface ip address
     * @throws SocketException
     */
    public String getIpAddress() throws SocketException {
        String ip = null;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // filters out 127.0.0.1 and inactive interfaces
                if (iface.isLoopback() || !iface.isUp() || !iface.supportsMulticast() ||
                        iface.getDisplayName().contains("wlan") || iface.getDisplayName().contains("Wireless LAN"))
                        //|| iface.getDisplayName().contains("wlp"))
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if(addr instanceof Inet4Address )
                        ip = addr.getHostAddress();
                    System.out.println(iface.getDisplayName() + " " + ip);
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        return ip;
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
        if(socket==null) {
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

    public void addObserver(Observer o) {
        observer.addObserver(o);
    }

    /**
     * constantly check for receiving multicast packets
     */
    @Override
    public void run() {

        if(socket==null) {
            if (!this.setupService()) {
                return;
            }
        }
        running = true;
        InetAddress groupAddress = null;
        try {
            groupAddress = InetAddress.getByName(multicast_ip);
            //Set the interface where to listen to the multicast packets
            socket.setInterface(InetAddress.getByName(interface_ip));
            socket.joinGroup(groupAddress);
            DatagramPacket packet;
            while(running) {
                byte[] buf = new byte[62*1024];
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
