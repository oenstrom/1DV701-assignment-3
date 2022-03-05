import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

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

  public Packet() {
    buffer = new byte[bufLen];
    //buf = new byte[512];
    contentLength = 512;
    packetLength = 516;
  }

  public Packet receive(DatagramSocket socket) throws IOException {
    DatagramPacket dp = new DatagramPacket(buffer, bufLen);
    socket.receive(dp);
  
    switch (ByteBuffer.wrap(buffer).getShort()) {
      case(opRrq):  return new ReadPacket(dp);
      case(opWrq):  return new WritePacket();
      case(opData): return new DataPacket(); 
      case(opAck):  return new AcknowledgmentPacket(dp);
      case(opErr):  return new ErrorPacket();
      default:    return null;
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

  public class RequestPacket extends Packet {
    public String fileName() {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      for (int i = 2; packet[i] != Character.MIN_VALUE; i++) {
        baos.write(packet[i]);
      }
      return new String(baos.toByteArray());
    }
  }

  public class ReadPacket extends RequestPacket {
    public ReadPacket(DatagramPacket dp) {
      clientAddress = new InetSocketAddress(dp.getAddress(), dp.getPort());
      packet = dp.getData();
      contentLength = dp.getLength();
    }
  }

  public class WritePacket extends RequestPacket {

  }

  public class DataPacket extends Packet {

    public DataPacket() {}

    public DataPacket(int blockNr, FileInputStream fis) throws IOException {
      //contentLength = fis.read(buf);
      contentLength = fis.read(buffer, 0, 512);
      packetLength = contentLength + 4;
      packet = new byte[packetLength];
      ByteBuffer bb = ByteBuffer.wrap(packet);
      bb.putShort(opData).putShort((short) blockNr).put(buffer, 0, contentLength);
    }
  }

  public class AcknowledgmentPacket extends Packet {
    private short blockNumber;

    public AcknowledgmentPacket(DatagramPacket dp) {
      packet = dp.getData();
      this.blockNumber = ByteBuffer.wrap(packet).getShort(2);
    }

    public short getBlockNumber() {
      return this.blockNumber;
    }
  }

  public class ErrorPacket extends Packet {
  
  }
}
