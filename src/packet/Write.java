package packet;

import java.net.DatagramPacket;

/**
 * Class representing TFTP-Write packet.
 */
public class Write extends Request {

  public Write(DatagramPacket dp) {
    super(dp);
  }

}