import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket

//-------SHARED BETWEEN SERVER AND CLIENT APPLICATIONS----------

const val PORT_NUMBER = 4444

const val CLIENT_SIGN_UP = 0x0
const val CLIENT_LOGIN = 0x1

const val SERVER_SIGN_UP_SUCCESS = 0x0
const val SERVER_SIGN_UP_FAILED = 0x1
const val SERVER_USER_ALREADY_EXISTS = 0x2

const val SERVER_LOGIN_SUCCESS = 0x0
const val SERVER_LOGIN_FAILED = 0x1
const val SERVER_USERNAME_INVALID = 0x1
const val SERVER_PASSWORD_INVALID = 0x2

//---------------------------------------------------------------

fun main(args: Array<String>) {
	val users = mutableMapOf<String, User>()
	
	val serverSocket = ServerSocket(PORT_NUMBER)
	
	println("Server started on PORT_NUMBER $PORT_NUMBER")
	
	while (true) {
		val clientSocket = serverSocket.accept()
		
		println("Client connected from ${clientSocket.remoteSocketAddress}")
		
		Thread {
			val inputStream = clientSocket.getInputStream()
			val outputStream = clientSocket.getOutputStream()
			
			val inputReader = BufferedReader(InputStreamReader(inputStream))
			
			while (true) {
				val receivedByte = inputStream.read()
				if (receivedByte == -1) break
				
				when (receivedByte) {
					CLIENT_SIGN_UP -> {
						val username = inputReader.readLine()
						val password = inputReader.readLine()
						
						var responseByte = -1
						var debugMessage = "WTF happened there"
						
						synchronized(users) {
							if (users.containsKey(username)) {
								responseByte = SERVER_SIGN_UP_FAILED or SERVER_USER_ALREADY_EXISTS
								debugMessage = "Failed sign up: $username from ${clientSocket.remoteSocketAddress}"
							} else {
								users[username] = User(password)
								
								responseByte = SERVER_SIGN_UP_SUCCESS
								debugMessage = "Successful sign up: $username from ${clientSocket.remoteSocketAddress}"
							}
						}
						
						outputStream.write(responseByte)
						println(debugMessage)
					}
					
					CLIENT_LOGIN -> {
						val username = inputReader.readLine()
						val password = inputReader.readLine()
						
						var responseByte = -1
						var debugMessage = "WTF happened there"
						
						synchronized(users) {
							if (users.containsKey(username)) {
								if (users[username]!!.checkPassword(password)) {
									responseByte = SERVER_LOGIN_SUCCESS
									debugMessage = "Successful login: $username from ${clientSocket.remoteSocketAddress}"
								} else {
									responseByte = SERVER_LOGIN_FAILED or SERVER_PASSWORD_INVALID
									debugMessage = "Failed login (invalid password): $username from ${clientSocket.remoteSocketAddress}"
								}
							} else {
								responseByte = SERVER_LOGIN_FAILED or SERVER_USERNAME_INVALID
								debugMessage = "Failed login (invalid username): $username from ${clientSocket.remoteSocketAddress}"
							}
						}
						
						outputStream.write(responseByte)
						println(debugMessage)
					}
				}
			}
			
			clientSocket.close()
			
			println("Exiting client thread")
		}.start()
	}
}

class User(private val password: String) {
	
	fun checkPassword(password: String): Boolean {
		return this.password == password
	}
}