import java.io.IOException;
import java.net.DatagramSocket;

public class App {
  private static int TFTP_PORT = 4970;

  public static void main(String[] args) {
    if (args.length > 0) {
      System.err.printf("usage: java %s\n", TftpServer.class.getCanonicalName());
      System.exit(1);
    }

    try {
      DatagramSocket socket = new DatagramSocket(TFTP_PORT);
      System.out.printf("Listening at port %d for new requests\n", TFTP_PORT);
  
      while (true) {
        Packet packet = Packet.receive(socket);
      }
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
  }
}
