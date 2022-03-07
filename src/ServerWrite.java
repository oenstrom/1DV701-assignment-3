import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;

/**
 * Class for handling write request.
 */
public class ServerWrite extends Server {

  public ServerWrite(Packet.Write packet) {
    this.packet = packet;
  }

  @Override
  protected void handleFile(File file) throws IOException {
    // TODO Auto-generated method stub
    
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