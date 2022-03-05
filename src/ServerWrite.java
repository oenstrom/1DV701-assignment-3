import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.file.Path;

/**
 * Class for handling write request.
 */
public class ServerWrite extends Thread {
  // TODO: Code dupl. from ServerRead?
  private String writeDir = Path.of(System.getProperty("user.dir"), "public").toString();
  private Packet.Write packet;
  private DatagramSocket socket;
  private final int retransmitLimit = 5;
  private final int timeOutMs = 4000;


  public ServerWrite(Packet.Write packet) {
    this.packet = packet;
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
      try {
        retrieveFile(file);
      } catch (IOException ioe) {
        // TODO Auto-generated catch block
        ioe.printStackTrace();
      }
    } 
  }
  
  private void retrieveFile(File file) throws IOException {
    //TODO Implement.
  }
  
  private boolean validFile(File file) { //TODO Code dupl. from ServerRead.
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