package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import packet.Error;
import packet.ErrorType;
import packet.Request;

/**
 * Abstract class for ServerRead and ServerWrite.
 */
public abstract class Server extends Thread {
  protected String readDir = Path.of(System.getProperty("user.dir"), "public").toString();
  protected final int timeOutMs = 1000;
  public DatagramSocket socket;
  protected Request packet;
  
  @Override
  public void run() {
    try {
      socket = new DatagramSocket(0);
      InetSocketAddress ca = packet.getClientAddress();
      socket.connect(ca);
      System.out.printf("request from %s using port %d\n", ca.getAddress(), ca.getPort());
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
      System.err.println("Write request to a file that already exists.");
      new Error(socket, ErrorType.FILE_ALREADY_EXISTS).send();
    } catch (FileNotFoundException e) {
      if (e.getLocalizedMessage().toLowerCase().contains("access is denied")) {
        System.err.println("The requested file could not be read.");
        new Error(socket, ErrorType.ACCESS_VIOLATION).send();
      } else {
        System.err.println("The requested file could not be found.");
        new Error(socket, ErrorType.FILE_NOT_FOUND).send();
      }
    } catch (AccessDeniedException e) {
      System.err.println("The requested file could not be read.");
      new Error(socket, ErrorType.ACCESS_VIOLATION).send();
    } catch (ConnectException e) {
      System.err.println("Timeout. The connection has been terminated.");
      // TODO: Should it be something else than premature termination?
      new Error(socket, ErrorType.PREMATURE_TERMINATION).send();
    } catch (IOException e) {
      System.err.println("The connection has been terminated.");
      e.printStackTrace();
      new Error(socket, ErrorType.PREMATURE_TERMINATION).send();
    }
  }

  protected abstract void validFile(File file) 
      throws FileAlreadyExistsException, FileNotFoundException, AccessDeniedException; 

  protected abstract void handleFile(File file) throws IOException;
}

