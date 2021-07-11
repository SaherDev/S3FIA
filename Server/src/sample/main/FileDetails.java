package sample.main;

public class FileDetails {

    private final int version;
    private final String hash;
    private  String server;

    public FileDetails(String server, int version, String hash){
        this.server=server;
        this.version=version;
        this.hash=hash;
    }

    public String getServer() {
        return server;
    }

    public String getHash() {
        return hash;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return getServer()+ " "+getVersion()+" "+getHash();
    }
}
