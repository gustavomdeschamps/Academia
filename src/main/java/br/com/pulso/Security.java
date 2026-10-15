package br.com.pulso;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.*;
import java.util.Base64;

public final class Security {
    private static final int ITERATIONS=120000;
    private Security() {}
    public static String hash(String password) throws Exception {
        byte[] salt=new byte[16]; new SecureRandom().nextBytes(salt);
        byte[] key=derive(password,salt,ITERATIONS);
        return ITERATIONS+":"+Base64.getEncoder().encodeToString(salt)+":"+Base64.getEncoder().encodeToString(key);
    }
    public static boolean verify(String password,String encoded) throws Exception {
        String[] pieces=encoded.split(":");
        byte[] expected=Base64.getDecoder().decode(pieces[2]);
        byte[] actual=derive(password,Base64.getDecoder().decode(pieces[1]),Integer.parseInt(pieces[0]));
        return MessageDigest.isEqual(expected,actual);
    }
    private static byte[] derive(String password,byte[] salt,int iterations) throws Exception {
        PBEKeySpec spec=new PBEKeySpec(password.toCharArray(),salt,iterations,256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        finally { spec.clearPassword(); }
    }
}
