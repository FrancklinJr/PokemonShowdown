package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class BatalhaClient {
	
	public static void main(String[] args) {
		
		String serverAddress = "localhost";
		int port = 5000;
		
		try (Socket socket = new Socket(serverAddress, port)) {
			PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader input = new BufferedReader( new InputStreamReader(socket.getInputStream()));
			
			Scanner scanner = new Scanner(System.in);
			
			new Thread(() -> {
				try {
					String response;
					while ((response = input.readLine()) != null) {
						System.out.println(response);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}).start();
			
			while (true) {
				String command = scanner.nextLine();
				output.println(command);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
