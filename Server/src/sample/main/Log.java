package sample.main;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
public class Log {
    private static final String FILENAME = "Log.txt";


    /**
     * add new line for log.txt file
     * @param line String line to write
     */
    public   static void addLine(String line) {
        File file = new File(FILENAME);
        FileWriter fr = null;
        try {
            fr = new FileWriter(file, true);
            BufferedWriter br = new BufferedWriter(fr);
            br.write(line);
            br.newLine();
            br.close();
            fr.close();
        } catch (IOException e) {
            System.out.println("Erorr");
        }
    }
}
