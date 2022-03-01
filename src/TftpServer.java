import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.rmi.ServerException;

public class TftpServer {
  public static final int TFTPPORT = 4970;
  public static final int BUFSIZE = 516;
  public static final String READDIR = System.getProperty("user.dir") + "\\"; // custom address at your PC
  public static final String WRITEDIR = "/home/username/write/"; // custom address at your PC
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
          } catch (IOException ioe) {
            ioe.printStackTrace();
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
        //TODO: Implement sendErr and add it here.
        //sendErr(params);
      } catch (ServerException se) { //No ACK was received.
        System.err.println(se.getLocalizedMessage()); 
      }
    }
    // } else if (opcode == OP_WRQ) {
    //   boolean result = receiveDataSendAck(params);
    // } else {
    //   System.err.println("Invalid request. Sending an error packet.");
    //   // See "TFTP Formats" in TFTP specification for the ERROR packet contents
    //   sendErr(params);
    //   return;
    // }
  }

  /**
   * Sends data and hreceives ACK's.
   *
   * @param sendSocket the socket used for communication.
   * @param requestedFile the file to send.
   * @throws IOException if an I/O-error occurs.
   */
  private void sendDataReceiveAck(DatagramSocket sendSocket, String requestedFile) 
      throws IOException {
    System.out.println(requestedFile);
    File file = new File(requestedFile);
    if (!file.exists()) {
      throw new FileNotFoundException("File " + requestedFile + " doesn't exist.");
    }
    byte[] fileData;

    fileData = Files.readAllBytes(file.toPath());
  
    int noPackets = (int) Math.ceil(fileData.length / 511.0);
    int lastPacketSize = (fileData.length % 512) + 4;
    byte[] header = {0, OP_DAT, 0, 1};
    for (int i = 0; i < noPackets; i++) {
      int size      = ((i + 1) == noPackets) ? lastPacketSize : 516;
      byte[] packet = new byte[size];

      System.arraycopy(header, 0, packet, 0, 4);
      System.arraycopy(fileData, i * 512, packet, 4, size - 4);

      sendSocket.send(new DatagramPacket(packet, packet.length));

      byte[] buf       = new byte[BUFSIZE];
      DatagramPacket p = new DatagramPacket(buf, BUFSIZE);
      sendSocket.receive(p);

      if (buf[1] != OP_ACK) {
        throw new ServerException("No ACK was received.");
      }
      header[3] = (byte) (i + 2); 
    }
  }

  // private boolean receiveDataSendAck(params) {
  //   return true;
  // }

  // private void sendErr(params) {
  // }
}
