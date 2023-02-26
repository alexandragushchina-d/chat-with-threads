
import org.junit.Assert;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ChatClientTest {
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
      ChatServer chatServer = new ChatServer(3001);
      chatServer.start();
      while (!chatServer.isAlive()) {
        Thread.sleep(100);
      }

      PipedInputStream input = new PipedInputStream();
      PipedOutputStream output = new PipedOutputStream();
      input.connect(output);

      List<String> messages = new ArrayList<>();

      ChatClient client = new ChatClient("127.0.0.1", 3001);
      client.setInputStream(input);
      client.setMessageList(messages);
      output.write("Vasya\n".getBytes());
      client.connectToServer();
      assertEquals(1, messages.size());
      assertEquals(hello, messages.get(0));
      client.closeConnection();

      chatServer.stop();
    } catch (Exception e) {
      Assert.fail("Sending username has failed (" + e.getMessage() + ")");
    }
  }

//  @Test
//  @Order(2)
//  public void userDoesntExist() {
//    try {
//      ChatServer chatServer = new ChatServer(3001);
//      chatServer.start();
//      while (!chatServer.isAlive()) {
//        Thread.sleep(100);
//      }
//
//      PipedInputStream input = new PipedInputStream();
//      PipedOutputStream output = new PipedOutputStream();
//      input.connect(output);
//
//      List<String> messages = new ArrayList<>();
//
//      ChatClient client = new ChatClient("127.0.0.1", 3001);
//      client.setInputStream(input);
//      client.setMessageList(messages);
//      output.write("Vasya\n".getBytes());
//      output.write("@amanda Message\n".getBytes());
//      client.connectToServer();
//      Thread.sleep(100);
//      assertEquals(1, messages.size());
//      assertEquals(hello, messages.get(0));
//      assertEquals(2, messages.size());
//      assertEquals("amanda doesn't exist.", messages.get(0));
//      client.closeConnection();
//
//      chatServer.stop();
//    } catch (Exception e) {
//      Assert.fail("Sending username has failed (" + e.getMessage() + ")");
//    }
//  }
}
