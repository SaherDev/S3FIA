package sample.main;

public class Constants {
    public final static String UPLOAD = "UPLOAD";
    public final static String DOWNLOAD = "DOWNLOAD";
    public final static String DELETE = "DELETE";
    public final static String MOVE = "MOVE";
    public final static String GET_LIST = "GET_LIST";
    public final static String GET_VERSION = "GET_VERSION";
    public final static String OK = "OK";
    public final static String OFFER = "OFFER";
    public final static String ERROR = "ERROR";


    public final static String CHALLENGE = "CHALLENGE";
    public final static String UNSUCCESSFUL_CHALLENGE = "UNSUCCESSFUL_CHALLENGE";
    public final static String IOException = "IOException";
    public final static String DECRYPTION = "DECRYPTION";
    public final static String FILE_DECRYPTION = "FILE_DECRYPTION";
    public final static String INVALID_HMAC = "INVALID_HMAC";
    public final static String COMMAND_SYNTAX = "COMMAND_SYNTAX";
    public final static String FILE_NOT_EXIST = "FILE_NOT_EXIST";
    public final static String FILE_WITH_DIFFERENT_HASH  = "FILE_WITH_DIFFERENT_HASH";
    public final static String SMALLER_VERSION_OR_SAME_HASH  = "SMALLER_VERSION_OR_SAME_HASH";
    public final static String There_is_no_files  = "there is no files";


    public final static String NO_ATTACK="NO_ATTACK";
    public final static String WRONG_NONCE = "WRONG_NONCE";
    public final static String WRONG_TIME = "WRONG_TIME";
    public final static String WRONG_PUBLIC_KEY="WRONG_PUBLIC_KEY";

    public final static String FILE_PATH_FORMAT = "PATH/FILE";
    public final static String HASH = "HASH=";

    public final static String AES="AES";
    public final static String SHA256 = "SHA-256";
    public final static String HMAC_SHA256 = "HmacSHA256";
    public final static String AES_CBC = "AES/CBC/PKCS5Padding";
    public final static String AES_GCM = "AES/GCM/NoPadding";
    public final static String RSA_ECB = "RSA/ECB/PKCS1Padding";

    public final static  int TAG_LENGTH_BIT = 128;
    public final static  int IV_LENGTH_BYTE= 16;
    public final static  int SESSION_KEY_BYTE= 16;


}
