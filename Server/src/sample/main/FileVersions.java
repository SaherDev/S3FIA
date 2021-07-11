package sample.main;

import java.util.LinkedList;
import java.util.List;

public class FileVersions {

    private String fileName;
    List<FileDetails> fileDetails;


    public FileVersions(String fileName){
        this.fileName=fileName;
        this.fileDetails = new LinkedList<>();
    }

    /***
     * addFileInServers
     * @param server server ip;port
     * @param version file version
     * @param hash file hash string
     */
    public void addFileInServers(String server, int version, String hash){
        fileDetails.add(new FileDetails(server,version,hash));
    }

    /***
     * get file name
     * @return string file name
     */
    public String getFileName() {
        return fileName;
    }


    /***
     * get file data
     * @return
     */
    public List<FileDetails> getFileDetails() {
        return fileDetails;
    }

    @Override
    public String toString() {
        return getFileName();
    }

}
