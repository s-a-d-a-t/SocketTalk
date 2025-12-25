

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MainServer {
    private static final Set<ClientKeyPair> clients = ConcurrentHashMap.newKeySet();
    private static class ClientKeyPair {
        private String key; // basically the client ID
        private ClientHandler client; 

        public ClientKeyPair(String key, ClientHandler client) {
            this.key = key;
            this.client = client;
        }

        public String getKey() {
            return key;
        }

        public ClientHandler getClient() {
            return client;
        }

    }
    private static class ClientHandler implements Runnable {

        private final Socket socket;
        private DataInputStream in;
        private DataOutputStream out;

        ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String message = in.readUTF();
                    System.out.println("Received: " + message);

                    MainServer.broadcast(message, this);
                }
            } catch (IOException e) {
            } finally {
                close();
            }
        }

        void send(String message) {
            try {
                out.writeUTF(message.substring(6));
                out.flush();
            } catch (IOException e) {
                close();
            }
        }

        private void close() {
            try {
                MainServer.remove(this);
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    public static void main(String[] args) {

        try (ServerSocket serverSocket = new ServerSocket(5555)) {

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected: " + socket);

                DataInputStream din = new DataInputStream(socket.getInputStream());
                String clientId = din.readUTF();
                
                ClientHandler handler = new ClientHandler(socket);
                clients.add(new ClientKeyPair(clientId, handler));

                for(ClientKeyPair pair: clients){
                    System.out.println( pair.getClient() + " " +pair.getKey());
                }

                new Thread(handler).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
                //sendTo
    static void broadcast(String message, ClientHandler sender) {
            
            for (ClientKeyPair pair : clients) {
                System.out.println(pair.getClient() + " ... " + pair.getKey() + " Sender ..." + sender);
                if(pair.getClient() != sender){
                    boolean foundKey = true;
                    String id = pair.getKey();
                    for(int i = 0; i<6; ++i){
                        if(id.charAt(i) != message.charAt(i)){
                            System.out.println(id.charAt(i) + "  " + message.charAt(i));
                            foundKey = false;
                            break;
                        }
                    }

                    if(foundKey){
                        System.out.println("Sending message");
                        pair.getClient().send(message);
                    }
                }
            
        }


    }

    /*  if (client != sender) {
                client.send(message);
            } */

    static void remove(ClientHandler handler) {
        //clients.remove(handler);
        System.out.println("Client disconnected");
    }

   
}
