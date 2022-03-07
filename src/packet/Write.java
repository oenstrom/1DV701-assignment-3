package packet;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Class representing TFTP-Write packet.
 */
public class Write extends Request {

  public Write(DatagramSocket socket, DatagramPacket dp) {
    super(socket, dp);
  }

}