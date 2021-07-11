package sample.Server;
import javafx.application.Platform;
import sample.Controller;
import sample.db.SQLiteDB;
import sample.main.*;
import sample.out.OutThread;
import java.io.IOException;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ServerMain {

    static InThread inThraed;
    static ServerSocket listenerServer;
    private static Controller controller;
    public static List<Server> servers;
    public static List<User> users;
    public static String fileRoot="Files/";
    public static String myIpPort ="";
    public static String serverName ="";
    public static String serverPrivateKey ="";
    public static String serverPublicKey ="";
    public static String hmacPassword="";
    public static String attack="";
    public ServerMain(Controller controller,String fileRoot) {
        this.controller=controller;
        this.fileRoot=fileRoot;
        servers= new LinkedList<Server>();
        users = new LinkedList<>();
    }

    /**
     *  print message to screen
     * @param client Client ip
     * @param fromTo IN | OUT
     * @param pMsg  the message to print
     */
    public static void printToScreen(String client,String fromTo,String pMsg,String iv,String hmac,String valid){
        //,String hmac,Boolean validhmac
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String fromToClient=fromTo+"   |   "+client+"   |   ";
        if (client.equals(""))
            fromToClient="";

        if (pMsg.contains(Constants.HASH))
            pMsg=pMsg.replace(Constants.HASH,"");

        if (pMsg.contains(Constants.OFFER))
            pMsg=pMsg.substring(0,pMsg.lastIndexOf(" "));

        String msg=currentTime+"   |   "+fromToClient+pMsg;

        String finalMsg = msg;
        Platform.runLater( () -> controller.printToScreen(finalMsg));

        msg= msg.replace('\n',';');
        if (!iv.equals(""))msg=msg+"   |   "+iv;
        if (!hmac.equals(""))msg=msg+"   |   "+hmac;
        if (!valid.equals(""))msg=msg+"   |   "+valid;
        Log.addLine(msg);
    }

    /***
     * send to other servers offer/move/delete
     * @param cmd message to other servers
     */
    public static void sendOutToAllServers(String cmd){
        for (Server s:servers){ new OutThread(s,cmd).start();}
    }

    /****
     * req file to download from server
     * @param pServers server(ip,port)
     * @param cmd the cmd to send (Downloads path/file)
     * @param version version>0 to save in file index when download the file
     */
    public static void requestDownloadFromServer(Server pServers, String cmd, int version){
        new OutThread(pServers,cmd,version).start();
    }

    /***
     * update list of all servers from Servers.txt
     * @param s
     */
    public static void updateServersList(List<Server> s){
        servers.clear();
        servers.addAll(s);
        Server.updateServerData(servers);
    }

    public static void updateUserList(List<User> s){
        users.clear();
        users.addAll(s);
    }

    /**
     * open new ServerSocket
     * start liten to given ip + port
     * @param port port
     * @param ip ip4Address (0.0.0.0)
     */
    public static void startIn(int port,InetAddress ip){
        try {

            SQLiteDB.connect("FileServer");
            listenerServer = new   ServerSocket(port,4,ip);
            myIpPort =ip.toString().replace("/","")+":"+port;

            //start up sync
            FilesSync filesSync=new FilesSync();
            filesSync.start();
            filesSync.join();

        } catch (IOException | InterruptedException e) {System.out.println("Error  startIn"+ e.getMessage());    }
        inThraed =new InThread(listenerServer);
        inThraed.start();
        printToScreen("","","start listening to  =>  "+ myIpPort,"","","");
    }



    /**
     * stop listen to the port and close the  ServerSocket
     */
    public static void stopIn(){

        try {
            System.out.println("server stop");
            inThraed.interrupt();
            listenerServer.close();
            printToScreen("","","stop listening","","","");
        } catch (IOException |NullPointerException e) {
            System.out.println("Error  stopIn"+ e.getMessage());
        }
    }



    /**
     *
     * @return local IP addresses from the network interface listing
     */
    public static List<InetAddress> localIpAddress(){

        List<InetAddress> addrList = new ArrayList<InetAddress>();
        try {
            addrList.add(InetAddress.getByAddress(new byte[]{0, 0, 0, 0}));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        Enumeration<NetworkInterface> interfaces = null;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        InetAddress localhost = null;

        try {
            localhost = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        while (interfaces.hasMoreElements()) {
            NetworkInterface ifc = interfaces.nextElement();
            Enumeration<InetAddress> addressesOfAnInterface = ifc.getInetAddresses();

            while (addressesOfAnInterface.hasMoreElements()) {
                InetAddress address = addressesOfAnInterface.nextElement();

                if (!address.equals(localhost) && !address.toString().contains(":")) {
                    addrList.add(address);
                }
            }
        }

        return addrList;
    }

    /***
     * get server name by ip
     * @param serverIp server ip
     * @return server name as string
     */
    public static String getServerName(String serverIp){
        String name="";
        for (Server s: servers){
            if (s.getIp().trim().equals(serverIp.trim()))
                name=s.getName();
        }
        return name;
    }

    /***
     * get server name
     * @param serverIp server ip
     * @param port server port
     * @return string server name
     */
    public static String getServerName(String serverIp,String port){
        String name="";
        for (Server s: servers){
            if (s.getIp().trim().equals(serverIp.trim())&&s.getPort().equals(port))
                name=s.getName();
        }
        return name;
    }

    /***
     * get password for client / server
     * @param name server /client pass
     * @return string pass or ""
     */
    public static String getPassword(String name){

        for (User usr:users ) {
            if (usr.getName().equals(name))
                return "U "+usr.getPass();
        }

        for (Server server:servers ) {
            if (server.getName().equals(name))
                return "S "+server.getPublicKey();
        }

        return "";
    }


    /***
     * get wrong pass for attack
     * @param name
     * @return
     */
    public static String getWrongPassword(String name){

        List<Server> tmpServers= new LinkedList<>();
        for (Server server:servers ) {
            if (!server.getName().equals(name))
                tmpServers.add(server);
        }
        return tmpServers.get(0).getPublicKey();
    }
}





