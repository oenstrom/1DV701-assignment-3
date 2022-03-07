package packet;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

/**
 * Class representing TFTP-data packet.
 */
public class Data extends Packet {
  private byte[] header = new byte[4];
  private byte[] content;
  private short blockNumber;

  /**
   * Constructor for creating a data packet when receiving data.
   */
  public Data(DatagramSocket socket, DatagramPacket dp) {
    super(socket);
    packetLength = dp.getLength();
    contentLength = packetLength - header.length;
    content = new byte[contentLength];
    packet = dp.getData();
    blockNumber = ByteBuffer.wrap(packet).get(header, 0, 4).get(header.length, content).getShort(2);
  }

  /**
   * Sets contentLength, packetLength and 'packet' for TFTP-Data packet.
   *
   * @param blockNr the block number corresponding to the TFTP_Data packet.
   * @param fis the FileInputStream to read data.
   */
  public Data(DatagramSocket socket, short blockNr, FileInputStream fis) throws IOException {
    super(socket);
    this.blockNumber = blockNr;
    contentLength = fis.read(buffer, 0, 512);
    packetLength = contentLength + 4;
    packet = new byte[packetLength];
    ByteBuffer bb = ByteBuffer.wrap(packet);
    bb.putShort(opData).putShort(blockNr).put(buffer, 0, contentLength);
  }

  public byte[] getHeader() {
    return this.header;
  }

  public byte[] getContent() {
    return this.content;
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
  public Acknowledgment retransmit(short blockNumber) throws IOException, ConnectException {
    Packet p = new Packet(socket).receive();
    for (int i = 0; !(p instanceof Acknowledgment
        && ((Acknowledgment) p).getBlockNumber() == blockNumber); i++) {
      send(socket);
      try {
        p = p.receive();
      } catch (SocketTimeoutException e) {
        if (i == RETRANSMIT_LIMIT) {
          throw new ConnectException("Client not responding.");
        }
      }
    }
    return (Acknowledgment) p;
  }
}