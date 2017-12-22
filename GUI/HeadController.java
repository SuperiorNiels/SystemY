package GUI;


import javafx.fxml.FXMLLoader;
import Node.Node;
import javafx.scene.Parent;

import java.io.IOException;


/**
 * this controller controls all the controllers for each view
 * it creates a loader , controller and parent for each view
 * afterwarts each viewcontroller can ask the HeadController to open another view
 */
public class HeadController {

    private FXMLLoader login    = new FXMLLoader();
    private FXMLLoader main     = new FXMLLoader();
    private FXMLLoader network  = new FXMLLoader();
    private FXMLLoader logoff   = new FXMLLoader();
    private FXMLLoader loading  = new FXMLLoader();

    private LoginController   loginController  ;
    private MainController    mainController   ;
    private NetworkController networkController;
    private LogoffController  logoffController ;
    private LoadingController loadingController;

    private Parent rlogin  ;
    private Parent rmain   ;
    private Parent rnetwork;
    private Parent rlogoff ;
    private Parent rloading;

    private Node node;
    private int delay = 300;

    public HeadController() throws IOException {
        login.setLocation(   getClass().getResource("LoginView.fxml"  ));
        main.setLocation(    getClass().getResource("mainView.fxml"   ));
        network.setLocation( getClass().getResource("networkView.fxml"));
        logoff.setLocation(  getClass().getResource("logoffView.fxml" ));
        loading.setLocation( getClass().getResource("LoadingView.fxml"));

        rlogin   = login.load();
        rmain    = main.load();
        rnetwork = network.load();
        rlogoff  = logoff.load();
        rloading = loading.load();

        loginController   = login.getController()  ;
        mainController    = main.getController()   ;
        networkController = network.getController();
        logoffController  = logoff.getController() ;
        loadingController = loading.getController();

        loginController.init(this  );
        mainController.init(this   );
        networkController.init(this);
        logoffController.init(this );
        loadingController.init(this);
    }

    public Node getNode(){
        return node;
    }

    public void setNode(Node node){
        this.node = node;
    }

    public void toLogin(){loginController.view(rlogin);}

    public void toLogoff(){
        logoffController.initData();
        logoffController.view(rlogoff);
    }

    public void toMain(){
        mainController.initData();
        mainController.view(rmain);
    }

    public void toNetwork(){networkController.view(rnetwork);}

    public void toLoading(){
        loadingController.view(rloading);
    }

    public void closeLoading(){
        loadingController.close();
    }

    public int getDelay(){return this.delay;}
}
