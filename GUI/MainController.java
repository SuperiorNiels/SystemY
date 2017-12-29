package GUI;

import Agents.FileRequest;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import Node.Node;
import Node.FileLocationException;
import javafx.stage.WindowEvent;

import java.util.*;

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
    private TreeMap<String, FileRequest> newFiles;
    private TreeMap<String, FileRequest> oldFiles;
    private int delay;

public void delete(){
    String file = fileName_list.getSelectionModel().getSelectedItem().toString();
    System.out.println("delete : " + file);
}

public void deleteLocal() {
    String file = fileName_list.getSelectionModel().getSelectedItem().toString();
    System.out.println("deleteLocal : " + file);
    try {
        node.locallyRemoveFile(file);
    } catch (NullPointerException e) {

    } catch (FileLocationException e) {

    }
}

public void init(HeadController headcontroller){
        this.headController =headcontroller;
        this.delay = headcontroller.getDelay();
    };

public void initData(){
        node = headController.getNode();
        nameLabel.setText(node.getName());
        node.setMainController(this);
}

public void logOff(){
        Stage currentWindow = (Stage) Log_off_bnt.getScene().getWindow();
        currentWindow.close();
        headController.toLogoff();
    }

public void open(){
    headController.toLoading();
    String file = fileName_list.getSelectionModel().getSelectedItem().toString();
    try {
        if (Desktop.isDesktopSupported()) {
            File localf = new File("files\\local\\"+ file);
            File replif = new File("files\\replicated\\"+ file);
            File downlf = new File("files\\download\\" + file);
            if(localf.exists()){
                System.out.println("in local");
                Desktop.getDesktop().open(localf);
            }
            else if(replif.exists()) {
                System.out.println("in replicated");
                Desktop.getDesktop().open(replif);
            }
            else if(downlf.exists()){
                System.out.println("in download");
                Desktop.getDesktop().open(downlf);
            } else{
                //download the file from the network

            }
        }
    } catch (IOException ioe) {
        System.err.println("could not open file");;
    }
    headController.closeLoading();
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
        headController.toLoading();
        headController.toNetwork();
        headController.closeLoading();
    }

public void update(){
    System.out.println("update");
        newFiles = node.getFiles();

        if(oldFiles == null){
            fileName_list.getItems().clear();
            for (Map.Entry<String, FileRequest> entry : newFiles.entrySet()) {
                fileName_list.getItems().add(entry.getKey());
                System.out.println(entry.getKey());
            }
            fileName_list.getSelectionModel().selectFirst();
            oldFiles = newFiles;
        }else {
            Set values1 = new HashSet(newFiles.values());
            Set values2 = new HashSet(oldFiles.values());
            boolean equal = values1.equals(values2);

            if (!equal) {
                fileName_list.getItems().clear();
                for (Map.Entry<String, FileRequest> entry : newFiles.entrySet()) {
                    fileName_list.getItems().add(entry.getKey());
                    System.out.println(entry.getKey());
                }
                fileName_list.getSelectionModel().selectFirst();
            } else {
                oldFiles = newFiles;
            }
        }
    }

}
