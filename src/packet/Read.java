package packet;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Class representing TFTP-Read packet.
 */
public class Read extends Request {

  public Read(DatagramSocket socket, DatagramPacket dp) {
    super(socket, dp);
  }

}