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
  private Packet.ReadPacket rp;
  private DatagramSocket socket;
  private int retransmitLimit = 5;

  public ServerRead(Packet.ReadPacket rp) {
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

    File file = new File(readDir, rp.fileName());
    if (validFile(file)) {
      sendFile(file);
    } else {
      // Send error "Access Violation"
      System.out.println("Access Violation");
    }
  }

  /**
   * Read a stream of bytes from the file and send a datapacket.
   *
   * @param fileToSend the file to read bytes from.
   */
  private void sendFile(File fileToSend) {
    try (FileInputStream fis = new FileInputStream(fileToSend)) {
      socket.setSoTimeout(4000);
      Packet packet = new Packet();
      for (short blockNr = 1; packet.getContentLength() == 512; blockNr++) {
        packet = packet.new DataPacket(blockNr, fis);
        packet.send(socket);

        for (int i = 0; i < retransmitLimit; i++) {
          Packet ack = new Packet().receive(socket);
          if (!(ack instanceof Packet.AcknowledgmentPacket)) {
            throw new IllegalArgumentException("Not an ack packet");
            //TODO: Use some other exception, just picked one for now.
          }
          Packet.AcknowledgmentPacket a = (Packet.AcknowledgmentPacket) ack;
          if (a.getBlockNumber() != blockNr) {
            // Last packet lost
            packet.send(socket);
            continue;
          }
          System.out.println("Ack received!");
          break;
        }
      }
      System.out.println("DONE!");
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private boolean validFile(File file) {
    return file.exists() && file.isFile() && file.canRead();
  }
}