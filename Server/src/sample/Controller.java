package sample;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import sample.Server.ServerMain;
import sample.main.*;
import javax.swing.*;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;

public class Controller {
    public TextArea taMessages;
    public Button btnStartStop;
    public TextField tfPort;
    public ListView lvLocalAddresses;
    public Label lbIp;
    public Button btnUpdatePass;
    public ListView lvServers;
    public TextField tfServerIpPort;
    public TextField tfServerName;
    public TextField tfPrivateKey;
    public TextField tfPublicKey;
    public TextField tfhmacPass;
    public TextField tfServerPublicPass;
    public ListView lvUsers;
    public Button btnUpdateUser;
    public TextField tfUserName;
    public TextField tfUserPass;
    public TextField tfUserPassView;
    public TextField tfServerPassView;
    public TextField tfName;
    public RadioButton rbNoAttack;
    public RadioButton rbWrongNonce;
    public RadioButton rbWrongTime;
    public RadioButton rbWrongPublicKey;
    private ServerMain serverMain;
    private int port;
    List<InetAddress> myIpAddresses;
    MessageDigest mesDigest = null;
    public void initialize(){
        ToggleGroup toggleGroup = new ToggleGroup();
        rbNoAttack.setToggleGroup(toggleGroup);
        rbWrongNonce.setToggleGroup(toggleGroup);
        rbWrongTime.setToggleGroup(toggleGroup);
        rbWrongPublicKey.setToggleGroup(toggleGroup);
        try {
            mesDigest = MessageDigest.getInstance(Constants.SHA256);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("error password");
        }
        updateAppData();
        updateLocalAddresses();
        updateServersList();
        tfPort.setText(port+"");
        tfServerPassView.setDisable(true);
        tfUserPassView.setDisable(true);
    }


    /***
     * update Local Addresses
     */
    private void updateLocalAddresses() {
        myIpAddresses=ServerMain.localIpAddress();
        for (InetAddress ip:myIpAddresses) {
            lvLocalAddresses.getItems().add(ip.toString().replace("/",""));
        }
    }

    /**
     * get data from config files
     */
    private void updateAppData() {
        btnStartStop.setDisable(true);
        lvLocalAddresses.setDisable(true);
        List<String> data=AppData.getAppData();
        if (data.size()<2||!MethodHelper.validPort(data.get(1).trim())||!data.get(0).trim().endsWith("/")) { openErrorDialog("File ERROR","File Data Error"); return;}
        serverMain= new ServerMain(this,data.get(0));

        if (data.size()==7){
        ServerMain.serverPrivateKey =data.get(2);
        ServerMain.serverPublicKey = data.get(3);
        ServerMain.hmacPassword= data.get(4);
        ServerMain.serverName = data.get(6);

        String[] users =data.get(5).split(";");
        List<User> users1= new LinkedList<>();


            for (String user :  users) {
                String[] split=user.split("=");
                if (split.length!=2)
                    continue;
                users1.add(new User(split[0],split[1]));
                lvUsers.getItems().add(split[0]+" "+split[1]);
            }
          ServerMain.updateUserList(users1);
        }

        port=Integer.parseInt(data.get(1));
        tfName.setText(ServerMain.serverName );
        btnStartStop.setDisable(false);
        lvLocalAddresses.setDisable(false);
    }



    /***
     * get neighbors data from Neighbors.txt
     */
    private void updateServersList(){

        List<Server> servers= Server.getAllServers();

        if (servers.size()==0)
        {openErrorDialog("File ERROR","No such file or directory Servers.txt "); return; }

        for (Server s: servers){

            lvServers.getItems().add(s+" "+s.getPublicKey());
        }

        ServerMain.updateServersList(servers);
    }

    /**
     * print message on the screen
     * @param msg string message
     */
    public void printToScreen(String msg) {
        taMessages.appendText(msg+"\n");
    }


    /**
     * start the server
     * @param actionEvent
     */
    public void Start(ActionEvent actionEvent) {

        if(btnStartStop.getText().equals("start")) {
            if (!MethodHelper.validPort(tfPort.getText())) { btnStartStop.requestFocus(); return;}
            int index=lvLocalAddresses.getSelectionModel().getSelectedIndex();
            if (index==-1){lvLocalAddresses.requestFocus();return;}
            if (ServerMain.serverPrivateKey.equals("")){ tfPrivateKey.requestFocus();return;}
            if (ServerMain.serverPublicKey.equals("")){ tfPublicKey.requestFocus();return;}
            if (ServerMain.hmacPassword.equals("")){ tfhmacPass.requestFocus();return;}
            if (ServerMain.serverName.equals("")){ tfName.requestFocus();return;}
            lbIp.setText(lvLocalAddresses.getItems().get(index).toString());
            btnStartStop.setText("stop");
            ServerMain.startIn(Integer.parseInt(tfPort.getText()),myIpAddresses.get(index));
            AppData.setVariable(1,tfPort.getText());
            AppData.setVariable(6,tfName.getText());
            ServerMain.serverName=tfName.getText();
            tfPort.setDisable(true);
            lvLocalAddresses.setDisable(true);
            tfPrivateKey.setDisable(true);
            tfPublicKey.setDisable(true);
            tfServerName.setDisable(true);
            tfName.setDisable(true);
            tfServerIpPort.setDisable(true);
            tfUserName.setDisable(true);
            tfUserPass.setDisable((true));
            tfhmacPass.setDisable(true);
            tfServerPublicPass.setDisable(true);
        }
        else {
            btnStartStop.setText("start");
            ServerMain.stopIn();
            tfPort.setDisable(false);
            lvLocalAddresses.setDisable(false);
            tfPrivateKey.setDisable(false);
            tfName.setDisable(false);
            tfPublicKey.setDisable(false);
            tfServerName.setDisable(false);
            tfServerIpPort.setDisable(false);
            tfUserName.setDisable(false);
            tfUserPass.setDisable((false));
            tfhmacPass.setDisable(false);
            tfServerPublicPass.setDisable(false);
        }
    }

    /***
     * update the server/client server/server mac password
     * @param actionEvent
     */
    public void updatePassword(ActionEvent actionEvent) {
        
        if (tfPrivateKey.getText().equals("")&&tfPublicKey.getText().equals("")&&tfhmacPass.getText().equals("")){
            tfPrivateKey.requestFocus();
        return;
        }

        String privatePass="";
        String publicPass="";


        if (!tfPrivateKey.getText().equals("")){
            privatePass= tfPrivateKey.getText();
            ServerMain.serverPrivateKey =privatePass;
            AppData.setVariable(2,privatePass);
            tfPrivateKey.setText("");
        }
        if (!tfPublicKey.getText().equals("")){
            publicPass=  tfPublicKey.getText();
            ServerMain.serverPublicKey = publicPass;
            AppData.setVariable(3,publicPass);
            tfPublicKey.setText("");
        }

        if (!tfhmacPass.getText().equals("")){
            publicPass=  MethodHelper.bytesToHex(mesDigest.digest(tfhmacPass.getText().getBytes(StandardCharsets.UTF_8)));
            ServerMain.hmacPassword= publicPass;
            AppData.setVariable(4,publicPass);
            tfhmacPass.setText("");
        }
        
    }

    /***
     * error meesgae
     * @param title titile
     * @param error the error message
     */
    public void openErrorDialog(String title,String error){
        JFrame frame=new JFrame();
        JOptionPane.showMessageDialog(frame, error, title, JOptionPane.WARNING_MESSAGE);
    }

    /***
     * choose server from the list
     * @param mouseEvent
     */
    public void chooseServer(MouseEvent mouseEvent) {
            int inddex=lvServers.getSelectionModel().getSelectedIndex();
            String[] servers=lvServers.getItems().get(inddex).toString().split(" ");
            tfServerName.setText(servers[0]);
            tfServerIpPort.setText(servers[1]);
            tfServerPassView.setText(servers[2]);
            tfServerPublicPass.setText("");
    }

    /***
     * update server data
     * @param actionEvent
     */
    public void setServersList(ActionEvent actionEvent) {

        if (tfServerName.getText().equals("")||tfServerIpPort.getText().equals("")){
            tfServerName.requestFocus();
            return;
        }

        if (!MethodHelper.validServerName(tfServerName.getText())){
            tfServerName.requestFocus();
            return;
        }

        if (!MethodHelper.validPIpPort(tfServerIpPort.getText())){
            tfServerIpPort.requestFocus();
            return;
        }

        int index=lvServers.getSelectionModel().getSelectedIndex();

        String serverPublicKey=tfServerPublicPass.getText();
        if (serverPublicKey.equals(""))
            serverPublicKey=tfServerPassView.getText();

        tfServerPassView.setText(serverPublicKey);
        lvServers.getItems().set(index,tfServerName.getText()+" "+ tfServerIpPort.getText()+" "+serverPublicKey );
        String ip=tfServerIpPort.getText().split(":")[0];
        String port=tfServerIpPort.getText().split(":")[1].split(" ")[0];
        ServerMain.servers.set(index,new Server(tfServerName.getText(),ip,port,serverPublicKey));
        Server.updateServerData(ServerMain.servers);

    }

    /***
     * save user data
     * @param actionEvent
     */
    public void setUsers(ActionEvent actionEvent) {

        if (tfUserName.getText().equals("")){
            tfUserName.requestFocus();
            return;
        }
        if (tfUserPass.getText().equals("")){
            tfUserPass.requestFocus();
            return;
        }

        if (!MethodHelper.validServerName(tfUserName.getText())){
            tfUserName.requestFocus();
            return;
        }

        int index=lvUsers.getSelectionModel().getSelectedIndex();

        if (index==-1)
            return;
        String UserPass="";
        UserPass=MethodHelper.bytesToHex(mesDigest.digest(tfUserPass.getText().getBytes(StandardCharsets.UTF_8)));
        tfUserPassView.setText(UserPass);
        lvUsers.getItems().set(index,tfUserName.getText()+" "+UserPass);
        ServerMain.users.set(index,new User(tfUserName.getText(),UserPass));
         String[] data = {""};
        ServerMain.users.forEach(o -> data[0] +=   o.toString()+";");
        AppData.setVariable(5, data[0]);


    }

    /***
     * choose user and view name + password
     * @param mouseEvent
     */
    public void chooseUser(MouseEvent mouseEvent) {

        int inddex=lvUsers.getSelectionModel().getSelectedIndex();
        String[] servers=lvUsers.getItems().get(inddex).toString().split(" ");
        tfUserName.setText(servers[0]);
        tfUserPassView.setText(servers[1]);
        tfUserPass.setText("");
    }

    /**
     * choose any attack
     * @param actionEvent
     */
    public void attackSelect(ActionEvent actionEvent) {
        if (rbNoAttack.isSelected())
            ServerMain.attack=Constants.NO_ATTACK;

        if (rbWrongPublicKey.isSelected())
           ServerMain.attack=Constants.WRONG_PUBLIC_KEY;

        if (rbWrongNonce.isSelected())
            ServerMain.attack=Constants.WRONG_NONCE;

        if (rbWrongTime.isSelected())
            ServerMain.attack=Constants.WRONG_TIME;



    }
}
