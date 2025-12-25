package Clients;
import UI.ClientTwoApp;

import java.net.*;
import java.io.*;
import java.util.*;

class C2SendMessage implements Runnable{

    ArrayList<String> text = ClientTwoApp.getText();
    Socket socket = null;
    C2SendMessage(Socket socket){
        this.socket = socket;
    }
    @Override
    public void run(){
        int oldSize = 0;
        while(true){
            try {
                Thread.sleep(200);
                DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
                while(oldSize < text.size()){
                    for(int i=oldSize; i<text.size(); ++i){
                        String message = text.get(i);               
                        dout.writeUTF("111111" + message);   
                    } 
                    oldSize=text.size();   
                }  
            } catch (Exception e) {
                // TODO: handle exception
            }

        }
        
    }
}

class C2ReceiveMessage implements Runnable{
    Socket socket = null;
    ClientTwo clientTwo = null;

    C2ReceiveMessage(Socket socket ,ClientTwo clientTwo ){
        this.socket = socket;
        this.clientTwo = clientTwo;
    }

    @Override 
    public void run(){
        while (true) {
            try {
                Thread.sleep(200);
                DataInputStream din = new DataInputStream(socket.getInputStream());
                String incomingText = din.readUTF();
                System.out.println(incomingText);
                clientTwo.receivedMessages.add(incomingText);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }
}

public class ClientTwo {
    ArrayList<String> receivedMessages = new ArrayList<>();

    public ArrayList<String> getMessage(){
        return receivedMessages;
    }
    public void start() throws Exception{
        Socket socket = new Socket("localhost" , 5555);

        DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
        String id = "222222";
        dout.writeUTF(id);

        C2SendMessage sendMessage = new C2SendMessage(socket);
        C2ReceiveMessage receiveMessage = new C2ReceiveMessage(socket,this);
        
        Thread sendMessageThread = new Thread(sendMessage);
        Thread receiveMessageThread = new Thread(receiveMessage);

        sendMessageThread.start();
        receiveMessageThread.start();
    }
}
