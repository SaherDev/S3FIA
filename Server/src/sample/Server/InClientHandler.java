package sample.Server;
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
import java.util.Arrays;
import java.util.List;

class InClientHandler extends Thread
{
    Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    String clientIp;
    String filesDirectory=ServerMain.fileRoot.trim();
    String sessionKey="";
    String hmacPassword=ServerMain.hmacPassword;
    Boolean isFromServer=false;
    Boolean isvalidHmac =true;
    public InClientHandler(Socket sock) {this.socket=sock;}




    @Override
    public void run()
    {
        PrintWriter pw =  null;
        BufferedReader br = null;

        try {
            //check challenge if succeed
            if (challenge().equals("none"))
                throw new challengeException(Constants.ERROR+" "+Constants.UNSUCCESSFUL_CHALLENGE);

            inputStream=socket.getInputStream();
            outputStream=socket.getOutputStream();
            br = new BufferedReader(new InputStreamReader(inputStream));
            String incomeInc="";
            String cmdInc="";
            String cmdDec="";
            String ivIncome="";
            String hmac="";
            String hmacOut="";
            String msgOut="";
            incomeInc=br.readLine();

            String remoteSocAdd=  socket.getRemoteSocketAddress().toString().substring(1);
            clientIp= InetAddress.getByName(remoteSocAdd.split(":")[0].trim()).toString().replace("/", "");


            if (incomeInc.split(" ")[0].equals("S")){
                incomeInc=incomeInc.substring(incomeInc.indexOf(" ")+1);
                ivIncome=incomeInc.split(" ")[0].trim();
                cmdInc=incomeInc.split(" ")[1].trim();

                //decrypt the message using aes/gcm + client shared pass
                cmdDec=Crypto.decryptMessage(Constants.AES_GCM,sessionKey,ivIncome,cmdInc);
                isFromServer=true;
                clientIp=ServerMain.getServerName(clientIp)+" "+clientIp;
            }
            else  {
                ivIncome=incomeInc.split(" ")[0].trim();
                cmdInc=incomeInc.split(" ")[1].trim();
                hmac=incomeInc.split(" ")[2].trim();

                //decrypt the message using aes/cbc + server shared pass
                cmdDec=Crypto.decryptMessage(Constants.AES_CBC,sessionKey,ivIncome,cmdInc);
                byte[] ivcmdIncCombined=MethodHelper.concat(Crypto.getIvParameterSpec(ivIncome).getIV(),MethodHelper.stringToBytes(cmdInc));
                String hmacToCheck=MethodHelper.bytesToHex(Crypto.computeHMAC(hmacPassword,ivcmdIncCombined));
                if (!hmacToCheck.equals(hmac)) isvalidHmac =false;
            }


            //  System.out.println(" len :   "+cmdInc.length()+"  "+cmdInc);
            pw = new PrintWriter(outputStream, true);
            if(cmdDec.equals(""))throw new encryptionException(Constants.ERROR+" "+Constants.DECRYPTION+" "+clientIp);

            String[] cmdDecSplit = cmdDec.split(" ",2);;

            ServerMain.printToScreen(clientIp,"IN ",cmdDec,MethodHelper.bytesToHex(MethodHelper.stringToBytes(ivIncome)),hmac,isvalidHmac?"VALID":"INVALID");
            if (!isvalidHmac) throw new IOException(Constants.ERROR+" "+Constants.INVALID_HMAC);
            IvParameterSpec iv;

            switch (cmdDecSplit[0].trim()){
                //upload file.txt
                case Constants.UPLOAD:

                    //syntax check
                    if (!MethodHelper.validateFilepath(cmdDecSplit[1])) {
                        throw new IOException(Constants.ERROR+" "+Constants.COMMAND_SYNTAX+ " >  " +
                                Constants.UPLOAD+"  "+Constants.FILE_PATH_FORMAT);
                    }

                    iv = Crypto.generateIV();
                    msgOut=hmacOut="";
                    msgOut=Message.createEncMessage(Constants.AES_CBC,sessionKey,hmacPassword,iv,Constants.OK);
                    hmacOut=msgOut.split(" ",3)[2];
                    pw.println(msgOut);
                    ServerMain.printToScreen(clientIp,"OUT",Constants.OK,MethodHelper.bytesToHex(iv.getIV()),hmacOut,"");
                    upload(cmdDecSplit[1]);
                    break;
                case Constants.DOWNLOAD:

                    //syntax check
                    if (!MethodHelper.validateFilepath(cmdDecSplit[1])) {
                        throw new IOException(Constants.ERROR+" "+Constants.COMMAND_SYNTAX+ " >  "+
                                Constants.DOWNLOAD+"  "+Constants.FILE_PATH_FORMAT);
                    }
                    //check if file exist
                    if (!MethodHelper.isFiletExist(filesDirectory+cmdDecSplit[1]) ||SQLiteDB.getFileVersion(cmdDecSplit[1])==0 ){
                        throw new IOException(Constants.ERROR+" "+Constants.FILE_NOT_EXIST);
                    }

                    iv = Crypto.generateIV();
                    if (isFromServer){
                        pw.println(Message.createEncMessageToServer(sessionKey,iv,Constants.OK));
                    }
                    else {
                        msgOut=hmacOut="";
                        msgOut=Message.createEncMessage(Constants.AES_CBC,sessionKey,hmacPassword,iv,Constants.OK);
                        pw.println(msgOut);
                        hmacOut=msgOut.split(" ",3)[2];
                    }
                    ServerMain.printToScreen(clientIp,"OUT",Constants.OK,MethodHelper.bytesToHex(iv.getIV()),hmacOut,"");
                    download(cmdDecSplit[1],isFromServer);
                    break;

                case Constants.DELETE:
                    String[] splitted;
                    String hash="";
                    String fp="";

                    //if the req from server
                    if (cmdDecSplit[1].contains(Constants.HASH)){
                        splitted=cmdDecSplit[1].split(" ",2);
                        hash=splitted[0].replace(Constants.HASH,"").trim();
                        fp=splitted[1].trim();
                        isFromServer=true;
                    }

                    else
                        fp = cmdDecSplit[1];


                    //syntax check
                    if (!MethodHelper.validateFilepath(fp)) {
                        throw new IOException(Constants.ERROR+" "+Constants.COMMAND_SYNTAX+ " >  "+
                                Constants.DELETE+"  "+Constants.FILE_PATH_FORMAT);
                    }
                    //check if file exist
                    if (!MethodHelper.isFiletExist(filesDirectory+fp) ||SQLiteDB.getFileVersion(fp)==0){
                        throw new IOException(Constants.ERROR+" "+Constants.FILE_NOT_EXIST); }


                    //if the req from server check the hash(reqHash==hashInFileIndex)
                    if (isFromServer){
                        List<String> versionHashList= SQLiteDB.getVersionHashList(fp);

                        if (!versionHashList.get(1).trim().equals(hash)){
                            throw new IOException(Constants.ERROR+" "+Constants.FILE_WITH_DIFFERENT_HASH);
                        }
                    }
                    else
                        hash=SQLiteDB.getVersionHashList(fp).get(1);

                    delete(fp);
                    SQLiteDB.deleteFile(fp);
                    iv = Crypto.generateIV();
                    if (isFromServer){ pw.println(Message.createEncMessageToServer(sessionKey,iv,Constants.OK)); }
                    else {
                        msgOut=hmacOut="";
                        msgOut=Message.createEncMessage(Constants.AES_CBC,sessionKey,hmacPassword,iv,Constants.OK);
                        pw.println(msgOut);
                        hmacOut=msgOut.split(" ",3)[2];
                    }

                    ServerMain.printToScreen(clientIp,"OUT",Constants.OK,MethodHelper.bytesToHex(iv.getIV()),hmacOut,"");

                    //send req to other servers to delete the file
                    if (!isFromServer){

                        String cmdToServer=Constants.DELETE+" "+Constants.HASH+hash+" "+fp;
                        ServerMain.sendOutToAllServers(cmdToServer);
                    }
                    break;

                case Constants.MOVE:
                    String[] filesPaths;
                    String[] splittedFromServer;
                    isFromServer=false;
                    String hashMove="";

                    //if the req from server
                    if (cmdDecSplit[1].contains(Constants.HASH)) {
                        splittedFromServer=cmdDecSplit[1].split(" ",2);
                        hashMove=splittedFromServer[0].replace(Constants.HASH,"").trim();
                        filesPaths=splittedFromServer[1].split(";");
                        isFromServer = true;
                    }
                    else
                        filesPaths=cmdDecSplit[1].split(";");


                    //syntax check
                    if (filesPaths.length!=2) {
                        throw new IOException(Constants.ERROR + " " + Constants.COMMAND_SYNTAX + " >  " +
                                Constants.MOVE + "  " + Constants.FILE_PATH_FORMAT + ";" + Constants.FILE_PATH_FORMAT);
                    }
                    String firstPath=filesPaths[0].trim();
                    String second=filesPaths[1].trim();

                    //syntax check old and new path
                    if (!MethodHelper.validateFilepath(firstPath)){
                        throw new IOException(Constants.ERROR+" "+Constants.COMMAND_SYNTAX+ " >  "+
                                Constants.MOVE+"  "+Constants.FILE_PATH_FORMAT+";"+Constants.FILE_PATH_FORMAT);
                    }
                    if (!MethodHelper.validateFilepath(second)){
                        throw new IOException(Constants.ERROR+" "+Constants.COMMAND_SYNTAX+ " >  "+
                                Constants.MOVE+"  "+Constants.FILE_PATH_FORMAT+";"+Constants.FILE_PATH_FORMAT);
                    }
                    if (!MethodHelper.isFiletExist(filesDirectory+firstPath)|| SQLiteDB.getFileVersion(firstPath)==0){
                        throw new IOException(Constants.ERROR+" "+Constants.FILE_NOT_EXIST);
                    }


                    //the req from server check the hash(reqHash==hashInFileIndex)
                    if (isFromServer){

                        List<String> versionHashList= SQLiteDB.getVersionHashList(firstPath);
                        if (!versionHashList.get(1).trim().equals(hashMove)){
                            throw new IOException(Constants.ERROR+" "+Constants.FILE_WITH_DIFFERENT_HASH);
                        }

                    }
                    else
                        hashMove = SQLiteDB.getVersionHashList(filesPaths[0]).get(1);


                    move(firstPath,second);
                    SQLiteDB.updatedFilePath(firstPath,second);
                    iv = Crypto.generateIV();
                    if (isFromServer){
                        pw.println(Message.createEncMessageToServer(sessionKey,iv,Constants.OK));
                    }
                    else {
                        msgOut=hmacOut="";
                        msgOut=Message.createEncMessage(Constants.AES_CBC,sessionKey,hmacPassword,iv,Constants.OK);
                        pw.println(msgOut);
                        hmacOut=msgOut.split(" ",3)[2];
                    }

                    ServerMain.printToScreen(clientIp,"OUT",Constants.OK,MethodHelper.bytesToHex(iv.getIV()),hmacOut,"");

                    //send move to other servers
                    if (!isFromServer){
                        String cmdToServer=Constants.MOVE+" "+Constants.HASH+hashMove+" "+cmdDecSplit[1];
                        ServerMain.sendOutToAllServers(cmdToServer);
                    }
                    break;
                case Constants.GET_LIST:
                    String list=getList();
                    iv = Crypto.generateIV();
                    if (isFromServer){
                        pw.println(Message.createEncMessageToServer(sessionKey,iv,list));
                    }
                    else {
                        msgOut=hmacOut="";
                        msgOut=Message.createEncMessage(Constants.AES_CBC,sessionKey,hmacPassword,iv,list);
                        pw.println(msgOut);
                        hmacOut=msgOut.split(" ",3)[2];
                    }

                    ServerMain.printToScreen(clientIp,"OUT",list.replace(';','\n'),MethodHelper.bytesToHex(iv.getIV()),hmac,"");
                    break;
                case Constants.GET_VERSION:

                    String[] files=cmdDecSplit[1].split(";");
                    String version="";

                    //on file getVersion req
                    if (!isFromServer){
                        files[0]=files[0].trim().replace(";","");

                        if( files.length==1){
                            //syntax check
                            if (!MethodHelper.validateFilepath(files[0])) {
                                throw new IOException(Constants.ERROR+" "+Constants.COMMAND_SYNTAX+ " >  "+
                                        Constants.GET_VERSION+"  "+Constants.FILE_PATH_FORMAT);
                            }
                            //check if file exist
                            if (!MethodHelper.isFiletExist(filesDirectory+files[0])|| SQLiteDB.getFileVersion(files[0])==0){
                                throw new IOException(Constants.ERROR+" "+Constants.FILE_NOT_EXIST);
                            }
                        }
                        version=getVersion(cmdDecSplit[1],false);

                    }//req from server with multiple files (file1;file2)
                    else {
                        for(String f:files){
                            if (SQLiteDB.getFileVersion(f)==0)
                                version+=f+"=0;";
                            else
                                version+=f+"="+getVersion(f,true)+";";
                        }

                    }

                    iv = Crypto.generateIV();

                    if (isFromServer){
                        pw.println(Message.createEncMessageToServer(sessionKey,iv,version));
                    }
                    else {
                        msgOut=hmacOut="";
                        msgOut=Message.createEncMessage(Constants.AES_CBC,sessionKey,hmacPassword,iv,version);
                        pw.println(msgOut);
                        hmacOut=msgOut.split(" ",3)[2];
                    }

                    ServerMain.printToScreen(clientIp,"OUT",version,MethodHelper.bytesToHex(iv.getIV()),hmacOut,"");
                    break;
                case Constants.OFFER:
                    String offerMsg= cmdDecSplit[1].substring(0,cmdDecSplit[1].lastIndexOf(" "));
                    String server  =cmdDecSplit[1].substring(cmdDecSplit[1].lastIndexOf(" ")+1);
                    String msgBack;
                    //need to check the offer fisrt
                    if (checkOffer(offerMsg)){
                        String filePath=offerMsg.split(" ",3)[2].trim();
                        int fileVersion=Integer.parseInt(offerMsg.split(" ",3)[0].trim());
                        msgBack=Constants.OK;
                        iv = Crypto.generateIV();
                        pw.println(Message.createEncMessageToServer(sessionKey,iv,msgBack));

                        //send download file req to server back
                        String cmdToServer=Constants.DOWNLOAD+" "+filePath;
                        String ServeripPortName[]=server.split(":");
                        ServerMain.requestDownloadFromServer(new Server(ServeripPortName[0],ServeripPortName[1]),cmdToServer,fileVersion);
                    }
                    else {
                        msgBack = Constants.ERROR + " " + Constants.SMALLER_VERSION_OR_SAME_HASH;
                        iv = Crypto.generateIV();
                        pw.println(Message.createEncMessageToServer(sessionKey,iv,msgBack));

                    }

                    ServerMain.printToScreen(clientIp,"OUT",msgBack,MethodHelper.bytesToHex(iv.getIV()),"","");
                    break;
                default://decryption error
                    throw new encryptionException(Constants.ERROR+" "+Constants.DECRYPTION);

            }

            try { br.close(); } catch (Exception ex) {}
            try { pw.close(); } catch (Exception ex) {}


        } catch (UnknownHostException e) {
            System.out.println("UnknownHostException"+e.getMessage());
        } catch (SocketException e) {
            System.out.println("SocketException "+e.getMessage());
        }catch (IOException |NullPointerException   e) {
            String msgout="";
            String hmacOut="";
            IvParameterSpec iv=Crypto.generateIV();
            if (isFromServer){
                pw.println(Message.createEncMessageToServer(sessionKey,iv,e.getMessage()));
            }
            else {
                msgout=Message.createEncMessage(Constants.AES_CBC,sessionKey,hmacPassword,iv,e.getMessage());
                hmacOut=msgout.split(" ",3)[2];
                pw.println(msgout);
            }

            ServerMain.printToScreen(clientIp,"OUT",e.getMessage(),MethodHelper.bytesToHex(iv.getIV()),hmacOut,"");

        } catch (encryptionException e) {
            pw.println("error");
            ServerMain.printToScreen("","",e.getMessage(),"","","");
        }
        catch (challengeException e) {
            ServerMain.printToScreen("","",e.getMessage(),"","","");
        }
        try { socket.close(); } catch (Exception ex) {}

    }

    /***
     * check the offer from other servers
     * @param offer [offer version hash filepath]
     * @return true/false if it ok to send
     */
    private Boolean checkOffer(String offer) {

        String[] offerSplitted=offer.split(" ",3);
        String offerversion ;
        String hash;
        String file;

        if (offerSplitted.length!=3)
            return false;

        offerversion=offerSplitted[0].trim();
        hash=offerSplitted[1].trim();
        file=offerSplitted[2].trim();

        if (SQLiteDB.getFileVersion(file)==0)
            return true;

        List<String> versionHashList= SQLiteDB.getVersionHashList(file);

        //if offer version > version here  then continue
        if (Integer.parseInt(offerversion)<Integer.parseInt(versionHashList.get(0)))
            return false;

        //the check here not correct
        if (versionHashList.get(1).trim().equals(hash))
            return false;

        return true;
    }

    /**
     * Requests the current version of a file from a file server
     *
     * @param file path/file
     * @return version (number) + Hexhash(file)
     */
    private String getVersion(String file,boolean justNumber)  {
        String version;
        LockRegistry.INSTANCE.acquire(file, LockRegistry.LockType.READ); //lock file to getVersion
        if (justNumber)
            version= SQLiteDB.getFileVersion(file)+"";
        else
            version= SQLiteDB.getFileVersionAndHash(file);
        LockRegistry.INSTANCE.release(file, LockRegistry.LockType.READ); //release the file loc
        return version;
    }

    /**
     *
     * @returnlist of all paths and file names in the files index
     */
    private String getList() {
        String filesList="";
        for (String file:MethodHelper.filesList(filesDirectory)){
            file=file.replace("\\","/");  // windows path change to  (dir/subdir/file)
            filesList+=file.replace(filesDirectory,"")+";";}
        if (filesList.equals(""))filesList=Constants.There_is_no_files;
        return filesList;
    }

    /**
     *
     * Changes the path and/or file name of a file on a file server
     * @param oldP oldFile path (path/file)
     * @param newP newFile path (path/file)
     * @throws IOException
     */
    private void move(String oldP, String newP) throws IOException {
        String oldPath=filesDirectory+oldP;
        String newPath=filesDirectory+newP;
        Path source = Paths.get(oldPath);
        Path target = Paths.get(newPath);
        Path path = Paths.get(newPath);
        Path parentDir = path.getParent();
        if (!Files.exists(parentDir))
            Files.createDirectories(parentDir);

        if (Files.exists(target)) {
            new File(newPath).delete();//delete target file
            SQLiteDB.deleteFile(newP);//delete from the db
        }
        try {
            LockRegistry.INSTANCE.acquire(oldPath, LockRegistry.LockType.WRITE); //lock file to move
            Files.move(source, target);
            LockRegistry.INSTANCE.release(oldPath, LockRegistry.LockType.WRITE); //release the file lock

        } catch (IOException e) {
            throw new IOException(Constants.ERROR+" "+Constants.IOException);
        }
    }

    /**
     * Deletes a file on one server
     * @param filePath file path (path/file)
     */
    private void delete(String filePath)  {

        filePath=filesDirectory+filePath;
        LockRegistry.INSTANCE.acquire(filePath, LockRegistry.LockType.WRITE); //lock file to delete
        File file = new File(filePath);
        file.delete();
        LockRegistry.INSTANCE.release(filePath, LockRegistry.LockType.WRITE); //release the file lock
    }

    /**
     * start sending the file data from server to client
     * @param fp file path (path/file)
     * @throws IOException
     */
    private void download(String fp,Boolean toServer ) throws IOException {

        String filePath=filesDirectory+fp;

        LockRegistry.INSTANCE.acquire(filePath, LockRegistry.LockType.READ); //lock file to read
        byte[] fileBytes=  MethodHelper.readFileBytes(filePath);
        LockRegistry.INSTANCE.release(filePath, LockRegistry.LockType.READ); //release the file lock
        byte[] fileEnc;
        IvParameterSpec iv= Crypto.generateIV();
        if (toServer){
            fileEnc= Message.createEnFileToServer(sessionKey,iv,fileBytes);
        }
        else {
            fileEnc= Message.createEncFile(Constants.AES_CBC,sessionKey,hmacPassword,iv,fileBytes);
        }

        int dataLength=fileEnc.length;
        int count=0;
        byte[] fileChunk;
        while(count!=dataLength){
            int size = 1024*1024;
            if(dataLength - count >= size){count += size;}
            else{size = (int)(dataLength - count); count = dataLength;}
            fileChunk = new byte[size];
            System.arraycopy(fileEnc,(count-size),fileChunk,0,size);
            outputStream.write(fileChunk);
        }
    }


    /**
     * start upload file to server from client and save it
     * @param fp file path (path/file)
     * @throws IOException
     */
    private void upload(String fp) throws IOException {

        String filePath=filesDirectory+fp;
        String myIpPort= ServerMain.myIpPort.trim();
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
        byte[] hmacAndFile=MethodHelper.splitByte(byteArray," ".getBytes()).get(1);
        String hmac=MethodHelper.bytesToHex(MethodHelper.splitByte(hmacAndFile," ".getBytes()).get(0));
        byte[] fileEnc=MethodHelper.splitByte(hmacAndFile," ".getBytes()).get(1);
        String hmacCheck=MethodHelper.bytesToHex(Crypto.computeHMAC(hmacPassword,fileEnc));

        if (!hmacCheck.equals(hmac))isvalidHmac=false;
        else isvalidHmac =true;
        //decrypt the file that arrived from client
        fileData=Crypto.decryptFile(Constants.AES_CBC,sessionKey,iv,fileEnc);

        if (Arrays.equals(fileData,new  byte[0])) throw new IOException(Constants.ERROR+" "+Constants.FILE_DECRYPTION);
        ServerMain.printToScreen(clientIp,"IN ","file: "+fp,MethodHelper.bytesToHex(MethodHelper.stringToBytes(iv)),hmac,isvalidHmac?"VALID":"INVALID");
        if (!isvalidHmac) throw new IOException(Constants.ERROR+" "+Constants.INVALID_HMAC);

        hash= MethodHelper.fileHash(fileData);
        fileReader.close();
        Path path = Paths.get(filePath);
        Path parentDir = path.getParent();
        if (!Files.exists(parentDir))
            Files.createDirectories(parentDir);

        //save the file for the first time with version 1
        if (!MethodHelper.isFiletExist(filePath))
            SQLiteDB.addFile(fp,hash,1);
        else
            SQLiteDB.incFileVersionAndChangeHash(fp,hash);//inc the file version and change hash

        LockRegistry.INSTANCE.acquire(filePath, LockRegistry.LockType.WRITE);  //lock file to write
        Files.write(path, fileData);
        LockRegistry.INSTANCE.release(filePath, LockRegistry.LockType.WRITE);  //release the file lock

        List<String> versionHashList= SQLiteDB.getVersionHashList(fp);
        if (versionHashList.size()==2){
            String version=versionHashList.get(0);
            hash=versionHashList.get(1);
            //start offer the file to all servers
            String cmdToServer=Constants.OFFER+" "+version+" "+hash+" "+fp+" "+myIpPort;
            ServerMain.sendOutToAllServers(cmdToServer);
        }

    }

    /***
     * start challenge in
     * @return
     */
    private  String challenge(){

        String result="none";
        InChallengeHandler thread =new InChallengeHandler(socket);
        thread.start();
        try {
            thread.join();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        result=thread.getResult().trim();
        isFromServer=thread.isFromServer;

        sessionKey=result;

        return result;
    }


}
