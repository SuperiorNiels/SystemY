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

    private FXMLLoader opening  = new FXMLLoader();
    private FXMLLoader server   = new FXMLLoader();
    private FXMLLoader login    = new FXMLLoader();
    private FXMLLoader main     = new FXMLLoader();
    private FXMLLoader network  = new FXMLLoader();
    private FXMLLoader logoff   = new FXMLLoader();
    private FXMLLoader loading  = new FXMLLoader();

    private OpeningController openingController;
    private ServerController  serverController ;
    private LoginController   loginController  ;
    private MainController    mainController   ;
    private NetworkController networkController;
    private LogoffController  logoffController ;
    private LoadingController loadingController;

    private Parent ropening;
    private Parent rserver ;
    private Parent rlogin  ;
    private Parent rmain   ;
    private Parent rnetwork;
    private Parent rlogoff ;
    private Parent rloading;

    private Node node;

    public HeadController() throws IOException {
        opening.setLocation( getClass().getResource("OpeningView.fxml"));
        server.setLocation(  getClass().getResource("ServerView.fxml" ));
        login.setLocation(   getClass().getResource("LoginView.fxml"  ));
        main.setLocation(    getClass().getResource("mainView.fxml"   ));
        network.setLocation( getClass().getResource("networkView.fxml"));
        logoff.setLocation(  getClass().getResource("logoffView.fxml" ));
        loading.setLocation( getClass().getResource("LoadingView.fxml"));

        ropening = opening.load();
        rserver  = server.load();
        rlogin   = login.load();
        rmain    = main.load();
        rnetwork = network.load();
        rlogoff  = logoff.load();
        rloading = loading.load();

        openingController = opening.getController();
        serverController  = server.getController() ;
        loginController   = login.getController()  ;
        mainController    = main.getController()   ;
        networkController = network.getController();
        logoffController  = logoff.getController() ;
        loadingController = loading.getController();

        openingController.init(this);
        serverController.init(this );
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

    public void toOpening(){
        openingController.view(ropening);
    }

    public void toServer(){
        serverController.view(rserver);
        serverController.initData();
    }

    public void toLoading(){
        loadingController.view(rloading);
    }

    public void closeLoading(){
        loadingController.close();
    }
}
