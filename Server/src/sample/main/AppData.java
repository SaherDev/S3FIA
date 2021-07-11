package sample.main;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class AppData {

    private static final String FILENAME = "AppData.txt";

    /**
     * read data from file.txt
     *  line 1: fileRoot (FileRoot/)
     *  line 2: port (port)
     *  line 3: hashed server-server shared password.
     *  line 4: hashed client-server shared password.
     * @return List<String>  app data
     */
    public  static List<String> getAppData(){
        List<String>appData= new LinkedList<>();


        try
        {
            File f = new File(FILENAME);
            if(!f.exists()){
                f.createNewFile();}

            FileInputStream fis=new FileInputStream(f);
            Scanner sc=new Scanner(fis);
            while(sc.hasNextLine())
            {
                appData.add(sc.nextLine());
            }
            sc.close();
        }
        catch(IOException |ArrayIndexOutOfBoundsException e)
        {
            System.out.println("error getAppData");
            return new LinkedList<>();
        }
        return appData;
    }

    /***
     * change selected line with new txt
     * @param lineNumber line number
     * @param data the new line data
     */
    public static void setVariable(int lineNumber, String data)  {
        Path path = Paths.get(FILENAME);
        List<String> lines = null;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            lines.set(lineNumber , data);
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("error setVariable");
        }

    }
}
