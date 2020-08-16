package priv.zujool.crypto;

import lombok.SneakyThrows;

import javax.crypto.*;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class CryptoUtils {

    private static final String HEX_SEQUENCE = "0123456789abcdef";

    private final static String KEY_ENCODING = "UTF-8";
    private final static String WORK_MODE_CBC = "CBC";
    private final static String WORK_MODE_ECB = "ECB";

    public static byte[] desedeCBCEncrypt(byte[] source, String secretKey, String iv) {
        return desedeCrypt(source, WORK_MODE_CBC, secretKey, iv, Cipher.ENCRYPT_MODE);
    }

    public static byte[] desedeCBCDecrypt(byte[] source, String secretKey, String iv) {
        return desedeCrypt(source, WORK_MODE_CBC, secretKey, iv, Cipher.DECRYPT_MODE);
    }

    public static byte[] desECBEncrypt(byte[] source, String secretKey) {
        return desCrypt(source, WORK_MODE_ECB, secretKey, null, Cipher.ENCRYPT_MODE);
    }

    public static byte[] desECBDecrypt(byte[] source, String secretKey) {
        return desCrypt(source, WORK_MODE_ECB, secretKey, null, Cipher.DECRYPT_MODE);
    }

    /**
     * 加密
     * CBC工作模式需要IVParam
     */
    public static byte[] desedeCrypt(byte[] source, String mode, String secretKey, String iv, int cipherMode) {
        try {
            if (WORK_MODE_CBC.equals(mode)) {
                DESedeKeySpec spec = new DESedeKeySpec(secretKey.getBytes(KEY_ENCODING));
                SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DESede");
                SecretKey key = keyFactory.generateSecret(spec);
                Cipher cipher = Cipher.getInstance("desede/CBC/PKCS5Padding");
                IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes());
                cipher.init(cipherMode, key, ivParameterSpec);
                return cipher.doFinal(source);
            } else {
                SecureRandom random = new SecureRandom();
                DESKeySpec spec = new DESKeySpec(secretKey.getBytes(KEY_ENCODING));
                SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DESede");
                SecretKey key = keyFactory.generateSecret(spec);
                Cipher cipher = Cipher.getInstance("DESede");
                cipher.init(cipherMode, key, random);
                return cipher.doFinal(source);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] desCrypt(byte[] source, String mode, String secretKey, String iv, int cipherMode) {
        Key deskey;
        try {
            if (WORK_MODE_CBC.equals(mode)) {
                DESedeKeySpec spec = new DESedeKeySpec(secretKey.getBytes());
                SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
                deskey = keyFactory.generateSecret(spec);
                Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
                IvParameterSpec ivps = new IvParameterSpec(iv.getBytes());
                cipher.init(cipherMode, deskey, ivps);
                return cipher.doFinal(source);
            } else {
                SecureRandom random = new SecureRandom();
                DESKeySpec spec = new DESKeySpec(secretKey.getBytes());
                SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
                SecretKey key = keyFactory.generateSecret(spec);
                Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
                cipher.init(cipherMode, key, random);
                return cipher.doFinal(source);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] aesEncrypt(String plainText, String key) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
            //创建密码器
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            byte[] byteContent = plainText.getBytes("UTF-8");
            //初始化
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return cipher.doFinal(byteContent);
        } catch (NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | UnsupportedEncodingException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] aesDecrypt(String plainText, String key) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
            //创建密码器
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            byte[] byteContent = Base64.getDecoder().decode(plainText);
            //初始化
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return cipher.doFinal(byteContent);
        } catch (NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | UnsupportedEncodingException | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
        return new byte[]{};
    }

    /**
     * 用私钥对信息生成数字签名
     *
     * @param data       加密数据
     * @param privateKey 私钥
     */
    public static String rsaSign(byte[] data, String privateKey) throws Exception {
        // 解密由base64编码的私钥
        byte[] keyBytes = Base64.getDecoder().decode(privateKey);
        // 构造PKCS8EncodedKeySpec对象
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
        // KEY_ALGORITHM 指定的加密算法
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        // 取私钥匙对象
        PrivateKey priKey = keyFactory.generatePrivate(pkcs8KeySpec);
        // 用私钥对信息生成数字签名
        Signature signature = Signature.getInstance("MD5withRSA");
        signature.initSign(priKey);
        signature.update(data);
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    /**
     * 公钥加密
     *
     * @param data         加密数据
     * @param base64PubKey 私钥
     */
    public static byte[] rsaPubKeyEncrypt(byte[] data, String base64PubKey) throws Exception {
        // 解密由base64编码的私钥
        byte[] keyBytes = Base64.getDecoder().decode(base64PubKey);
        RSAPublicKey rsaPubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
        Cipher cipher = Cipher.getInstance("RSA");
        //初始化
        cipher.init(Cipher.ENCRYPT_MODE, rsaPubKey);
        return cipher.doFinal(data);
    }

    /**
     * 私钥解密
     *
     * @param data         加密数据
     * @param base64PriKey 私钥
     */
    @SneakyThrows
    public static byte[] rsaPriKeyDecrypt(byte[] data, String base64PriKey){
        // 解密由base64编码的私钥
        byte[] keyBytes = Base64.getDecoder().decode(base64PriKey);
        RSAPrivateKey rsaPriKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        Cipher cipher = Cipher.getInstance("RSA");
        //初始化
        cipher.init(Cipher.DECRYPT_MODE, rsaPriKey);
        return cipher.doFinal(data);
    }

    @SneakyThrows
    public static List<String> rsaKeyPairGenerate(){
        List<String> keys = new ArrayList<>(2);
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(1024,new SecureRandom());
        KeyPair keyPair = keyPairGen.generateKeyPair();
        // 得到私钥
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        // 得到公钥
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        String publicKeyString = new String(Base64.getEncoder().encode(publicKey.getEncoded()));
        // 得到私钥字符串
        String privateKeyString = new String(Base64.getEncoder().encode((privateKey.getEncoded())));
        keys.add(publicKeyString);
        keys.add(privateKeyString);
        return keys;
    }

    /**
     * 16进制字符串转字节数组 两位16进制转一个字节
     *
     * @param hexStr hex
     * @return bytes
     */
    public static byte[] hexStrToBytes(String hexStr) {
        char[] hexs = hexStr.toCharArray();
        byte[] bytes = new byte[hexStr.length() / 2];

        for (int i = 0; i < bytes.length; ++i) {
            int n = HEX_SEQUENCE.indexOf(hexs[2 * i]) * 16;
            n += HEX_SEQUENCE.indexOf(hexs[2 * i + 1]);
            bytes[i] = (byte) n;
        }
        return bytes;
    }

    /**
     * 字节数组转hex串
     *
     * @param bytes bs
     * @return string
     */
    public static String bytesToHexStr(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }


}
