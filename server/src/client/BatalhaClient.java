package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class BatalhaClient {

    public static void main(String[] args) {

        String host = "localhost";
        int port = 5000;

        try {

            Socket socket = new Socket(host, port);

            BufferedReader input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            PrintWriter output = new PrintWriter(
                    socket.getOutputStream(), true);

            BufferedReader keyboard = new BufferedReader(
                    new InputStreamReader(System.in));

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

            String userInput;

            while ((userInput = keyboard.readLine()) != null) {

                output.println(userInput);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}