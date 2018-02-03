import java.io.*
import java.net.Socket

//Constants are declared in MessengerServer.kt

fun main(args: Array<String>) {
	println("Connecting to server")
	
	print("Enter server address: ")
	val serverAddress = readLine()
	
	val socket = Socket(serverAddress, PORT_NUMBER)
	println("Connected to server")
	
	val inputStream = DataInputStream(socket.getInputStream())
	val outputStream = DataOutputStream(socket.getOutputStream())
	
	mainloop@ while (true) {
		print("Enter S to sign up, L to log in or E to exit: ")
		val line = readLine()
		if (line.equals("E", true)) break
		
		val signingUp = line.equals("S", true)
		
		print("Enter username: ")
		val username = readLine()
		
		print("Enter password: ")
		val password = readLine()
		
		if (signingUp) {
			outputStream.write(CLIENT_SIGN_UP)
			outputStream.writeUTF(username)
			outputStream.writeUTF(password)
			outputStream.flush()
			
			val receivedByte = inputStream.read()
			
			when(receivedByte) {
				SERVER_SIGN_UP_RESPONSE or SERVER_ACTION_SUCCESSFUL -> {
					println("Sign up successful")
				}
				
				SERVER_SIGN_UP_RESPONSE or SERVER_ACTION_FAILED or SERVER_USERNAME_UNAVAILABLE -> {
					println("Sign up failed: user already exists")
				}
			}
		} else {
			outputStream.write(CLIENT_LOGIN)
			outputStream.writeUTF(username)
			outputStream.writeUTF(password)
			outputStream.flush()
			
			val receivedByte = inputStream.read()
			
			when(receivedByte) {
				SERVER_LOGIN_RESPONSE or SERVER_ACTION_SUCCESSFUL -> {
					println("Login successful")
					
					MessageReceiveThread(inputStream).start()
					
					while (true) {
						print("Enter username to send message to (or E to exit): ")
						val username = readLine()
						
						if (username.equals("E", true)) {
							break@mainloop
						}
						
						print("Enter message: ")
						val content = readLine()
						
						outputStream.writeUTF(username)
						outputStream.writeUTF(content)
					}
					
				}
				
				SERVER_LOGIN_RESPONSE or SERVER_ACTION_FAILED -> {
					println("Login failed: username or password invalid")
				}
			}
		}
	}
	
	socket.close()
	
	println("Exiting")
}

private class MessageReceiveThread(private val inputStream: DataInputStream): Thread() {
	override fun run() {
		try {
			while (true) {
				inputStream.read() //Read byte which says this is a message
				val username = inputStream.readUTF()
				val content = inputStream.readUTF()
				val timestamp = inputStream.readLong()
				
				println("Received $content from $username at $timestamp")
			}
		} catch (e: IOException) {
			println("SERVER DISCONNECTED")
			System.exit(-1)
		}
	}
}