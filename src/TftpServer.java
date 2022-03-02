import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.ServerException;

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
   * Hej.
   */
  public static void main(String[] args) {
    if (args.length > 0) {
      System.err.printf("usage: java %s\n", TftpServer.class.getCanonicalName());
      System.exit(1);
    }
    // Starting the server
    try {
      TftpServer server = new TftpServer();
      server.start();
    } catch (SocketException e) {
      e.printStackTrace();
    }
  }

  private void start() throws SocketException {
    byte[] buf = new byte[BUFSIZE];

    // Create socket
    DatagramSocket socket = new DatagramSocket(null);

    // Create local bind point
    SocketAddress localBindPoint = new InetSocketAddress(TFTPPORT);
    socket.bind(localBindPoint);

    System.out.printf("Listening at port %d for new requests\n", TFTPPORT);

    // Loop to handle client requests
    while (true) {

      final InetSocketAddress clientAddress = receiveFrom(socket, buf);

      // If clientAddress is null, an error occurred in receiveFrom()
      if (clientAddress == null) {
        continue;
      }

      final StringBuffer requestedFile = new StringBuffer();
      final int reqtype = parseRQ(buf, requestedFile);

      new Thread() {
        public void run() {
          try (DatagramSocket sendSocket = new DatagramSocket(0);) {
            
            sendSocket.setSoTimeout(10000);
            // Connect to client
            sendSocket.connect(clientAddress);

            System.out.printf("%s request for %s from %s using port %d\n",
                (reqtype == OP_RRQ) ? "Read" : "Write",
                clientAddress.getHostName(), clientAddress.getAddress(), clientAddress.getPort());

            // Read request
            if (reqtype == OP_RRQ) {
              requestedFile.insert(0, READDIR);
              handleRQ(sendSocket, requestedFile.toString(), OP_RRQ);
            } else { // Write request
              requestedFile.insert(0, WRITEDIR);
              handleRQ(sendSocket, requestedFile.toString(), OP_WRQ);
            }
            sendSocket.close();
          } catch (SocketTimeoutException ste) {
            System.err.println("Socket timed out.");
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
  private InetSocketAddress receiveFrom(DatagramSocket socket, byte[] buf) {
    // Create datagram packet
    DatagramPacket packet = new DatagramPacket(buf, buf.length);
    // Receive packet
    try {
      socket.receive(packet);
    } catch (IOException e) {
      e.printStackTrace();
    }
    // Get client address and port from the packet
    return new InetSocketAddress(packet.getAddress(), packet.getPort());
  }

  /**
   * Parses the request in buf to retrieve the type of request and requestedFile.
   *
   * @param buf           (received request)
   * @param requestedFile (name of file to read/write)
   * @return opcode (request type: RRQ or WRQ)
   */
  private int parseRQ(byte[] buf, StringBuffer requestedFile) {
    char[] charBuf = new String(buf, StandardCharsets.US_ASCII).toCharArray();
    for (int i = 2; charBuf[i] != Character.MIN_VALUE; i++) {
      requestedFile.append(charBuf[i]);
    }
    return buf[0] + buf[1];
  }

  /**
   * Handles RRQ and WRQ requests.
   *
   * @param sendSocket (socket used to send/receive packets)
   * @param requestedFile (name of file to read/write)
   * @param opcode (RRQ or WRQ)
   * @throws IOException if an I/O error-occurs.
   */
  private void handleRQ(DatagramSocket sendSocket, String requestedFile, int opcode) 
      throws IOException {
    if (opcode == OP_RRQ) {
      // See "TFTP Formats" in TFTP specification for the DATA and ACK packet contents
      try {
        sendDataReceiveAck(sendSocket, requestedFile);      
      } catch (FileNotFoundException fnfe) {
        System.err.println(fnfe.getLocalizedMessage());
        sendErr(sendSocket, Error.File_Not_Found);
      } catch (ServerException se) { //No ACK was received.
        System.err.println(se.getLocalizedMessage()); //TODO: Maybe not?
      }
    } else if (opcode == OP_WRQ) {
      generateFile(sendSocket, requestedFile);
    } else {
      System.err.println("Invalid request. Sending an error packet.");
      // See "TFTP Formats" in TFTP specification for the ERROR packet contents
      sendErr(sendSocket, Error.Illegal_Operation);
      return;
    }
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
   * Sends data and hreceives ACK's.
   *
   * @param sendSocket the socket used for communication.
   * @param requestedFile the file to send.
   * @throws IOException if an I/O-error occurs.
   * @throws FileNotFoundException if requestedFile does not exist.
   * @throws ServerException if no ACK is received after file is sent.
   */
  private void sendDataReceiveAck(DatagramSocket sendSocket, String requestedFile) 
      throws IOException {
    byte[] fileData    = getFileData(requestedFile);
    int noPackets      = (int) Math.ceil(fileData.length / 511.9);
    byte[] header      = {0, OP_DAT, 0, 1}; //TODO: BLOCK NUMBER CAN BE TWO BYTES!
    int lastPacketSize = (fileData.length % 512) + header.length;

    for (int i = 0; i < noPackets; i++) {
      // All packets but last one should be 516 bytes long.
      int packetLength = (i == (noPackets - 1)) ? lastPacketSize : 516;
      ByteBuffer bb = ByteBuffer.allocate(packetLength);
      bb.put(header).put(fileData, i * 512, packetLength - header.length);
      DatagramPacket packetToSend = new DatagramPacket(bb.array(), packetLength);

      sendSocket.send(packetToSend);
      if (retransmit(sendSocket, packetToSend) == null) {
        break;
      }

      // if (ack.getData()[OP_POS] != OP_ACK) {
      //   throw new ServerException("No ACK was received."); //TODO: Temporary type of exception.  
      // }                                                    //TODO: To be changed.
      header[OP_POS + 2] = (byte) (i + 2); //TODO: BLOCK NUMBER CAN BE TWO BYTES!
    }
  }

  /**
   * Attempts to retreive packet from client. Creates file from packet data if successfull.
   *
   * @param sendSocket the socket to send and receive through.
   * @return the generated file.
   * @throws FileAlreadyExistsException if filename already exists.
   */
  private void generateFile(DatagramSocket sendSocket, String fileName) throws IOException {
    //TODO: Finalize this.
    byte[] ack = {0, OP_ACK, 0, 0};
    DatagramPacket ackPacket = new DatagramPacket(ack, ack.length);    
    sendSocket.send(ackPacket);
    DatagramPacket receivedPacket = retransmit(sendSocket, ackPacket);
    if (receivedPacket == null) {
      throw new ServerException("Client didn't respond!");
    }

    File fileToWrite = new File(fileName);
    if (fileToWrite.exists()) {
      throw new FileAlreadyExistsException("File already exists!");
    }

    FileOutputStream fos = new FileOutputStream(fileToWrite);
    do {
      System.out.println("0. " + receivedPacket.getData().length);
      System.out.println("1. " + receivedPacket.getLength());
      byte[] data = receivedPacket.getData();
      fos.write(data, HEADERSIZE, data.length - HEADERSIZE);
      ack[BLOCK_NR_POS]++;
      ackPacket = new DatagramPacket(ack, ack.length);
      sendSocket.send(ackPacket);
    } while (receivedPacket.getLength() == 516 
          && (receivedPacket = retransmit(sendSocket, ackPacket)) != null);
    //TODO: Blir något error i receivePacket() efter man tagit emot.
    fos.flush();
    fos.close();
  }

  /**
   * Attempts to transmit packet until a packet is received with 
   * the correct block number. Or a maximum of 5 times.
   *
   * @param sendSocket the socket to send and receive through.
   * @param last the packet to retransmit. 
   * @return Packet received.
   */
  private DatagramPacket retransmit(DatagramSocket sendSocket, DatagramPacket last)
      throws IOException {
    //TODO: Maybe change the name of this method?
    short blockNr = ByteBuffer.wrap(last.getData()).getShort(2);
    DatagramPacket dp = null;
    for (int i = 0; (i < 5); i++) {
      if ((dp = receivePacket(sendSocket)) != null
          && blockNr == ByteBuffer.wrap(dp.getData()).getShort(2)) {
        return dp;
      }
      sendSocket.send(last);
    }
    return dp;
  }

  /**
   * Tries to receive packet from client. 
   *
   * @param socket the socket used for communication.
   * @return the received packet or null if the socket times out.
   */
  private DatagramPacket receivePacket(DatagramSocket sendSocket) throws IOException {
    sendSocket.setSoTimeout(RETRANSMIT_TIME);
    byte[] buf = new byte[BUFSIZE];
    DatagramPacket receivedPacket = new DatagramPacket(buf, buf.length);
    try {
      sendSocket.receive(receivedPacket);
    } catch (SocketTimeoutException ste) {
      return null;
    }
    return receivedPacket;
  }

  /**
   * Send an error code and message.
   *
   * @param error the error code to be sent with the following error message.
   */
  private void sendErr(DatagramSocket sendSocket, Error error) throws IOException {
    byte[] header = {0, OP_ERR, 0, error.code};

    byte[] packet = new byte[header.length + error.message.length + 1];
    
    System.arraycopy(header, 0, packet, 0, header.length);
    System.arraycopy(error.message, 0, packet, header.length, error.message.length);

    sendSocket.send(new DatagramPacket(packet, packet.length));
  }
}
