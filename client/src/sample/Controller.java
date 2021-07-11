package sample;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import sample.main.ClientMain;
import sample.main.Constants;
import sample.main.MethodHelper;
import sample.main.Server;

import javax.swing.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;

public class Controller {
    public TextField tfCommand;
    public ComboBox cbCommandsTypes;
    public Button btnChoose;
    public  TextArea taMessages;
    public ListView lvServers;
    public Label labelSelectedServer;
    public TextField tfpass;
    public Button btnstart;
    public Button btnUpdatePass;
    public TextField tfhmacPass;
    public TextField tfname;
    public RadioButton rbNoAttack;
    public RadioButton rbWrongNonce;
    public RadioButton rbWrongTime;

    private List<Server> selectedServers;
    private ClientMain clientMain;
    public void initialize(){
        ToggleGroup toggleGroup = new ToggleGroup();
        rbNoAttack.setToggleGroup(toggleGroup);
        rbWrongNonce.setToggleGroup(toggleGroup);
        rbWrongTime.setToggleGroup(toggleGroup);

        ObservableList<String> commandsTypes = FXCollections.observableArrayList(Constants.UPLOAD,Constants.DOWNLOAD,Constants.DELETE,Constants.MOVE,Constants.GET_LIST,Constants.GET_VERSION,Constants.GET_VERSION_ALL);
        cbCommandsTypes.setItems(commandsTypes);
        updateServersList();
        setDisableToCommand(false);
        selectedServers = new LinkedList<>();
        clientMain =new ClientMain(this);
        ClientMain.attack=Constants.NO_ATTACK;
    }



    /***
     * send command to server from tfCommand TextField
     * @param keyEvent
     */
    public void SendCommand(KeyEvent keyEvent) {

        String commandType="";
        List<String> cmd= new LinkedList<>();
        int indexcb=cbCommandsTypes.getSelectionModel().getSelectedIndex();
        if (indexcb!=-1) {
            commandType = cbCommandsTypes.getSelectionModel().getSelectedItem().toString();
        }
        else{cbCommandsTypes.requestFocus(); return;}

        if (tfpass.getText().equals("")){
            tfpass.requestFocus();return;
        }

        if (tfhmacPass.getText().equals("")){
            tfhmacPass.requestFocus();return;
        }

        int index=lvServers.getSelectionModel().getSelectedIndex();
        if (index==-1&&!commandType.equals(Constants.GET_VERSION_ALL)) {
            lvServers.requestFocus();
            return;
        }

        String msg=tfCommand.getText().replace("\n","").trim();
        if(keyEvent.getCode().equals(KeyCode.ENTER)){

            if (commandType.equals(Constants.UPLOAD)){
               String filePath= ClientMain.openFileDialog();
                if (filePath.equals("")){openErrorDialog("File ERROR","Please Choose File");return;}
                String slash=msg.equals("")?"":"/";
                cmd.add(commandType+" "+msg+slash+MethodHelper.getFileNameFromRealPath(filePath));
                cmd.add(filePath);
            }
            if (commandType.equals(Constants.GET_VERSION_ALL)){

                this.selectedServers.clear();
                this.selectedServers=Server.getAllServers();
                commandType=Constants.GET_VERSION;
                cmd.add(commandType+" "+msg);
            }
            else
                cmd.add(commandType+" "+msg);

            printMessage(cmd.get(0));
            clientMain.sendOutServer(selectedServers,cmd);
        }
        tfCommand.clear();

    }

    /***
     * print message in the screen
     * @param msg
     */
    public void printMessage(String msg){
        String server="All Servers";
        if (selectedServers.size()==0)
            server="";
        if (selectedServers.size()==1)
            server= selectedServers.get(0).toString();

        String messagePrint="  To   "+server +"  >    "+msg;
        taMessages.appendText(  messagePrint+"\n");
    }


    /***
     * print on the screen
     * @param from server
     * @param msg string message
     */
    public  void printServerResponse(String from,String msg){
        String messagePrint="  From   "+from +"  >    "+msg;
        try {
            taMessages.appendText(  messagePrint+"\n");
        }catch (Exception ex){
            System.out.println(ex);
        }
    }

    /***
     * print error on the screen
     * @param server
     * @param error
     */
    public void printError(String server, String error) {
        String messagePrint="                                "+error+"     "+server ;
        taMessages.appendText(  messagePrint+"\n");
    }



    /***
     * //get neighbors data from Neighbors.txt
     */
    private void updateServersList(){
     List<Server> servers= Server.getAllServers();
        lvServers.getItems().clear();

        if (servers.size()==0)
        {openErrorDialog("File ERROR","No such file or directory Servers.txt "); return;}

        for(Server s:servers){
            lvServers.getItems().add(s);
        }

    }

    /***
     * choose server from the list
     * @param actionEvent
     */
    public void chooseServer(ActionEvent actionEvent) {

        if (lvServers.getItems().size()==0)
            return;
        try{
            int inddex=lvServers.getSelectionModel().getSelectedIndex();
                this.selectedServers.clear();
                Server server;
                String serverName=lvServers.getItems().get(inddex).toString().split(" ")[0];
                String[] serverString=lvServers.getItems().get(inddex).toString().split(" ")[1].split(":");
                server=new Server(serverName,serverString[0],serverString[1]);
                labelSelectedServer.setText(server.toString());
                this.selectedServers.add(server);


        } catch (ArrayIndexOutOfBoundsException ex){

        }

    }


    /***
     * disable the commands
     * @param value
     */
    private void setDisableToCommand(boolean value){
        cbCommandsTypes.setDisable(value);
        tfCommand.setDisable(value);
        taMessages.setDisable(value);
        cbCommandsTypes.setDisable(!value);
        tfCommand.setDisable(!value);

    }

    /**
     * error dialog
     * @param title
     * @param error
     */
    public void openErrorDialog(String title,String error){
        JFrame frame=new JFrame();
        JOptionPane.showMessageDialog(frame, error, title, JOptionPane.WARNING_MESSAGE);
    }


    /***
     * check selected from list
     * @param actionEvent
     */
    public void checkSeletcted(ActionEvent actionEvent) {
        String commandType;
        int indexcb=cbCommandsTypes.getSelectionModel().getSelectedIndex();
        if (indexcb!=-1) {
            commandType = cbCommandsTypes.getSelectionModel().getSelectedItem().toString();
        }
        else{cbCommandsTypes.requestFocus(); return;}

        if (commandType.equals(Constants.GET_VERSION_ALL))
            labelSelectedServer.setText("All Servers");
        else {
            int inddex=lvServers.getSelectionModel().getSelectedIndex();

            if (inddex==-1) {
                lvServers.requestFocus();return;
            }

            this.selectedServers.clear();
            Server server;
            String serverName=lvServers.getItems().get(inddex).toString().split(" ")[0];
            String[] serverString=lvServers.getItems().get(inddex).toString().split(" ")[1].split(":");
            server=new Server(serverName,serverString[0],serverString[1]);
            this.selectedServers.add(server);
            labelSelectedServer.setText(server.toString());

        }


    }

    /***
     * start the app
     * @param actionEvent
     */
    public void start(ActionEvent actionEvent) {
        if(btnstart.getText().equals("start")) {

            if (tfname.getText().equals("")){
                tfname.requestFocus();return;
            }
            if (tfpass.getText().equals("")){
                tfpass.requestFocus();return;
            }
            if (tfhmacPass.getText().equals("")){
                tfhmacPass.requestFocus();return;
            }

            MessageDigest mesDigest = null;
            try {
                mesDigest = MessageDigest.getInstance(Constants.SHA256);
            } catch (NoSuchAlgorithmException e) {
                System.out.println("error password");
            }
            String sharedPass=  MethodHelper.bytesToHex(mesDigest.digest(tfpass.getText().getBytes(StandardCharsets.UTF_8)));
            String hmacPass=  MethodHelper.bytesToHex(mesDigest.digest(tfhmacPass.getText().getBytes(StandardCharsets.UTF_8)));
            ClientMain.sharedPass=sharedPass;
            ClientMain.hmacPass=hmacPass;
            ClientMain.name=tfname.getText();
            btnstart.setText("stop");
            tfpass.setDisable(true);
            tfhmacPass.setDisable(true);
            tfname.setDisable(true);
            cbCommandsTypes.setDisable(false);
            tfCommand.setDisable(false);
        }
        else {
            btnstart.setText("start");
            tfpass.setDisable(false);
            tfhmacPass.setDisable(false);
            tfname.setDisable(false);
            cbCommandsTypes.setDisable(true);
            tfCommand.setDisable(true);
        }


    }

    /***
     * change attack bu radio button
     * @param actionEvent
     */
    public void attackSelect(ActionEvent actionEvent) {

        if (rbNoAttack.isSelected())
            ClientMain.attack=Constants.NO_ATTACK;

        if (rbWrongNonce.isSelected())
            ClientMain.attack=Constants.WRONG_NONCE;

        if (rbWrongTime.isSelected())
            ClientMain.attack=Constants.WRONG_TIME;

    }
}
