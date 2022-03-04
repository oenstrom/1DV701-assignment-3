import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

class Packet {
  protected static short opcode;
  protected static byte[] buffer;
  protected static  final int bufLen = 516; 
  
  protected static final short RRQ  = 1;
  protected static final short WRQ  = 2;
  protected static final short DATA = 3;
  protected static final short ACK  = 4;
  protected static final short ERR  = 5;
  
  // Individual packet fields
  protected DatagramSocket sendSocket;
  protected InetSocketAddress clientAddress;
  protected byte[] content;
  protected int contentLength;
  protected int packetLength;

  Packet() {
    buffer = new byte[bufLen];
    contentLength = 512;
    packetLength = 516;
  }

  Packet receive(DatagramSocket socket) throws IOException {
    DatagramPacket dp = new DatagramPacket(buffer, bufLen);
    socket.receive(dp);
  
    switch (ByteBuffer.wrap(buffer).getShort()) {
      case(RRQ):  return new ReadPacket(dp);
      case(WRQ):  return new WritePacket();
      case(DATA): return new DataPacket(); 
      case(ACK):  return new AcknowledgmentPacket(dp);
      case(ERR):  return new ErrorPacket();
      default:    return null;
    }
  }

  public void send(DatagramSocket socket) throws IOException {
    socket.send(new DatagramPacket(content, packetLength));
  }

  class RequestPacket extends Packet {
    public String fileName() {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      for (int i = 2; content[i] != Character.MIN_VALUE; i++) {
        baos.write(content[i]);
      }
      return new String(baos.toByteArray());
    }
  }

  class ReadPacket extends RequestPacket {
    ReadPacket(DatagramPacket dp) {
      clientAddress = new InetSocketAddress(dp.getAddress(), dp.getPort());
      content = dp.getData();
      contentLength = dp.getLength();
    }
  }

  class WritePacket extends RequestPacket {

  }

  class DataPacket extends Packet {
    DataPacket() {}

    DataPacket(int blockNr, FileInputStream fis) throws IOException {
      contentLength = fis.read(buffer);
      packetLength = contentLength + 4;
      content = new byte[packetLength];
      ByteBuffer bb = ByteBuffer.wrap(content);
      bb.putShort(DATA).putShort((short) blockNr).put(buffer, 0, contentLength);
    }
  }

  class AcknowledgmentPacket extends Packet {
    AcknowledgmentPacket(DatagramPacket dp) {
      content = dp.getData();
    }

    public short blockNumber() {
      return ByteBuffer.wrap(content).getShort(2);
    }
  }

  class ErrorPacket extends Packet {
  
  }
}
