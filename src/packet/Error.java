package packet;

import java.net.DatagramSocket;
import java.nio.ByteBuffer;

/**
 * Class representing TFTP-ERR packet.
 */
public class Error extends Packet {
  private final int headerLength = 5;

  public Error(DatagramSocket socket) {
    super(socket);
  }

  /**
   * Constructor for an Error packet for a specific error type.
   *
   * @param socket the socket used for communication.
   * @param errorType the type of TFTP error.
   */
  public Error(DatagramSocket socket, ErrorType errorType) {
    super(socket);
    packetLength = errorType.message.length + headerLength;
    packet = new byte[packetLength];
    ByteBuffer.wrap(packet)
      .putShort(opErr).putShort(errorType.code).put(errorType.message).put((byte) 0);
  }
}