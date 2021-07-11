package sample.main;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class Crypto {
    private static MessageDigest mesDigest;

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
        Cipher crypto;
        Boolean isGcm=false;
        try {
            if (cipher.equals(Constants.AES_GCM))isGcm=true;

            mesDigest = MessageDigest.getInstance(Constants.SHA256);
            crypto = Cipher.getInstance(cipher);
            SecretKeySpec key = new SecretKeySpec(MethodHelper.hexToBytes(pass), Constants.AES);

            if (isGcm){
                GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(Constants.TAG_LENGTH_BIT, iv.getIV());
                crypto.init(Cipher.ENCRYPT_MODE, key,gcmParameterSpec);
                crypto.updateAAD("add".getBytes(StandardCharsets.UTF_8));
            }else crypto.init(Cipher.ENCRYPT_MODE, key,iv);

            ciphertext = crypto.doFinal(message.getBytes(StandardCharsets.UTF_8));

        } catch (InvalidKeyException| BadPaddingException|IllegalBlockSizeException |IllegalStateException|InvalidAlgorithmParameterException|NoSuchAlgorithmException|NoSuchPaddingException e) {
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
        Boolean isGcm=false;
        try {
            if (cipher.equals(Constants.AES_GCM))isGcm=true;
            mesDigest = MessageDigest.getInstance(Constants.SHA256);
            crypto = Cipher.getInstance(cipher);
            SecretKeySpec key = new SecretKeySpec(MethodHelper.hexToBytes(pass), Constants.AES);

            if (isGcm){
                GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(Constants.TAG_LENGTH_BIT, getIvParameterSpec(ivString).getIV());
                crypto.init(Cipher.DECRYPT_MODE, key,gcmParameterSpec);
                crypto.updateAAD("add".getBytes(StandardCharsets.UTF_8));
            }else crypto.init(Cipher.DECRYPT_MODE, key,getIvParameterSpec(ivString));

            byte[]  plaintext = crypto.doFinal(MethodHelper.stringToBytes(message));
            return   new String(plaintext, StandardCharsets.UTF_8);

        } catch (InvalidKeyException|BadPaddingException|IllegalBlockSizeException|IllegalStateException|InvalidAlgorithmParameterException|NoSuchAlgorithmException |NoSuchPaddingException e) {
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
        Cipher crypto;
        Boolean isGcm=false;
        try {
            if (cipher.equals(Constants.AES_GCM))isGcm=true;
            mesDigest = MessageDigest.getInstance(Constants.SHA256);
            crypto = Cipher.getInstance(cipher);
            SecretKeySpec key = new SecretKeySpec(MethodHelper.hexToBytes(pass), Constants.AES);

            if (isGcm){
                GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(Constants.TAG_LENGTH_BIT, iv.getIV());
                crypto.init(Cipher.ENCRYPT_MODE, key,gcmParameterSpec);
                crypto.updateAAD("add".getBytes(StandardCharsets.UTF_8));
            }else crypto.init(Cipher.ENCRYPT_MODE, key,iv);

            ciphertFile = crypto.doFinal(file);

        } catch (InvalidKeyException|BadPaddingException|IllegalBlockSizeException|IllegalStateException |InvalidAlgorithmParameterException |NoSuchAlgorithmException |NoSuchPaddingException e) {
            System.out.println("Error:  when encryptFile : " + e.getMessage());
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
        Boolean isGcm=false;
        try {
            if (cipher.equals(Constants.AES_GCM))isGcm=true;
            mesDigest = MessageDigest.getInstance(Constants.SHA256);
            crypto = Cipher.getInstance(cipher);
            SecretKeySpec key = new SecretKeySpec(MethodHelper.hexToBytes(pass), Constants.AES);
            if (isGcm){
                GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(Constants.TAG_LENGTH_BIT, getIvParameterSpec(ivString).getIV());
                crypto.init(Cipher.DECRYPT_MODE, key,gcmParameterSpec);
                crypto.updateAAD("add".getBytes(StandardCharsets.UTF_8));
            }else crypto.init(Cipher.DECRYPT_MODE, key,getIvParameterSpec(ivString));

            fileDec = crypto.doFinal(file);

        } catch ( InvalidKeyException|BadPaddingException|IllegalBlockSizeException|IllegalStateException|InvalidAlgorithmParameterException|NoSuchAlgorithmException |NoSuchPaddingException e) {
            System.out.println("Error: when decrypt File: " + e.getMessage());
            return new byte[0];
        }
        return fileDec;
    }

    /***
     * Encrypts the text using RSA and public key provided
     * @param cipher Rsa cipher
     * @param plainText The text to encrypt
     * @param publicKeyParam The public key to use for the encryption
     * @return
     */
    public static byte[] encryptRSA(String cipher, String plainText, String  publicKeyParam )
    {
        byte[] cipherByte = new byte[0];
        byte[] publicKeyBytes = null;
        RSAPublicKey publicKey;
        Cipher crypto;
        try {

            publicKeyBytes=MethodHelper.hexToBytes(publicKeyParam);
            X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            publicKey = (RSAPublicKey) keyFactory.generatePublic(publicSpec);
            crypto = Cipher.getInstance(cipher);
            crypto.init(Cipher.ENCRYPT_MODE, publicKey);
            cipherByte = crypto.doFinal(plainText.getBytes());
        }
        catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException | IllegalStateException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeySpecException e) {
        System.out.println("Error: when encryptAes: " + e.getMessage());
        return new byte[0];
        }
        return cipherByte;
    }

    /***
     * deccrypt the text using RSA and public key provided
     * @param cipher Rsa cipher
     * @param cipherText The text to encrypt
     * @param privateKeyParam The private key to use for the encryption
     * @return
     */
    public static String decryptRSA(String cipher, String cipherText, String privateKeyParam)
    {

        Cipher crypto;
        byte[] privateKeyBytes = null;
        RSAPrivateKey privateKey;
        try {

            privateKeyBytes=MethodHelper.hexToBytes(privateKeyParam);
            PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            privateKey = (RSAPrivateKey) keyFactory.generatePrivate(privateSpec);

            crypto= Cipher.getInstance(cipher);
            crypto.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedTextArray = crypto.doFinal(MethodHelper.stringToBytes(cipherText));
            return new String(decryptedTextArray);
        }
        catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException | IllegalStateException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeySpecException e) {
            System.out.println("Error when decrypting " + e.getMessage());
            return "";
         }
    }


    public static IvParameterSpec getIvParameterSpec(String iv){
        IvParameterSpec ivf =new IvParameterSpec(MethodHelper.stringToBytes(iv));
        return ivf;
    }

    /***
     * generate new session key 16B
     * @return
     */
    public static byte[]  generatekey(){
        SecureRandom randomSecureRandom = new SecureRandom();
        byte[] sessionKey = new byte[Constants.SESSION_KEY_BYTE];
        randomSecureRandom.nextBytes(sessionKey);
        return sessionKey;
    }

    /***
     * generate new random iv
     * @return IvParameterSpec
     */
    public static IvParameterSpec generateIV(){
        SecureRandom randomSecureRandom = new SecureRandom();
        byte[] iv = new byte[Constants.IV_LENGTH_BYTE];
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
