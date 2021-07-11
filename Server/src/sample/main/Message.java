package sample.main;

import javax.crypto.spec.IvParameterSpec;

public class Message {


    /***
     * encrypt Message the message and attach the iv, message, hmac togother
     * @param sharedPassword client server shared password
     * @param macPassword client/server mac password
     * @param iv 128 bite Iv Parameter
     * @param message  the message the need to encrepte
     * @return encrypt Message  [iv,message,hmac] using cipher
     */
    public static String createEncMessage(String cipher, String sharedPassword,String macPassword, IvParameterSpec iv, String message){
        byte[] encMessage=Crypto.encryptMessage(cipher,sharedPassword,iv,message);
        String msgEncStr=MethodHelper.bytesToString(encMessage);
        String ivString=MethodHelper.bytesToString(iv.getIV()) ;
        byte[] combined=MethodHelper.concat(iv.getIV(),encMessage);
        String hmacString=MethodHelper.bytesToHex(Crypto.computeHMAC(macPassword,combined));
        return ivString+" "+msgEncStr+" "+hmacString;
    }


    /***
     * encrypt the file data and attach the iv, hmac togother
     * @param sharedPassword client server shared password
     * @param macPassword client/server mac password
     * @param iv 128 bite Iv Parameter
     * @param file file data
     * @return encrypted file [iv,hmac,file] using cipher
     */
    public static byte[] createEncFile(String cipher, String sharedPassword,String macPassword, IvParameterSpec iv, byte[] file) {
        byte[] fileEnc=Crypto.encryptFile(cipher,sharedPassword,iv,file);
        byte[] hmac=Crypto.computeHMAC(macPassword,fileEnc);
        byte[] ivSpace=MethodHelper.concat(iv.getIV()," ".getBytes());
        byte[] ivhmac=MethodHelper.concat(ivSpace,hmac," ".getBytes());
        return MethodHelper.concat(ivhmac,fileEnc);
    }

    /***
     * encrypt message to the server/server message using AES/GCM
     * @param sharedPassword servers /shared message
     * @param iv 128 bite Iv Parameter
     * @param message the message that need to encrypt
     * @return  encrypted Message  [iv,message] using AES/GCM
     */
    public static String createEncMessageToServer(String sharedPassword, IvParameterSpec iv, String message){
        byte[] encMessage=Crypto.encryptMessage(Constants.AES_GCM,sharedPassword,iv,message);
        String msgEncStr;
        String tag;
        msgEncStr=MethodHelper.bytesToString(encMessage);
        String ivString=MethodHelper.bytesToString(iv.getIV()) ;
        return "S"+" "+ivString+" "+msgEncStr+" S";
    }

    /***
     * encrypt file to the server/server message using AES/GCM
     * @param sharedPassword servers /shared message
     * @param iv 128 bite Iv Parameter
     * @param file data that  need to encrypt
     * @return  encrypted file  [iv,message] using AES/GCM
     */
    public static byte[] createEnFileToServer( String sharedPassword, IvParameterSpec iv, byte[] file) {
        byte[] ivSpac=MethodHelper.concat(iv.getIV()," ".getBytes());
        byte[] fileEnc=Crypto.encryptFile(Constants.AES_GCM,sharedPassword,iv,file);
        return MethodHelper.concat(ivSpac,fileEnc);

    }
}
