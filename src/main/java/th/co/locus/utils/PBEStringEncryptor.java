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
		return encrypted;
	}

	public String decrypt(String strToDecrypt) {
		String decrypted = this.stringEncryptor.decrypt(strToDecrypt);
		return decrypted;
	}
}
