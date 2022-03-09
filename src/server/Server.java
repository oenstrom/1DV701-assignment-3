package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.net.SocketTimeoutException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import packet.Error;
import packet.Packet;
import packet.Request;

/**
 * Abstract class for ServerRead and ServerWrite.
 */
public abstract class Server extends Thread {
  protected String readDir = Path.of(System.getProperty("user.dir"), "public").toString();
  protected final int retransmitLimit = 5;
  protected final int timeOutMs = 1500;
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
        new Error(socket, Error.Type.FILE_ALREADY_EXISTS).send();
      } catch (FileNotFoundException e) {
        if (e.getLocalizedMessage().toLowerCase().contains("access is denied")) {
          System.err.println("The requested file could not be read.");
          new Error(socket, Error.Type.ACCESS_VIOLATION).send();
        } else {
          System.err.println("The requested file could not be found.");
          new Error(socket, Error.Type.FILE_NOT_FOUND).send();
        }
      } catch (AccessDeniedException e) {
        System.err.println("The requested file could not be read.");
        new Error(socket, Error.Type.ACCESS_VIOLATION).send();
      } catch (ConnectException e) {
        System.err.println("Timeout. The connection has been terminated.");
        new Error(socket, Error.Type.PREMATURE_TERMINATION).send();
      }
    } catch (PortUnreachableException e) {
      System.err.println("Could not reach the client. Terminating connection.");
    } catch (IOException e) {
      try {
        new Error(socket, Error.Type.PREMATURE_TERMINATION).send();
      } catch (IOException e1) {
        System.err.println("Could not send error message.");
      }
      e.printStackTrace();
      System.err.println("Closing socket and terminating thread.");
    } finally {
      socket.close();
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Send a packet and receive an answer. Used in retransmit.
   *
   * @param toSend the packet to send/retransmit.
   * @param round the integer to check against if retransmit limit is reached.
   * @return the received packet or a general Packet if the receive times out.
   * @throws ConnectException if the retransmit limit is reached.
   */
  protected Packet sendAndReceive(Packet toSend, int round)
      throws ConnectException, IOException {
    if (round != 0) {
      toSend.send();
    }
    try {
      return new Packet(socket).receive();
    } catch (SocketTimeoutException e) {
      if (round == retransmitLimit) {
        throw new ConnectException("Client not responding.");
      }
    }
    return new Packet(socket);
  }

  protected abstract void validFile(File file) 
      throws FileAlreadyExistsException, FileNotFoundException, AccessDeniedException; 

  protected abstract void handleFile(File file) throws IOException;
}

