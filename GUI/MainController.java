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

    public void delete(){
        String file = fileName_list.getSelectionModel().getSelectedItem().toString();
        //System.out.println("delete : " + file);
        if(node.fileInSystem(file)) {
            node.deleteFileOwner(file,false);
        }
        //no error but a pop-up that the file is delted
        headController.toError("file deleted" , false);
    }

    public void deleteLocal() {
        String file = fileName_list.getSelectionModel().getSelectedItem().toString();
        //System.out.println("deleteLocal : " + file);
        if(node.fileInSystem(file)) {
            try {
                node.locallyRemoveFile(file);
                headController.toError("file deleted.",false);
            } catch (NullPointerException e) {
                headController.toError("Error: Cannot delete file!",false);
            } catch (FileLocationException e) {
                headController.toError("Error: file is replicated or local, file not deleted!",false);
            }
        }
    }

    public void init(HeadController headcontroller){
        this.headController =headcontroller;
    }

    public void initData(){
        node = headController.getNode();
        nameLabel.setText(node.getName());
    }

    public void logOff(){
        Stage currentWindow = (Stage) Log_off_bnt.getScene().getWindow();
        currentWindow.close();
        headController.toLogoff();
    }

    public void open(){
        //headController.toLoading();
        String file = fileName_list.getSelectionModel().getSelectedItem().toString();
        node.openFile(file);
        //headController.closeLoading();
        System.out.println("opening : " + file);
    }

    public void view(Parent root){
        if(view == null)
            view = new Scene(root,viewWidth,viewHeight);
        Stage stage = new Stage();
        stage.setResizable(false);
        stage.setOnCloseRequest(arg0 -> headController.toLogoff());
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
        //System.out.println("update");
        newFiles = node.getFiles();
        fileName_list.getItems().clear();
        for (Map.Entry<String, FileRequest> entry : newFiles.entrySet()) {
            fileName_list.getItems().add(entry.getKey());
            //System.out.println(entry.getKey());
        }
        fileName_list.getSelectionModel().selectFirst();
    }

}
