import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Enumeration;

/**
 * Created by Robert Norgren Erneborg on 2015-04-12.
 */
public class SIPSpeaker implements RTPAppIntf{

	private static final String DEFAULT_SIP_USER = "sipspeaker";
	private static final String DEFAULT_SIP_INTERFACE = "0.0.0.0";
	private static final int DEFAULT_SIP_PORT = 5060;
	private static final String DEFAULT_CFG_FILE = "sipspeaker.cfg";
	private static final String DEFAULT_SOUND_FILE = "wav_files/burger.wav";
	private static final int DEFAULT_RTP_PORT = 49102;

	//private static final String DEFAULT_HTTP_ADDR = "127.0.0.1:8080";
	//private static final String DEFAULT_MESSAGE_TEXT = "Welcome to SIP Speaker. This is my own answering machine. You have no new messages.";

	static String cfgFile = null;
	static String sipURI = null;
	static String sipUser = null;
	static String sipInterface = null;
	static int sipPort = 0;
	static int rtpPort = 0;
	static String HTTPBindAddress = null;
	static String wavFile = null;

	static HashSet<String> sessionIDs = new HashSet<String>();
	static HashMap<String, SIPRequest> callID = new HashMap<String, SIPRequest>();
	//static String messageText = null;

	public static void main(String [] args){
		new SIPSpeaker(args);
	}

	public SIPSpeaker(String [] args){
		parseCommandLineArguments(args);
		if(cfgFile == null) cfgFile = DEFAULT_CFG_FILE;
		SIPConfig sipCfg  = new SIPConfig(cfgFile);

		if(sipUser == null){
			if((sipUser = sipCfg.getSipUser()) == null || sipUser.equals(""))
				sipUser = DEFAULT_SIP_USER;
		}
		if(sipInterface == null){
			if((sipInterface = sipCfg.getSipInterface()) == null || sipInterface.equals(""))
				sipInterface = DEFAULT_SIP_INTERFACE;
		}
		if(sipPort == 0){
			String tmp = null;
			if((tmp = sipCfg.getSipPort()) != null || !tmp.equals("")){
				try{ sipPort = Integer.parseInt(tmp);
				}catch (Exception e){
					System.out.println("Failed to assign port from configuration file");
					sipPort = DEFAULT_SIP_PORT;
				}
			}
			else sipPort = DEFAULT_SIP_PORT;
		}

		if(sipPort == DEFAULT_RTP_PORT)
			rtpPort = DEFAULT_RTP_PORT + 2;
		else
			rtpPort = DEFAULT_RTP_PORT;

		if((wavFile = sipCfg.getMessage()) == null || wavFile.equals("")){
			wavFile = DEFAULT_SOUND_FILE;
			// todo consider pre-loading the file
		}

		DatagramSocket serverSocket = null;


		try{serverSocket = new DatagramSocket(sipPort, InetAddress.getByName(sipInterface));
		}catch (Exception e){
			System.out.println("Can not bind address: " + sipInterface + ". " +
					"It is already in use or it is not a local address");
			//System.exit(1);
		}
		byte[] receiveData = new byte[1500];                    // 1500 is maximum size when MTU is unknown

		// Await SIP message
		while(true){
			try{
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				serverSocket.receive(receivePacket);
				String input = new String(receivePacket.getData(), 0, receivePacket.getLength());
				System.out.println(input);

				String [] rows = input.split("\n");
				if(rows[0].startsWith("INVITE")){
					handleInvite(rows);
				}
				else if(rows[0].startsWith("ACK")){
					handleAck(rows);
				}
				else{
					// todo if bye ????
				}

			}catch (IOException e){
				e.printStackTrace();
			}
		}
	}

	void handleInvite(String [] rows){
		SIPRequest sipRequest = new SIPRequest();

		sipRequest.cmd = "INVITE";
		for(int i = 1; i < rows.length; i++){
			if(rows[i].startsWith("Via:"))
				sipRequest.via.addLast(rows[i].substring("Via:".length()).trim());
			else if(rows[i].startsWith("From:"))
				sipRequest.from = rows[i].substring("From:".length()).trim();
			else if(rows[i].startsWith("To:"))
				sipRequest.to = rows[i].substring("To:".length()).trim();
			else if(rows[i].startsWith("Contact:"))
				sipRequest.contact = rows[i].substring("Contact:".length()).trim();
			else if(rows[i].startsWith("Call-ID:"))
				sipRequest.callId = rows[i].substring("Call-ID:".length()).trim();
			else if(rows[i].startsWith("CSeq:"))
				sipRequest.cSeq = rows[i].substring("CSeq:".length()).trim();
			else if(rows[i].startsWith("Max-Forwards:"))
				sipRequest.maxForwards = rows[i].substring("Max-Forwards:".length()).trim();
			else if(rows[i].startsWith("User-Agent:"))
				sipRequest.userAgent = rows[i].substring("User-Agent:".length()).trim();
			else if(rows[i].startsWith("Content-Length:"))
				sipRequest.contentLength = rows[i].substring("Content-Length:".length()).trim();
			else if(rows[i].startsWith("Content-Type:"))
				sipRequest.contentType = rows[i].substring("Content-Type:".length()).trim();
			else if(rows[i].startsWith("Supported:"))
			sipRequest.supported = rows[i].substring("Supported:".length()).trim();
			else if(rows[i].startsWith("v="))
				sipRequest.sdpVersion = rows[i].substring("v=".length()).trim();
			else if(rows[i].startsWith("o="))
				sipRequest.sdpOrigin = rows[i].substring("o=".length()).trim();
			else if(rows[i].startsWith("s="))
				sipRequest.sdpSessionName = rows[i].substring("s=".length()).trim();
			else if(rows[i].startsWith("c="))
				sipRequest.sdpConnectInfo = rows[i].substring("c=".length()).trim();
			else if(rows[i].startsWith("t="))
				sipRequest.sdpSessionTime = rows[i].substring("t=".length()).trim();
			else if(rows[i].startsWith("m="))
				sipRequest.sdpMediaTransportAdressCodec = rows[i].substring("m=".length()).trim();
			else if(rows[i].startsWith("a=rtpmap:0"))
				sipRequest.sdpRTPCodec = rows[i].substring("a=rtpmap:0".length()).trim();
		}

		// session already handled
		if(!sessionIDs.add(sipRequest.sdpOrigin))
			return;

		String tmp = sipRequest.via.getFirst().split(" ")[1].split(";")[0];
		String [] tmp2 = tmp.split(":");
		if(tmp2.length > 1){
			sipRequest.rInterface = tmp2[0];
			sipRequest.rPort = Integer.parseInt(tmp2[1]);
		}else{
			sipRequest.rInterface = tmp;
			sipRequest.rPort = DEFAULT_SIP_PORT;
		}

		try{
			if(!sipRequest.to.substring("<sip:".length()).startsWith(sipUser + "@")){
				try{
					System.out.println("error message");
					sendDatagram(notFoundMessage(sipRequest), InetAddress.getByName(sipRequest.rInterface), sipRequest.rPort);
				}catch (UnknownHostException e){
					e.printStackTrace();
				}
				return;
			}
		}catch (Exception e){return;}

		//System.out.println("ip: " + sipRequest.rInterface + "\nport: " + sipRequest.rPort);

		// SIP 180 Ringing
		StringBuilder sipResponse = new StringBuilder("");
		sipResponse.append("SIP/2.0 180 Ringing\r\n");
		for(String s : sipRequest.via)
			sipResponse.append("Via: " + s + "\r\n");
		sipResponse.append("From: " + sipRequest.from + "\r\n");
		sipResponse.append("To: " + sipRequest.to + "\r\n");
		sipResponse.append("Call-ID: " + sipRequest.callId + "\r\n");
		sipResponse.append("CSeq: " + sipRequest.cSeq + "\r\n");
		sipResponse.append("Contact: " + sipRequest.contact + "\r\n");
		sipResponse.append("Content-Length: 0\r\n");

		// Send Ringing message
		try{
			System.out.println(sipResponse.toString());
			sendDatagram(sipResponse.toString(), InetAddress.getByName(sipRequest.rInterface), sipRequest.rPort);
		}catch (UnknownHostException e){
			e.printStackTrace();
		}

		// SIP 200 OK
		StringBuilder sdpResponse = new StringBuilder("");
		sdpResponse.append("v=0\r\n");
		sdpResponse.append("o=" + sipRequest.sdpOrigin + "\r\n");
		sdpResponse.append("s=" + sipRequest.sdpSessionName + "\r\n");
		sdpResponse.append("i=SipSpeaker call\r\n");
		sdpResponse.append("c=" + sipRequest.sdpConnectInfo+ "\r\n");
		sdpResponse.append("t=0 0\r\n");
		sdpResponse.append("m=audio " + rtpPort + " RTP/AVP 0\r\n"); //audio 49158 RTP/AVP 3 97 98 8 0 101
		sdpResponse.append("a=rtpmap:0 PCMU/8000\r\n");

		sipResponse = new StringBuilder("");
		sipResponse.append("SIP/2.0 200 OK\r\n");
		for(String s : sipRequest.via)
			sipResponse.append("Via:" + s + "\r\n");
		sipResponse.append("From: " + sipRequest.from + ">\r\n");
		sipResponse.append("To: " + sipRequest.to + "\r\n");
		sipResponse.append("Call-ID: " + sipRequest.callId + "\r\n");
		sipResponse.append("CSeq: " + sipRequest.cSeq + "\r\n");
		sipResponse.append("Contact: " + sipRequest.contact + "\r\n");
		sipResponse.append("Content-Type:  application/sdp\r\n");
		sipResponse.append("Allow: INVITE, OPTIONS, BYE, CANCEL, ACK, PRACK\r\n");
		sipResponse.append("Supported: replaces,norefersub,timer\r\n");         /// jfhdxgjhgfjhgfjhgfjhgf
		sipResponse.append("Content-Length: " + sdpResponse.toString().getBytes().length + "\r\n");
		sipResponse.append("\r\n" + sdpResponse.toString());

		// increment rtpPort so we don't listen on the same port for two different clients!
		sipRequest.receiveRtpPort = rtpPort;
		rtpPort += 2;
		if(sipPort == rtpPort)
			rtpPort = rtpPort + 2;
		if(rtpPort > 65532){
			rtpPort = DEFAULT_RTP_PORT;
		}

		// todo error handling
		if(sipRequest.sdpMediaTransportAdressCodec != null && sipRequest.sdpMediaTransportAdressCodec.length() > 1){
			sipRequest.sendRtpPort = Integer.parseInt(sipRequest.sdpMediaTransportAdressCodec.split(" ")[1]);
		}

		// Send OK message
		try{
			System.out.println(sipResponse.toString());
			sendDatagram(sipResponse.toString(), InetAddress.getByName(sipRequest.rInterface) ,sipRequest.rPort);

			// save the SIPRequest
			callID.put(sipRequest.callId,sipRequest);
		}catch (UnknownHostException e){
			e.printStackTrace();
		}

	}

	void handleAck(String [] rows){
		SIPRequest sipRequest = null;
		for(String row : rows){
			if(row.startsWith("Call-ID:")){
				if((sipRequest = callID.get(row.substring("Call-ID:".length()).trim())) == null)
					return;
				else if(sipRequest.sdpMediaTransportAdressCodec == null || sipRequest.sdpMediaTransportAdressCodec.equals(""))
					return;
				else if(sipRequest.via == null || sipRequest.via.isEmpty())
					return;
			}
		}

		// Create sockets for the RTPSession
		DatagramSocket rtpSocket = null;
		DatagramSocket rtcpSocket = null;
		try {
			rtpSocket = new DatagramSocket(/*sipRequest.receiveRtpPort*/);
			rtcpSocket = new DatagramSocket(/*sipRequest.receiveRtpPort + 1*/);
		} catch (Exception e) {
			// todo error handling
			System.out.println("RTPSession failed to obtain port");
		}

		// Create RTP session
		RTPSession rtpSession = new RTPSession(rtpSocket, rtcpSocket);
		rtpSession.RTPSessionRegister(this,null, null);
		System.out.println("CNAME: " + rtpSession.CNAME()); // ????

		Participant p = new Participant(sipRequest.rInterface, sipRequest.sendRtpPort, sipRequest.sendRtpPort + 1);
		rtpSession.addParticipant(p);

		File soundFile = new File(wavFile);
		if (!soundFile.exists()) {
			System.err.println("Sound file not found: " + wavFile);
			// todo send bye
			return;
		}
		AudioInputStream audioInputStream = null;
		try {
			audioInputStream = AudioSystem.getAudioInputStream(soundFile);
			System.out.println("format :" + audioInputStream.getFormat().toString() + " FrameLength:" + audioInputStream.getFrameLength());
		} catch (UnsupportedAudioFileException e1) {
			System.out.println(e1.getMessage());
			// todo send bye
			return;
		} catch (IOException e1) {
			System.out.println(e1.getMessage());
			// todo send bye
			return;
		}

		// todo match encoding against SIPRequest
		//AudioFormat format = audioInputStream.getFormat();
		AudioFormat.Encoding encoding =  new AudioFormat.Encoding("PCM_SIGNED");
		AudioFormat format = new AudioFormat(encoding,((float) 8000.0), 16, 2, 4, ((float) 8000.0) ,false);

		SourceDataLine auline = null;

		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

		try {
			auline = (SourceDataLine) AudioSystem.getLine(info);
			auline.open(format);
		} catch (LineUnavailableException e) {
			e.printStackTrace();
			return;
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		if (auline.isControlSupported(FloatControl.Type.PAN)) {
			FloatControl pan = (FloatControl) auline
					.getControl(FloatControl.Type.PAN);
			if (this.curPosition == Position.RIGHT)
				pan.setValue(1.0f);
			else if (this.curPosition == Position.LEFT)
				pan.setValue(-1.0f);
		}

		auline.start();

		int nBytesRead = 0;
		byte[] data = new byte[1024];
		int pktCount = 0;
		long start = System.currentTimeMillis();
		try {

			while (nBytesRead != -1 && pktCount < 200) {
				nBytesRead = audioInputStream.read(data, 0, data.length);

				if (nBytesRead >= 0) {
					rtpSession.sendData(data);
					auline.write(data, 0, data.length);
					pktCount++;
				}
				if(pktCount == 100) {
					System.out.println("pktCount == 100");
					Enumeration<Participant> iter = rtpSession.getParticipants();
					//System.out.println("iter " + iter.hasMoreElements());
					Participant pp = null;

					while(iter.hasMoreElements()) {
						pp = iter.nextElement();

						String name = "name";
						byte[] nameBytes = name.getBytes();
						String sdata= "abcd";
						byte[] dataBytes = sdata.getBytes();

						System.out.println("penis 3");
						int ret = rtpSession.sendRTCPAppPacket(pp.getSSRC(), 0, nameBytes, dataBytes);
						System.out.println("!!!!!!!!!!!! ADDED APPLICATION SPECIFIC " + ret);
						continue;
					}
					if(pp == null)
						System.out.println("No participant with SSRC available :(");
				}
			}
		}catch (IOException e) {
			System.out.println("hej " + e.getMessage());
		}finally{
			rtpSession.endSession();
		}

		/*try{
			Thread.sleep(5000);
		}catch (InterruptedException e){
			e.printStackTrace();
		}*/

		try{
			sendDatagram(byeMessage(sipRequest), InetAddress.getByName(sipRequest.rInterface), sipRequest.rPort);
			callID.remove(sipRequest.callId);
		}catch (UnknownHostException e){
			e.printStackTrace();
		}

	}

	Position curPosition;
	boolean local;
	enum Position {
		LEFT, RIGHT, NORMAL
	};

	String byeMessage(SIPRequest sipRequest){
		String to = sipRequest.from.split("<")[1].split(">")[0];
		StringBuilder sipResponse = new StringBuilder("");
		sipResponse.append("BYE " + to + " SIP/2.0\r\n");
		for(String s : sipRequest.via)
			sipResponse.append("Via: " + s + "\r\n");
		sipResponse.append("From: " + sipRequest.to + "\r\n");
		sipResponse.append("To: " + sipRequest.from + "\r\n");
		sipResponse.append("Call-ID: " + sipRequest.callId + "\r\n");
		sipResponse.append("CSeq: " + sipRequest.cSeq + "\r\n");
		sipResponse.append("Content-Length: 0\r\n");
		return sipResponse.toString();
	}

	String notFoundMessage(SIPRequest sipRequest){
		StringBuilder sipResponse = new StringBuilder("");
		sipResponse.append("SIP/2.0 404 Not Found\r\n");
		for(String s : sipRequest.via)
			sipResponse.append("Via: " + s + "\r\n");
		sipResponse.append("From: " + sipRequest.from + "\r\n");
		sipResponse.append("To: " + sipRequest.to + "\r\n");
		sipResponse.append("Call-ID: " + sipRequest.callId + "\r\n");
		sipResponse.append("CSeq: " + sipRequest.cSeq + "\r\n");
		sipResponse.append("Reason: Q.850 ;cause=1 ; text=\"Unallocated (unassigned) number\" \r\n");
		sipResponse.append("Content-Length: 0\r\n");
		return sipResponse.toString();
	}

	void sendDatagram(String message, InetAddress inetAddress, int port){
		DatagramSocket clientSocket = null;
		try{
			clientSocket = new DatagramSocket();
			byte[] sendData;
			sendData = message.getBytes();
			//System.out.println("inet :::" + inetAddress);
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, inetAddress, port);
			clientSocket.send(sendPacket);
			clientSocket.close();
		}catch (SocketException e){e.printStackTrace();
		}catch (IOException e){e.printStackTrace();}
	}

	/**
	 * Parse and set the command line arguments
	 * @param args the input parameters
	 */
	void parseCommandLineArguments(String [] args){
		int size = args.length;

		for(int i = 0; i < size; i++){
			if(args[i].equals("-c")){
				if(size >= ++i)
					cfgFile = args[i];
			}
			else if(args[i].equals("-user")){
				if(size >= ++i)
					sipURI = args[i];
				try{
					String[] array = args[i].split("@");
					if(array[0].length() >= 1)
						sipUser = array[0];

					array = array[1].split(":");
					if(array[0].length() >= 1)
						sipInterface = array[0];
					if(array[1].length() >= 1)
						sipPort = Integer.parseInt(array[1]);
				}catch (Exception e){
					System.out.print("Failed to parse input parameter (-user)");
					System.exit(0);
				}
			}
			else if(args[i].equals("-http")){
				if(size >= ++i)
					HTTPBindAddress = args[i];
			}
			else{
				System.out.print("Bad syntax. " +
						"Use: java SIPSpeaker [-c config_file_name] [-user sip_uri] [-http http_bind_address] ");
				System.exit(-1);
			}
		}
	}

	@Override
	public void receiveData(DataFrame frame, Participant participant){
		// todo
	}

	@Override
	public void userEvent(int type, Participant[] participant){
		// todo
	}

	@Override
	public int frameSize(int payloadType){
		// todo
		return 0;
	}
}
