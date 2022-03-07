package packet;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;

/**
 * Class representing TFTP-ACK packet.
 */
public class Acknowledgment extends Packet {
  private short blockNumber;

  public Acknowledgment(DatagramPacket dp) {
    packet = dp.getData();
    this.blockNumber = ByteBuffer.wrap(packet).getShort(2);
  }

  /**
   * Constructor for write request ack.
   */
  public Acknowledgment() {
    packetLength = 4;
    blockNumber = 0;
    packet = ByteBuffer.allocate(packetLength).putShort(opAck).putShort(blockNumber).array();
  }

  public short getBlockNumber() {
    return this.blockNumber;
  }
}