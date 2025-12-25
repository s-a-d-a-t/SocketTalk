package UI;

import Clients.ClientTwo;
import UI.ComponentHandlers.ClientTwoHandler.*;

import java.util.*;
import javafx.application.*;
import javafx.beans.property.*;
import javafx.geometry.*;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

 class C2ReceiveMessageLabel implements Runnable{
        ClientTwo client = null;
        GridPane gridPane = null;
        
        C2ReceiveMessageLabel(GridPane gridPane, ClientTwo client){
            this.gridPane = gridPane;
            this.client = client;
        }
        @Override
        public void run(){
            int oldSize =0;
            while(true){
                try {
                    while(oldSize < client.getMessage().size()){
                        Label label = new Label();
                        label.setWrapText(true);
                        for(int i =oldSize; i< client.getMessage().size(); ++i){
                            int index = i;
                            label.setText(client.getMessage().get(index));
                            Platform.runLater(() ->{
                                ClientTwoApp.addToGrid(gridPane, label);                                
                            });
                        }
                    oldSize = client.getMessage().size();      
                    }
                }catch (Exception e) {
                    // TODO: handle exception
                }
                Thread.yield();
            }
        }
            
    }


public class ClientTwoApp extends Application{
    
    static ArrayList<String> textStorage = new ArrayList<>();
    TextField messageBox = new TextField();
    Button messageButton = new Button("send");
    GridPane gridPane = new GridPane();
   
    public static ArrayList<String> getText(){
        return textStorage;
    }

    public static void addToGridSelf(GridPane gridPane, Label label){
            HBox hbox = null;
            for(Node child: gridPane.getChildren()){
                hbox = new HBox();
                if(GridPane.getRowIndex(child) == null){
                    continue;
                }
                else{
                    int i = GridPane.getRowIndex(child);
                    GridPane.setRowIndex(child, i-1);
                }
                
            }

            int row = 0;
            hbox.getChildren().add(label);
            hbox.setMaxWidth(200);
            hbox.setPadding(new Insets(8));
            hbox.setStyle("-fx-border-color: brown");

            hbox.setBackground( new Background(new BackgroundFill(Color.BEIGE, CornerRadii.EMPTY, Insets.EMPTY)));
            gridPane.add(hbox, 1, getBottomGrid(row, gridPane)+1);
            GridPane.setHalignment(hbox, HPos.RIGHT);
        }
   
    public static void addToGrid(GridPane gridPane, Label label){
            HBox hbox = null;
            for(Node child: gridPane.getChildren()){
                hbox = new HBox();
                if(GridPane.getRowIndex(child) == null){
                    continue;
                }
                else{
                    int i = GridPane.getRowIndex(child);
                    GridPane.setRowIndex(child, i-1);
                }
                
            }

            int row = 0;
            hbox.getChildren().add(label);
            hbox.setMaxWidth(200);
            hbox.setPadding(new Insets(8));
            hbox.setStyle("-fx-border-color: brown");
            hbox.setBackground( new Background(new BackgroundFill(Color.BEIGE, CornerRadii.EMPTY, Insets.EMPTY)));
            gridPane.add(hbox, 1, getBottomGrid(row, gridPane)+1);
            
        }

    public static int getBottomGrid(int row, GridPane gridPane){
        
        for(Node child: gridPane.getChildren()){
            if (GridPane.getRowIndex(child) == null){
                continue;
            }
            else{
                row = GridPane.getRowIndex(child);
            }
        }
        return row;
    }


    @Override
    public void start(Stage primaryStage) throws Exception{
        
        ClientTwo clientTwo = new ClientTwo();
        clientTwo.start();
        
        HBox text = new HBox(5);
        text.getChildren().addAll(messageBox,messageButton);
        text.setAlignment(Pos.CENTER_RIGHT);

        BorderPane borderPane = new BorderPane();
        borderPane.setBottom(text);
        Label init = new Label();
        gridPane.add(init, 1, 10000);
  
        GridPane.setHgrow(init, Priority.ALWAYS);
       /*GridPane.setHalignment(init, HPos.RIGHT); */
        gridPane.setGridLinesVisible(false);

        ScrollPane scrollPane = new ScrollPane(gridPane);
        //scrollPane.setContent(pane);
        //scrollPane.setPannable(true);
        //scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
        //scrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        scrollPane.setFitToWidth(true);

        scrollPane.vvalueProperty().bind(gridPane.heightProperty());

        borderPane.setCenter(scrollPane);
    
        ButtonHandler buttonHandler = new ButtonHandler(messageButton, messageBox, gridPane);
        buttonHandler.buttonHandler();

        TextFieldHandler textFieldHandler = new TextFieldHandler(messageButton, messageBox, gridPane);
        textFieldHandler.textFieldHandler();

        C2ReceiveMessageLabel c2ReceiveMessageLabel = new C2ReceiveMessageLabel(gridPane, clientTwo);
        Thread C2MessageReceiveThread = new Thread(c2ReceiveMessageLabel);
        C2MessageReceiveThread.start();
        
        Scene scene = new Scene(borderPane, 500, 400);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Client Two Messaging App");
        primaryStage.show();
        
    }

    public static void main(String[] args){
        launch(args);
    }
}
