package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
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
        new Error(socket, ErrorType.PREMATURE_TERMINATION).send();
      }
    } catch (IOException e) {
      try {
        new Error(socket, ErrorType.PREMATURE_TERMINATION).send();
      } catch (IOException e1) {
        System.err.println("Could not send error message.");
      }
      e.printStackTrace();
      System.err.println("Closing socket and terminating thread.");
      socket.close();
      Thread.currentThread().interrupt();
    }
  }

  protected abstract void validFile(File file) 
      throws FileAlreadyExistsException, FileNotFoundException, AccessDeniedException; 

  protected abstract void handleFile(File file) throws IOException;
}

