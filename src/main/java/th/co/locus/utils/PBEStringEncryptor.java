package th.co.locus.utils;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

public class PBEStringEncryptor {
	
	StandardPBEStringEncryptor stringEncryptor;
	
	public PBEStringEncryptor(String secretKey) {
		stringEncryptor = new StandardPBEStringEncryptor();
		stringEncryptor.setAlgorithm("PBEWithMD5AndDES");
		stringEncryptor.setPassword(secretKey);
	}
	
	public String encrypt(String strToEncrypt) {
		String encrypted = this.stringEncryptor.encrypt(strToEncrypt);
		System.out.println("Encrypted = " + encrypted);
		return encrypted;
	}

	public String decrypt(String strToDecrypt) {
		String decrypted = this.stringEncryptor.decrypt(strToDecrypt);
		System.out.println("decrypted = " + decrypted);
		return decrypted;
	}

	public static void main(String[] args) {
//		final String secretKey = "locus123";
//
//		String originalString = "Locus@123";
//		String encryptedString = AES.encrypt(originalString, secretKey);
//		String decryptedString = AES.decrypt(encryptedString, secretKey);
//
//		System.out.println(originalString);
//		System.out.println(encryptedString);
//		System.out.println(decryptedString);

		String text = "P@ssw0rd";
		System.out.println("Text = " + text);

//        StandardPBEByteEncryptor encryptor = new StandardPBEByteEncryptor();
//        encryptor.setAlgorithm("PBEWithMD5AndDES");
//        encryptor.setPassword("locus123");
//
//        byte[] encrypted = encryptor.encrypt(text.getBytes());
//        System.out.println("Encrypted = " + new String(encrypted, StandardCharsets.UTF_8));
//
//        byte[] original = encryptor.decrypt(encrypted);
//        System.out.println("Original  = " + new String(original, StandardCharsets.UTF_8));

		StandardPBEStringEncryptor stringEncryptor = new StandardPBEStringEncryptor();
		stringEncryptor.setAlgorithm("PBEWithMD5AndDES");
		stringEncryptor.setPassword("locus123");
		String encrypted = stringEncryptor.encrypt(text);
		System.out.println("Encrypted = " + encrypted);
	}
}
