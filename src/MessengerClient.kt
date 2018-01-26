import java.io.PrintWriter
import java.net.Socket

//Constants are declared in MessengerServer.kt

fun main(args: Array<String>) {
	println("Connecting to server")
	val socket = Socket("localhost", PORT_NUMBER)
	println("Connected to server")
	
	val inputStream = socket.getInputStream()
	val outputStream = socket.getOutputStream()
	
	val outputWriter = PrintWriter(outputStream, true)
	
	while (true) {
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
			outputWriter.println(username)
			outputWriter.println(password)
			
			val receivedByte = inputStream.read()
			
			when(receivedByte) {
				SERVER_SIGN_UP_SUCCESS -> {
					println("Sign up successful")
				}
				
				SERVER_SIGN_UP_FAILED or SERVER_USER_ALREADY_EXISTS -> {
					println("Sign up failed: user already exists")
				}
			}
		} else {
			outputStream.write(CLIENT_LOGIN)
			outputWriter.println(username)
			outputWriter.println(password)
			
			val receivedByte = inputStream.read()
			
			when(receivedByte) {
				SERVER_LOGIN_SUCCESS -> {
					println("Login successful")
				}
				
				SERVER_LOGIN_FAILED or SERVER_USERNAME_INVALID -> {
					println("Login failed: username invalid")
				}
				
				SERVER_LOGIN_FAILED or SERVER_PASSWORD_INVALID -> {
					println("Login failed: password invalid")
				}
			}
		}
	}
	
	socket.close()
	
	println("Exiting")
}