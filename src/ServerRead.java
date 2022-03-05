import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.file.Path;

/**
 * Class for handling read request.
 */
public class ServerRead extends Thread {
  private String readDir = Path.of(System.getProperty("user.dir"), "public").toString();
  private Packet.Read rp;
  private DatagramSocket socket;
  private int retransmitLimit = 5;

  public ServerRead(Packet.Read rp) {
    this.rp = rp;
  }

  @Override
  public void run() {
    try {
      socket = new DatagramSocket(0);
      InetSocketAddress ca = rp.getClientAddress();
      socket.connect(ca);
      System.out.printf("Read request from %s using port %d\n", ca.getAddress(), ca.getPort());
    } catch (SocketException e) {
      e.printStackTrace();
      socket.close();
      Thread.currentThread().interrupt();
    }

    File file = new File(readDir, rp.getFileName());
    if (validFile(file)) {
      try {
        sendFile(file);
      } catch (IOException e) {
        // TODO Try to send error message and terminate thread.
        e.printStackTrace();
      }
    } else {
      // TODO Send error "Access Violation"
      System.out.println("Access Violation");
    }
  }

  /**
   * Read a stream of bytes from the file and send a datapacket.
   *
   * @param fileToSend the file to read bytes from.
   */
  private void sendFile(File fileToSend) throws IOException {
    try (FileInputStream fis = new FileInputStream(fileToSend)) {
      socket.setSoTimeout(4000);
      Packet packet = new Packet();
      for (short blockNr = 1; packet.getContentLength() == 512; blockNr++) {
        packet = packet.new Data(blockNr, fis);
        packet.send(socket);
        for (int i = 0; i < retransmitLimit; i++) {
          Packet ack = new Packet().receive(socket);
          if (!(ack instanceof Packet.Acknowledgment)) {
            throw new IllegalArgumentException("Not an ack packet");
            //TODO: Use some other exception, just picked one for now.
          }
          //Packet.Acknowledgment a = (Packet.Acknowledgment) ack;
          if (((Packet.Acknowledgment) ack).getBlockNumber() != blockNr) {
            // Last packet lost
            packet.send(socket);
            continue;
          }
          break;
        }
      }
      System.out.println("File '" + fileToSend.getName() + "' sent.");
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