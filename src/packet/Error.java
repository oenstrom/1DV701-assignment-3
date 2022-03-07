package packet;

import java.net.DatagramSocket;

/**
 * Class representing TFTP-ERR packet.
 */
public class Error extends Packet {
  public Error(DatagramSocket socket) {
    super(socket);
  }
}