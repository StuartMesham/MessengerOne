import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.LinkedBlockingQueue

//-------SHARED BETWEEN SERVER AND CLIENT APPLICATIONS----------

const val PORT_NUMBER = 4444

const val CLIENT_SIGN_UP = 0x0
const val CLIENT_LOGIN = 0x1

//Bits 1 and 2
const val SERVER_SIGN_UP_RESPONSE = 0x0       //0000
const val SERVER_LOGIN_RESPONSE = 0x1         //0001
const val SERVER_MESSAGE_DELIVERY = 0x2       //0010

//Bit 3
const val SERVER_ACTION_SUCCESSFUL = 0x4      //0100
const val SERVER_ACTION_FAILED = 0x0          //0000

//Bit 4
const val SERVER_USERNAME_UNAVAILABLE = 0x8   //1000
const val SERVER_USERNAME_AVAILABLE = 0x0     //0000

//---------------------------------------------------------------

fun main(args: Array<String>) {
	val users = mutableMapOf<String, User>()
	
	val serverSocket = ServerSocket(PORT_NUMBER)
	
	println("Server started on PORT_NUMBER $PORT_NUMBER")
	
	while (true) {
		val clientSocket = serverSocket.accept()
		
		println("Client connected from ${clientSocket.remoteSocketAddress}")
		
		Thread {
			val inputStream = DataInputStream(clientSocket.getInputStream())
			val outputStream = DataOutputStream(clientSocket.getOutputStream())
			
			var handoffComplete = false
			while (!handoffComplete) {
				val receivedByte = inputStream.read()
				if (receivedByte == -1) break
				
				when (receivedByte) {
					CLIENT_SIGN_UP -> {
						val username = inputStream.readUTF()
						val password = inputStream.readUTF()
						
						var responseByte = -1
						var debugMessage = "What happened there"
						
						synchronized(users) {
							if (users.containsKey(username)) {
								responseByte = SERVER_SIGN_UP_RESPONSE or SERVER_ACTION_FAILED or SERVER_USERNAME_UNAVAILABLE
								debugMessage = "Failed sign up: $username from ${clientSocket.remoteSocketAddress}"
							} else {
								users[username] = User(username, password)
								
								responseByte = SERVER_SIGN_UP_RESPONSE or SERVER_ACTION_SUCCESSFUL
								debugMessage = "Successful sign up: $username from ${clientSocket.remoteSocketAddress}"
							}
						}
						
						outputStream.write(responseByte)
						println(debugMessage)
					}
					
					CLIENT_LOGIN -> {
						val username = inputStream.readUTF()
						val password = inputStream.readUTF()
						
						var responseByte = -1
						var debugMessage = "What happened there"
						
						synchronized(users) {
							if (users.containsKey(username)) {
								val user = users[username]!!
								
								if (user.checkPassword(password)) {
									user.signIn(ServerMessageSendThread(user, outputStream), ServerMessageReceiveThread(users, user, inputStream), clientSocket)
									
									responseByte = SERVER_LOGIN_RESPONSE or SERVER_ACTION_SUCCESSFUL
									debugMessage = "Successful login: $username from ${clientSocket.remoteSocketAddress}"
									
									handoffComplete = true
								} else {
									responseByte = SERVER_LOGIN_RESPONSE or SERVER_ACTION_FAILED
									debugMessage = "Failed login (invalid password): $username from ${clientSocket.remoteSocketAddress}"
								}
							} else {
								responseByte = SERVER_LOGIN_RESPONSE or SERVER_ACTION_FAILED
								debugMessage = "Failed login (invalid username): $username from ${clientSocket.remoteSocketAddress}"
							}
						}
						
						outputStream.write(responseByte)
						println(debugMessage)
					}
				}
			}
			
			//clientSocket.close()
			
			println("Exiting client handoff thread")
		}.start()
	}
}

class ServerMessageReceiveThread(private val users: MutableMap<String, User>, private val user: User, private val inputStream: DataInputStream): Thread() {
	override fun run() {
		try {
			while (!isInterrupted) {
				val receiverUsername = inputStream.readUTF()
				val content = inputStream.readUTF()
				val timestamp = System.currentTimeMillis()
				
				println("Received $content for $receiverUsername at $timestamp")
				
				val message = Message(user, content, timestamp)
				
				users[receiverUsername]?.send(message)
			}
		} catch (e: IOException) {
			if (!isInterrupted) {
				user.disconnect()
			}
		} catch (e: InterruptedException) {
			println("Server receive thread interrupted")
		} finally {
			println("Exiting server receive thread")
		}
	}
}

class ServerMessageSendThread(private val user: User, private val outputStream: DataOutputStream): Thread() {
	override fun run() {
		try {
			while (!isInterrupted) {
				val message = user.getMessage()
				outputStream.write(SERVER_MESSAGE_DELIVERY)
				outputStream.writeUTF(message.sender.username)
				outputStream.writeUTF(message.content)
				outputStream.writeLong(message.timestamp)
			}
		} catch (e: IOException) {
			if (!isInterrupted) {
				user.disconnect()
			}
		} catch (e: InterruptedException) {
			println("Server message send thread interrupted")
		} finally {
			println("Exiting server message send thread")
		}
	}
}

class User(val username: String, private val password: String) {
	
	private val addLock = Object()
	private val removeLock = Object()
	
	private val pendingMessages = LinkedBlockingQueue<Message>()
	
	private val threadsLock = Object()
	
	private var sendThread: ServerMessageSendThread? = null
	private var receiveThread: ServerMessageReceiveThread? = null
	private var socket: Socket? = null
	
	/**
	 * Used to "send" a message to this user. The message is put in a queue and the user's client will download it when ready
	 */
	fun send(message: Message) {
		synchronized(addLock) {
			pendingMessages.put(message)
		}
	}
	
	/**
	 * Removes the message from the front of the queue (of messages pending for this user) and returns it.
	 */
	fun getMessage(): Message {
		synchronized(removeLock) {
			return pendingMessages.take()
		}
	}
	
	fun signIn(sendThread: ServerMessageSendThread, receiveThread: ServerMessageReceiveThread, socket: Socket) {
		synchronized(threadsLock) {
			disconnect()
			
			this.sendThread = sendThread
			this.receiveThread = receiveThread
			this.socket = socket
			
			sendThread.start()
			receiveThread.start()
		}
	}
	
	fun disconnect() {
		synchronized(threadsLock) {
			sendThread?.interrupt()
			receiveThread?.interrupt()
			socket?.close()
		}
	}
	
	fun checkPassword(password: String): Boolean {
		return this.password == password
	}
}

class Message (val sender: User, val content: String, val timestamp: Long)