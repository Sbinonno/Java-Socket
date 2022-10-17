package servers;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Semaphore;

public class SrvrSocket1 {
	public static boolean serverOn = true;

	public static void main(String[] args) throws IOException, InterruptedException {
		ServerSocket ws = null; // Welcoming Socket
		int port = 7979; // Connection Port
		Semaphore mutex = new Semaphore(1); // Semaphore
		ArrayList<Socket> list = new ArrayList<>();
		try {
			ws = new ServerSocket(port); // Initializing Welcoming Socket
		} catch (IOException e) {
			System.out.println("La porta " + port + " è già utilizzata!");
			e.printStackTrace();
			System.exit(0);
		}
		System.out.println("Server TCP is running...");

		while (serverOn) {
			Socket s = null;
			try {
				s = ws.accept();
				if (!serverOn) {
					s.close();
					break;
				}
				mutex.acquire();
				list.add(s);
				mutex.release();
				new Thread(new ClientHandler(s, mutex, list)).start();
			} catch (Exception e) {
				System.out.println("Eccezione durante la Accept!");
				e.printStackTrace();
				continue;
			}
		}
	}

	public static void shutdown(ArrayList<Socket> list, Semaphore m) throws IOException, InterruptedException {
		m.acquire();
		for (Socket s : list) {
			PrintWriter out = new PrintWriter(s.getOutputStream(), true);
			out.println("You Have been kicked out!");
			out.println("Server shutting down...");
			s.close();

// list.remove(s);

		}
		m.release();
	}
}

class ClientHandler extends Thread {
	final Socket s;
	private Semaphore m;
	private ArrayList<Socket> list;
	private String msg = "";

	public ClientHandler(Socket s, Semaphore m, ArrayList<Socket> list) {
		this.s = s;
		this.m = m;
		this.list = list;
	}

	public void run() {
		BufferedReader in = null;
		PrintWriter out = null;
		boolean StayConnected = true;
		try {
			in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			out = new PrintWriter(s.getOutputStream(), true);
			System.out.println("Client " + s.getInetAddress() + ":" + s.getPort() + " connected");
			out.println("Welcome to Tommaso's Server...");
			while (StayConnected) {
				try {
					msg = in.readLine();
				} catch (IOException e) {

				}
				switch (msg.toLowerCase()) {
				case "shutdown":
					out.println("Shutting down...");
					StayConnected = false;
					SrvrSocket1.shutdown(list, m);
					SrvrSocket1.serverOn = false;
					break;
				case "date":
					out.println(new Date().toString());
					break;
				default:
					out.println(msg.toUpperCase());
					break;
				}

			}
			out.close();
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			m.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		list.remove(s);
		m.release();
	}
}