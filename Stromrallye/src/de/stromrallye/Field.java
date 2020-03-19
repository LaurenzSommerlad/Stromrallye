package de.stromrallye;

import javafx.event.EventHandler;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Field extends Rectangle {

    private boolean robotOnTop = false;
    // batteryCharge = -1 if there's no battery
    private int batteryCharge = -1; //TODO: Whats the standard value?=
    private List<Field> reachables = new ArrayList<>();
    private List<Field> allReachables = new ArrayList<>();

    private int posX;
    private int posY;
    private int width = Main.FIELD_WIDTH;

    private boolean pressed = false;

    public Field(int posX, int posY) {
        this.posX = posX;
        this.posY = posY;
        this.setX((this.posX)*width);
        this.setY((this.posY)*width);
        this.setWidth(width);
        this.setHeight(width);

        setup();
        clickOptions();
    }

    private void setup() {
        this.setFill(Color.TRANSPARENT);
    }

    private void clickOptions() {
        this.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getButton() == MouseButton.PRIMARY) {
                    if (pressed) {
                        setFill(Color.TRANSPARENT);
                        for (Field reachable: allReachables) {
                            reachable.setFill(Color.TRANSPARENT);
                        }
                        pressed = false;
                    } else {
                        setFill(Color.rgb( 255, 0, 0, 0.60));
                        for (Field reachable: allReachables) {
                            reachable.setFill(Color.rgb( 253, 180, 0, 0.60));
                        }
                        pressed = true;
                    }
                }
            }
        });
    }

    public boolean isRobotOnTop() {
        return robotOnTop;
    }

    public void setRobotOnTop(boolean robotOnTop) {
        this.robotOnTop = robotOnTop;
    }

    public void setBatteryCharge(int batteryCharge) {
        this.batteryCharge = batteryCharge;
    }

    public int getBatteryCharge() {
        return batteryCharge;
    }

    public List<Field> getReachables() {
        return reachables;
    }

    public List<Field> getAllReachables() {
        return allReachables;
    }

    public void collectReachables(final Field[][] board, final int curX, final  int curY) {
        // check for each field with battery which other batteries are in range
        for (int i = 0; i < board.length; i++) {
            for(int j = 0; j < board.length; j++) {
                // check field that has a battery on top, is not the current field  (to prevent distance 0) and if robot is on top (robot does not count as battery)
                if(board[i][j] != null && !board[i][j].equals(this) && !board[i][j].isRobotOnTop()) {
                    // calculate the shortest distance between two fields (with battery), by adding delta-X to delta-Y
                    if(batteryCharge >= Math.abs(curX - i) + Math.abs(curY - j)) {
                        // if battery charge of field is big enough (bigger or same as distance) add it to the reachable Fields of this Field
                        reachables.add(board[i][j]);
                    }
                }
            }
        }
        //System.out.println(reachables);
    }

    public void collectAllReachables(final Field[][] board, final int curX, final  int curY) {
        for (int i = 0; i < board.length; i++) {
            for(int j = 0; j < board.length; j++) {
                // check field that has a battery on top, is not the current field  (to prevent distance 0)
                if(!board[i][j].equals(this)) {
                    // calculate the shortest distance between two fields (with battery), by adding delta-X to delta-Y
                    if(batteryCharge >= Math.abs(curX - i) + Math.abs(curY - j)) {
                        // if battery charge of field is big enough (bigger or same as distance) add it to the reachable Fields of this Field
                        allReachables.add(board[i][j]);
                    }
                }
            }
        }
    }

}
