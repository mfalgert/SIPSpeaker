import java.util.LinkedList;

/**
 * Created by Robert Norgren Erneborg on 2015-04-23.
 */
public class SIPRequest {

	// SIP

	public int rPort;

	public String rInterface;

	public String cmd;

	public LinkedList<String> via = new LinkedList<String>();

	public String from;

	public String to;

	public String contact;

	public String cSeq;

	public String maxForwards;

	public String userAgent;

	public String callId;

	public String contentLength;

	public String contentType;

	public String supported;

	// SDP

	public String sdpVersion;

	public String sdpOrigin;

	public String sdpSessionName;

	public String sdpConnectInfo;

	public String sdpSessionTime;

	public String sdpMediaTransportAdressCodec;

	public String sdpRTPCodec;

	// RTP
	public int receiveRtpPort;

	public int sendRtpPort;

	@Override
	public String toString(){
		return "SIPRequest{" +
				"rPort=" + rPort +
				", rInterface='" + rInterface + '\'' +
				", cmd='" + cmd + '\'' +
				", via=" + via +
				", from='" + from + '\'' +
				", to='" + to + '\'' +
				", contact='" + contact + '\'' +
				", cSeq='" + cSeq + '\'' +
				", maxForwards='" + maxForwards + '\'' +
				", userAgent='" + userAgent + '\'' +
				", callId='" + callId + '\'' +
				", contentLength='" + contentLength + '\'' +
				", contentType='" + contentType + '\'' +
				", supported='" + supported + '\'' +
				", sdpVersion='" + sdpVersion + '\'' +
				", sdpOrigin='" + sdpOrigin + '\'' +
				", sdpSessionName='" + sdpSessionName + '\'' +
				", sdpConnectInfo='" + sdpConnectInfo + '\'' +
				", sdpSessionTime='" + sdpSessionTime + '\'' +
				", sdpMediaTransportAdressCodec='" + sdpMediaTransportAdressCodec + '\'' +
				", sdpRTPCodec='" + sdpRTPCodec + '\'' +
				'}';
	}
}
