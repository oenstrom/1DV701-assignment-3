package packet;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;

/**
 * Class representing TFTP-ACK packet.
 */
public class Acknowledgment extends Packet {
  private short blockNumber;

  /**
   * Constructor for a received ack packet.
   */
  public Acknowledgment(DatagramSocket socket, DatagramPacket dp) {
    super(socket);
    packet = dp.getData();
    this.blockNumber = ByteBuffer.wrap(packet).getShort(2);
  }

  /**
   * Constructor for write request ack.
   */
  public Acknowledgment(DatagramSocket socket, short blockNumber) {
    super(socket);
    packetLength = 4;
    this.blockNumber = blockNumber;
    packet = ByteBuffer.allocate(packetLength).putShort(opAck).putShort(blockNumber).array();
  }

  public short getBlockNumber() {
    return this.blockNumber;
  }
}