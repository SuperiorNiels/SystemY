package GUI;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import Node.Node;
import javafx.stage.WindowEvent;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;


public class MainController {

    @FXML
    Button Log_off_bnt;
    @FXML
    Label nameLabel;
    @FXML
    ListView fileName_list;
    private int viewWidth  = 570;
    private int viewHeight = 433;
    private Node node;
    private HeadController headController;
    private Scene view;
    private TreeMap<String, Boolean> files;

public void delete(){
    String file = fileName_list.getSelectionModel().getSelectedItem().toString();
    System.out.println("delete : " + file);
}

public void deleteLocal(){
    String file = fileName_list.getSelectionModel().getSelectedItem().toString();
    System.out.println("deleteLocal : " + file);
}

public void init(HeadController headcontroller){
        this.headController =headcontroller;
    };

public void initData(){
        node = headController.getNode();
        node.setController(this);
        nameLabel.setText(node.getName());
        update();
}

public void logOff(){
        Stage currentWindow = (Stage) Log_off_bnt.getScene().getWindow();
        currentWindow.close();
        headController.toLogoff();
    }

public void open(){
    String file = fileName_list.getSelectionModel().getSelectedItem().toString();
    try {
        if (Desktop.isDesktopSupported()) {
            File localf = new File("files\\local\\"+ file);
            File replif = new File("files\\replicated\\"+ file);
            if(localf.exists()){
                System.out.println("in local");
                Desktop.getDesktop().open(localf);
            }
            else if(replif.exists()) {
                System.out.println("in replicated");
                Desktop.getDesktop().open(replif);
            }else{
                //download the file from the network
            }
        }
    } catch (IOException ioe) {
        System.err.println("could not open file");;
    }
    System.out.println("opening : " + file);
}

public void view(Parent root){
        if(view == null)
            view = new Scene(root,viewWidth,viewHeight);
        Stage stage = new Stage();
        stage.setResizable(false);
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(final WindowEvent arg0) {
                headController.toLogoff();
            }
        });
        stage.setTitle("SystemY GUI");
        stage.setScene(view);
        stage.show();
    }

public void viewNetwork(){

        Stage currentWindow = (Stage) Log_off_bnt.getScene().getWindow();
        currentWindow.close();
        headController.toLoading();
        headController.toNetwork();
        headController.closeLoading();

    }

public void update(){
    /*
    files = node.getFiles();
    fileName_list.getItems().clear();
    for (Map.Entry<String, Boolean> entry : files.entrySet()) {
        fileName_list.getItems().add(entry.getKey());
        System.out.println(entry.getKey());
    }
    fileName_list.getSelectionModel().selectFirst();
    */
}
}
