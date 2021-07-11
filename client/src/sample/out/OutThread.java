package sample.out;
import javafx.application.Platform;
import sample.main.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class OutThread extends Thread {
     Server server;
     List<String> cmd;
     Socket client ;
     private OutputStream outputStream;
     private InputStream inputStream;
     String name=ClientMain.name;
     String serverSharedPass=ClientMain.sharedPass;
     String hmacPasswword=ClientMain.hmacPass;
     String attack=ClientMain.attack.trim();
     Boolean isvalidHmac =true;

    public OutThread(Server server,List<String> cmd){
        this.server=server;
        this.cmd=new LinkedList<>();
        this.cmd.addAll(cmd);
    }

    @Override
    public void run() {
        {

            PrintWriter pw =  null;
            BufferedReader br = null;
            String incomeFromServer="none";
            String msgOut="";
            String hmacOut="";

            try {

                InetAddress serverAddress = InetAddress.getByName(server.getIp());
                client  = new Socket(serverAddress, Integer.parseInt(server.getPort()));
                outputStream=client.getOutputStream();
                inputStream=client.getInputStream();
                pw = new PrintWriter(outputStream, true);
                br = new BufferedReader(new InputStreamReader(inputStream));


                //START CHALLENGE
                challenge(pw,br);

                IvParameterSpec iv = Crypto.generateIV();
                //encrypt the message using aes/cbc + client pass
                msgOut=Message.createMessage(serverSharedPass,hmacPasswword,iv,cmd.get(0));
                hmacOut=msgOut.split(" ",3)[2];
                pw.println(msgOut);
                ClientMain.addLogLine(server.toString(),"OUT",cmd.get(0),MethodHelper.bytesToHex(iv.getIV()),hmacOut,"");

                String incomeInc=br.readLine();
               if (incomeInc.split(" ").length<2)
                   throw  new SocketException();


                if (incomeInc.split(" ").length<3){
                    throw new encryptionException(Constants.ERROR+" "+Constants.DECRYPTION);
                }
                String ivIncome=incomeInc.split(" ")[0];
                String cmdInc=incomeInc.split(" ")[1];
                String hmac=incomeInc.split(" ")[2];

                byte[] ivcmdIncCombined=MethodHelper.concat(Crypto.getIvParameterSpec(ivIncome).getIV(),MethodHelper.stringToBytes(cmdInc));
                String hmacToCheck=MethodHelper.bytesToHex(Crypto.computeHMAC(hmacPasswword,ivcmdIncCombined));
                if (!hmacToCheck.equals(hmac)) isvalidHmac =false;


                //decrypt the message
                String cmdDec=Crypto.decryptMessage(Constants.AES_CBC,serverSharedPass,ivIncome,cmdInc);
                if (!cmdDec.equals("")) incomeFromServer =cmdDec;

                switch (cmd.get(0).split(" ")[0].trim()){
                    case Constants.UPLOAD:
                        if (incomeFromServer.equals(Constants.OK))
                            upload();
                        break;
                    case Constants.DOWNLOAD:
                        if (incomeFromServer.equals(Constants.OK))
                             download();
                        break;
                    case Constants.GET_LIST:
                        incomeFromServer="\n"+incomeFromServer.replace(';','\n');
                        break;
                }

                ClientMain.printFromServer(server.toString(),incomeFromServer,MethodHelper.bytesToHex(MethodHelper.stringToBytes(ivIncome)),hmac,isvalidHmac?"VALID":"INVALID");
                try { br.close(); } catch (Exception ex) {}
                try { pw.close(); } catch (Exception ex) {}


            }  catch (UnknownHostException e) {
                ClientMain.printError(server.toString(),"CONNECTION FAILED");
                System.out.println("UnknownHostException"+e.getMessage());
            } catch (SocketException  e) {
                ClientMain.printError(server.toString(),"CONNECTION FAILED");
                System.out.println("SocketException "+e.getMessage());
            } catch (IOException  e) {
                ClientMain.printError(server.toString(), "CONNECTION FAILED");
                System.out.println("IOException " + e.getMessage());
            } catch (encryptionException e) {
                ClientMain.printError(server.toString(),e.getMessage());

            }

        }
        try { client.close(); } catch (Exception ex) {}
    }


    /***
     * start challenge response with the server
     * @param pw
     * @param br
     * @throws IOException
     */
    private void challenge(PrintWriter pw, BufferedReader br) throws IOException {

        String nonce="";
        String encMsg="";
        String  hmacOut="";
        String time="";
        //step 1 send client name
        pw.println(name);
        ClientMain.addLogLine(server.toString(),"OUT",name,"","","");

        //step 2 get nonce
        nonce=br.readLine();
        ClientMain.addLogLine(server.toString(),"IN",nonce,"","","");

        //atack with wronge nonce
        if (attack.equals(Constants.WRONG_NONCE))
            nonce=generateNonce();

        IvParameterSpec iv = Crypto.generateIV();
        time=getCurrentTime();

        //attack with wrong time
        if (attack.equals(Constants.WRONG_TIME))
           time= generateWrongTime();

        encMsg=Message.createMessage(serverSharedPass,hmacPasswword,iv,nonce +" "+time);
        //step 3 send nonce + time
        pw.println(encMsg);
        hmacOut=encMsg.split(" ",3)[2];
        ClientMain.addLogLine(server.toString(),"OUT",nonce +" "+time,MethodHelper.bytesToHex(iv.getIV()),hmacOut,"");

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
     * start upload file to server
     * @throws IOException
     */
    private void upload() throws IOException {
        IvParameterSpec iv = Crypto.generateIV();
        //encrypt the file using aes/cbc + client password
        byte[] fileByte=  Message.createFile(serverSharedPass,hmacPasswword,iv,MethodHelper.readFileBytes(cmd.get(1))) ;
        int dataLength=fileByte.length;
        int count=0;
        byte[] fileChunk;
        while(count!=dataLength){
            int size = 1024*1024;
            if(dataLength - count >= size){count += size;}
            else{size = (int)(dataLength - count); count = dataLength;}
            fileChunk = new byte[size];
            System.arraycopy(fileByte,(count-size),fileChunk,0,size);
            outputStream.write(fileChunk);
        }
    }



    /***
     * start download file from server
     * @throws IOException
     */
    private void download() throws IOException,encryptionException {

        BufferedInputStream fileReader = new BufferedInputStream(inputStream);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1];
        int nRead;
        while ((nRead = fileReader.read(data, 0, data.length)) != -1) { buffer.write(data, 0, nRead);   }
        buffer.flush();
        byte[] byteArray= buffer.toByteArray();
        fileReader.close();
        byte[] fileData ;

        String iv= MethodHelper.bytesToString(MethodHelper.splitByte(byteArray," ".getBytes()).get(0));
        byte[] hmacAndFile=MethodHelper.splitByte(byteArray," ".getBytes()).get(1);
        String hmac=MethodHelper.bytesToHex(MethodHelper.splitByte(hmacAndFile," ".getBytes()).get(0));
        byte[] fileEnc=MethodHelper.splitByte(hmacAndFile," ".getBytes()).get(1);
        String hmacCheck=MethodHelper.bytesToHex(Crypto.computeHMAC(hmacPasswword,fileEnc));

        if (hmacCheck.equals(hmac))isvalidHmac=true;
        else isvalidHmac =false;

        //decrypt the file from the server
        fileData=Crypto.decryptFile(Constants.AES_CBC,serverSharedPass,iv,fileEnc);
        if (Arrays.equals(fileData,new  byte[0])) throw new encryptionException(Constants.ERROR+" "+Constants.DECRYPTION);

        String filePath=MethodHelper.getFileNameFromStringPath(cmd.get(0).split(" ",2)[1].trim());
        ClientMain.addLogLine(server.toString(),"IN ","file: "+filePath,MethodHelper.bytesToHex(MethodHelper.stringToBytes(iv)),hmac,isvalidHmac?"VALID":"INVALID");

        if (!isvalidHmac) throw new IOException(Constants.ERROR+" "+Constants.INVALID_HMAC);
        Platform.runLater(()->{
            try {
                ClientMain.saveFileToDirectory(fileData,filePath);
            } catch (IOException e) {
                System.out.println("error file save");
            }
        });

    }


}

