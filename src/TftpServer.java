import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.ServerException;
import java.util.concurrent.TimeoutException;
import packet.Error.Type;

/**
 * TFTP server.
 */
public class TftpServer {
  public static final int TFTPPORT = 4970;
  public static final int BUFSIZE = 516;
  public static final int HEADERSIZE = 4;
  public static final int OP_POS = 1;
  public static final int BLOCK_NR_POS = 3;
  public static final int RETRANSMIT_TIME = 5000;
  public static final String RUNDIR = System.getProperty("user.dir");
  public static final String READDIR = Paths.get(RUNDIR, "").toString() + File.separatorChar;
  public static final String WRITEDIR = Paths.get(RUNDIR, "").toString() + File.separatorChar;
  // OP codes
  public static final int OP_RRQ = 1;
  public static final int OP_WRQ = 2;
  public static final int OP_DAT = 3;
  public static final int OP_ACK = 4;
  public static final int OP_ERR = 5;

  /**
   * Start the TFTP server.
   */
  public static void main(String[] args) {
    if (args.length > 0) {
      System.err.printf("usage: java %s\n", TftpServer.class.getCanonicalName());
      System.exit(1);
    }

    try {
      new TftpServer().start();
    } catch (BindException be) {
      System.err.println("Could not bind socket. Port probably already in use.");
    } catch (SocketException se) {
      se.printStackTrace();
    } catch (IOException ie) {
      System.err.println("I/O error.");
    }
  }

  private void start() throws SocketException, IOException {
    DatagramSocket socket        = new DatagramSocket(null);
    SocketAddress localBindPoint = new InetSocketAddress(TFTPPORT);
    socket.bind(localBindPoint);
    
    System.out.printf("Listening at port %d for new requests\n", TFTPPORT);
    
    byte[] buf = new byte[BUFSIZE];
    while (true) {
      final InetSocketAddress clientAddress = getClientAddr(socket, buf);

      if (clientAddress == null) {
        continue;
      }

      final StringBuffer requestedFile = new StringBuffer();
      final int reqtype = parseRq(buf, requestedFile);

      new Thread() {
        public void run() {
          try (DatagramSocket sendSocket = new DatagramSocket(0);) {
            sendSocket.connect(clientAddress);

            System.out.printf("%s request for %s from %s using port %d\n",
                (reqtype == OP_RRQ) ? "Read" : "Write",
                clientAddress.getHostName(), clientAddress.getAddress(), clientAddress.getPort());

            switch (reqtype) {
              case OP_RRQ:
                readRequest(requestedFile.insert(0, READDIR).toString(), sendSocket);
                break;
              case OP_WRQ:
                writeRequest(requestedFile.insert(0, WRITEDIR).toString(), sendSocket);
                break;
              default:
                System.err.println("Invalid request. Sending an error packet.");
                sendErr(sendSocket, Type.ILLEGAL_OPERATION);
                break;
            }
          } catch (IOException ioe) {
            ioe.printStackTrace();
          } finally {
            Thread.currentThread().interrupt();
          }
        }
      }.start();
    }
  }

  /**
   * Reads the first block of data, i.e., the request for an action (read or
   * write).
   *
   * @param socket (socket to read from)
   * @param buf    (where to store the read data)
   * @return socketAddress (the socket address of the client)
   */
  private InetSocketAddress getClientAddr(DatagramSocket socket, byte[] buf) throws IOException {
    DatagramPacket packet = new DatagramPacket(buf, buf.length);
    socket.receive(packet);
    return new InetSocketAddress(packet.getAddress(), packet.getPort());
  }

  /**
   * Handle write request.
   *
   * @param requestedFile the requested file to be written.
   * @param sendSocket the socket used for communication.
   */
  private void writeRequest(String requestedFile, DatagramSocket sendSocket) throws IOException {
    try {
      getDataWriteFile(sendSocket, requestedFile);
    } catch (TimeoutException te) {
      sendErr(sendSocket, Type.PREMATURE_TERMINATION);
    } catch (FileAlreadyExistsException faee) {
      sendErr(sendSocket, Type.FILE_ALREADY_EXISTS);
    }
  }

  /**
   * Handle read request.
   *
   * @param requestedFile the requested file to be read.
   * @param sendSocket the socket used for communication.
   */
  private void readRequest(String requestedFile, DatagramSocket sendSocket) throws IOException {
    try {
      sendData(sendSocket, requestedFile); 
    } catch (TimeoutException te) {
      sendErr(sendSocket, Type.PREMATURE_TERMINATION);
    } catch (FileNotFoundException fnfe) {
      System.err.println(fnfe.getLocalizedMessage());
      sendErr(sendSocket, Type.FILE_NOT_FOUND);
    }
  }

  /**
   * Parses the request in buf to retrieve the type of request and requestedFile.
   *
   * @param buf           (received request)
   * @param requestedFile (name of file to read/write)
   * @return opcode (request type: RRQ or WRQ)
   */
  private int parseRq(byte[] buf, StringBuffer requestedFile) {
    char[] charBuf = new String(buf).toCharArray();
    for (int i = 2; charBuf[i] != Character.MIN_VALUE; i++) {
      requestedFile.append(charBuf[i]);
    }
    return buf[0] + buf[1];
  }

  /**
   * Sends data to tftp client. 
   *
   * @param sendSocket the socket used for communication.
   * @param requestedFile the file to send.
   * @throws IOException if an I/O-error occurs.
   * @throws FileNotFoundException if requestedFile does not exist.
   * @throws ServerException if no ACK is received after file is sent.
   */
  private void sendData(DatagramSocket sendSocket, String requestedFile) 
      throws TimeoutException, IOException {
    byte[] fileData    = getFileData(requestedFile);
    int noPackets      = (int) Math.ceil(fileData.length / 511.9);
    byte[] header      = {0, OP_DAT, 0, 1}; //TODO: BLOCK NUMBER CAN BE TWO BYTES!
    int lastPacketSize = (fileData.length % 512) + header.length;
    DatagramPacket ack = null;

    for (int i = 0; i < noPackets; i++) {
      // All packets but last one should be 516 bytes long.
      int packetLength = (i == (noPackets - 1)) ? lastPacketSize : 516;
      ByteBuffer bb = ByteBuffer.allocate(packetLength);
      bb.put(header).put(fileData, i * 512, packetLength - header.length);
      DatagramPacket packetToSend = new DatagramPacket(bb.array(), packetLength);

      if ((ack = sendAndReceive(sendSocket, packetToSend)) == null) {
        throw new TimeoutException("Client didn't respond in time.");
      } else if (ack.getData()[OP_POS] != OP_ACK) {
        throw new ServerException("No ACK was received.");
      }
      header[OP_POS + 2] = (byte) (i + 2); //TODO: BLOCK NUMBER CAN BE TWO BYTES!
    }                                       // Maybe ByteBuffer.putShort()?
  }
  
  /**
   * Get all bytes from the requested file.
   *
   * @param requestedFile the name of the requested file.
   * @return bytes of the requested file.
   * @throws FileNotFoundException if the requested file doesn't exist.
   */
  private byte[] getFileData(String requestedFile) throws IOException {
    File file = new File(requestedFile);
    if (!file.exists()) {
      throw new FileNotFoundException("File " + requestedFile + " does not exist.");
    }
    return Files.readAllBytes(file.toPath());
  }

  /**
   * Attempts to retreive packet from tftp client. 
   * Creates file from packet data if successful.
   *
   * @param sendSocket the socket to send and receive through.
   * @return the generated file.
   * @throws FileAlreadyExistsException if filename already exists.
   * @throws TimeoutException if the client doesn't respond in time.
   */
  private void getDataWriteFile(DatagramSocket sendSocket, String fileName)
      throws TimeoutException, IOException {
    File fileToWrite = new File(fileName);
    if (fileToWrite.exists()) {
      throw new FileAlreadyExistsException("File already exists!");
    }

    byte[] ack = {0, OP_ACK, 0, 0};
    DatagramPacket ackPacket = new DatagramPacket(ack, ack.length);
    DatagramPacket receivedPacket = sendAndReceive(sendSocket, ackPacket);
    if (receivedPacket == null) {
      throw new TimeoutException("Client didn't respond in time.");
    }

    FileOutputStream fos = new FileOutputStream(fileToWrite);
    do {
      fos.write(receivedPacket.getData(), HEADERSIZE, receivedPacket.getLength() - HEADERSIZE);
      ack[BLOCK_NR_POS]++;
      ackPacket = new DatagramPacket(ack, ack.length);
    } while (receivedPacket.getLength() == 516
          && (receivedPacket = sendAndReceive(sendSocket, ackPacket)) != null);
    sendSocket.send(ackPacket);
    // TODO: dally();
    fos.flush();
    fos.close();
  }

  /**
   * Attempts to transmit packet until a packet is received with 
   * the correct block number. Or a maximum of 5 times.
   *
   * @param sendSocket the socket to send and receive through.
   * @param packet the packet to transmit. 
   * @return the packet received. Null if there are no packets to receive.
   */
  private DatagramPacket sendAndReceive(DatagramSocket sendSocket, DatagramPacket packet)
      throws IOException {
    sendSocket.send(packet);
    byte[] buf = new byte[BUFSIZE];
    DatagramPacket receivedPacket = new DatagramPacket(buf, buf.length);
    for (int i = 0; (i < 5); i++) {
      try {
        sendSocket.setSoTimeout(RETRANSMIT_TIME);
        sendSocket.receive(receivedPacket);
        return receivedPacket;
      } catch (SocketTimeoutException ste) {
        sendSocket.send(packet);
      }
    }
    return null;
  }

  /**
   * Send an error code and message.
   *
   * @param error the error code to be sent with the following error message.
   */
  private void sendErr(DatagramSocket sendSocket, Type error) throws IOException {
    byte[] header = {0, OP_ERR, 0, (byte) error.code};
    byte[] packet = new byte[header.length + error.message.length + 1];
    
    System.arraycopy(header, 0, packet, 0, header.length);
    System.arraycopy(error.message, 0, packet, header.length, error.message.length);

    sendSocket.send(new DatagramPacket(packet, packet.length));
  }
}
