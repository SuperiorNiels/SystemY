package GUI;

import Node.Node;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;


/**
 * LoginController is the controller for the loginView.
 * Loginview is created with JavaFx scenebuilder.
 * Loginview is the view that the user gets when it choose for Client in the OpeningView.
 * In the loginView you HAVE to enter a name and you can choose your interface.
 * When the user clicks on the button login a new node is created, this view closes and the mainview opens.
 */
public class LoginController {

    private int viewWidth  = 333;
    private int viewHeight = 181;
    private Node node;
    private Scene view;
    private Boolean validEntered =true;
    @FXML
    private TextField login_name_text;
    @FXML
    private ChoiceBox interface_select;
    @FXML
    private Label errorLabel;
    private boolean triedToLogin = false;
    Stage stage;

    private GUI_Controller gui;

    private HeadController headController;
    public void init(HeadController headcontroller) throws SocketException {
        this.headController = headcontroller;

        for (NetworkInterface ni :
                Collections.list(NetworkInterface.getNetworkInterfaces())) {
            for (InetAddress address : Collections.list(ni.getInetAddresses())) {
                if (address instanceof Inet4Address) {
                    interface_select.getItems().add(address);
                }
            }
        }

        interface_select.getSelectionModel().selectFirst();
    }

    public void setGUI(GUI_Controller gui) {
        this.gui = gui;
    }

    public void login() {
        validEntered = true;
        String name = login_name_text.getText();
        if (name.equals(""))
            validEntered = false;

        String ip = interface_select.getSelectionModel().getSelectedItem().toString();
        String ipParts[] = ip.split("/");
        System.out.println(ipParts[1]);

        if (validEntered) {
            Stage currentWindow = (Stage) login_name_text.getScene().getWindow();
            if (!triedToLogin) {
                node = new Node(name, ipParts[1], gui);
            } else
                node.setName(name);

            headController.setNode(node);
            headController.toLoading();
            node.bootstrap();
            /*while ((node.getLoggedIn() == false)&& !nodeExist) {
                //check if name already exits
                //if name already exits stop and warn the user
                //else wait for bootstrap, etc.
            }*/
            headController.closeLogin();
        } else {
            errorLabel.setText("Please enter a name");
        }
    }

    public void view(Parent root){
        if(view == null)
            view = new Scene(root,viewWidth,viewHeight);
        if(stage == null)
            stage = new Stage();

        stage.setResizable(false);
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(final WindowEvent arg0) {
                System.exit(0);
            }
        });
        stage.setTitle("SystemY Login");
        stage.setScene(view);
        stage.show();
    }

    public void textEnterd(){
        errorLabel.setText("");
    }

    public void close(){
        stage.close();
    }

}
