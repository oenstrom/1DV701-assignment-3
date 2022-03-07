package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import packet.Acknowledgment;
import packet.Data;
import packet.Packet;
import packet.Read;

/**
 * Class for handling read request.
 */
public class ServerRead extends Server {

  public ServerRead(Read packet) {
    this.packet = packet;
  }

  /**
   * Read a stream of bytes from the file and send a datapacket.
   *
   * @param fileToSend the file to read bytes from.
   */
  @Override
  protected void handleFile(File fileToSend) throws IOException {
    try (FileInputStream fis = new FileInputStream(fileToSend)) {
      socket.setSoTimeout(timeOutMs);
      Packet packet = new Packet();
      for (short blockNr = 1; packet.getContentLength() == 512; blockNr++) {
        packet = new Data(blockNr, fis);
        packet.send(socket);
        for (int i = 0; i < retransmitLimit; i++) {
          Packet ack = new Packet().receive(socket);
          if (!(ack instanceof Acknowledgment)) {
            throw new IllegalArgumentException("Not an ack packet");
            //TODO: Use some other exception, just picked one for now.
          }
          if (((Acknowledgment) ack).getBlockNumber() != blockNr) {
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

  @Override
  protected void validFile(File file) throws FileNotFoundException, IllegalAccessException {
    if (!file.exists() || !file.isFile()) {
      throw new FileNotFoundException("File " + file.getName() + " does not exist.");
    } else if (!file.canRead()) {
      throw new IllegalAccessException("Access violation!");
    }
  }
}