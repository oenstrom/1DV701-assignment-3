package packet;

import java.io.IOException;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
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

  /**
   * Retransmit a packet.
   *
   * @param blockNumber the block number to check.
   * @return the received data packet.
   * @throws ConnectException if the client doesn't respond.
   */
  public Data retransmit(short blockNumber) throws IOException, ConnectException {
    Packet p = new Packet(socket);
    for (int i = 0; !(p instanceof Data && ((Data) p).getBlockNumber() == blockNumber); i++) {
      if (i != 0) {
        send();
      }
      try {
        p = p.receive();
      } catch (SocketTimeoutException e) {
        if (i == RETRANSMIT_LIMIT) {
          throw new ConnectException("Client not responding.");
        }
      }
    }
    return (Data) p;
  }
}