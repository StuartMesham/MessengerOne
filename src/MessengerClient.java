import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class MessengerClient {
	
	//-------SHARED BETWEEN SERVER AND CLIENT APPLICATIONS----------

	static final int PORT_NUMBER = 4444;
	
	static final int CLIENT_SIGN_UP = 0x0;
	static final int CLIENT_LOGIN = 0x1;

	//Bits 1 and 2
	static final int SERVER_SIGN_UP_RESPONSE = 0x0;       //0000
	static final int SERVER_LOGIN_RESPONSE = 0x1;         //0001
	static final int SERVER_MESSAGE_DELIVERY = 0x2;       //0010

	//Bit 3
	static final int SERVER_ACTION_SUCCESSFUL = 0x4;      //0100
	static final int SERVER_ACTION_FAILED = 0x0;          //0000

	//Bit 4
	static final int SERVER_USERNAME_UNAVAILABLE = 0x8;   //1000
	static final int SERVER_USERNAME_AVAILABLE = 0x0;     //0000

	//---------------------------------------------------------------
	
	public static void main(String[] args) throws Exception {
		Scanner scanner = new Scanner(System.in);
		
		System.out.print("Enter server address: ");
		String address = scanner.nextLine();
		
		System.out.println("Connecting to server");
		Socket socket = new Socket(address, PORT_NUMBER);
		System.out.println("Connected to server");
		
		DataInputStream inputStream = new DataInputStream(socket.getInputStream());
		DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
		
		mainloop: while (true) {
			System.out.print("Enter S to sign up, L to log in or E to exit: ");
			String line = scanner.nextLine();
			if (line.equalsIgnoreCase("E")) break;
			
			boolean signingUp = line.equalsIgnoreCase("S");
			
			System.out.print("Enter username: ");
			String username = scanner.nextLine();
			
			System.out.print("Enter password: ");
			String password = scanner.nextLine();
			
			if (signingUp) {
				outputStream.write(CLIENT_SIGN_UP);
				outputStream.writeUTF(username);
				outputStream.writeUTF(password);
				outputStream.flush();
				
				int receivedByte = inputStream.read();
				
				switch (receivedByte) {
					case SERVER_SIGN_UP_RESPONSE | SERVER_ACTION_SUCCESSFUL:
						System.out.println("Sign up successful");
						break;
					case SERVER_SIGN_UP_RESPONSE | SERVER_ACTION_FAILED | SERVER_USERNAME_UNAVAILABLE:
						System.out.println("Sign up failed: user already exists");
						break;
				}
			} else {
				outputStream.write(CLIENT_LOGIN);
				outputStream.writeUTF(username);
				outputStream.writeUTF(password);
				outputStream.flush();
				
				int receivedByte = inputStream.read();
				
				switch (receivedByte) {
					case SERVER_LOGIN_RESPONSE | SERVER_ACTION_SUCCESSFUL:
						System.out.println("Login successful");
						
						new MessageReceiveThread(inputStream).start();
						
						while (true) {
							System.out.print("Enter username to send message to (or E to exit): ");
							username = scanner.nextLine();
							
							if (username.equalsIgnoreCase("E")) {
								break mainloop;
							}
							
							System.out.print("Enter message: ");
							String content = scanner.nextLine();
							
							outputStream.writeUTF(username);
							outputStream.writeUTF(content);
							outputStream.flush();
						}
						
					case SERVER_LOGIN_RESPONSE | SERVER_ACTION_FAILED:
						System.out.println("Login failed: username or password invalid");
						break;
				}
			}
		}
		
		socket.close();
		
		System.out.println("Exiting");
	}
	
	private static class MessageReceiveThread extends Thread {
		
		private DataInputStream inputStream;
		
		public MessageReceiveThread(DataInputStream inputStream) {
			super();
			this.inputStream = inputStream;
		}
		
		@Override
		public void run() {
			try {
				while (true) {
					inputStream.read(); //Read byte which says this is a message
					String username = inputStream.readUTF();
					String content = inputStream.readUTF();
					long timestamp = inputStream.readLong();
					
					System.out.println("Received " + content + " from " + username + " at " + timestamp);
				}
			} catch (IOException ex) {
				System.out.println("SERVER DISCONNECTED");
				System.exit(-1);
			}
		}
	}
}
