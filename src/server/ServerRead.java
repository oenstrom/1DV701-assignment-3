package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.AccessDeniedException;
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
  protected void handleFile(File fileToSend) throws ConnectException, IOException {
    try (FileInputStream fis = new FileInputStream(fileToSend)) {
      socket.setSoTimeout(timeOutMs);      
      Data p;
      short blockNr = 1;
      do {
        p = new Data(socket, blockNr++, fis);
        p.send(socket);
        p.retransmit();
      } while (p.getContentLength() == Packet.MAX_CONTENT_LENGTH);
      System.out.println("File '" + fileToSend.getName() + "' sent.");
    }
  }

  @Override
  protected void validFile(File file) throws FileNotFoundException, AccessDeniedException {
    if (!file.exists() || !file.isFile()) {
      throw new FileNotFoundException("File " + file.getName() + " does not exist.");
    } else if (!file.canRead()) {
      throw new AccessDeniedException("Access violation");
    }
  }
}