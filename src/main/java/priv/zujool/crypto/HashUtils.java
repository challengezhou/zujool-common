package priv.zujool.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 
 * @author zujool  At 2020/4/7 10:23
**/
public class HashUtils {

    public static final String ALG_MD5 = "md5";
    public static final String ALG_SHA1 = "sha-1";


    private MessageDigest messageDigest;

    private void setNowMessageDigest(MessageDigest messageDigest) {
        this.messageDigest = messageDigest;
    }

    private HashUtils(){}

    /**
     * 应用hash算法
     * @param hashAlgorithm 支持的hash算法
     * @return 工具类
     */
    public static HashUtils use(String hashAlgorithm){
        HashUtils hashUtil = new HashUtils();
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(hashAlgorithm);
            hashUtil.setNowMessageDigest(messageDigest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return hashUtil;
    }

    public String getHashString(String ...target){
        StringBuilder builder = new StringBuilder();
        if (target.length == 0){
            throw new RuntimeException("hash string can not be empty");
        }
        for (String str:target){
            builder.append(str);
        }
        String finalStr = builder.toString();
        byte[] bytes = finalStr.getBytes();
        byte[] results = messageDigest.digest(bytes);
        StringBuilder stringBuilder = new StringBuilder();
        for (byte result : results) {
            // 将byte数组转化为16进制字符存入stringBuilder中
            stringBuilder.append(String.format("%02x", result));
        }
        return stringBuilder.toString();
    }

}
