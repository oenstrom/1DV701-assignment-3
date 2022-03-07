package packet;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;

/**
 * Class representing TFTP-data packet.
 */
public class Data extends Packet {

  /**
   * Constructor for creating a data packet when receiving data.
   */
  public Data(DatagramPacket dp) {
    packetLength = dp.getLength();
    contentLength = packetLength - 4;
    packet = dp.getData();
  }

  /**
   * Sets contentLength, packetLength and 'packet' for TFTP-Data packet.
   *
   * @param blockNr the block number corresponding to the TFTP_Data packet.
   * @param fis the FileInputStream to read data.
   */
  public Data(int blockNr, FileInputStream fis) throws IOException {
    contentLength = fis.read(buffer, 0, 512);
    packetLength = contentLength + 4;
    packet = new byte[packetLength];
    ByteBuffer bb = ByteBuffer.wrap(packet);
    bb.putShort(opData).putShort((short) blockNr).put(buffer, 0, contentLength);
  }
}