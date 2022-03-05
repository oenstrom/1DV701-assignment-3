import java.io.File;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.file.Path;

/**
 * Class for handling write request.
 */
public class ServerWrite extends Thread {
  private String writeDir = Path.of(System.getProperty("user.dir"), "public").toString();
  private Packet.Write packet;
  private DatagramSocket socket;
  private int retransmitLimit = 5;

  public ServerWrite(Packet.Write packet) {
    this.packet = packet;
  }

  private void retrieveFile(File file) {

  }

  @Override
  public void run() {
    try {
      socket = new DatagramSocket(0);
      InetSocketAddress ca = packet.getClientAddress();
      socket.connect(ca);
      System.out.printf("Write request from %s using port %d\n", ca.getAddress(), ca.getPort());
    } catch (SocketException e) {
      e.printStackTrace();
      socket.close();
      Thread.currentThread().interrupt();
    }

    File file = new File(writeDir, packet.getFileName());
    if (validFile(file)) {
      retrieveFile(file);
    } 
  }
  

  private boolean validFile(File file) {
    if (file.exists()) {
      // Send error 6.
    } else if (!file.isFile()) {
      //send error 1
    } else if (!file.canRead()) {
      //Send error 2
    }
    return file.exists() && file.isFile() && file.canRead();
  }

}