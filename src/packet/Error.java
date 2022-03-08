package packet;

import java.net.DatagramSocket;
import java.nio.ByteBuffer;

/**
 * Class representing TFTP-ERR packet.
 */
public class Error extends Packet {


  /**
   * Enum representing TFTP errors.
   */
  public enum Type {
    PREMATURE_TERMINATION((short) 0, "The connection was terminated prematurly.".getBytes()),
    NOT_DEFINED((short) 0, "Not defined.".getBytes()),
    FILE_NOT_FOUND((short) 1, "File not found.".getBytes()),
    ACCESS_VIOLATION((short) 2, "Access violation.".getBytes()),
    DISK_FULL((short) 3, "Disk full or allocation exceeded.".getBytes()),
    ILLEGAL_OPERATION((short) 4, "Illegal TFTP operation.".getBytes()),
    UNKNOWN_TRANSFER_ID((short) 5, "Unknown tranfer ID.".getBytes()),
    FILE_ALREADY_EXISTS((short) 6, "File already exists.".getBytes()),
    NO_SUCH_USER((short) 7, "No such user.0".getBytes());

    public short code;
    public byte[] message;
    
    Type(short code, byte[] message) {
      this.code = code;
      this.message = message;
    }
  }


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
  public Error(DatagramSocket socket, Type errorType) {
    super(socket);
    packetLength = errorType.message.length + headerLength;
    packet = new byte[packetLength];
    ByteBuffer.wrap(packet)
      .putShort(opErr).putShort(errorType.code).put(errorType.message).put((byte) 0);
  }
}