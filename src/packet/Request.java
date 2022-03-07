package packet;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

/**
 * Abstract class for requests, i.e. Reads and Writes.
 */
public abstract class Request extends Packet {
  private String fileName;

  /**
   * Sets clientAddress, packet, contentLength and fileName for request packet.
   *
   * @param dp the packet for request.
   */
  public Request(DatagramPacket dp) {
    clientAddress = new InetSocketAddress(dp.getAddress(), dp.getPort());
    packet = dp.getData();
    contentLength = dp.getLength();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    for (int i = 2; packet[i] != Character.MIN_VALUE; i++) {
      baos.write(packet[i]);
    }
    this.fileName = baos.toString();
  }

  /**
   * Getter for String fileName.
   *
   * @return fileName.
   */
  public String getFileName() {
    return this.fileName;
  }
}