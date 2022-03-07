import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * Class that defines a TFTP Packet.
 */
public class Packet {
  protected byte[] buffer;
  private final int bufLen = 516; 
  
  // OP CODES.
  private final short opRrq  = 1;
  private final short opWrq  = 2;
  private final short opData = 3;
  private final short opAck  = 4;
  private final short opErr  = 5;
  
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
    buffer = new byte[bufLen];
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
    DatagramPacket dp = new DatagramPacket(buffer, bufLen);
    socket.receive(dp);
  
    switch (ByteBuffer.wrap(buffer).getShort()) {
      case(opRrq):  return new Read(dp);
      case(opWrq):  return new Write(dp);
      case(opData): return new Data(); 
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

  /**
   * Class representing TFTP-Read packet.
   */
  public class Read extends Request {

    public Read(DatagramPacket dp) {
      super(dp);
    }

  }

  /**
   * Class representing TFTP-Write packet.
   */
  public class Write extends Request {

    public Write(DatagramPacket dp) {
      super(dp);
    }

  }

  /**
   * Class representing TFTP-data packet.
   */
  public class Data extends Packet {

    public Data() {}

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

  /**
   * Class representing TFTP-ERR packet.
   */
  public class Error extends Packet {
  
  }
}
