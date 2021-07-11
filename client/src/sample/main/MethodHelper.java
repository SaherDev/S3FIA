package sample.main;
import java.io.*;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MethodHelper {

    /**
     * check ip format
     * @param ip ip4Address (0.0.0.0)
     * @return true / false if the ip valid or not
     */
    private static boolean validIP (String ip) {
        try {
            if ( ip == null || ip.isEmpty() ) {
                return false;
            }

            String[] parts = ip.split( "\\." );
            if ( parts.length != 4 ) {//ipv4
                return false;
            }

            for ( String s : parts ) {
                int i = Integer.parseInt( s );
                if ( (i < 0) || (i > 255) ) {//the number ranges is [0-255]
                    return false;
                }
            }
            if ( ip.endsWith(".") ) {
                return false;
            }

            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }


    /**
     * check ip format
     * @param port port (number)
     * @return  true / false if the port valid or not
     */
    private static boolean validPort (String port) {
        try {
            if ( port == null || port.isEmpty() ) {
                return false;
            }
            int i = Integer.parseInt( port );
            if ( (i < 0) )
                return false;


            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }



    /**
     * check ip and format  format
     * @param ipPort p4Address+port (0.0.0.0:0)
     * @return true / false if the ip/port valid or not
     */
    public static boolean validPIpPort (String ipPort) {

        String ipPortArray[]=ipPort.split(":");
        if (ipPortArray.length!=2)
            return false;
        if (!validIP(ipPortArray[0]))
            return false;
        if (!validPort(ipPortArray[1]))
            return false;

        return true;
    }




    /***
     * split fileName from path/***
     * @param pathFile
     * @return
     */
    public static String getFileNameFromRealPath(String pathFile){
       return Paths.get(pathFile).getFileName().toString();
    }


    /***
     * split the filee path to get file name
     * @param pathFile file path
     * @return file name
     */
    public static String getFileNameFromStringPath(String pathFile){
        String[] pathSplit= pathFile.split("/");
        return pathSplit[pathSplit.length-1];
    }

    /***
     * convert string to byte
     * @param s
     * @return
     */
    public static byte[] stringToBytes(String s){
        String[] parts = s.split("_");
        byte[] bytework = new byte[parts.length];
        int total = 0;

        for (int i = 0; i < parts.length; i++)
        {
            try {
                bytework[total] = Byte.parseByte(parts[i]);
                total++;
            } catch (Exception ex) {
                System.err.println( ex.getMessage());
            }
        }

        if ( total < 0)
        {
            return new byte[0];
        }
        byte[] bytesFinal = new byte[total];
        System.arraycopy(bytework, 0, bytesFinal, 0, bytesFinal.length);
        return bytesFinal;
    }


    /***
     * convert byte to string
     * @param array
     * @return
     */
    public static String bytesToString(byte[] array) {
        StringBuilder sb = new StringBuilder();
        IntStream.range(0, array.length).forEach(i -> {
            sb.append(array[i] + "_");
        });
        return sb.toString().trim();
    }

    /**
     * Converts a byte array to a hexadecimal string representation
     * @param array The byte array to write out
     * @return The bytes of the array in hexadecimal notation
     */
    public static String bytesToHex(byte[] array)
    {
        String sb = IntStream.range(0, array.length).mapToObj(i -> String.format("%02X", array[i])).collect(Collectors.joining());
        return sb;
    }

    /**
     * Converts a hexademical string to a byte array.
     * @param s A hexadecimal string to convert to byte array
     * @return The byte array representation of the string
     * @throws IllegalArgumentException If the array (after trimming) doesn't have an even number of characters
     * @implNote https://stackoverflow.com/a/140861
     */
    public static byte[] hexToBytes (String s)
    {
        String trimmed = s.trim();
        if (trimmed.length() % 2 != 0)
        {
            throw new IllegalArgumentException("Must provide an even number of characters in the hex string");
        }
        byte[] bytes = new byte[trimmed.length() / 2];
        for (int i = 0; i < trimmed.length(); i += 2)
        {
            bytes[i / 2] = (byte) ((Character.digit(trimmed.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }
        return bytes;
    }
    public static boolean validServerName(String name) {
        return Pattern.matches("^[a-zA-Z\\s]+", name);
    }

    /**
     * concat many byte array
     * @param arrays
     * @return
     */
    public static byte[] concat(byte[]...arrays)
    {
        int totalLength = 0;
        for (int i = 0; i < arrays.length; i++)
        {
            totalLength += arrays[i].length;
        }
        byte[] result = new byte[totalLength];
        int currentIndex = 0;
        for (int i = 0; i < arrays.length; i++)
        {
            System.arraycopy(arrays[i], 0, result, currentIndex, arrays[i].length);
            currentIndex += arrays[i].length;
        }
        return result;
    }


    /***
     * split byte[]
     * @param array byte[] array
     * @param delimiter
     * @return List<byte[]>
     */
    public static List<byte[]> splitByte(byte[] array, byte[] delimiter)
    {
        int index= indexOf(array,delimiter);
        List<byte[]> byteArrays = new LinkedList<byte[]>();
        byteArrays.add(Arrays.copyOfRange(array, 0, index));
        byteArrays.add(Arrays.copyOfRange(array, index+1, array.length));
        return byteArrays;
    }


    /***
     * find index of the patther
     * @param data
     * @param pattern
     * @return
     */
    private static int indexOf(byte[] data, byte[] pattern) {
        int[] failure = computeFailure(pattern);
        int j = 0;
        for (int i = 0; i < data.length; i++) {
            while (j > 0 && pattern[j] != data[i]) {
                j = failure[j - 1];
            }
            if (pattern[j] == data[i]) {
                j++;
            }
            if (j == pattern.length) {
                return i - pattern.length + 1;
            }
        }
        return -1;
    }

    /***
     * computeFailure
     * @param pattern
     * @return
     */
    private static int[] computeFailure(byte[] pattern) {
        int[] failure = new int[pattern.length];
        int j = 0;
        for (int i = 1; i < pattern.length; i++) {
            while (j>0 && pattern[j] != pattern[i]) {
                j = failure[j - 1];
            }
            if (pattern[j] == pattern[i]) {
                j++;
            }
            failure[i] = j;
        }
        return failure;
    }


    /***
     * read file byt[] from file path
     * @param filePath
     * @return
     */
    public static byte[] readFileBytes(String filePath)  {
        if (filePath.equals("")) return new byte[0];
        File file = new File(filePath);

        ByteArrayOutputStream ous = null;
        InputStream ios = null;
        try {
            byte[] buffer = new byte[4096];
            ous = new ByteArrayOutputStream();
            ios = new FileInputStream(file);
            int read = 0;
            while ((read = ios.read(buffer)) != -1) {
                ous.write(buffer, 0, read);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (ous != null)
                    ous.close();
            } catch (IOException e) {
            }

            try {
                if (ios != null)
                    ios.close();
            } catch (IOException e) {
            }
        }
        return ous.toByteArray();
    }
}
