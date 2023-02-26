
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class ChatServer {
  private int port = 3000;
  private ServerSocket serverSocket;
  private Thread serverThread;
  private AtomicBoolean stopFlag = new AtomicBoolean(false);
  private AtomicBoolean isAliveFlag = new AtomicBoolean(false);
  private int userId = 0;
  private Map<Integer, ServerUser> unhandledUsers = new HashMap<>();
  private Map<String, ServerUser> users = new HashMap<>();

  static final String[] penguinFacts = {
    "Penguins are flightless birds.",
    "Penguins can drink sea water.",
    "Most penguins live in the Southern Hemisphere.",
    "No penguins live at the North Pole.",
    "Penguins in Antarctica have no land based predators.",
    "The fastest species is the Gentoo Penguin, which can reach swimming speeds up to 22 mph.",
    "Fossils place the earliest penguin relative at some 60 million years ago.",
    "Many penguins will mate with the same member of the opposite sex season after season."
  };

  public ChatServer() {
  }

  public ChatServer(int port) {
    this.port = port;
  }

  public void start() throws IOException {
    serverSocket = new ServerSocket(port);
    serverThread = new Thread() {
      private ChatServer server;

      public Thread setServer(ChatServer server) {
        this.server = server;
        return this;
      }

      @Override
      public void run() {
        if (server == null) {
          return;
        }

        server.run();
      }
    }.setServer(this);

    serverThread.start();
  }

  public boolean isAlive() {
    return isAliveFlag.get();
  }

  public void stop() throws IOException {
    stopFlag.set(true);
    IOException closeSocketException = null;
    try {
      if (serverSocket != null) {
        serverSocket.close();
      }
    } catch (Exception e) {
      System.out.println("Closing the socket has failed (" + e.getMessage() + ")");
      closeSocketException = new IOException(e.getMessage());
    }

    synchronized (users) {
      for (ServerUser user : users.values()) {
        if (user != null) {
          try {
            user.close();
          } catch (Exception e) {
            System.err.println("Closing the userserver has failed (" + e.getMessage() + ").");
          }
        }
      }
    }

    synchronized (unhandledUsers) {
      for (ServerUser unhandledUser : unhandledUsers.values()) {
        if (unhandledUser != null) {
          try {
            unhandledUser.close();
          } catch (Exception e) {
            System.err.println("Closing the userserver has failed (" + e.getMessage() + ").");
          }
        }
      }
    }

    if (serverThread != null) {
      if (serverThread.isAlive()) {
        try {
          serverThread.join();
        } catch (Exception e) {
          System.out.println("Waiting the thread has failed (" + e.getMessage() + ")");
          throw new IOException(e.getMessage());
        }
      }
      serverThread = null;
    }

    if (closeSocketException != null) {
      throw closeSocketException;
    }
  }

  private void run() {
    isAliveFlag.set(true);
    while (!stopFlag.get()) {
      try {
        Socket userSocket = serverSocket.accept();
        ServerUser user = new ServerUser(this, userId++, userSocket);
        synchronized (unhandledUsers) {
          unhandledUsers.put(user.getUserId(), user);
        }
        user.initialize();
      } catch (Exception e) {
        System.out.println("Accepting a connection has failed (" + e.getMessage() + ")");
      }
    }
    isAliveFlag.set(false);
  }

  private boolean handleUser(int id) {
    synchronized (unhandledUsers) {
      if (unhandledUsers.get(id).getUserName() != null) {
        if (unhandledUsers.get(id).getUserName().contains(" ")) {
          unhandledUsers.get(id).sendMessage("The username is invalid.");
          return false;
        }

        synchronized (users) {
          if (!users.containsKey(unhandledUsers.get(id).getUserName())) {
            users.put(unhandledUsers.get(id).getUserName(), unhandledUsers.get(id));
            sendBroadcastMessage(
              unhandledUsers.get(id),
              "***** " + unhandledUsers.get(id).getUserName() +
                " has joined to the chat room.*****",
              false);
            unhandledUsers.remove(id);
            return true;
          } else {
            unhandledUsers.get(id).sendMessage("This username is already taken.");
            return false;
          }
        }
      } else {
        unhandledUsers.get(id).sendMessage("The username is invalid.");
        return false;
      }
    }
  }

  private String formMessage(ServerUser from, String msg) {
    return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSSSSS")) + " " + from.getUserName() + ": " + msg;
  }

  private boolean sendUserMessage(ServerUser from, String userName, String msg) {
    synchronized (users) {
      if (users.containsKey(userName)) {
        return users.get(userName).sendMessage(formMessage(from, msg));
      } else {
        from.sendMessage(userName + " doesn't exist.");
      }
    }

    return false;
  }

  private boolean sendBroadcastMessage(
    ServerUser from, String msg, boolean addUserName) {
    List<ServerUser> local_users = null;
    synchronized (users) {
      local_users = users.values().stream().collect(Collectors.toList());
    }

    if (addUserName) {
      msg = formMessage(from, msg);
    }

    boolean result = true;
    for (ServerUser user : local_users) {
      if (user.equals(from)) {
        continue;
      }

      if (!user.sendMessage(msg)) {
        result = false;
      }
    }
    return result;
  }

  private boolean deleteUser(String userName) {
    synchronized (users) {
      if (users.containsKey(userName)) {
        users.get(userName).close();
        users.remove(userName);
        return true;
      }
      return false;
    }
  }

  private String randomFactPengu() {
    Random random = new Random();
    return penguinFacts[random.nextInt(penguinFacts.length)];
  }

  private String getWHOIS(String exclude) {
    List<String> usersWHOIS = new ArrayList<>();
    synchronized (users) {
      users.values().stream().filter(user -> user.getUserName() != exclude)
        .sorted(Comparator.comparing(ServerUser::getUserName))
        .forEach(user -> usersWHOIS.add(user.getWHOIS()));
    }

    String result = new String();
    for (int i = 0; i < usersWHOIS.size(); i++) {
      if (i != 0) {
        result += "\n";
      }
      result += String.valueOf(i + 1) + ") " + usersWHOIS.get(i);
    }

    return result;
  }

  private static class ServerUser extends Thread {
    private ChatServer server;
    private int userId;
    private Socket clientSocket;
    private ObjectInputStream reader;
    private ObjectOutputStream writer;
    private AtomicBoolean stopFlag = new AtomicBoolean(false);
    private String userName;
    private final LocalTime since = LocalTime.now();

    public ServerUser(ChatServer server, int userId, Socket socket) throws Exception {
      this.server = server;
      this.userId = userId;
      this.clientSocket = socket;
    }

    private void initialize() throws Exception {
      reader = new ObjectInputStream(clientSocket.getInputStream());
      writer = new ObjectOutputStream(clientSocket.getOutputStream());
      super.start();
    }

    @Override
    public void run() {
      try {
        userName = reader.readUTF();
        if (!server.handleUser(userId)) {
          return;
        }

        sendMessage(
          "Hello! Welcome to the chatroom.\n " +
            "Instructions:\n" +
            "- Simply type the message to send broadcast to all active clients\n" +
            "- Type '@username<space>yourmessage' without quotes to send message to desired client\n" +
            "- Type 'WHOIS' without quotes to see list of active clients\n" +
            "- Type 'LOGOUT' without quotes to logoff from server\n" +
            "- Type 'PENGU' without quotes to request a random penguin fact");

        while (!stopFlag.get()) {
          String message = reader.readUTF();

          if (message.length() > 0 && message.substring(0, 1).equals("@")) {
            String[] line = message.split(" ", 2);
            if (!server.sendUserMessage(this, line[0].substring(1), line[1])) {
              sendMessage("The message hasn't been sent.");
            }
          } else if (message.equals("WHOIS")) {
            sendMessage(server.getWHOIS(userName));
          } else if (message.equals("LOGOUT")) {
            break;
          } else if (message.equals("PENGU")) {
            String fact = server.randomFactPengu();
            sendMessage(fact);
            server.sendBroadcastMessage(this, fact, false);
          } else {
            if (!server.sendBroadcastMessage(this, message, true)) {
              sendMessage("The message hasn't been sent to all users.");
            }
          }
        }
      } catch (IOException e) {
        System.out.println("Reading a message has failed (" + e.getMessage() + ")");
      }
      this.server.deleteUser(userName);
    }

    public String getUserName() {
      return userName;
    }

    public int getUserId() {
      return userId;
    }

    public synchronized boolean sendMessage(String msg) {
      try {
        writer.writeUTF(msg);
        writer.flush();
      } catch (IOException e) {
        System.out.println("Sending a message has failed (" + e.getMessage() + ")");
        close();
        return false;
      }
      return true;
    }

    public void close() {
      stopFlag.set(true);
      try {
        interrupt();

        if (reader != null) {
          reader.close();
        }
        if (writer != null) {
          writer.close();
        }
        if (clientSocket != null) {
          clientSocket.close();
        }
      } catch (Exception e) {
        System.out.println("Closing a user has failed (" + e.getMessage() + ")");
      }
    }

    private String getWHOIS() {
      return getUserName() + " since " + since.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSSSSS"));
    }

    @Override
    public int hashCode() {
      return userName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof ServerUser)) {
        return false;
      }
      return Objects.equals(userName, ((ServerUser) obj).userName);
    }
  }
}
