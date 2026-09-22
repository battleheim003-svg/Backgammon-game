package games.mrlaki5.backgammon.GameModel;

import android.os.Bundle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import com.royalbackgammon.core.scoring.MatchState;
import com.royalbackgammon.core.variant.Variant;

import java.util.Collections;

import games.mrlaki5.backgammon.Beans.BoardFieldState;
import games.mrlaki5.backgammon.Beans.DiceThrow;
import games.mrlaki5.backgammon.GameControllers.GameActivity;
import games.mrlaki5.backgammon.Menus.MenuActivity;
import games.mrlaki5.backgammon.Players.Bot;
import games.mrlaki5.backgammon.Players.Human;
import games.mrlaki5.backgammon.Players.Player;

//Class used for model storing and loading
public class ModelLoader {

    //Method used for loading model from file
    public Model loadModel(Bundle extras, GameActivity activity){
        Model model=null;
        //Open saving file
        File file=new File(activity.getFilesDir(), MenuActivity.GAME_CONTINUE_SAVE_FILE_NAME);
        //If file doesn't exist create new model (new game creation)
        if(!file.exists()){
            model= new Model(extras, activity);
        }
        //If file exists load model from it
        else{
            //Create empty model
            model=new Model();
            BufferedReader in=null;
            String line=null;
            try{
                //Number of line in file
                int i=0;
                String[] data;
                //Create empty fields for model
                in = new BufferedReader(new FileReader(file));
                String p1Name="";
                String p2Name="";
                Player[] players=new Player[2];
                BoardFieldState[] BoardFields= new BoardFieldState[28];
                DiceThrow[] DiceThrows=new DiceThrow[4];
                //Go line by line through file
                while((line=in.readLine()) != null){
                    switch (i){
                        //Line 0: load player1 name=player1Name
                        case 0:
                            p1Name=line;
                            break;
                        //Line 1: load player2 name=player2Name
                        case 1:
                            p2Name=line;
                            break;
                        //Line 2: load player kinds=player1Kind player2Kind
                        case 2:
                            data=line.split(" ");
                            if(data[0].equals("1")){
                                players[0]=new Human(activity, p1Name);
                            }
                            else{
                                players[0]=new Bot(activity, p1Name, model);
                            }
                            if(data[1].equals("1")){
                                players[1]=new Human(activity, p2Name);
                            }
                            else{
                                players[1]=new Bot(activity, p2Name, model);
                            }
                            model.setPlayers(players);
                            break;
                        //Line 3: load board fields=F0NumChip,F0Player[,F0Pinned] F1NumChip,F1Player[,F1Pinned]...
                        case 3:
                            data=line.split(" ");
                            for(int j=0; j<BoardFields.length; j++){
                                String[] dataTemp=data[j].split(",");
                                int numOfChips=Integer.parseInt(dataTemp[0]);
                                int player=Integer.parseInt(dataTemp[1]);
                                int pinned=dataTemp.length>2 ? Integer.parseInt(dataTemp[2]) : 0;
                                BoardFields[j]=new BoardFieldState(numOfChips, player, pinned);
                            }
                            model.setBoardFields(BoardFields);
                            break;
                        //Line 4: load dice throws=Throw0Num,Throw0Used Throw1Num,Throw1Used...
                        case 4:
                            data=line.split(" ");
                            for(int j=0; j<DiceThrows.length; j++){
                                String[] dataTemp=data[j].split(",");
                                int throwNum=Integer.parseInt(dataTemp[0]);
                                int alreadyUsed=Integer.parseInt(dataTemp[1]);
                                DiceThrows[j]=new DiceThrow(throwNum, alreadyUsed);
                            }
                            model.setDiceThrows(DiceThrows);
                            break;
                        //Line 5: load current player and game state=CurrentPlayer State
                        case 5:
                            data=line.split(" ");
                            int currentPlayer=Integer.parseInt(data[0]);
                            int state=Integer.parseInt(data[1]);
                            model.setCurrentPlayer(currentPlayer);
                            model.setState(state);
                            break;
                        //Line 6: load variant name (absent in saves from older versions)
                        case 6:
                            model.setVariant(Model.parseVariant(line.trim()));
                            break;
                        //Line 8: load turn counters=turnsPlayed headMovesThisTurn
                        case 8:
                            data=line.trim().split(" ");
                            if(data.length==2){
                                model.setTurnsPlayed(Integer.parseInt(data[0]));
                                model.setHeadMovesThisTurn(Integer.parseInt(data[1]));
                            }
                            break;
                        //Line 7: load match=target whiteScore redScore gamesPlayed [tavliFlag]
                        case 7:
                            data=line.trim().split(" ");
                            if(data.length>=4){
                                boolean tavli=data.length>4 && "1".equals(data[4]);
                                model.setMatch(new MatchState(model.getVariant(),
                                        tavli ? Variant.TAVLI_ROTATION : Collections.<Variant>emptyList(),
                                        Math.max(1, Integer.parseInt(data[0])),
                                        Integer.parseInt(data[1]), Integer.parseInt(data[2]),
                                        Integer.parseInt(data[3])));
                            }
                            break;
                        default:
                            break;
                    }
                    i++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            //Close input buffer
            finally {
                try{
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //Load file and delete it (when continue starts save gets deleted)
            file=new File(activity.getFilesDir().getAbsolutePath(), MenuActivity.GAME_CONTINUE_SAVE_FILE_NAME);
            file.delete();

        }
        return model;
    }

    //Method used for saving model state to file
    public void saveModel(Model model, GameActivity activity){
        //Open file
        File file=new File(activity.getFilesDir(), MenuActivity.GAME_CONTINUE_SAVE_FILE_NAME);
        PrintWriter out=null;
        try{
            //Create print writer
            out=new PrintWriter(file);
            //Line 0: save player1 name=player1Name
            String tempOut=model.getPlayers()[0].getPlayerName()+"\n";
            out.append(tempOut);
            //Line 1: save player2 name=player2Name
            tempOut=model.getPlayers()[1].getPlayerName()+"\n";
            out.append(tempOut);
            //Line 2: save player kinds=player1Kind player2Kind
            tempOut="";
            if(model.getPlayers()[0] instanceof Human){
                tempOut+="1 ";
            }
            else{
                tempOut+="2 ";
            }
            if(model.getPlayers()[1] instanceof Human){
                tempOut+="1"+"\n";
            }
            else{
                tempOut+="2"+"\n";
            }
            out.append(tempOut);
            //Line 3: save board fields=F0NumChip,F0Player,F0Pinned F1NumChip,F1Player,F1Pinned...
            tempOut="";
            BoardFieldState[] BoardFields=model.getBoardFields();
            for(int j=0; j<BoardFields.length; j++){
                tempOut+=BoardFields[j].getNumberOfChips()+","+BoardFields[j].getPlayer()+","
                        +BoardFields[j].getPinnedPlayer()+" ";
            }
            tempOut+="\n";
            out.append(tempOut);
            //Line 4: save dice throws=Throw0Num,Throw0Used Throw1Num,Throw1Used...
            tempOut="";
            DiceThrow[] DiceThrows=model.getDiceThrows();
            for(int j=0; j<DiceThrows.length; j++){
                tempOut+=DiceThrows[j].getThrowNumber()+","+DiceThrows[j].getAlreadyUsed()+" ";
            }
            tempOut+="\n";
            out.append(tempOut);
            //Line 5: save current player and game state=CurrentPlayer State
            tempOut=""+model.getCurrentPlayer()+" "+model.getState()+"\n";
            out.append(tempOut);
            //Line 6: save variant name
            out.append(model.getVariant().name()).append("\n");
            //Line 7: save match=target whiteScore redScore gamesPlayed tavliFlag
            MatchState match=model.getMatch();
            out.append(match.getTargetPoints()+" "+match.getWhiteScore()+" "
                    +match.getRedScore()+" "+match.getGamesPlayed()+" "
                    +(match.getRotation().isEmpty() ? "0" : "1")+"\n");
            //Line 8: save turn counters=turnsPlayed headMovesThisTurn
            out.append(model.getTurnsPlayed()+" "+model.getHeadMovesThisTurn()+"\n");
            //Flush writer
            out.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        //Close writer
        finally {
            if (out != null) {
                out.close();
            }
        }
    }
}
