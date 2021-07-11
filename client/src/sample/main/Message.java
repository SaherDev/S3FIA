package sample.main;

import javax.crypto.spec.IvParameterSpec;

public class Message {
    /***
     * encryptMessage the message and attach the iv, message, hmac togother
     * @param sharedPassword client server shared password
     * @param hmacPassword client/server mac password
     * @param iv 128 bite Iv Parameter
     * @param message  the message the need to encrepte
     * @return encrypt Message  [iv,message,hmac]
     */
    public static String createMessage(String sharedPassword,String hmacPassword, IvParameterSpec iv, String message){
        byte[] encMessage=Crypto.encryptMessage(Constants.AES_CBC,sharedPassword,iv,message);
        String msgEncStr=MethodHelper.bytesToString(encMessage);
        String ivString=MethodHelper.bytesToString(iv.getIV()) ;
        byte[] combined=MethodHelper.concat(iv.getIV(),encMessage);
        String hmacString=MethodHelper.bytesToHex(Crypto.computeHMAC(hmacPassword,combined));
        return ivString+" "+msgEncStr+" "+hmacString;
    }

    /***
     * encrepte the file data and attach the iv, hmac togother
     * @param sharedPassword client server shared password
     * @param hmacPassword client/server mac password
     * @param iv 128 bite Iv Parameter
     * @param file file data
     * @return encrepted file [iv,hmac,file]
     */
    public static byte[] createFile(String sharedPassword,String hmacPassword,IvParameterSpec iv,byte[] file) {

        byte[] fileEnc=Crypto.encryptFile(Constants.AES_CBC,sharedPassword,iv,file);
        byte[] hmac=Crypto.computeHMAC(hmacPassword,fileEnc);
        byte[] ivSpace=MethodHelper.concat(iv.getIV()," ".getBytes());
        byte[] ivhmac=MethodHelper.concat(ivSpace,hmac," ".getBytes());
        return MethodHelper.concat(ivhmac,fileEnc);
    }
}
