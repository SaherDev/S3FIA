package sample.out;
import sample.Server.ServerMain;
import sample.db.SQLiteDB;
import sample.main.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class OutThread extends Thread {
    Server server;
    String cmd;
    Socket client ;
    private OutputStream outputStream;
    private InputStream inputStream;
    private String incomeFromServerDec ="none";
    String filesDirectory=ServerMain.fileRoot.trim();
    String sessionKey ="";
    String privateKey=ServerMain.serverPrivateKey;
    String serverName=ServerMain.serverName;
    String attack=ServerMain.attack.trim();
    int version;

    public OutThread(Server server, String cmd){
        this.server=server;
        this.cmd=cmd;
    }

    public OutThread(Server server, String cmd,int version){
        this.server=server;
        this.cmd=cmd;
        this.version=version;
    }

    @Override
    public void run() {
        {
            PrintWriter pw =  null;
            BufferedReader br = null;

            try {
                InetAddress serverAddress = InetAddress.getByName(server.getIp());
                client  = new Socket(serverAddress, Integer.parseInt(server.getPort()));
                outputStream=client.getOutputStream();
                inputStream=client.getInputStream();
                pw = new PrintWriter(outputStream, true);
                br = new BufferedReader(new InputStreamReader(inputStream));
                IvParameterSpec iv = Crypto.generateIV();

                challenge(pw,br);
                pw.println(Message.createEncMessageToServer(sessionKey,iv,cmd));
                ServerMain.printToScreen(server.toString(),"OUT",cmd,MethodHelper.bytesToHex(iv.getIV()),"","");

                String[] cmdSplt=cmd.split(" ",2);

                String incomeInc="";
                incomeInc  =br.readLine();

                if (incomeInc.split(" ").length==1)
                    throw  new SocketException();

                if (incomeInc.split(" ").length<2){
                    throw new encryptionException(Constants.ERROR+" "+Constants.DECRYPTION);
                }

                if (incomeInc.split(" ").length==4&&incomeInc.split(" ")[0].equals("S")){
                    incomeInc=incomeInc.substring(incomeInc.indexOf(" ")+1);
                }
                String ivIncome=incomeInc.split(" ")[0];
                String cmdInc=incomeInc.split(" ")[1];

                //decrypt the message
                String cmdDec=Crypto.decryptMessage(Constants.AES_GCM, sessionKey,ivIncome,cmdInc);

                if(cmdDec.equals(""))throw new IOException(Constants.ERROR+" "+Constants.DECRYPTION);
                this.incomeFromServerDec =cmdDec;

                switch (cmdSplt[0].trim()){
                    case Constants.DOWNLOAD:
                        if (incomeFromServerDec.equals(Constants.OK)){
                            download(cmdSplt[1]);}
                        break;

                }

                ServerMain.printToScreen(server.toString(),"IN ", incomeFromServerDec,MethodHelper.bytesToHex(MethodHelper.stringToBytes(ivIncome)),"","VALID");
                try { br.close(); } catch (Exception ex) {}
                try { pw.close(); } catch (Exception ex) {}


            }  catch (UnknownHostException |SocketException|NullPointerException e) {
                ServerMain.printToScreen(server.toString(),"OUT","CONNECTION FAILED","","","");
            } catch (IOException  e) {
                ServerMain.printToScreen(server.toString(),"OUT",e.getMessage(),"","","");
            } catch (encryptionException  e) {
                ServerMain.printToScreen("","",e.getMessage(),"","","");
            }

        }
        try { client.close(); } catch (Exception ex) {}
    }

    /***
     * start challenge response out to other servers
     * @param pw PrintWriter
     * @param br BufferedReader
     * @throws IOException IOException
     * @throws NullPointerException NullPointerException
     * @throws encryptionException encryptionException
     */
    private void challenge(PrintWriter pw, BufferedReader br) throws IOException, NullPointerException, encryptionException {

        String nonce="";
        String encMsgIn="";
        String encMsgOut="";
        String recPublicKey="";
        String sessKey="";
        String currenTime="";

        //step 1 send severname
        pw.println(serverName);
        ServerMain.printToScreen(server.toString(),"OUT",serverName,"","","");
        encMsgIn=br.readLine();

        //step 2 get nonce from server 2
        nonce=Crypto.decryptRSA(Constants.RSA_ECB,encMsgIn,privateKey);
        if (nonce.equals(""))
            throw  new encryptionException(Constants.ERROR+" "+Constants.DECRYPTION);
        ServerMain.printToScreen(server.toString(),"IN",nonce,"","","");

        //attack change nonce
        if (attack.equals(Constants.WRONG_NONCE))
            nonce=generateNonce();

        recPublicKey=ServerMain.getPassword(ServerMain.getServerName(server.getIp(),server.getPort())).split(" ")[1];
        currenTime=getCurrentTime();

        //attack change time
        if (attack.equals(Constants.WRONG_TIME))
            currenTime= generateWrongTime();

        sessKey=MethodHelper.bytesToHex(Crypto.generatekey());
        encMsgOut=MethodHelper.bytesToString(Crypto.encryptRSA(Constants.RSA_ECB,nonce +" "+currenTime+" "+sessKey,recPublicKey));

        //step 3 send nonce + time + session key
        pw.println(encMsgOut);
        sessionKey=sessKey;
        ServerMain.printToScreen(server.toString(),"OUT",nonce +" "+currenTime+" "+sessKey,"","","");

    }

    /***
     * generate new nonce 16b
     * @return string hex nonce
     */
    private  static String generateNonce() {
        Random random = ThreadLocalRandom.current();
        byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);
        return MethodHelper.bytesToHex(Base64.getUrlEncoder().encode(randomBytes));
    }

    /***
     * generate wrong datetime for attack
     * @return string time string format
     */
    private static String generateWrongTime(){
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyyHH:mm:ss");
        Date date = new Date();
        date.setHours(date.getHours()-1);
        return formatter.format(date);

    }

    /***
     * string currrent time
     * @return string time string format
     */
    private static String getCurrentTime(){
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyyHH:mm:ss");
        Date date = new Date();
        return formatter.format(date);

    }
    /***
     * download file from other servers
     * @param fp filepath
     * @throws IOException
     */
    private void download(String fp) throws IOException{

        String filePath=filesDirectory+fp;
        BufferedInputStream fileReader = new BufferedInputStream(this.inputStream);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1];
        String hash="";
        int nRead;
        while ((nRead = fileReader.read(data, 0, data.length)) != -1) { buffer.write(data, 0, nRead);   }
        buffer.flush();
        byte[] byteArray= buffer.toByteArray();
        byte[] fileData;

        String iv= MethodHelper.bytesToString(MethodHelper.splitByte(byteArray," ".getBytes()).get(0));

        byte[] fileDec=MethodHelper.splitByte(byteArray," ".getBytes()).get(1);

        //decrypt the file from the server
        fileData=Crypto.decryptFile(Constants.AES_GCM, sessionKey,iv,fileDec);

        if (Arrays.equals(fileData,new  byte[0])) throw new IOException(Constants.ERROR+" "+Constants.FILE_DECRYPTION);
        ServerMain.printToScreen(server.toString(),"IN ","file: "+fp,MethodHelper.bytesToHex(MethodHelper.stringToBytes(iv)),"","");
        hash= MethodHelper.fileHash(fileData);
        fileReader.close();
        Path path = Paths.get(filePath);
        Path parentDir = path.getParent();
        if (!Files.exists(parentDir))
            Files.createDirectories(parentDir);

        //save the file with offered version
        if (!MethodHelper.isFiletExist(filePath))
            SQLiteDB.addFile(fp,hash,version);
        else
            SQLiteDB.updateVersionHash(fp,version,hash);

        LockRegistry.INSTANCE.acquire(filePath, LockRegistry.LockType.WRITE);  //lock file to write
        Files.write(path, fileData);
        LockRegistry.INSTANCE.release(filePath, LockRegistry.LockType.WRITE);  //release the file lock

    }

    public String getResult() {
        return incomeFromServerDec;
    }



}

