import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

public class CMD_ChatServer extends Thread {
	
	private HashMap<String, DataOutputStream> clients;
	
	static Socket socket;
	private static ServerSocket serverSocket;
	
	public CMD_ChatServer() {
		clients = new HashMap<String, DataOutputStream>();
		Collections.synchronizedMap(clients);
	}

	@Override
	public void run() {
		try {
			serverSocket = new ServerSocket(7778);			
			
			CMD_Server.jta.append(CMD_Server.getTime() + " Chat server started.\n");
			
			while(true) {
				CMD_ChatServer.socket = serverSocket.accept();
				ServerReceiver receiver = new ServerReceiver(CMD_ChatServer.socket);
				receiver.start();
			}
		} catch (IOException e) {
			CMD_Server.jta.append(CMD_Server.getTime() +
					" Error: Chatting socket create error!\n" +
					"Check if 7778 port in use.");
		}
	}
	
	class ServerReceiver extends Thread {
		Socket socket;
		DataInputStream input;
		DataOutputStream output;
		
		public ServerReceiver(Socket socket) {
			this.socket = socket;
			try {
				input = new DataInputStream(socket.getInputStream());
				output = new DataOutputStream(socket.getOutputStream());
			} catch (IOException e) {
			}
		}
		
		@Override
		public void run() {
			String name = "";
			String message = "";
			
			try {
				name = input.readUTF();
				sendToAll("* " + name + "[" + socket.getInetAddress() + ":" +
							socket.getPort() + "]" + "connected to chatServer.\n");
				clients.put(name, output);
				CMD_Server.jta.append(CMD_Server.getTime() + " " + name + "(" +
								socket.getInetAddress() + ":" +
								socket.getPort() + ")" + " connected to chatServer.\n");
				CMD_Server.jta.append(CMD_Server.getTime() + " " +
								clients.size() + " user connected to chatServer.\n");
				CMD_Server.jsb.setValue(CMD_Server.jsb.getMaximum());
				
				while (input != null) {
					message = input.readUTF();
					
					sendToAll(message);
				}
			} catch (IOException e) {
			} finally {
				clients.remove(name);
				sendToAll("* " + name + "(" + socket.getInetAddress() + ":" +
                        socket.getPort() + ")" + " disconnected from chatServer.");
				CMD_Server.jta.append(CMD_Server.getTime() +  " " + name + "(" + socket.getInetAddress() + ":" +
                        socket.getPort() + ")" + " disconnected from chatServer.\n");
				CMD_Server.jta.append(CMD_Server.getTime() +  " " +
                        				clients.size() + " user connected to chatServer.\n");
				CMD_Server.jsb.setValue(CMD_Server.jsb.getMaximum());
			}
		}
		
		public void sendToAll(String message) {
            Iterator<String> it = clients.keySet().iterator();

            while (it.hasNext()) {
                try {
                    DataOutputStream dos = clients.get(it.next());
                    dos.writeUTF(message);
                } catch (Exception e) {
                }
            }
        }
	}
}
