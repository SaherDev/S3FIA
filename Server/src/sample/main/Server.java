package sample.main;
import java.io.*;
import java.nio.file.Files;
import java.security.DigestOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class Server {
    private String ip;
    private String port;
    private String name;
    private String publicKey;
    private static final String FILENAME = "Servers.txt";

    public Server(String ip,String port){
        this.ip=ip;
        this.port=port;
    }
    public Server(String name,String ip,String port,String publicKey) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.publicKey=publicKey;
        }


    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }

    public String getPublicKey() { return publicKey;  }

    /***
     * get all servers ip addresses from Servers.txt file
     * @return list<Server> of servers
     */
    public static List<Server> getAllServers() {
        List<Server> servers = new LinkedList<Server>();
        try {


            File f = new File(FILENAME);
            if(!f.exists()){
                f.createNewFile();}

            FileInputStream fis = new FileInputStream(f);
            Scanner sc = new Scanner(fis);
            while (sc.hasNextLine()) {
                String[] server=sc.nextLine().split("=");
                if (server.length<3) continue;
                String name=server[0];
                String serverIpPort=server[1];
                String publicKey=server[2];
                if (MethodHelper.validPIpPort(serverIpPort)&&MethodHelper.validServerName(name)){
                    String ipPortArray[]=serverIpPort.split(":");
                    servers.add(new Server(name,ipPortArray[0],ipPortArray[1],publicKey));
                }
            }
            sc.close();
        } catch (IOException | ArrayIndexOutOfBoundsException e) {
            System.out.println("Erorr getAllServerss"+e.getMessage());
            return new LinkedList<Server>() ;
        }
        return servers;
    }

    /***
     * update serverdata
     * @param servers servers list
     */
    public static void updateServerData(List<Server> servers){

        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new FileWriter(FILENAME));
        } catch (IOException e) {
            System.out.println("error updateServerData");
        }
        pw.write("");
        for (Server server :servers) {
            pw.write((server.toString()+" "+ server.getPublicKey()).replace(" ","=")+"\n");
        }

        pw.close();
    }

    @Override
    public String toString() {
        return getName()+" "+getIp()+ ":" + getPort() ;
    }



}
