package ch.kanti_baden.pu_marc_14b.traffictimewaste;

/*
Copyright (C) 2012 Sveinung Kval Bakken, sveinung.bakken@gmail.com
Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:
The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;


class SecurePreferences {
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String KEY_TRANSFORMATION = "AES/CFB/PKCS5Padding";
    private static final String SECRET_KEY_HASH_TRANSFORMATION = "SHA-256";
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private final boolean encryptKeys;
    private final Cipher writer;
    private final Cipher reader;
    private final Cipher keyWriter;
    private final SharedPreferences preferences;

    /**
     * This will initialize an instance of the SecurePreferences class
     * @param context your current context.
     * @param secureKey the key used for encryption, finding a good key scheme is hard.
     * Hardcoding your key in the application is bad, but better than plaintext preferences. Having the user enter the key upon application launch is a safe(r) alternative, but annoying to the user.
     * true will encrypt both values and keys. Keys can contain a lot of information about
     * the plaintext value of the value which can be used to decipher the value.
     * @throws GeneralSecurityException
     */
    SecurePreferences(Context context, String secureKey, String ivStr) throws GeneralSecurityException {
        this.writer = Cipher.getInstance(TRANSFORMATION);
        this.reader = Cipher.getInstance(TRANSFORMATION);
        this.keyWriter = Cipher.getInstance(KEY_TRANSFORMATION);

        initCiphers(secureKey, ivStr);

        this.preferences = context.getSharedPreferences("credentials", Context.MODE_PRIVATE);

        this.encryptKeys = false; // A value of "true" WILL fail, as the key is not recognized after a restart of the app (don't ask me why ^^)
    }

    private void initCiphers(String secureKey, String ivStr) throws NoSuchAlgorithmException, InvalidKeyException,
            InvalidAlgorithmParameterException {
        IvParameterSpec ivSpec = getIv(ivStr);
        SecretKeySpec secretKey = getSecretKey(secureKey);

        writer.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        reader.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        keyWriter.init(Cipher.ENCRYPT_MODE, secretKey);
    }

    private IvParameterSpec getIv(String ivStr) {
        byte[] iv = new byte[writer.getBlockSize()];
        byte[] bytes = ivStr.getBytes(StandardCharsets.UTF_8);

        System.arraycopy(bytes, 0, iv, 0, writer.getBlockSize());
        return new IvParameterSpec(iv);
    }

    private SecretKeySpec getSecretKey(String key) throws NoSuchAlgorithmException {
        byte[] keyBytes = createKeyBytes(key);
        return new SecretKeySpec(keyBytes, TRANSFORMATION);
    }

    private byte[] createKeyBytes(String key) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(SECRET_KEY_HASH_TRANSFORMATION);
        md.reset();
        return md.digest(key.getBytes(CHARSET));
    }

    void put(String key, String value) {
        if (value == null) {
            preferences.edit().remove(toKey(key)).apply();
        }
        else {
            putValue(toKey(key), value);
        }
    }

    String getString(String key) {
        if (preferences.contains(toKey(key))) {
            String securedEncodedValue = preferences.getString(toKey(key), "");
            return decrypt(securedEncodedValue);
        } else
            return null;
    }

    private String toKey(String key) {
        if (encryptKeys)
            return encrypt(key, keyWriter);
        else
            return key;
    }

    private void putValue(String key, String value) {
        String secureValueEncoded = encrypt(value, writer);

        preferences.edit().putString(key, secureValueEncoded).apply();
    }

    private String encrypt(String value, Cipher writer) {
        byte[] secureValue;
        secureValue = convert(writer, value.getBytes(CHARSET));
        return Base64.encodeToString(secureValue, Base64.NO_WRAP);
    }

    @Nullable
    private String decrypt(String securedEncodedValue) {
        byte[] securedValue = Base64.decode(securedEncodedValue, Base64.NO_WRAP);
        byte[] value = convert(reader, securedValue);
        if (value != null)
            return new String(value, CHARSET);
        else
            return null;
    }

    @Nullable
    private static byte[] convert(Cipher cipher, byte[] bs) {
        try {
            return cipher.doFinal(bs);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            Log.e("TrafficTimeWaste", "Could not read SecurePreferences", e);
            return null;
        }
    }
}