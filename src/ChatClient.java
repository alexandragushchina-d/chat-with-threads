
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatClient {
  private String host = "127.0.0.1";
  private int port = 3000;
  private String userName = "";

  private Socket socket;
  private ObjectInputStream reader;
  private ObjectOutputStream writer;
  private Thread readerThread;
  private Thread writerThread;
  private AtomicBoolean stopFlag = new AtomicBoolean(false);
  private InputStream inputStream = System.in;
  private List<String> messages;

  public ChatClient() {
  }

  public ChatClient(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public void connectToServer() {
    try {
      socket = new Socket(host, port);

      if (userName == null || userName.length() == 0) {
        System.out.println("Choose please your username.");
        Scanner sc = new Scanner(inputStream);
        userName = sc.nextLine();
      }

      writer = new ObjectOutputStream(socket.getOutputStream());
      reader = new ObjectInputStream(socket.getInputStream());

      writer.writeUTF(userName);
      writer.flush();

      String firstAnswer = reader.readUTF();
      if (messages != null) {
        messages.add(firstAnswer);
      }
      System.out.println(firstAnswer);
      if (!firstAnswer.substring(0, "Hello!".length()).equals("Hello!")) {
        writer.close();
        writer = null;
        reader.close();
        reader = null;
        socket.close();
        socket = null;
        return;
      }

      readerThread = new Thread() {
        private ChatClient parent;

        public Thread setParent(ChatClient parent) {
          this.parent = parent;
          return this;
        }

        public void run() {
          if (parent != null) {
            parent.readMessages();
          }
        }
      }.setParent(this);
      readerThread.start();

      writerThread = new Thread() {
        private ChatClient parent;

        public Thread setParent(ChatClient parent) {
          this.parent = parent;
          return this;
        }

        public void run() {
          if (parent != null) {
            parent.sendMessages();
          }
        }
      }.setParent(this);
      writerThread.start();
    } catch (Exception e) {
      System.err.println("Can't connect to ChatServer (" + e.getMessage() + ")");
    }
  }

  public String getName() {
    return userName;
  }

  public void setName(String name) {
    userName = name;
  }

  public void setInputStream(InputStream stream) {
    inputStream = stream;
  }

  public void setMessageList(List<String> list) {
    messages = list;
  }

  public void sendMessages() {
    try {
      Scanner sc = new Scanner(inputStream);
      while (!stopFlag.get()) {
        String msg = sc.nextLine();
        writer.writeUTF(msg);
        writer.flush();
        if (msg.equals("LOGOUT")) {
          stopFlag.set(true);
          readerThread.interrupt();
        }
      }

      writer.close();
      writer = null;
    } catch (Exception e) {
      System.out.println("Sending a message has failed (" + e.getMessage() + ")");
    }
  }

  public void readMessages() {
    try {
      while (!stopFlag.get()) {
        String msg = reader.readUTF();
        if (messages != null) {
          messages.add(msg);
        }
        System.out.println(reader.readUTF());
      }

      reader.close();
      reader = null;
    } catch (Exception e) {
      System.out.println("Getting a message has failed (" + e.getMessage() + ")");
    }
  }

  public void closeConnection() {
    try {
      stopFlag.set(true);

      if (writerThread != null) {
        writerThread.interrupt();
        writerThread = null;
      }

      if (readerThread != null) {
        readerThread.interrupt();
        readerThread = null;
      }

      if (socket != null) {
        socket.close();
        socket = null;
      }
    } catch (Exception e) {
      System.err.println("Closing connection has failed (" + e.getMessage() + ")");
    }
  }

  public void join() throws Exception {
    if (readerThread != null && readerThread.isAlive()) {
      readerThread.join();
    }

    if (writerThread != null && writerThread.isAlive()) {
      writerThread.join();
    }
  }
}
