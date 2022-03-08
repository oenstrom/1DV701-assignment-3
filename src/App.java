import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import packet.Error;
import packet.Packet;
import packet.Read;
import packet.Write;
import server.ServerRead;
import server.ServerWrite;

/**
 * Entry point for the TFTP server.
 */
public class App {
  private static int TFTP_PORT = 4970;

  /**
   * Entry main method.
   */
  public static void main(String[] args) {
    if (args.length > 0) {
      System.err.printf("usage: java %s\n", TftpServer.class.getCanonicalName());
      System.exit(1);
    }

    try (DatagramSocket socket = new DatagramSocket(new InetSocketAddress(TFTP_PORT))) { 
      System.out.printf("Listening at port %d for new requests\n", TFTP_PORT);
  
      while (true) {
        Packet packet = new Packet(socket).receive();

        if (packet instanceof Read) {
          System.out.print("Read ");
          new ServerRead((Read) packet).start();
        } else if (packet instanceof Write) {
          System.out.print("Write ");
          new ServerWrite((Write) packet).start();
        } else {
          DatagramSocket ds = new DatagramSocket(packet.getClientAddress());
          ds.connect(packet.getClientAddress());
          new Error(ds, Error.Type.ILLEGAL_OPERATION).send();
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
  }
}
