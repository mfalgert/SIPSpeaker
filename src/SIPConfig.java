/**
 * Created by Robert Norgren Erneborg on 2015-04-23.
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class SIPConfig {
	String fileName;
	String default_message_wav = null;
	String message_wav = null;
	String sip_interface = null;
	String sip_port = null;
	String sip_user = null;

	public SIPConfig(String fileName){
		this.fileName = fileName;

		File file = new File(fileName);
		if (file.exists()) {
			try {
				BufferedReader fileReader = new BufferedReader(new FileReader(file));
				String line, field, value, trimmedField, trimmedValue;
				String[] parts;

				while ((line = fileReader.readLine()) != null) {
					parts = line.split("=");
					if(parts.length == 2){
						field = parts[0];
						trimmedField = field.replaceAll("\\s","");
						value = parts[1];
						trimmedValue = value.replaceAll("\\s","");

						if(trimmedField.equals("default_message_wav")){
							default_message_wav = trimmedValue;
						}
						else if(trimmedField.equals("message_wav")){
							message_wav = trimmedValue;
						}
						else if(trimmedField.equals("sip_interface")){
							sip_interface = trimmedValue;
						}
						else if(trimmedField.equals("sip_port")){
							sip_port = trimmedValue;
						}
						else if(trimmedField.equals("sip_user")){
							sip_user = trimmedValue;
						}
					}
				}
			}
			catch(Exception e){
				System.out.println("Failed to read the contents of the config file");
			}
		}
		else {
			System.out.println("The specified config file does not exist");
		}

		System.out.println(default_message_wav + " / " + message_wav + " / " + sip_interface + " / " + sip_port + " / " + sip_user);
	}

	public String getDefaultMessage(){
		return default_message_wav;
	}
	public String getMessage(){
		return message_wav;
	}
	public String getSipInterface(){
		return sip_interface;
	}
	public String getSipPort(){
		return sip_port;
	}
	public String getSipUser(){
		return sip_user;
	}
}
