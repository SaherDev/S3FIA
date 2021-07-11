package sample.db;
import java.io.File;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;

public class SQLiteDB {
    public static Connection conn = null;
    public static String sqliteServer = "jdbc:sqlite:";
    public static String resetPath = "";

    /**
     *
     * @param dbFilePath
     * @return TRUE/FALSE if db file exist
     */
    public static boolean isDatabaseExist(String dbFilePath){
        File dbFile = new File(dbFilePath);
        return dbFile.exists();
    }

    /**
     *  this function connect to db file or
     *  create new db if not exist
     * @param fileName the db filename to connect for
     */
    public static void connect(String fileName) {
        try {
            fileName="/"+fileName;
            // db parameters
            sqliteServer = "jdbc:sqlite:";
            String getFilePath = new File("").getAbsolutePath();
            String fileAbsolutePath = getFilePath.concat(fileName+".db");
            resetPath = fileAbsolutePath;

            // create a connection to the database
            if(isDatabaseExist(fileAbsolutePath)){
                conn = DriverManager.getConnection(sqliteServer+fileAbsolutePath);
            }else{
                try{
                    createNewDatabase("database", fileName);
                }catch (Exception ex){
                    System.out.println("Error: " + ex);
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

    }

    /**
     * create new db with given fileName
     *    tblFiles(file,version,hash)
     * @param fileSubFolder
     * @param fileName db name
     */
    public static void createNewDatabase(String fileSubFolder, String fileName) {

        String getFilePath = new File("").getAbsolutePath();
        String fileAbsolutePath = "";

        if(fileSubFolder.isEmpty()){
            fileAbsolutePath = getFilePath.concat(fileName+".db");
            resetPath = fileAbsolutePath;
        }else{
            fileAbsolutePath = getFilePath.concat(fileName+".db");
            resetPath = fileAbsolutePath;
        }
        try (Connection conn = DriverManager.getConnection(sqliteServer+fileAbsolutePath)) {
            if (conn != null) {
                String sql = "CREATE TABLE IF NOT EXISTS tblFiles (\n"
                        + "	file text ,\n"
                        + "	version int ,\n"
                        + "	hash text\n"
                        + ");";
                DatabaseMetaData meta = conn.getMetaData();
                Statement statement  = conn.createStatement();
                statement.executeQuery(sql);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * insert to table new file
     * @param file file name (path/file)
     * @param hash hash that calcuated from file
     */
    public static void addFile(String file, String hash,int version) {
        String sql = "INSERT INTO tblFiles (file, version, hash) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(sqliteServer+resetPath)) {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, file);
            pstmt.setInt(2, version);
            pstmt.setString(3, hash);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * update row with given file to new version in table
     * @param file file name(path/file)
     * @param version the file version
     */
    public static void updatedFileVersion(String file, int version) {
        String sql = "UPDATE tblFiles SET version = " + version + " " + "WHERE file='" + file + "' ";
        try (Connection conn = DriverManager.getConnection(sqliteServer+resetPath)) {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     *  update file name to new one in table
     * @param oldFile old filename(path/file)
     * @param newfile new fileName (path/file)
     */
    public static void updatedFilePath(String oldFile, String newfile) {
        String sql = "UPDATE tblFiles SET file = '" + newfile + "' " + "WHERE file like '" + oldFile + "' ";
        try (Connection conn = DriverManager.getConnection(sqliteServer+resetPath)) {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println("updatedFilePath"+e.getMessage());
        }
    }

    /**
     *delete row where the file
     * @param file fileName(path/file)
     */
    public static void deleteFile(String file) {
        String sql = "delete  from tblFiles where file ='" + file + "'";;
        try (Connection conn = DriverManager.getConnection(sqliteServer+resetPath)) {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println("updatedFilePath "+e.getMessage());
        }
    }


    /***
     * get all files list
     * @return list of all files in the system (file=version hash)
     */
    public static String getAllFiles() {
        String files="";
        String sql = "select *  from tblFiles ";
        try (Connection conn = DriverManager.getConnection(sqliteServer+resetPath)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                files +=rs.getString("file")+ "="+ rs.getInt("version")+" "+ rs.getString("hash")+";";
            }
            conn.close();
        } catch (SQLException e) {
            System.out.println("getFileVersion "+e.getMessage());
        }
        return files;
    }



    /**
     *
     * @param file fileName(path/file)
     * @return version number or 0 if file not found
     */
    public static int getFileVersion(String file) {
        int version=0;
        String sql = "select version from tblFiles where file ='" + file + "'";
        try (Connection conn = DriverManager.getConnection(sqliteServer+resetPath)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                version = rs.getInt("version");
            }
            conn.close();
        } catch (SQLException e) {
            System.out.println("getFileVersion "+e.getMessage());
        }
        return version;
    }

    /**
     * r
     * @param file fileName (path/file)
     * @return  version +  hash(file)
     */
    public static String getFileVersionAndHash(String file) {
        String versionHash="";
        String sql = "select version,hash from tblFiles where file ='" + file + "'";
        try (Connection conn = DriverManager.getConnection(sqliteServer+resetPath)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                versionHash = "Version: "+ rs.getInt("version")+"   "+ rs.getString("hash");
            }
            conn.close();
        } catch (SQLException e) {
            System.out.println("getFileVersion "+e.getMessage());
        }
        return versionHash;
    }

    /***
     *
     * @param file path/file
     * @return list that contains [version,hash]
     */
    public static List<String> getVersionHashList(String file) {
        List<String> versionHash=new LinkedList<String>();
        String sql = "select version,hash from tblFiles where file ='" + file + "'";
        try (Connection conn = DriverManager.getConnection(sqliteServer+resetPath)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                versionHash.add(rs.getInt("version")+"");
                versionHash.add(rs.getString("hash"));

            }
            conn.close();
        } catch (SQLException e) {
            System.out.println("getFileVersion "+e.getMessage());
        }
        return versionHash;
    }



    /**
     * increase file version
     * @param file fileName
     */
    public static void incFileVersion(String file) {
        int version=getFileVersion(file);
        if (version!=0)
            updatedFileVersion(file,++version);
    }

    /***
     *  increment the file version an change the hash
     * @param file path/file
     * @param hash hash
     */
    public static void incFileVersionAndChangeHash(String file,String hash) {
        int version=getFileVersion(file);
        if (version!=0){
            updateVersionHash(file,++version,hash);
        }

    }

    /***
     * update file hash+version
     * @param file path/file
     * @param version version>0
     * @param hash hash
     */
    public static void updateVersionHash(String file,int version,String hash) {
            String sql = "UPDATE tblFiles SET version = " + version + ", hash = '" +hash + "' " + "WHERE file='" + file + "' ";
            try (Connection conn = DriverManager.getConnection(sqliteServer+resetPath)) {
                Statement stmt = conn.createStatement();
                stmt.executeUpdate(sql);
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }


}

