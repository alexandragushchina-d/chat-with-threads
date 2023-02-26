
import org.junit.Assert;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ChatServerTest {
  private static final String hello =
    "Hello! Welcome to the chatroom.\n " +
      "Instructions:\n" +
      "- Simply type the message to send broadcast to all active clients\n" +
      "- Type '@username<space>yourmessage' without quotes to send message to desired client\n" +
      "- Type 'WHOIS' without quotes to see list of active clients\n" +
      "- Type 'LOGOUT' without quotes to logoff from server\n" +
      "- Type 'PENGU' without quotes to request a random penguin fact";

  @Test
  @Order(1)
  public void sendUsername() {
    try {
      ChatServer chatServer = new ChatServer(3000);
      chatServer.start();
      while (!chatServer.isAlive()) {
        Thread.sleep(100);
      }
      Socket socket = new Socket("127.0.0.1", 3000);
      ObjectOutputStream writer = new ObjectOutputStream(socket.getOutputStream());
      ObjectInputStream reader = new ObjectInputStream(socket.getInputStream());
      writer.writeUTF("alice");
      writer.flush();
      String result = reader.readUTF();
      assertEquals(hello, result);
      chatServer.stop();
    } catch (Exception e) {
      Assert.fail("Sending username has failed (" + e.getMessage() + ")");
    }
  }

  @Test
  @Order(2)
  public void sendWHOIS() {
    try {
      ChatServer chatServer = new ChatServer(3000);
      chatServer.start();
      while (!chatServer.isAlive()) {
        Thread.sleep(100);
      }
      Socket socket1 = new Socket("127.0.0.1", 3000);
      ObjectOutputStream writer1 = new ObjectOutputStream(socket1.getOutputStream());
      ObjectInputStream reader1 = new ObjectInputStream(socket1.getInputStream());

      Socket socket2 = new Socket("127.0.0.1", 3000);
      ObjectOutputStream writer2 = new ObjectOutputStream(socket2.getOutputStream());
      ObjectInputStream reader2 = new ObjectInputStream(socket2.getInputStream());

      Socket socket3 = new Socket("127.0.0.1", 3000);
      ObjectOutputStream writer3 = new ObjectOutputStream(socket3.getOutputStream());
      ObjectInputStream reader3 = new ObjectInputStream(socket3.getInputStream());

      writer2.writeUTF("amanda");
      writer2.flush();
      Thread.sleep(100);
      writer3.writeUTF("bob");
      writer3.flush();
      Thread.sleep(100);
      writer1.writeUTF("alice");
      writer1.flush();

      String result = reader1.readUTF();
      assertEquals(hello, result);

      writer1.writeUTF("WHOIS");
      writer1.flush();
      result = reader1.readUTF();
      String[] users = result.split("\n", 2);
      assertEquals(2, users.length);
      assert (users[0].startsWith("1) amanda since"));
      assert (users[1].startsWith("2) bob since"));

      chatServer.stop();
    } catch (Exception e) {
      Assert.fail("Sending WHOIS has failed (" + e.getMessage() + ")");
    }
  }

  @Test
  @Order(3)
  public void sendBroadcastMessage() {
    try {
      ChatServer chatServer = new ChatServer(3000);
      chatServer.start();
      while (!chatServer.isAlive()) {
        Thread.sleep(100);
      }
      Socket socket1 = new Socket("127.0.0.1", 3000);
      ObjectOutputStream writer1 = new ObjectOutputStream(socket1.getOutputStream());
      ObjectInputStream reader1 = new ObjectInputStream(socket1.getInputStream());

      Socket socket2 = new Socket("127.0.0.1", 3000);
      ObjectOutputStream writer2 = new ObjectOutputStream(socket2.getOutputStream());
      ObjectInputStream reader2 = new ObjectInputStream(socket2.getInputStream());

      Socket socket3 = new Socket("127.0.0.1", 3000);
      ObjectOutputStream writer3 = new ObjectOutputStream(socket3.getOutputStream());
      ObjectInputStream reader3 = new ObjectInputStream(socket3.getInputStream());

      writer1.writeUTF("alice");
      writer1.flush();
      Thread.sleep(100);
      writer2.writeUTF("amanda");
      writer2.flush();
      Thread.sleep(100);
      writer3.writeUTF("bob");
      writer3.flush();
      Thread.sleep(100);

      String result = reader1.readUTF();
      assertEquals(hello, result);

      writer1.writeUTF("Broadcast message");
      writer1.flush();
      Thread.sleep(100);

      result = reader2.readUTF();
      assertEquals(hello, result);

      result = reader2.readUTF();
      assertEquals("***** bob has joined to the chat room.*****", result);

      result = reader2.readUTF();
      assertEquals("alice: Broadcast message", result.substring(16));

      result = reader3.readUTF();
      assertEquals(hello, result);

      result = reader3.readUTF();
      assertEquals("alice: Broadcast message", result.substring(16));

      chatServer.stop();
    } catch (Exception e) {
      Assert.fail("Sending message has failed (" + e.getMessage() + ")");
    }
  }

  @Test
  @Order(4)
  public void userDoesntExist() {
    try {
      ChatServer chatServer = new ChatServer(3000);
      chatServer.start();
      while (!chatServer.isAlive()) {
        Thread.sleep(100);
      }
      Socket socket1 = new Socket("127.0.0.1", 3000);
      ObjectOutputStream writer1 = new ObjectOutputStream(socket1.getOutputStream());
      ObjectInputStream reader1 = new ObjectInputStream(socket1.getInputStream());

      Socket socket2 = new Socket("127.0.0.1", 3000);
      ObjectOutputStream writer2 = new ObjectOutputStream(socket2.getOutputStream());
      ObjectInputStream reader2 = new ObjectInputStream(socket2.getInputStream());

      writer1.writeUTF("alice");
      writer1.flush();
      Thread.sleep(100);
      writer2.writeUTF("amanda");
      writer2.flush();
      Thread.sleep(100);

      String result = reader1.readUTF();
      assertEquals(hello, result);

      result = reader2.readUTF();
      assertEquals(hello, result);

      result = reader1.readUTF();
      assertEquals("***** amanda has joined to the chat room.*****", result);

      writer1.writeUTF("WHOIS");
      writer1.flush();
      result = reader1.readUTF();
      assert (result.startsWith("1) amanda since"));

      writer1.writeUTF("@amanda Test1");
      writer1.flush();
      result = reader2.readUTF();
      assertEquals("alice: Test1", result.substring(16));

      writer2.writeUTF("LOGOUT");
      writer2.flush();
      writer2.close();
      Thread.sleep(100);

      writer1.writeUTF("@amanda Test2");
      writer1.flush();
      result = reader1.readUTF();
      assertEquals("amanda doesn't exist.", result);

      chatServer.stop();
    } catch (Exception e) {
      Assert.fail("Sending WHOIS has failed (" + e.getMessage() + ")");
    }
  }

  @Test
  @Order(5)
  public void getPengu() {
    try {
      ChatServer chatServer = new ChatServer(3000);
      chatServer.start();
      while (!chatServer.isAlive()) {
        Thread.sleep(100);
      }
      Socket socket1 = new Socket("127.0.0.1", 3000);
      ObjectOutputStream writer1 = new ObjectOutputStream(socket1.getOutputStream());
      ObjectInputStream reader1 = new ObjectInputStream(socket1.getInputStream());

      Socket socket2 = new Socket("127.0.0.1", 3000);
      ObjectOutputStream writer2 = new ObjectOutputStream(socket2.getOutputStream());
      ObjectInputStream reader2 = new ObjectInputStream(socket2.getInputStream());

      writer1.writeUTF("alice");
      writer1.flush();
      Thread.sleep(100);
      writer2.writeUTF("amanda");
      writer2.flush();
      Thread.sleep(100);

      String result = reader1.readUTF();
      assertEquals(hello, result);

      result = reader1.readUTF();
      assertEquals("***** amanda has joined to the chat room.*****", result);

      writer1.writeUTF("PENGU");
      writer1.flush();
      result = reader1.readUTF();
      assert (result.contains("enguin"));

      result = reader2.readUTF();
      assertEquals(hello, result);

      result = reader2.readUTF();
      assert (result.contains("enguin"));

      chatServer.stop();
    } catch (Exception e) {
      Assert.fail("Sending username has failed (" + e.getMessage() + ")");
    }
  }
}
