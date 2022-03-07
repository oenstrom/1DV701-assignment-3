package packet;

import java.net.DatagramPacket;

/**
 * Class representing TFTP-Read packet.
 */
public class Read extends Request {

  public Read(DatagramPacket dp) {
    super(dp);
  }

}