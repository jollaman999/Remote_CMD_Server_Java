import java.awt.AWTException;
import java.awt.Color;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class CMD_Server extends Thread implements MouseListener {
	
	static final String REMOTECMD_SERVER_TITLE = "RemoteCMD_Server"; 
	static String SERVER_PASSWAORD = "jmpasswd";
	
	static int user_counter = 0;
	
	static JFrame jframe = new JFrame();
	static JTextArea jta = new JTextArea("");
	static JScrollPane jsp = new JScrollPane(jta,
							JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
							JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	static JScrollBar jsb = jsp.getVerticalScrollBar();
	
	TrayIcon trayicon;
	SystemTray systemtray;
	
	static private Socket socket;
	static private ServerSocket serverSocket;
	
	public CMD_Server() {
		
		jta.setBackground(Color.WHITE);
		jta.setEditable(false);
		
		jframe.setTitle(REMOTECMD_SERVER_TITLE);
		jframe.setSize(1080, 768);
		jframe.add(jsp);
		jframe.setVisible(false);
		
		jframe.addWindowListener(new WindowListener() {
			
			@Override
			public void windowOpened(WindowEvent e) {
			}
			
			@Override
			public void windowIconified(WindowEvent e) {				
			}
			
			@Override
			public void windowDeiconified(WindowEvent e) {
			}
			
			@Override
			public void windowDeactivated(WindowEvent e) {
			}
			
			@Override
			public void windowClosing(WindowEvent e) {
				jframe.setVisible(false);
			}
			
			@Override
			public void windowClosed(WindowEvent e) {
			}
			
			@Override
			public void windowActivated(WindowEvent e) {
			}
		});
		
		if (SystemTray.isSupported()) {
			
			systemtray = SystemTray.getSystemTray();
			Image image = Toolkit.getDefaultToolkit().getImage("tray.png");
			
			PopupMenu menu = new PopupMenu();
			MenuItem item_open = new MenuItem("Open Window");
			MenuItem item_exit = new MenuItem("Exit");
			
			menu.add(item_open);
			menu.add(item_exit);
			
			item_open.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					jframe.setVisible(true);
				}
			});
			
			item_exit.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					if (CMD_Server.socket != null) {
						try {
							CMD_Server.socket.close();
						} catch (IOException e1) {
						}
					}
					
					if (CMD_ChatServer.socket != null) {
						try {
							CMD_ChatServer.socket.close();
						} catch (IOException e1) {
						}
					}
					
					System.exit(0);
				}
			});
			
			trayicon = new TrayIcon(image, "CMD Server", menu);
			
			trayicon.setImageAutoSize(true);
			trayicon.addMouseListener(this);
			
			try {
				systemtray.add(trayicon);
			} catch (AWTException e1) {
				jta.append(getTime() + " Error: Failed to add tray icon.\n");
			}
		} else {
			jta.append(getTime() + " Warning: System tray is currently not supporeted.\n");
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1)
			jframe.setVisible(true);
	}
	
	@Override
	public void mouseExited(MouseEvent e) {
	}
	
	@Override
	public void mouseEntered(MouseEvent e) {
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
	}
	
	@Override
	public void run() {
		try {
			serverSocket = new ServerSocket(7777);
			
			jta.append(getTime() + " Server started.\n");
			
			new CMD_ChatServer().start();
			
			while(true) {
				CMD_Server.socket = serverSocket.accept();
				ServerReceiver receiver = new ServerReceiver(CMD_Server.socket);
				receiver.start();
			}
		} catch (IOException e) {
			jta.append(getTime() + " Error: Socket create error!\n"
					+ "Check if 7777 port in use or close running server "
					+ "then restart server.");
		}
	}
	
    static String getTime() {
        SimpleDateFormat f = new SimpleDateFormat("[hh:mm:ss]");
        return f.format(new Date());
    }
	
	class ServerReceiver extends Thread {
		
		Socket socket;
		DataInputStream data_in;
		DataOutputStream data_out;
		String input;
		String output;
		
		public ServerReceiver (Socket socket) {
			this.socket = socket;
			
			try {
				data_in = new DataInputStream(socket.getInputStream());
				data_out = new DataOutputStream(socket.getOutputStream());
			} catch (IOException e) {
			}
		}
		
		public String cmdExec(String cmdLine) {
		    String line;
		    String output = "";
		    try {
			        Process p = Runtime.getRuntime().exec(cmdLine);
			        BufferedReader input = new BufferedReader(
			        				new InputStreamReader(p.getInputStream()));
			        
			        while ((line = input.readLine()) != null)
			            output += (line + '\n');
			        
			        input.close();
		        }
		    catch (Exception ex) {
		    	output = " Error: Error while executing command!\n";

		    	jta.append(getTime() + output);
		    	jsb.setValue(jsb.getMaximum());
		    }

		    return output;
		}
		
		@Override
		public void run() {
			boolean is_password_ok = false;
			
			try {
				for (int i = 2; i >= 0 ; i--) {
					input = data_in.readUTF();
					if (!input.equals(SERVER_PASSWAORD)) {
						jta.append(getTime() + " Warning: There was a incorrent password from a client!\n");
						jta.append(getTime() + " Client tried password: " + input
												+ ", Tries left: " + i + "\n");
						jsb.setValue(jsb.getMaximum());
						
						if (i == 0) {
							jta.append(getTime() + "Close client socket!\n");
							jsb.setValue(jsb.getMaximum());
							output = "DISCONNECTED";
							data_out.writeUTF(output);
							
							socket.close();
							return;
						}
						
						output = "Password incorrect!!\n"
								+ "Will be disconnect from server after "
								+ i + " more tries...";
						data_out.writeUTF(output);
					} else {
						is_password_ok = true;
						break;
					}
				}
				
				if (is_password_ok) {
					jta.append(getTime() + " New user connected.\n");
					jta.append(getTime() + " Connected users: " + ++user_counter + "\n");
					jsb.setValue(jsb.getMaximum());
					
					data_out.writeUTF("CONNECTED");
				}
				
				while (is_password_ok && data_in != null) {
					input = data_in.readUTF();
					
					output = cmdExec(input);
					jta.append(getTime() + "Command input from client: " + input + "\n");
					
					data_out.writeUTF(output);
					if (!output.equals("")) {
						jta.append(getTime() + "\n" + output);
					}
					
					jsb.setValue(jsb.getMaximum());
				}
			} catch (IOException e1) {
			} finally {
				if (is_password_ok) {
					jta.append(getTime() + " Disconnected by user.\n");
					jta.append(getTime() + " Connected users: " + --user_counter + "\n");
					jsb.setValue(jsb.getMaximum());
				}
			}
		}
	}
	
	public static void main(String[] args) {
		new CMD_Server().start();
	}
}