package Clients;
import UI.ClientOneApp;

import java.net.*;
import java.io.*;
import java.util.*;


class C1SendMessage implements Runnable{
    
    ArrayList<String> ClientOneText = ClientOneApp.getText();
    Socket socket = null;
    C1SendMessage(Socket socket){
        this.socket = socket;
    }
    @Override
    public void run(){
        int oldSize = 0;
        while(true){
            try {
                Thread.sleep(200);
                DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
                while(oldSize < ClientOneText.size()){
                    for(int i = oldSize; i < ClientOneText.size() ; ++i){
                        String message = ClientOneText.get(i);
                        dout.writeUTF( "333333" + message);
                    }
                    oldSize = ClientOneText.size();
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }
}

class C1ReceiveMessage implements Runnable{
    Socket socket = null;
    ClientOne clientOne = null;

    C1ReceiveMessage( Socket socket, ClientOne clientOne){
        this.socket = socket;
        this.clientOne = clientOne;
       
    }

    @Override 
    public void run(){
        while (true) {
            
            try {
                Thread.sleep(200);
                DataInputStream din = new DataInputStream(socket.getInputStream());
                String incomingText = din.readUTF();
                clientOne.getMessage().add(incomingText);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }
}

public class ClientOne {
    ArrayList<String> receivedMessages = new ArrayList<>();

    public ArrayList<String> getMessage(){
        return receivedMessages;
    }
    
    public void start() throws Exception{
        Socket socket = new Socket("localhost" , 5555);
        
        DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
        String id = "111111";
        dout.writeUTF(id);

        C1SendMessage sendMessage = new C1SendMessage(socket);
        C1ReceiveMessage receiveMessage = new C1ReceiveMessage(socket, this);

        Thread sendMessageThread = new Thread(sendMessage);
        Thread receiveMessageThread = new Thread(receiveMessage);

        sendMessageThread.start();
        receiveMessageThread.start();

    }
    
}