package UI.ComponentHandlers.ClientTwoHandler;
import UI.ClientTwoApp;

import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;


public class TextFieldHandler {
    Button messageButton;
    TextField messageBox;
    GridPane gridPane;

    public TextFieldHandler (Button messageButton, TextField messagBox, GridPane gridPane){
        this.messageButton = messageButton;
        this.messageBox = messagBox;
        this.gridPane = gridPane;
    }

    public void textFieldHandler(){
        
        messageBox.addEventHandler(KeyEvent.KEY_PRESSED, e ->{
            if(e.getCode().equals(KeyCode.ENTER)){
                String message = messageBox.getText();
                if(!message.isEmpty()){
                    ClientTwoApp.getText().add(message);
                    Label label = new Label();
                    label.setWrapText(true);
                    label.setText(messageBox.getText());
                    ClientTwoApp.addToGridSelf(gridPane, label);
                    messageBox.setText(null);
                
                }
            }
        });
    }
}
