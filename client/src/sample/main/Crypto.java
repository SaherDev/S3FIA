package sample.main;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;

public class Crypto {
    private static MessageDigest mesDigest;
    private static Cipher crypto;
    private static Mac mac;
    /***
     * encrypt String Message using cipher, password, iv
     * @param cipher aes/ctr or aes/cbc
     * @param pass shred password
     * @param iv iv
     * @param message the message to encrypt
     * @return encrypted message
     */
    public static byte[] encryptMessage(String cipher,String pass, IvParameterSpec iv ,String message){

        byte[] ciphertext = new byte[0];
        try {
            mesDigest = MessageDigest.getInstance(Constants.SHA256);
            crypto = Cipher.getInstance(cipher);
            SecretKeySpec key = new SecretKeySpec(MethodHelper.hexToBytes(pass), Constants.AES);
            crypto.init(Cipher.ENCRYPT_MODE, key,iv);
            ciphertext = crypto.doFinal(message.getBytes(StandardCharsets.UTF_8));

        } catch (InvalidKeyException| BadPaddingException|IllegalBlockSizeException |InvalidAlgorithmParameterException|NoSuchAlgorithmException|NoSuchPaddingException e) {
            System.out.println("Error when encrypting " + e.getMessage());
            return new byte[0];
        }
        return ciphertext;
    }

    /***
     *decrypt String Message using cipher, password, iv
     * @param cipher aes/ctr or aes/cbc
     * @param pass shred password
     * @param ivString sting iv the converted to byt[]
     * @param message the message to encrypt
     * @return string decrypted message
     */
    public static String decryptMessage(String cipher, String pass, String ivString,String message){

        MessageDigest mesDigest;
        Cipher crypto;
        try {

            mesDigest = MessageDigest.getInstance(Constants.SHA256);
            crypto = Cipher.getInstance(cipher);
            SecretKeySpec key = new SecretKeySpec(MethodHelper.hexToBytes(pass), Constants.AES);
            crypto.init(Cipher.DECRYPT_MODE, key,getIvParameterSpec(ivString));
            byte[]  plaintext = crypto.doFinal(MethodHelper.stringToBytes(message));
            return   new String(plaintext, StandardCharsets.UTF_8);

        } catch (InvalidKeyException|BadPaddingException|IllegalBlockSizeException|InvalidAlgorithmParameterException|NoSuchAlgorithmException |NoSuchPaddingException e) {
            System.out.println("Error when decrypting " + e.getMessage());
            return "";
        }
    }

    /***
     * encrypt byt[] file using cipher, password, iv
     * @param cipher aes/ctr or aes/cbc
     * @param pass shred password
     * @param iv iv
     * @param file the file data to encrypt
     * @return encrypted file[]
     */
    public static byte[] encryptFile(String cipher,String pass, IvParameterSpec iv ,byte[] file){

        byte[] ciphertFile = new byte[0];
        try {
            mesDigest = MessageDigest.getInstance(Constants.SHA256);
            crypto = Cipher.getInstance(cipher);
            SecretKeySpec key = new SecretKeySpec(MethodHelper.hexToBytes(pass), Constants.AES);
            crypto.init(Cipher.ENCRYPT_MODE, key,iv);
            ciphertFile = crypto.doFinal(file);

        } catch (InvalidKeyException|BadPaddingException|IllegalBlockSizeException |InvalidAlgorithmParameterException |NoSuchAlgorithmException |NoSuchPaddingException e) {
            System.out.println("Error:  when encrypting: " + e.getMessage());
            return new byte[0];
        }
        return ciphertFile;
    }

    /***
     *decrypt file byte[] using cipher, password, iv
     * @param cipher aes/ctr or aes/cbc
     * @param pass shred password
     * @param ivString
     * @param file the file to decrypt
     * @return decrypted file[]
     */
    public static byte[] decryptFile(String cipher, String pass, String ivString,byte[] file){

        MessageDigest mesDigest;
        Cipher crypto;
        byte[]  fileDec;
        try {
            mesDigest = MessageDigest.getInstance(Constants.SHA256);
            crypto = Cipher.getInstance(cipher);
            SecretKeySpec key = new SecretKeySpec(MethodHelper.hexToBytes(pass), Constants.AES);
            crypto.init(Cipher.DECRYPT_MODE, key,getIvParameterSpec(ivString));
            fileDec = crypto.doFinal(file);

        } catch ( InvalidKeyException|BadPaddingException|IllegalBlockSizeException|InvalidAlgorithmParameterException|NoSuchAlgorithmException |NoSuchPaddingException e) {
            System.out.println("Error: Invalid key when encrypting: " + e.getMessage());
            return new byte[0];
        }
        return fileDec;
    }

    /***
     * convert string iv to getIvParameterSpec
     * @param iv string iv
     * @return getIvParameterSpec
     */
    public static IvParameterSpec getIvParameterSpec(String iv){
        IvParameterSpec ivf =new IvParameterSpec(MethodHelper.stringToBytes(iv));
        return ivf;
    }

    /***
     * generate new random iv
     * @return IvParameterSpec
     */
    public static IvParameterSpec generateIV(){
        SecureRandom randomSecureRandom = new SecureRandom();
        byte[] iv = new byte[16];
        randomSecureRandom.nextBytes(iv);
        IvParameterSpec ivParams = new IvParameterSpec(iv);
        return ivParams;
    }

    /***
     * compute hmac using hmac password and data
     * @param hmacPass hmac password
     * @param data file or string
     * @return byte[]
     */
    public static byte[] computeHMAC(String hmacPass, byte[] data){
        byte[] hmac;
        try {
            mesDigest = MessageDigest.getInstance(Constants.SHA256);
            mac = Mac.getInstance(Constants.HMAC_SHA256);
            SecretKeySpec key = new SecretKeySpec(MethodHelper.hexToBytes(hmacPass), Constants.HMAC_SHA256);
            mac.init(key);
            hmac= mac.doFinal(data);
        } catch (NoSuchAlgorithmException e) {
            System.out.println( "NoSuchAlgorithmException "+e.getMessage());
            return new byte[0];
        } catch (InvalidKeyException e) {
            System.out.println( "InvalidKeyException "+e.getMessage());
            return new byte[0];
        }
        return hmac;
    }
}
