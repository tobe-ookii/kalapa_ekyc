package vn.kalapa.ekyc.managers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class AESCryptor {
    static {
        System.loadLibrary("klpsecure");
    }

    public static final int ENCRYPT = 0;

    public static final int DECRYPT = 1;

    public native static byte[] crypt(byte[] data, long time, int mode);

    public native static byte[] read(String path, long time);

    public static String encryptText(String str) {
        byte[] encData = str.getBytes(StandardCharsets.UTF_8);
        return bytes2HexStr(crypt(encData, System.currentTimeMillis(), ENCRYPT));
    }

    public static String encryptByteArray(byte[] bytes) {
        return bytes2HexStr(crypt(bytes, System.currentTimeMillis(), ENCRYPT));
    }

    public static String decryptText(String str) {
        byte[] encData = hexStr2Bytes(str);
        return new String(crypt(encData, System.currentTimeMillis(), DECRYPT), StandardCharsets.UTF_8);
    }

    public static byte[] hexStr2Bytes(String hexStr) {
        if (hexStr.length() < 1)
            return null;
        byte[] result = new byte[hexStr.length() / 2];
        for (int i = 0; i < hexStr.length() / 2; i++) {
            int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
            int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
            result[i] = (byte) (high * 16 + low);
        }
        return result;
    }

    public static String bytes2HexStr(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        StringBuilder buf = new StringBuilder();
        for (byte datum : data) {
            if (((int) datum & 0xff) < 0x10) {
                buf.append("0");
            }
            buf.append(Long.toHexString((int) datum & 0xff));
        }
        return buf.toString();
    }

    public static String hash(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder result = new StringBuilder();
            for (byte b : digest) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (Exception e) {
            return "";
        }
    }

}
