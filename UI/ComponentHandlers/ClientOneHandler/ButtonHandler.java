package UI.ComponentHandlers.ClientOneHandler;
import UI.ClientOneApp;

import javafx.scene.control.*;
import javafx.scene.layout.*;


public class ButtonHandler {
    Button messageButton;
    TextField messageBox;
    GridPane gridPane;

    public ButtonHandler(Button messageButton, TextField messagBox, GridPane gridPane){
        this.messageButton = messageButton;
        this.messageBox = messagBox;
        this.gridPane = gridPane;
    }

    public void buttonHandler(){
        
        messageButton.setOnAction(e->{
                String message = messageBox.getText();
                if(!message.isEmpty()){
                    ClientOneApp.getText().add(message);
                    Label label = new Label();
                    label.setText(messageBox.getText());
                    label.setWrapText(true);
                    ClientOneApp.addToGridSelf(gridPane, label);

                    messageBox.setText(null);


                }    
            });
    }
}
