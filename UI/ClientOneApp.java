    package UI;
    import Clients.ClientOne;
    import UI.ComponentHandlers.ClientOneHandler.*;

    import java.util.*;

    import javafx.application.*;
    import javafx.geometry.*;
    import javafx.stage.Stage;
    import javafx.scene.Scene;
    import javafx.scene.Node;
    import javafx.scene.control.*;
    import javafx.scene.control.ScrollPane.ScrollBarPolicy;
    import javafx.scene.layout.*;
    import javafx.scene.paint.Color;
    
    

    class C1ReceiveMessageLabel implements Runnable{
        ClientOne client = null;
        Label messageLabel = null;
        GridPane gridPane = null;        
        
        C1ReceiveMessageLabel(GridPane gridPane, ClientOne client){
            this.gridPane = gridPane;
            this.client = client;
        }
        @Override
        public void run(){
            int oldSize =0;
            while(true){
                try {
                    while(oldSize < client.getMessage().size()){
                        for(int i =oldSize; i< client.getMessage().size(); ++i){
                            Label label = new Label();
                            label.setWrapText(true);
                            int index = i;
                            System.out.println(client.getMessage().get(index));
                            label.setText(client.getMessage().get(index));
                            Platform.runLater(() ->{
                                ClientOneApp.addToGrid(gridPane, label);
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

    public class ClientOneApp extends Application{
        static ArrayList<String> textStorage = new ArrayList<>();
        TextField messageBox = new TextField();
        Button messageButton = new Button("send");
        Label messageLabel = new Label();
        GridPane gridPane = new GridPane();
        HBox hBox = new HBox();
    
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
            ClientOne clientOne = new ClientOne();
            clientOne.start();

            messageLabel.setWrapText(true);

            HBox text = new HBox(5);
            text.getChildren().addAll(messageBox,messageButton);
            text.setAlignment(Pos.CENTER_RIGHT);  
            
            gridPane.setGridLinesVisible(false);

            BorderPane borderPane = new BorderPane();
            borderPane.setBottom(text);
            //BorderPane.setAlignment(text, Pos.CENTER_RIGHT);

            Label init = new Label();
            gridPane.add(init, 1, 10000);
            GridPane.setHgrow(init, Priority.ALWAYS);

            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            
            scrollPane.setContent(gridPane);
            //scrollPane.setPannable(true);
            scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
            scrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

            scrollPane.vvalueProperty().bind(gridPane.heightProperty());

            borderPane.setCenter(scrollPane);
            BorderPane.setAlignment(scrollPane, Pos.TOP_LEFT);

            ButtonHandler buttonHandler = new ButtonHandler(messageButton, messageBox, gridPane);
            buttonHandler.buttonHandler();

            TextFieldHandler textFieldHandler = new TextFieldHandler(messageButton, messageBox, gridPane);
            textFieldHandler.textFieldHandler();
            

            C1ReceiveMessageLabel C1receiveMessageLabel = new C1ReceiveMessageLabel(gridPane,clientOne);

            Thread C1MessageLabelThread = new Thread (C1receiveMessageLabel);

            C1MessageLabelThread.start();

            Scene scene = new Scene(borderPane, 500, 400);
            primaryStage.setScene(scene);
            primaryStage.setTitle("Client One Messaging App");
            primaryStage.show();
            
        }

        public static void main(String[] args){
            launch(args);
        }
        
    }



