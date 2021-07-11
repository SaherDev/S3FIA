package sample.Server;
import sample.main.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class InChallengeHandler extends Thread{
    Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    String clientIp;
    private String chalengeResult ="none";
    private String serverPrivateKey=ServerMain.serverPrivateKey.trim();
    String attack=ServerMain.attack.trim();
    public Boolean isFromServer=false;
    public InChallengeHandler(Socket socket){
        this.socket=socket;
    }


    @Override
    public void run()
    {
        PrintWriter pw =  null;
        BufferedReader br = null;

        try {
            String name="";
            String pass="";
            String nonceOut="";
            String nonceIn="";
            String timeIn="";
            String currentTime="";

            String incomeInc="";
            String msgEnc="";
            String msgDec="";
            String iv="";

            inputStream=socket.getInputStream();
            outputStream=socket.getOutputStream();
            br = new BufferedReader(new InputStreamReader(inputStream));
            String remoteSocAdd=  socket.getRemoteSocketAddress().toString().substring(1);
            clientIp= InetAddress.getByName(remoteSocAdd.split(":")[0].trim()).toString().replace("/", "");
            pw = new PrintWriter(outputStream, true);

            //step 1 server /client name
            name= br.readLine();
            pass=ServerMain.getPassword(name);
            ServerMain.printToScreen(clientIp,"IN ",name, "","","");
            if (pass.equals(""))
                throw new IOException(Constants.ERROR+" "+Constants.CHALLENGE);

            if(pass.split(" ")[0].equals("S"))
                isFromServer=true;

            String opt=isFromServer?"S":"U";

            switch (opt){
                case "S": //server
                    pass=pass.split(" ")[1];

                    //attack change the server 2 public pass
                    if (attack.equals(Constants.WRONG_PUBLIC_KEY))
                        pass= ServerMain.getWrongPassword(name);

                    //step 2 send nonce
                    String nonceTmp=generateNonce();
                    nonceOut=MethodHelper.bytesToString(Crypto.encryptRSA(Constants.RSA_ECB,nonceTmp,pass));
                    pw.println(nonceOut);
                    ServerMain.printToScreen(clientIp,"OUT ",nonceTmp, "","","");

                    incomeInc= br.readLine();
                    if (incomeInc.equals(""))
                        throw new IOException(Constants.ERROR+" "+Constants.CHALLENGE);

                    msgDec=Crypto.decryptRSA(Constants.RSA_ECB,incomeInc,serverPrivateKey);
                    if(msgDec.equals(""))throw new encryptionException(Constants.ERROR+" "+Constants.DECRYPTION);
                    ServerMain.printToScreen(clientIp,"IN ",msgDec, "","","");

                    //step 3 get nonce + time + time
                    nonceIn =msgDec.split(" ")[0];
                    timeIn =msgDec.split(" ")[1];
                    String sessionKey=msgDec.split(" ")[2];
                    currentTime=getCurrentTime();

                    //servers attacks check
                    if (!nonceIn.equals(nonceTmp))
                        throw new challengeException(Constants.ERROR+" "+Constants.WRONG_NONCE);
                    if (!checkTimeperiod(currentTime,timeIn))
                        throw new challengeException(Constants.ERROR+" "+Constants.WRONG_TIME);

                    chalengeResult=sessionKey;
                    break;
                case "U": //user
                    pass=pass.split(" ")[1];
                    nonceOut=generateNonce();
                    //step 2 send nonce
                    pw.println(nonceOut);
                    ServerMain.printToScreen(clientIp,"OUT ",nonceOut, "","","");
                    incomeInc= br.readLine();

                    if (incomeInc.equals(""))
                        throw new IOException(Constants.ERROR+" "+Constants.CHALLENGE);

                    iv=incomeInc.split(" ")[0].trim();
                    msgEnc=incomeInc.split(" ")[1].trim();
                    msgDec=Crypto.decryptMessage(Constants.AES_CBC,pass,iv,msgEnc);
                    if(msgDec.equals(""))throw new encryptionException(Constants.ERROR+" "+Constants.DECRYPTION);
                    ServerMain.printToScreen(clientIp,"IN ",msgDec, "","","");
                    //step 3 get nonce + time
                    nonceIn =msgDec.split(" ")[0];
                    timeIn =msgDec.split(" ")[1];

                    currentTime=getCurrentTime();

                    //client attacks check
                    if (!nonceIn.equals(nonceOut))
                         throw new challengeException(Constants.ERROR+" "+Constants.WRONG_NONCE);
                    if (!checkTimeperiod(currentTime,timeIn))
                         throw new challengeException(Constants.ERROR+" "+Constants.WRONG_TIME);

                    chalengeResult=pass;

                    break;

            }


        }  catch (UnknownHostException |SocketException e) {
            ServerMain.printToScreen(clientIp,"OUT","CONNECTION FAILED","","","");
        } catch (IOException e) {
            ServerMain.printToScreen(clientIp,"OUT",e.getMessage(),"","","");
        }catch (encryptionException  e) {
            ServerMain.printToScreen("","",e.getMessage(),"","","");
        } catch (challengeException e) {
            pw.println("error");
            ServerMain.printToScreen("","",e.getMessage(),"","","");
        }


    }

    /***
     * generate nonce 16B
     * @return
     */
    private  static String generateNonce() {
        Random random = ThreadLocalRandom.current();
        byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);
        return MethodHelper.bytesToHex(Base64.getUrlEncoder().encode(randomBytes));
    }

    /**
     * check time
     * @param current
     * @param in
     * @return
     */
    public   static Boolean checkTimeperiod(String current,String in) {

        try {
            Date currentDate = new SimpleDateFormat("dd/MM/yyyyHH:mm:ss", Locale.ENGLISH).parse(current);
            Date inDate = new SimpleDateFormat("dd/MM/yyyyHH:mm:ss", Locale.ENGLISH).parse(in);
           if ((currentDate.getTime()-inDate.getTime())<=5000)
               return true;
        } catch (ParseException e) {
           return false;
        }
        return false;
    }

    /***
     * get current date time
     * @return
     */
    private static String getCurrentTime(){
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyyHH:mm:ss");
        Date date = new Date();
        return formatter.format(date);
    }
    public String getResult() {
        return chalengeResult;
    }
}
