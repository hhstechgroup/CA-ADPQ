package com.agreeya.chhs.bd.impl;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

import com.agreeya.chhs.bd.AESSecurityBD;
import com.agreeya.chhs.exception.WSException;
import com.agreeya.chhs.util.CHHSErrorCodes;

/**
 * Imlementation for AESSecurityBD, this class provides methods
 * for AES security encryption, Decryption
 */
public class AESSecurityBDImpl implements AESSecurityBD {

	private static Logger log = Logger.getLogger(AESSecurityBDImpl.class);

	@SuppressWarnings("unused")
	private final SecureRandom rand;
	private final String KEY_ALGORITHM = "PBEWITHSHA256AND128BITAES-CBC-BC";
	private final String MODE_PADDING = "/CBC/PKCS5Padding";
	private final int DEFAULT_SALT_SIZE_BYTES = 32;
	private final String passwd = "8g5qT74KdUY";

	public AESSecurityBDImpl() throws Exception {
		rand = SecureRandom.getInstance("SHA1PRNG");
	}

	/* overloaded method called to encrypt the data for email */
	public String encryptAES(String data) throws WSException {
		log.info("enter into AESSecurityBDImpl encryptAES() overloadewd() method....................");
		String encryptedString = "";
		Security.addProvider(new BouncyCastleProvider());
		try {
			encryptedString = new String(Base64.encode(encrypt("AES", 128, data.getBytes())));
		} catch (Exception e) {
			log.error(CHHSErrorCodes.DATA_ENCRYPTION_ERROR_MESSAGE, e);
		}
		log.info("exit from AESSecurityBDImpl encryptAES() overloadewd method returns...................." + encryptedString);
		return encryptedString;
	}

	/*
	 * @purpose : this method is used to encrypt the data
	 * 
	 * @parameter :algorithm,keySize,message
	 */
	public byte[] encrypt(String algorithm, int keySize, final byte[] message) throws Exception {
		Cipher cipher = Cipher.getInstance(algorithm + MODE_PADDING);

		// The salt size for the chosen algorithm is set to be equal
		// to the algorithm's block size (if it is a block algorithm).
		int saltSizeBytes = DEFAULT_SALT_SIZE_BYTES;
		int algorithmBlockSize = cipher.getBlockSize();
		if (algorithmBlockSize > 0) {
			saltSizeBytes = algorithmBlockSize;
		}
		// Create salt
		final byte[] salt = generateSalt(saltSizeBytes);
		SecretKey key = generateKey(algorithm, keySize, salt);
		// create a new IV for each encryption
		final IvParameterSpec ivParamSpec = new IvParameterSpec(generateIV(cipher));
		// Perform encryption using the Cipher
		cipher.init(Cipher.ENCRYPT_MODE, key, ivParamSpec);
		byte[] encryptedMessage = cipher.doFinal(message);

		// append the IV and salt
		encryptedMessage = appendArrays(ivParamSpec.getIV(), encryptedMessage);
		encryptedMessage = appendArrays(salt, encryptedMessage);
		return encryptedMessage;
	}

	/*
	 * @purpose : this method is used to generate the salt
	 * 
	 * @parameter :size
	 */
	private byte[] generateSalt(int size) {
		byte[] salt = new byte[size];
		rand.nextBytes(salt);
		return salt;
	}

	/*
	 * 
	 * @purpose : this method is used to generate the random key using SALT,method is called on encrption and decryption
	 * 
	 * @parameter :algorithm,keySize,salt
	 */
	private SecretKey generateKey(String algorithm, int keySize, byte[] salt)
			throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException {
		SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
		PBEKeySpec pbeKeySpec = new PBEKeySpec(passwd.toCharArray(), salt, 100000);
		SecretKey tmpKey = factory.generateSecret(pbeKeySpec);
		byte[] keyBytes = new byte[keySize / 8];
		System.arraycopy(tmpKey.getEncoded(), 0, keyBytes, 0, keyBytes.length);
		return new SecretKeySpec(keyBytes, algorithm);
	}

	// generateIV method
	private byte[] generateIV(Cipher cipher) {
		byte[] iv = new byte[cipher.getBlockSize()];
		rand.nextBytes(iv);

		return iv;
	}

	private byte[] appendArrays(byte[] firstArray, byte[] secondArray) {
		final byte[] result = new byte[firstArray.length + secondArray.length];

		System.arraycopy(firstArray, 0, result, 0, firstArray.length);
		System.arraycopy(secondArray, 0, result, firstArray.length, secondArray.length);
		return result;
	}

	/*
	 * @purpose : this method is called from decryptAES() to decrypt the encrypt data
	 * 
	 * @parameter : algorithm, keySize, encryptedMessage
	 */
	public byte[] decrypt(String algorithm, int keySize, final byte[] encryptedMessage) throws Exception {
		Cipher cipher = Cipher.getInstance(algorithm + MODE_PADDING);

		// determine the salt size for the first layer of encryption
		int saltSizeBytes = DEFAULT_SALT_SIZE_BYTES;
		int algorithmBlockSize = cipher.getBlockSize();
		if (algorithmBlockSize > 0) {
			saltSizeBytes = algorithmBlockSize;
		}
		byte[] decryptedMessage = new byte[encryptedMessage.length];
		System.arraycopy(encryptedMessage, 0, decryptedMessage, 0, encryptedMessage.length);

		// extract the salt and IV from the incoming message
		byte[] salt = null;
		byte[] iv = null;
		byte[] encryptedMessageKernel = null;
		final int saltStart = 0;
		final int saltSize = (saltSizeBytes < decryptedMessage.length ? saltSizeBytes : decryptedMessage.length);
		// final int saltSize = 32;
		// System.out.println("saltSize:"+saltSize);

		final int ivStart = (saltSizeBytes < decryptedMessage.length ? saltSizeBytes : decryptedMessage.length);
		final int ivSize = cipher.getBlockSize();
		final int encMesKernelStart = (saltSizeBytes + ivSize < decryptedMessage.length ? saltSizeBytes + ivSize
				: decryptedMessage.length);
		final int encMesKernelSize = (saltSizeBytes + ivSize < decryptedMessage.length 
				? (decryptedMessage.length - saltSizeBytes - ivSize)
				: 0);

		salt = new byte[saltSize];
		iv = new byte[ivSize];

		encryptedMessageKernel = new byte[encMesKernelSize];

		System.arraycopy(decryptedMessage, saltStart, salt, 0, saltSize);
		System.arraycopy(decryptedMessage, ivStart, iv, 0, ivSize);
		System.arraycopy(decryptedMessage, encMesKernelStart, encryptedMessageKernel, 0, encMesKernelSize);

		SecretKey key = generateKey(algorithm, keySize, salt);

		IvParameterSpec ivParamSpec = new IvParameterSpec(iv);

		// Perform decryption using the Cipher
		cipher.init(Cipher.DECRYPT_MODE, key, ivParamSpec);
		decryptedMessage = cipher.doFinal(encryptedMessageKernel);

		// Return the results
		return decryptedMessage;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.agreeya.tta.bd.AESSecurityBD#decryptAES(java.lang.String)
	 */
	@Override
	public String decryptData(String encodedString) throws WSException {
		byte[] criptata = Base64.decode(encodedString);
		String decripdata = "";
		try {
			decripdata = new String(decrypt("AES", 128, criptata));
		} catch (Exception e) {
			log.error(CHHSErrorCodes.DECRYPT_DATA_ERROR_MESSAGE, e);
		}
		System.out.println(decripdata);
		return decripdata;

	}
}
