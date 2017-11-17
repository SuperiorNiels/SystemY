package Test;

import Network.MulticastObservable;
import Network.MulticastService;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

public class MulticastTest implements Observer{

    MulticastObservable ob;
    public MulticastTest(MulticastObservable ob){
        this.ob = ob;
    }

    public static void main(String[] args) {
        try {
            MulticastService service = new MulticastService("224.0.0.1", 4446);
            MulticastObservable observer = new MulticastObservable();
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


