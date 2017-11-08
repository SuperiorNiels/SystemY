package Test;

import Network.MulticastObserverable;
import Network.MulticastService;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Observable;
import java.util.Observer;

public class MulticastTest implements Observer{

    MulticastObserverable ob;
    public MulticastTest(MulticastObserverable ob){
        this.ob = ob;
    }

    public static void main(String[] args) {
        try {
            MulticastService service = new MulticastService("224.0.0.1", 4446);
            MulticastObserverable observer = new MulticastObserverable();
            MulticastTest ob = new MulticastTest(observer);
            observer.addObserver(ob);
            service.start();
            //service.sendMulticast("Hello world!");


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void update(Observable o, Object arg) {
        if(o==ob){
            System.out.println(arg);
        }
    }

}


