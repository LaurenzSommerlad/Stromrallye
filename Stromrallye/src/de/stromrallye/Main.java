package de.stromrallye;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.io.*;
import java.util.Arrays;

public class Main extends Application {

    final public static int SCENE_WIDTH = 1000, SCENE_HEIGHT = 700;
    // the width a Field on the board shall have
    final public static int FIELD_WIDTH = 30;

    private int robotX;
    private int robotY;
    private int robotCharge;

    int boardSize;
    int numBatteries;

    private Field board[][];

    Group group;

    @Override
    public void start(Stage primaryStage) throws Exception {
        BorderPane root = new BorderPane();
        primaryStage.setTitle("BwInf 38 - Runde 2 - Aufgabe 1: Stromrallye");
        primaryStage.setScene(new Scene(root, SCENE_WIDTH, SCENE_HEIGHT));
        primaryStage.show();
        // create a Group
        group = new Group();

        // read the file(s) that contains the boards
        readBoardFile("src/stromrallye0.txt"); //TODO: how to really read this correctly?
        // a canvas on which the board is drawn
        Canvas boardCanvas = drawBoard();
        // every empty Field so far is going to be added a rectangle
        fillOutRectangles();
        // a canvas on which the game elements (robot, batteries, charges) are drawn
        Canvas fieldCanvas = updateFields();
        /* - both canvases  are in a layer system (Group contains both)
           - the board canvas doesn't need to be updated
           the fieldCanvas has to be updated because of moving game elements
           => two separated canvases to not update everything every time
         */
        // scrollPane for the case that the board is too big to fit into the Stage
        ScrollPane sp = new ScrollPane();
        group.getChildren().addAll(boardCanvas, fieldCanvas);
        sp.setContent(group);
        root.setCenter(sp);
        // shifted back to bring Fields(Rectangles) to front
        boardCanvas.toBack();
        fieldCanvas.toBack();
        //System.out.println(Arrays.deepToString(board));

        findFieldConnections();

        //System.out.println(board[0][0].getReachables());
    }

    private void readBoardFile(final String path) {
        try(BufferedReader reader = new BufferedReader(new FileReader(new File(path)))) {
            // the first line of the text file contains the size of the board
            boardSize = Integer.parseInt(reader.readLine());
            // creating a new board which size is the one read from the text file
            board = new Field[boardSize][boardSize];
            // line that contains the information of the robot saved separated in an array
            String[] lineRobot = reader.readLine().split(",");
            // x coordinate of the robot
            robotX = Integer.parseInt(lineRobot[0])-1;
            // y coordinate of the current field
            robotY = Integer.parseInt(lineRobot[1])-1;
            // charge of the battery on the robot
            robotCharge = Integer.parseInt(lineRobot[2]);
            //  // creating the Field for the robot and add it the board
            board[robotX][robotY] = new Field(robotX, robotY);
            // adding the Rectangle "behind" the Robot to the Group (the Superclass)
            group.getChildren().add(board[robotX][robotY]);
            // this Field has a robot on top
            board[robotX][robotY].setRobotOnTop(true);
            board[robotX][robotY].setBatteryCharge(robotCharge);
            // TODO: batteryCharge of Robot as Field charge?
            numBatteries = Integer.parseInt(reader.readLine());
            for (int i = 0; i < numBatteries; i++) {
                // line that contains the information of the current field saved separated in an array
                String[] lineField = reader.readLine().split(",");
                // x coordinate of the current field
                int fieldX = Integer.parseInt(lineField[0])-1;
                // y coordinate of the current field
                int fieldY = Integer.parseInt(lineField[1])-1;
                // charge of the battery on the current field
                int fieldCharge = Integer.parseInt(lineField[2]);
                // creating the Field and add it the board
                board[fieldX][fieldY] = new Field(fieldX, fieldY);
                // adding the Rectangle "behind" the Field to the Group (the Superclass)
                group.getChildren().add(board[fieldX][fieldY]);
                // adding batteryCharge to the field
                board[fieldX][fieldY].setBatteryCharge(fieldCharge);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException nfe) {
            System.out.println("This File does not match the pattern it should.");
        }
    }

    private void fillOutRectangles() {
        // for every empty spot in board draw a Field without any specifications (like battery charge)
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board.length; j++) {
                if (board[i][j] == null) {
                    board[i][j] = new Field(i+1, j+1);
                }
            }
        }
    }


    private Canvas drawBoard() {
        // the width of the lines, the board is visualized with
        double lineWidth = 1;
        // creating a canvas that is just lineWidth wider that the board is, to make borders at the edges visible
        Canvas canvas = new Canvas(FIELD_WIDTH*boardSize+lineWidth, FIELD_WIDTH*boardSize+lineWidth);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setLineWidth(lineWidth);

        // draw borders and field separators for the board
        for (int i = 0; i <= boardSize; i++) {
                gc.strokeLine(i*FIELD_WIDTH,  0, i*FIELD_WIDTH, boardSize * FIELD_WIDTH);
                gc.strokeLine(0, i*FIELD_WIDTH, boardSize*FIELD_WIDTH, i*FIELD_WIDTH);
        }
        return canvas;
    }

    private Canvas updateFields() {
        // Canvas that is as wide as the board canvas (second layer)
        Canvas canvas = new Canvas(FIELD_WIDTH*boardSize, FIELD_WIDTH*boardSize);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        // center the text and add a Font to it
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 15));

        // for every Field that is in the board-Array draw a battery or possibly a robot
        for(int i = 0; i < boardSize; i++) {
            for(int j = 0; j < boardSize; j++) {
                // only if array-field not empty (contains a Field)
                if(board[i][j] != null) {
                    drawField(gc, board[i][j], i, j);
                }
            }
        }
        return canvas;
    }

    private void drawField(final GraphicsContext gc,  final Field curField, final int i, final int j) {
        // a ratio to not connect the robot or battery with the border of the Field
        float edgeDistRatio = 0.1f;
        // if robot on the Field -> color GREEN; else for battery -> color GRAY
        gc.setFill(curField.isRobotOnTop() ? Color.GREEN : Color.GRAY);
        // a circle as a robot or battery
        // formulas to center the circle in the Field
        // TODO: simplify the Formulas
        // show charge only if there is a battery
        if (board[i][j].getBatteryCharge() > -1) {
            gc.fillOval(i * FIELD_WIDTH + FIELD_WIDTH * edgeDistRatio, j * FIELD_WIDTH + FIELD_WIDTH * edgeDistRatio, FIELD_WIDTH - (FIELD_WIDTH * edgeDistRatio) * 2, FIELD_WIDTH - (FIELD_WIDTH * edgeDistRatio) * 2);
            gc.setFill(Color.BLACK);
            gc.fillText(String.valueOf(curField.getBatteryCharge()), i * FIELD_WIDTH + FIELD_WIDTH * 0.5, j * FIELD_WIDTH + FIELD_WIDTH * 0.5);
        }
    }

    private void findFieldConnections() {
        for (int i = 0; i < board.length; i++) {
            for(int j = 0; j < board.length; j++) {
                if(board[i][j] != null) {
                    board[i][j].collectReachables(board, i, j);
                    board[i][j].collectAllReachables(board, i, j);
                }
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}