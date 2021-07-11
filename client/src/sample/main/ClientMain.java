package sample.main;
import javafx.application.Platform;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import sample.Controller;
import sample.out.OutThread;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ClientMain {

    private static Controller controller;
    public static String sharedPass="";
    public static String hmacPass="";
    public static String name="";
    public static String attack="";

    public ClientMain(Controller controller) {this.controller=controller;}

    /***
     *  send cmd out to list of servers
     * @param servers server[ip:port]
     * @param cmd string cmd to send to server
     */
    public void sendOutServer(List<Server> servers,List<String > cmd){
        for (Server s:servers){ new OutThread(s,cmd).start();}
    }

    /***
     *  print message in the screen and add new ine to logfile
     * @param from server name
     * @param res the mesSAGE
     * @param iv iv paramater
     * @param hmac hmac
     * @param valid if the messge valid or not
     */
    public static void printFromServer(String from,String res,String iv,String hmac,String valid){
        Platform.runLater( () -> controller.printServerResponse(from,res));
        addLogLine(from,"IN ",res,iv, hmac, valid);
    }

    /***
     * print error message
     * @param server server name
     * @param error the error message
     */
    public static void printError(String server,String error){
        Platform.runLater( () ->controller.printError(server,error));
        addLogLine(server,"",error,"","","");
    }


    /***
     * add new line to log file
     * @param client client/server ip:port
     * @param fromTo out/in
     * @param pMsg the message
     * @param iv iv
     * @param hmac hmac
     * @param valid if valid or not
     */
    public static void addLogLine(String client,String fromTo,String pMsg,String iv,String hmac,String valid){
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String fromToClient=fromTo+"   |   "+client+"   |   ";
        if (fromTo.equals(""))
            fromToClient="";

        String msg=currentTime+"   |   "+fromToClient+pMsg;
        msg= msg.replace('\n',';');
        if (!iv.equals(""))msg=msg+"   |   "+iv;
        if (!hmac.equals(""))msg=msg+"   |   "+hmac;
        if (!valid.equals(""))msg=msg+"   |   "+valid;
        Log.addLine(msg);
    }



    /***
     * open file dialog to choose file
     * @return
     */
    public static  String openFileDialog(){
        String filePath="";
        Stage theStage = null;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose File ");
        fileChooser.setInitialDirectory(new File("."));
        File chosenFile = fileChooser.showOpenDialog(theStage);

        if ( chosenFile != null){ filePath=chosenFile.toString();}
        return filePath;
    }


    /***
     * save file data to chosen file path
     * @param fileData
     * @param fileName
     * @throws IOException
     */
    public static  void saveFileToDirectory(byte[] fileData,String fileName) throws IOException {
        String directory="";
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose Directory");
        directoryChooser.setInitialDirectory(new File("."));
        File selectedDirectory = directoryChooser.showDialog(null);
        directory= selectedDirectory.getAbsolutePath();
        if (directory.equals("")){return;}
        Path path = Paths.get(directory+"/"+fileName);
        Files.write(path, fileData);
    }

}



