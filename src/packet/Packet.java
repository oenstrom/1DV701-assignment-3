package packet;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * Class that defines a TFTP Packet.
 */
public class Packet {
  public static final int MAX_CONTENT_LENGTH = 512;
  protected final int bufferLen = 516;
  protected byte[] buffer;
  
  // OP CODES.
  protected final short opRrq  = 1;
  protected final short opWrq  = 2;
  protected final short opData = 3;
  protected final short opAck  = 4;
  protected final short opErr  = 5;
  
  // Individual packet fields
  protected DatagramSocket sendSocket;
  protected InetSocketAddress clientAddress;
  protected byte[] packet;
  protected int contentLength;
  protected int packetLength;

  /**
   * Creates a buffer and defines start-content and -packet lengths.
   */
  public Packet() {
    buffer = new byte[bufferLen];
    contentLength = 512;
    packetLength = 516;
  }

  /**
   * Receives a packet from the client and creates corresponding Packet.
   *
   * @param socket for communication to client.
   * @return the Packet created.
   */
  public Packet receive(DatagramSocket socket) throws IOException {
    DatagramPacket dp = new DatagramPacket(buffer, bufferLen);
    socket.receive(dp);
  
    switch (ByteBuffer.wrap(buffer).getShort()) {
      case(opRrq):  return new Read(dp);
      case(opWrq):  return new Write(dp);
      case(opData): return new Data(dp); 
      case(opAck):  return new Acknowledgment(dp);
      case(opErr):  return new Error();
      default:      return null; // TODO Handle null.
    }
  }

  public void send(DatagramSocket socket) throws IOException {
    socket.send(new DatagramPacket(packet, packetLength));
  }

  public int getContentLength() {
    return this.contentLength;
  }

  public InetSocketAddress getClientAddress() {
    return this.clientAddress;
  }
}
