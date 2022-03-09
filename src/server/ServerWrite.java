package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import packet.Acknowledgment;
import packet.Data;
import packet.Packet;
import packet.Write;

/**
 * Class for handling write request.
 */
public class ServerWrite extends Server {

  public ServerWrite(Write packet) {
    this.packet = packet;
  }

  @Override
  protected void handleFile(File file) throws ConnectException, IOException {
    socket.setSoTimeout(timeOutMs);
    short blockNr = 0;
    Acknowledgment ack = new Acknowledgment(socket, blockNr++);
    ack.send();

    FileOutputStream fos = new FileOutputStream(file);
    Data p;
    do {
      try {
        p = retransmit(blockNr, ack);
      } catch (ConnectException e) {
        fos.close();
        throw e;
      }
      fos.write(p.getContent());
      ack = new Acknowledgment(socket, blockNr++);
      ack.send();
    } while (p.getContentLength() == Packet.MAX_CONTENT_LENGTH);
    fos.flush();
    fos.close();
    System.out.println("File '" + file.getName() + "' received.");
  }

  @Override
  protected void validFile(File file) 
      throws FileAlreadyExistsException, FileNotFoundException, AccessDeniedException {
    if (file.exists()) {
      throw new FileAlreadyExistsException("File " + file.getName() + " already exists");
    }
    if (file.isDirectory()) {
      throw new FileNotFoundException("File " + file.getName() + " is a directory.");
    }
    try {
      file.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
      throw new AccessDeniedException("Access violation");
    }
  }

  /**
   * Retransmit an ack packet if needed.
   *
   * @param blockNr the block number to check.
   * @param ack the ack packet to retransmit if needed.
   * @return the received data packet.
   * @throws ConnectException if the client doesn't respond.
   */
  private Data retransmit(short blockNr, Acknowledgment ack) throws IOException, ConnectException {
    Packet p = new Packet(socket);
    for (int i = 0; !(p instanceof Data && ((Data) p).getBlockNumber() == blockNr); i++) {
      p = sendAndReceive(ack, i);
    }
    return (Data) p;
  }
}