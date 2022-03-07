package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import packet.Request;

/**
 * Abstract class for ServerRead and ServerWrite.
 */
public abstract class Server extends Thread {
  protected String readDir = Path.of(System.getProperty("user.dir"), "public").toString();
  protected final int retransmitLimit = 5;
  protected final int timeOutMs = 4000;
  protected DatagramSocket socket;
  protected Request packet;
  
  @Override
  public void run() {
    try {
      socket = new DatagramSocket(0);
      InetSocketAddress ca = packet.getClientAddress();
      socket.connect(ca);
      System.out.printf("Read request from %s using port %d\n", ca.getAddress(), ca.getPort());
    } catch (SocketException e) {
      e.printStackTrace();
      socket.close();
      Thread.currentThread().interrupt();
    }

    File file = new File(readDir, packet.getFileName());
    try {
      validFile(file);
      handleFile(file);
    } catch (FileAlreadyExistsException e) {
      // Send error 6.
      e.printStackTrace();
    } catch (FileNotFoundException e) {
      // Send error 1.
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      // Send error 2.
      e.printStackTrace();
    } catch (IOException e) {
      // Send Premature termination error.
      e.printStackTrace();
    }
  }

  protected abstract void validFile(File file) 
      throws FileAlreadyExistsException, FileNotFoundException, IllegalAccessException; 

  protected abstract void handleFile(File file) throws IOException;
}

