package sample.Server;
import sample.db.SQLiteDB;
import sample.main.*;
import sample.out.OutThread;
import java.util.*;


public class FilesSync  extends Thread{

    public FilesSync(){

    }

    /***
     * start up Sync files from other servers
     * 1] get files version and hash
     * 2] retrieve the version numbers and digests for each file (path/file=version hash)
     * 3] the server retrieves the highest number version available from another server
     * 4] download the file with highest version from servers
     */
    @Override
    public void run() {
        {
            //get files from my db
            String[] myFilesList= SQLiteDB.getAllFiles().split(";");
            String myFilesNames="";
            Boolean noFilesLocally=false;
            //this list means for every file we know what the (version,hash,which server)
            //    for example
            //          [1,ddffd,server1]
            // file1 => [2,ddffd,server2]
            //          [3,dd233,server3]
            List<FileVersions> files= new LinkedList<>();
            HashMap<String, String> filesToDownload = new HashMap<String, String>();

            ServerMain.printToScreen("","","start scanning files","","","");
            if (myFilesList[0].equals("")) {

                ArrayList<String> filesNamesFromServers= new ArrayList<>();

                for (Server s:ServerMain.servers){

                    OutThread thread =new OutThread(s,Constants.GET_LIST);
                    thread.start();
                    try {
                        thread.join();

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    String result=thread.getResult().trim();
                    if (!result.equals("none")){
                        if (!result.equals("")){
                            String[] spl=result.split(";");
                            if (spl[0].contains(Constants.There_is_no_files))
                                continue;
                            for (String sp:spl){
                                filesNamesFromServers.add(sp);
                            }
                        }

                    }


                }

                filesNamesFromServers=MethodHelper.removeDuplicates(filesNamesFromServers);

                if (filesNamesFromServers.size()==0){

                    ServerMain.printToScreen("","","there is no files","","","");
                    return;
                }

                int i=0;
                myFilesList= new String[filesNamesFromServers.size()];
                for (String f:filesNamesFromServers){
                    myFilesList[i]=f+"=0 01010101";
                    i++;

                }

            }


            for (String f:myFilesList){
                String FileName=f.substring(0,f.indexOf("="));
                String[] versionAndHash=f.substring(f.indexOf("=")+1).split(" ",2);
                int version=Integer.parseInt(versionAndHash[0]);
                String hash=versionAndHash[1];
                files.add(new FileVersions(FileName));
                files.get(files.size()-1 ).addFileInServers("MY_SERVER",version,hash);
                myFilesNames+=FileName+";";
            }

            //req from all the servers version of the files
            //servers send back for every file (file=version hash)
            //file=0 means the file doesn't exist in the server
            String cmd = Constants.GET_VERSION+" "+myFilesNames;
            for (Server s:ServerMain.servers){

                OutThread thread =new OutThread(s,cmd);
                thread.start();
                try {
                    thread.join();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                String result=thread.getResult();
                if (!result.equals("none")){
                    String[] tempFiles=result.split(";");

                    for (int i=0;i<tempFiles.length;i++){

                        if (!tempFiles[i].contains("="))
                            continue;
                        String filename=tempFiles[i].substring(0,tempFiles[i].indexOf("="));
                        String[] versionAndHash=tempFiles[i].substring(tempFiles[i].indexOf("=")+1).split(" ",2);
                        int version=Integer.parseInt(versionAndHash[0]);
                        if (version!=0){
                            //add file version+hash+server  with chosen file
                            String hash="hash";//versionAndHash[1];
                            files.get(i ).addFileInServers(s.toString(),version,hash);
                        }

                    }
                }

            }

            //get the highest version for each file
            for (FileVersions fileInVersions :files){
                FileDetails fileDetails=  Collections.max(fileInVersions.getFileDetails(), Comparator.comparing(s -> s.getVersion()));
                if (!fileDetails.getServer().equals("MY_SERVER"))
                    filesToDownload.put(fileDetails.getVersion()+" "+fileInVersions.getFileName(),fileDetails.getServer());

            }

            //start sending for every server file to download
            for (String i : filesToDownload.keySet()) {

                String[] iSplitt=i.split(" ");
                String[] ServerIpPort=filesToDownload.get(i).split(":");
                String fileName=iSplitt[1];
                int fileVersion=Integer.parseInt(iSplitt[0]);
                String serverIp=ServerIpPort[0].split(" ")[1];
                String serverPort=ServerIpPort[1];
                String cmdToServer=Constants.DOWNLOAD+" "+fileName;
                ServerMain.requestDownloadFromServer(new Server(serverIp,serverPort),cmdToServer,fileVersion);

            }


        }
    }

}
