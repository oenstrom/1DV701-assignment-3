package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import packet.Acknowledgment;
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
  protected void handleFile(File file) throws IOException {
    socket.setSoTimeout(timeOutMs);
    Acknowledgment ack = new Acknowledgment();
    ack.send(socket);
    // for (int i = 0; i < retransmitLimit; i++) {
    //   Packet packet = new Packet().receive(socket);
    //   if (!(packet instanceof Packet.Data)) {
    //     ack.send(socket);
    //   } else {
    //     // Do the tango.
    //     break;
    //   }

    // }
    Packet packet = new Packet().receive(socket);
    FileOutputStream fos = new FileOutputStream(file);

    for (int i = 0; packet.getContentLength() == Packet.MAX_CONTENT_LENGTH; i++) {
      
    }
  }

  @Override
  protected void validFile(File file) 
      throws FileAlreadyExistsException, FileNotFoundException, IllegalAccessException {
    if (file.exists()) {
      throw new FileAlreadyExistsException("File " + file.getName() + " already exists");
    } else if (!file.isFile()) {
      throw new FileNotFoundException("File " + file.getName() + " is a directory.");
    } else if (!file.canRead()) {
      throw new IllegalAccessException("Access violation!");
    }
  }
}