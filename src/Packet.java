import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class Packet {
  protected byte[] content;
  protected static short opcode;
  protected static byte[] buffer;
  protected static  final int bufLen = 516; 

  protected static final short RRQ  = 1;
  protected static final short WRQ  = 2;
  protected static final short DATA = 3;
  protected static final short ACK  = 4;
  protected static final short ERR  = 5;

  static Packet receive(DatagramSocket socket) throws IOException {
    DatagramPacket dp = new DatagramPacket(buffer, bufLen);
    socket.receive(dp);

    ByteBuffer bb = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
    opcode = bb.put(buffer[0]).put(buffer[1]).getShort(0);

    switch (opcode) {
      case(RRQ):  return new RequestPacket();
      case(WRQ):  return new RequestPacket();
      case(DATA): return new DataPacket(); 
      case(ACK):  return new AcknowledgmentPacket();
      case(ERR):  return new ErrorPacket();
      default:    return null;
    }
  }

  static class RequestPacket extends Packet {

  }

  static class DataPacket extends Packet {
  
  }

  static class AcknowledgmentPacket extends Packet {

    }

  static class ErrorPacket extends Packet {
  
  }
}
