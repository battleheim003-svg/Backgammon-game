package games.mrlaki5.backgammon.GameView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import android.util.AttributeSet;

import games.mrlaki5.backgammon.Beans.BoardFieldState;
import games.mrlaki5.backgammon.Beans.DiceThrow;
import games.mrlaki5.backgammon.R;

//View of game
public class OnBoardImage extends androidx.appcompat.widget.AppCompatImageView {
    private static final int COLOR_SURFACE_BASE = Color.rgb(17, 37, 50);
    private static final int COLOR_SURFACE_INSET = Color.rgb(11, 26, 36);
    private static final int COLOR_ACCENT_GOLD = Color.rgb(244, 176, 68);
    private static final int COLOR_ACCENT_ORANGE = Color.rgb(224, 104, 14);
    private static final int COLOR_ACCENT_SLATE = Color.rgb(136, 165, 183);
    private static final int COLOR_TEXT_PRIMARY = Color.rgb(247, 239, 213);
    private static final long MESSAGE_ANIMATION_MS = 260L;
    private final Object messageLock = new Object();

    //Chips matrix (with number of chips on triangle [0] and player [1] (1-white, 2-red)), length:24
    private BoardFieldState[] ChipMatrix;
    //Array with hints for next move (1-there is hint, 0- no hint), length:24
    private int[] NextMoveArray;
    //String for current game state message
    private String Message="";
    //Paint for red chips
    private Paint RedChipPaint;
    //Paint for white chips
    private Paint WhiteChipPaint;
    //Paint for boreder of chips
    private Paint BorderChipPaint;
    //Paint for next move hint triangle transparency
    private Paint NextTriangleTransparentPaint;
    //Rect in which chip is drawn
    private RectF ChipRect;
    //Image for top row hints
    private Bitmap NextTriangleImageTop;
    //Image for bottom row hints
    private Bitmap NextTriangleImageBottom;
    //Rect in which hint is drawn
    private RectF NextTriangleRect;
    //Image for end board hints
    private Bitmap EndBoardImage;
    //y top border
    private float YBaseTop;
    //x right border
    private float XBaseRight;
    //x left border
    private float XBaseLeft;
    //Width of board
    private float Width;
    //Full width of board
    private float RealWidth;
    //x coordinate of endBoards middle line
    private float EndBoardMidX;
    //Height of board
    private float Height;
    //Width of left side of board
    private float LeftX;
    //Width of right size of board
    private float RightX;
    //Padding of left side triangles
    private float PaddingXLeft;
    //Padding of right side triangles
    private float PaddingXRight;
    //Height of triangles
    private float TriangleHeight;
    //Height of end chip
    private float EndChipHeight;
    //Size of moving chip
    private float MoveChipSize;
    //Coordinates of moving chip (-1 if not used)
    private float MoveChipX=-1;
    private float MoveChipY=-1;
    //Player of moving chip (1-white, 2-red)
    private int MoveChipPlayer=-1;
    private final float[] FieldCenterX = new float[28];
    //Dice images
    private Bitmap DiceImages[]= new Bitmap[4];
    private Bitmap DiceBitmapCache[][]= new Bitmap[7][2];
    //Padding between dices
    private float DicePadding;
    //Size of dice
    private float DiceSize;
    //Dice center x coordinate
    private float DiceXCenter;
    //Dices starting y coordinates
    //when there is 2 or 4 dices drawn
    private float DiceYStartTwo;
    private float DiceYStartFour;
    //Dices x coordinates
    private float DiceXStart;
    private float DiceXEnd;
    //Drawing rect for dice images
    private RectF DiceRect;
    //Paint for drawing dices
    private Paint DicePaint;
    //Coordinates for text drawing
    private float TextXCoordinate;
    private float TextYCoordinate;
    //rect for text figure drawing
    private RectF TextFigureRect;
    //Paint for text figure
    private Paint TextFigurePaint;
    //Paint for text
    private Paint TextPaint;
    //size of text
    private float TextSize;
    private Paint MoveChipShadowPaint;
    private Paint MovePulsePaint;
    private Paint ChipHighlightPaint;
    private Paint ChipRimPaint;
    private Paint MessageOuterBorderPaint;
    private Paint MessageInnerBorderPaint;
    private Paint MessageDiceFillPaint;
    private Paint MessageDiceDotPaint;
    private final Handler animationHandler = new Handler(Looper.getMainLooper());
    private Runnable movePulseRunnable;
    private Runnable messageAnimationRunnable;
    private int movePulseField=-1;
    private boolean movePulseHit=false;
    private float movePulseProgress=1F;
    private long messageAnimationStart=0L;
    private float messageAnimationProgress=1F;
    private boolean messageRollPrompt=false;

    public OnBoardImage(Context context) {
        super(context);
        initOnBoardImage();
    }

    public OnBoardImage(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initOnBoardImage();
    }

    public OnBoardImage(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initOnBoardImage();
    }

    //Method used for initialization
    private void initOnBoardImage(){
        Message=getContext().getString(R.string.initial_game_message);
        //create color for red chips
        RedChipPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        RedChipPaint.setColor(COLOR_ACCENT_SLATE);
        //create color for white chips
        WhiteChipPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        WhiteChipPaint.setColor(COLOR_ACCENT_ORANGE);
        //create color for border of chips
        BorderChipPaint= new Paint();
        BorderChipPaint.setAntiAlias(true);
        BorderChipPaint.setStyle(Paint.Style.STROKE);
        //create rect that will be used for drawing chips
        ChipRect=new RectF();
        //create transparent paint for next move green triangles
        NextTriangleTransparentPaint=new Paint();
        NextTriangleTransparentPaint.setAlpha(150);
        //load green hint images
        NextTriangleImageTop= BitmapFactory.decodeResource(getResources(), R.drawable.triangle_up);
        NextTriangleImageBottom= BitmapFactory.decodeResource(getResources(),
                R.drawable.triangle_down);
        EndBoardImage= BitmapFactory.decodeResource(getResources(), R.drawable.square);
        //create rect that will be used for drawing triangles for next moves
        NextTriangleRect=new RectF();
        //Create rect that will be used for drawing dices
        DiceRect=new RectF();
        //Create paint for drawing dices
        DicePaint=new Paint();
        //Create paint for text drawing
        TextFigureRect=new RectF();
        TextFigurePaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        TextFigurePaint.setColor(Color.argb(222, 17, 37, 50));
        TextPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        TextPaint.setColor(COLOR_ACCENT_GOLD);
        TextPaint.setShadowLayer(5.0F, 1.5F, 2.5F, COLOR_SURFACE_INSET);
        TextPaint.setTypeface(Typeface.create("sans-serif-black",Typeface.BOLD));
        TextPaint.setFakeBoldText(true);
        TextPaint.setLetterSpacing(0.04F);
        TextPaint.setTextSkewX(0F);
        MoveChipShadowPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        MoveChipShadowPaint.setColor(Color.argb(132, 0, 0, 0));
        MovePulsePaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        MovePulsePaint.setStyle(Paint.Style.STROKE);
        MovePulsePaint.setStrokeWidth(4F);
        ChipHighlightPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        ChipHighlightPaint.setStyle(Paint.Style.STROKE);
        ChipHighlightPaint.setColor(Color.argb(120, 255, 255, 255));
        ChipRimPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        ChipRimPaint.setStyle(Paint.Style.STROKE);
        MessageOuterBorderPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        MessageOuterBorderPaint.setStyle(Paint.Style.STROKE);
        MessageOuterBorderPaint.setColor(COLOR_ACCENT_GOLD);
        MessageInnerBorderPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        MessageInnerBorderPaint.setStyle(Paint.Style.STROKE);
        MessageInnerBorderPaint.setColor(COLOR_SURFACE_INSET);
        MessageDiceFillPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        MessageDiceFillPaint.setStyle(Paint.Style.FILL);
        MessageDiceFillPaint.setColor(COLOR_TEXT_PRIMARY);
        MessageDiceDotPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        MessageDiceDotPaint.setStyle(Paint.Style.FILL);
        MessageDiceDotPaint.setColor(COLOR_SURFACE_BASE);
    }

    //Method called when size of board changes (is called on creation of view)
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //borders size
        YBaseTop=h*0.107F;
        XBaseLeft=w*0.04F;
        XBaseRight=w*0.05F;
        //Take width and height of canvas
        Width=w*0.8735F;
        Height=h*0.966F;
        RealWidth=w;
        //Width of middle wood border is 10% of board border
        //find width of left and right size of board
        LeftX=(Width-YBaseTop)*0.4565f;
        RightX=(Width-YBaseTop)*0.566f;
        //There is 12 triangles on left and right side, and 6 of them coun in width
        //Count width of left side and right side triangles
        PaddingXLeft=LeftX/6;
        PaddingXRight=((Width-XBaseLeft)-RightX)/6;
        //Calculate triangles height about 40% of boards height
        TriangleHeight=(Height-YBaseTop)*0.39f;
        //Calculate end boards middle line on x coordinate
        EndBoardMidX=Width+(PaddingXRight*3/4);
        calculateFieldCenters();
        //Calculate height of end chips
        EndChipHeight=TriangleHeight/15;
        //Calculate dice size and padding between dices
        DiceSize=XBaseRight*0.8F;
        DicePadding=DiceSize*0.4F;
        //Calculate x coordinates for dices
        DiceXCenter=RealWidth-XBaseRight/2F;
        DiceXStart=DiceXCenter-DiceSize/2F;
        DiceXEnd=DiceXCenter+DiceSize/2F;
        //Calculate y coordinates for dices
        DiceYStartTwo=YBaseTop+((Height-YBaseTop)/2F)-(DicePadding/2F)-DiceSize;
        DiceYStartFour=YBaseTop+((Height-YBaseTop)/2F)-((DicePadding*3/2F)/2F)-DiceSize*2F;
        //Calculate coordinates for text drawing
        TextXCoordinate=w/2F;
        TextYCoordinate=YBaseTop*0.68F;
        //Calculate size of text
        TextSize=Math.max(22F, YBaseTop*0.44F);
        TextPaint.setTextSize(TextSize);
    }

    private void startMessageAnimation() {
        if(messageAnimationRunnable!=null){
            animationHandler.removeCallbacks(messageAnimationRunnable);
        }
        messageAnimationStart=System.currentTimeMillis();
        messageAnimationProgress=0F;
        messageAnimationRunnable=new Runnable() {
            @Override
            public void run() {
                long elapsed=System.currentTimeMillis()-messageAnimationStart;
                messageAnimationProgress=Math.min(1F, elapsed/(float)MESSAGE_ANIMATION_MS);
                postInvalidateOnAnimation();
                if(messageAnimationProgress<1F){
                    animationHandler.postDelayed(this, 16L);
                }
                else{
                    messageAnimationRunnable=null;
                }
            }
        };
        animationHandler.post(messageAnimationRunnable);
    }

    private void calculateFieldCenters() {
        for (int i = 0; i < 6; i++) {
            FieldCenterX[i] = XBaseLeft + PaddingXLeft * (i + 0.5F);
            FieldCenterX[i + 6] = XBaseLeft + RightX + PaddingXRight * (i + 0.5F);
            FieldCenterX[i + 12] = FieldCenterX[i];
            FieldCenterX[i + 18] = FieldCenterX[i + 6];
        }
        FieldCenterX[24] = XBaseLeft + (Width - XBaseLeft) / 2F;
        FieldCenterX[25] = FieldCenterX[24];
        FieldCenterX[26] = EndBoardMidX;
        FieldCenterX[27] = EndBoardMidX;
    }

    //Method for setting chip matrix
    public synchronized void setChipMatrix(BoardFieldState []chips){
        this.ChipMatrix=chips;
    }

    //Method for setting next move hints
    public synchronized void setNextMoveArray(int []moves){
        this.NextMoveArray=moves;
    }

    //Method for setting coordinates and player of moving chip
    public void setMoveChip(float x, float y, int player){
        if(!((x-(MoveChipSize/2F))>=XBaseLeft)) {
            MoveChipX=XBaseLeft+MoveChipSize/2F;
        }
        else{
            if(!((x+(MoveChipSize/2F))<=(RealWidth-XBaseRight))){
                MoveChipX=RealWidth-XBaseRight-MoveChipSize/2F;
            }
            else{
                MoveChipX = x;
            }
        }
        if(!((y-(MoveChipSize/2))>=YBaseTop)){
            MoveChipY = YBaseTop + MoveChipSize/2F;
        }
        else{
            if(!((y+(MoveChipSize/2))<=Height)){
                MoveChipY = Height-MoveChipSize/2F;
            }
            else{
                MoveChipY = y;
            }
        }
        MoveChipPlayer=player;
    }

    //Method for unseting moving chip
    public boolean unsetMoveChip(){
        if(MoveChipX!=-1 && MoveChipY!=-1) {
            MoveChipX = -1;
            MoveChipY = -1;
            MoveChipPlayer = -1;
            return true;
        }
        return false;
    }

    //get x coordinate of moving chip
    public float getXMovPos(){
        return MoveChipX;
    }

    //get y coordinate of moving chip
    public float getYMovPos(){
        return MoveChipY;
    }

    //Method for moving the selected chip
    public boolean moveMoveChip(float x, float y){
        if(MoveChipX!=-1 && MoveChipY!=-1) {
            if((x-(MoveChipSize/2))>=XBaseLeft && (x+(MoveChipSize/2))<=(RealWidth-XBaseRight)) {
                MoveChipX = x;
            }
            if((y-(MoveChipSize/2))>=YBaseTop && (y+(MoveChipSize/2))<=Height){
                MoveChipY = y;
            }
            return true;
        }
        return false;
    }

    public void playMoveFeedback(final int destinationField, final boolean hit) {
        if(destinationField<0 || destinationField>=FieldCenterX.length){
            return;
        }
        if(movePulseRunnable!=null){
            animationHandler.removeCallbacks(movePulseRunnable);
        }
        movePulseField=destinationField;
        movePulseHit=hit;
        movePulseProgress=0F;
        movePulseRunnable=new Runnable() {
            @Override
            public void run() {
                movePulseProgress+=0.12F;
                if(movePulseProgress>=1F){
                    movePulseField=-1;
                    movePulseRunnable=null;
                    postInvalidateOnAnimation();
                    return;
                }
                postInvalidateOnAnimation();
                animationHandler.postDelayed(this, 16L);
            }
        };
        animationHandler.post(movePulseRunnable);
    }

    //Method for setting dices
    public void setDices(DiceThrow[] DiceThrows){
        synchronized (DiceImages){
            for(int i=0; i<DiceThrows.length; i++){
                //if value 0 then its not a throw and image should be null
                if(DiceThrows[i].getThrowNumber()==0){
                    DiceImages[i]=null;
                    continue;
                }
                int diceNumber=DiceThrows[i].getThrowNumber();
                int usedIndex=DiceThrows[i].getAlreadyUsed()==1 ? 1 : 0;
                if(DiceBitmapCache[diceNumber][usedIndex]==null){
                    DiceBitmapCache[diceNumber][usedIndex]=BitmapFactory.decodeResource(
                            getResources(), diceResourceId(diceNumber, usedIndex==1));
                }
                DiceImages[i]=DiceBitmapCache[diceNumber][usedIndex];
            }
        }
    }

    private int diceResourceId(int diceNumber, boolean used){
        switch(diceNumber){
            case 1: return used ? R.drawable.dice1d : R.drawable.dice1;
            case 2: return used ? R.drawable.dice2d : R.drawable.dice2;
            case 3: return used ? R.drawable.dice3d : R.drawable.dice3;
            case 4: return used ? R.drawable.dice4d : R.drawable.dice4;
            case 5: return used ? R.drawable.dice5d : R.drawable.dice5;
            case 6: return used ? R.drawable.dice6d : R.drawable.dice6;
            default: return R.drawable.dice1;
        }
    }

    //Method for setting up message and color of message (depending on player)
    public void setMessage(String text, int PlayerNum){
        setMessage(text, PlayerNum, false);
    }

    public void setMessage(String text, int PlayerNum, boolean rollPrompt){
        synchronized (messageLock){
            if(!Message.equals(text) || messageRollPrompt!=rollPrompt){
                Message=text;
                messageRollPrompt=rollPrompt;
                startMessageAnimation();
            }
            TextPaint.setColor(PlayerNum==1 ? COLOR_ACCENT_GOLD : COLOR_TEXT_PRIMARY);
        }
    }

    private void drawMessageDiceGlyph(Canvas canvas, float centerX, float centerY,
                                      float size) {
        RectF diceRect=new RectF(centerX-size/2F, centerY-size/2F,
                centerX+size/2F, centerY+size/2F);
        canvas.drawRoundRect(diceRect, size*0.20F, size*0.20F, MessageDiceFillPaint);
        MessageInnerBorderPaint.setStrokeWidth(Math.max(1F, size*0.07F));
        canvas.drawRoundRect(diceRect, size*0.20F, size*0.20F, MessageInnerBorderPaint);

        float dotRadius=size*0.075F;
        float offset=size*0.22F;
        canvas.drawCircle(centerX-offset, centerY-offset, dotRadius, MessageDiceDotPaint);
        canvas.drawCircle(centerX, centerY, dotRadius, MessageDiceDotPaint);
        canvas.drawCircle(centerX+offset, centerY+offset, dotRadius, MessageDiceDotPaint);
    }

    private int chipBaseColor(int player) {
        return player==1 ? COLOR_ACCENT_ORANGE : COLOR_ACCENT_SLATE;
    }

    private int chipLightColor(int player) {
        return player==1 ? COLOR_ACCENT_GOLD : COLOR_TEXT_PRIMARY;
    }

    private int chipDarkColor(int player) {
        return player==1 ? Color.rgb(115, 42, 5) : COLOR_SURFACE_BASE;
    }

    private void drawLuxuryChip(Canvas canvas, RectF rect, int player) {
        float radius=Math.max(rect.width(), rect.height())*0.58F;
        Paint fillPaint=player==1 ? WhiteChipPaint : RedChipPaint;
        fillPaint.setShader(new RadialGradient(rect.centerX()-rect.width()*0.22F,
                rect.centerY()-rect.height()*0.28F,
                radius,
                new int[]{chipLightColor(player), chipBaseColor(player), chipDarkColor(player)},
                new float[]{0F, 0.58F, 1F},
                Shader.TileMode.CLAMP));
        RectF shadowRect=new RectF(rect);
        shadowRect.offset(rect.width()*0.07F, rect.height()*0.10F);
        canvas.drawOval(shadowRect, MoveChipShadowPaint);
        canvas.drawOval(rect, fillPaint);
        fillPaint.setShader(null);

        ChipRimPaint.setStrokeWidth(Math.max(2F, rect.width()*0.07F));
        ChipRimPaint.setColor(player==1 ? COLOR_SURFACE_BASE : COLOR_ACCENT_GOLD);
        canvas.drawOval(rect, ChipRimPaint);

        RectF highlightRect=new RectF(rect);
        float inset=rect.width()*0.18F;
        highlightRect.inset(inset, inset);
        ChipHighlightPaint.setStrokeWidth(Math.max(1F, rect.width()*0.025F));
        canvas.drawOval(highlightRect, ChipHighlightPaint);
    }

    private void drawLuxuryEndChip(Canvas canvas, RectF rect, int player) {
        Paint fillPaint=player==1 ? WhiteChipPaint : RedChipPaint;
        fillPaint.setColor(chipBaseColor(player));
        RectF shadowRect=new RectF(rect);
        shadowRect.offset(rect.width()*0.06F, rect.height()*0.20F);
        canvas.drawRoundRect(shadowRect, rect.width()*0.12F, rect.width()*0.12F,
                MoveChipShadowPaint);
        canvas.drawRoundRect(rect, rect.width()*0.12F, rect.width()*0.12F, fillPaint);
        ChipRimPaint.setStrokeWidth(Math.max(1F, rect.width()*0.04F));
        ChipRimPaint.setColor(player==1 ? COLOR_SURFACE_BASE : COLOR_ACCENT_GOLD);
        canvas.drawRoundRect(rect, rect.width()*0.12F, rect.width()*0.12F, ChipRimPaint);
    }

    //Method for drawing on canvas
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //Text part
        synchronized (messageLock) {
            float easedProgress=1F-((1F-messageAnimationProgress)
                    *(1F-messageAnimationProgress));
            int savedAlpha=canvas.saveLayerAlpha(0F, 0F, getWidth(), getHeight(),
                    (int)(185F + (70F * easedProgress)));
            canvas.scale(0.96F + (0.04F * easedProgress),
                    0.96F + (0.04F * easedProgress), TextXCoordinate, TextYCoordinate);
            float textWidth=TextPaint.measureText(Message);
            float glyphSize=messageRollPrompt ? TextSize*0.82F : 0F;
            float glyphGap=messageRollPrompt ? TextSize*0.34F : 0F;
            float contentWidth=textWidth + glyphSize + glyphGap;
            float horizontalPadding=TextSize*0.65F;
            float verticalPadding=TextSize*0.42F;
            float boxLeft=Math.max(XBaseLeft, TextXCoordinate-(contentWidth/2F)-horizontalPadding);
            float boxRight=Math.min(RealWidth-XBaseRight,
                    TextXCoordinate+(contentWidth/2F)+horizontalPadding);
            float contentX=boxLeft + ((boxRight-boxLeft-contentWidth)/2F);
            float textX=contentX + glyphSize + glyphGap;
            TextFigureRect.set(boxLeft,
                    TextYCoordinate-TextSize-verticalPadding,
                    boxRight,
                    TextYCoordinate+verticalPadding);
            canvas.drawRoundRect(TextFigureRect, TextSize*0.58F, TextSize*0.58F,
                    TextFigurePaint);
            MessageOuterBorderPaint.setStrokeWidth(Math.max(2F, TextSize*0.08F));
            canvas.drawRoundRect(TextFigureRect, TextSize*0.58F, TextSize*0.58F,
                    MessageOuterBorderPaint);
            RectF innerFrame=new RectF(TextFigureRect);
            float innerInset=Math.max(3F, TextSize*0.16F);
            innerFrame.inset(innerInset, innerInset);
            MessageInnerBorderPaint.setStrokeWidth(Math.max(1F, TextSize*0.035F));
            canvas.drawRoundRect(innerFrame, TextSize*0.42F, TextSize*0.42F,
                    MessageInnerBorderPaint);
            if(messageRollPrompt){
                drawMessageDiceGlyph(canvas, contentX+(glyphSize/2F),
                        TextYCoordinate-(TextSize*0.32F), glyphSize);
            }
            canvas.drawText(Message, textX, TextYCoordinate, TextPaint);
            canvas.restoreToCount(savedAlpha);
        }
        //Dices part
        synchronized (DiceImages) {
            //set y coordinate for drawing dices (draw 4 or draw 2)
            float yDice;
            if (DiceImages[2] == null || DiceImages[3] == null) {
                //drawing 2
                yDice = DiceYStartTwo;
            } else {
                //drawing 4
                yDice = DiceYStartFour;
            }
            //Draw dices
            for (int i = 0; i < DiceImages.length; i++) {
                if (DiceImages[i] != null) {
                    DiceRect.set(DiceXStart, yDice, DiceXEnd, yDice + DiceSize);
                    canvas.drawBitmap(DiceImages[i], null, DiceRect, DicePaint);
                    yDice += DiceSize + DicePadding;
                }
            }
        }
        //Set starting coordinates to first triangles middle line
        float x=XBaseLeft+PaddingXLeft/2F;
        float y=YBaseTop;
        //padding to next triangles middle line
        float currPading=PaddingXLeft;
        //load current image for next step triangle
        Bitmap currentNextImage=NextTriangleImageTop;
        //if matrix exists draw chips
        if(ChipMatrix!=null) {
            boolean drawEndBoard=false;
            synchronized (this) {
                for (int i = 0; i < ChipMatrix.length; i++) {
                    x = FieldCenterX[i];
                    //after first six triangles jump to right top side of board
                    if (i == 6) {
                        //set right top side board coordinates
                        //set padding for right side of board
                        currPading = PaddingXRight;
                    }
                    //after first 12 triangles jump to left bottom side of board
                    if (i == 12) {
                        //set coordinates for right bottom side of board
                        y = Height;
                        //set padding for left side of board
                        currPading = PaddingXLeft;
                        //change next move triangle image
                        currentNextImage=NextTriangleImageBottom;
                    }
                    //after first 18 triangles jump to right bottom side of board
                    if (i == 18) {
                        //set coordinates for left bottom side of board
                        //set padding for right side of board
                        currPading = PaddingXRight;
                    }
                    //sideboard white chips (on up middle border)
                    if(i==24){
                        y=YBaseTop;
                        currPading = PaddingXLeft;
                    }
                    //sideboard red chips (on down middle border)
                    if(i==25){
                        y=Height;
                        currPading = PaddingXLeft;
                    }
                    if(i==26){
                        drawEndBoard=true;
                        y=YBaseTop;
                        currentNextImage=EndBoardImage;
                    }
                    if(i==27){
                        drawEndBoard=true;
                        x=EndBoardMidX;
                        y=Height;
                    }
                    //if there is any chips on current triangle
                    if (ChipMatrix[i].getNumberOfChips() > 0) {
                        //calculate x coordinates for rect where chip is drawn
                        float xChipStart = x - currPading * 0.35f;
                        float xChipEnd = x + currPading * 0.35f;
                        //calculate size of chip
                        float chipSize = Math.abs(xChipStart - xChipEnd);

                        float heightPadding = 0F;
                        float yChipStart=0F;
                        float yChipEnd=0F;
                        if (!drawEndBoard) {
                            //calculate padding in chip center (needed if there is more chips
                            //in triangle then triangles height is)
                            if ((chipSize * ChipMatrix[i].getNumberOfChips() > TriangleHeight)
                                    && (ChipMatrix[i].getNumberOfChips() > 1)) {
                                heightPadding = (chipSize * ChipMatrix[i].getNumberOfChips() -
                                        TriangleHeight) / (ChipMatrix[i].getNumberOfChips() - 1);
                            }
                            //calculate y coordinates for rect where chip is drawn
                            yChipStart = y;
                            yChipEnd = chipSize + y;
                            if (i >= 12 && i!=24) {
                                yChipEnd = y - chipSize;
                            }
                        }
                        else{
                            //calculate y coordinates for rect where end chip is drawn
                            yChipStart = y;
                            yChipEnd = EndChipHeight+y;
                            if (i==27) {
                                yChipEnd = y - EndChipHeight;
                            }
                        }
                        //set up width of border on chips
                        BorderChipPaint.setStrokeWidth(chipSize * 0.09F);
                        //set up color of chips, depending on player
                        Paint localPaint = null;
                        if (ChipMatrix[i].getPlayer() == 1) {
                            localPaint = WhiteChipPaint;
                        } else {
                            localPaint = RedChipPaint;
                        }
                        //go through all chips on triangle and draw them
                        for (int j = 0; j < ChipMatrix[i].getNumberOfChips(); j++) {
                            //set up coordinates for drawing current chip
                            if(i<12 || i==24 || i==26){
                                ChipRect.set(xChipStart, yChipStart, xChipEnd, yChipEnd);
                            }
                            else{
                                ChipRect.set(xChipStart, yChipEnd , xChipEnd, yChipStart);
                            }
                            if(!drawEndBoard) {
                                drawLuxuryChip(canvas, ChipRect, ChipMatrix[i].getPlayer());
                                //move y coordinates for drawing next chip on same triangle
                                if (i >= 12 && i != 24) {
                                    yChipStart = yChipEnd + heightPadding;
                                    yChipEnd = yChipEnd - chipSize + heightPadding;
                                } else {
                                    yChipStart = yChipEnd - heightPadding;
                                    yChipEnd = yChipEnd + (chipSize - heightPadding);
                                }
                            }
                            else{
                                drawLuxuryEndChip(canvas, ChipRect, ChipMatrix[i].getPlayer());
                                if (i == 27) {
                                    yChipStart = yChipEnd;
                                    yChipEnd = yChipEnd - EndChipHeight;
                                } else {
                                    yChipStart = yChipEnd;
                                    yChipEnd = yChipEnd + EndChipHeight;
                                }
                            }
                        }

                    }
                    //padding for calculating hints width
                    float tempHintWidth=0F;
                    if(!drawEndBoard){
                        tempHintWidth=currPading/2F;
                    }
                    else{
                        tempHintWidth=currPading*0.35F;
                    }
                    //check if next step hint (green triangle) is needed over triangle i
                    if(NextMoveArray!=null &&  i!=24 && i!=25 && NextMoveArray[i]!=0){
                        //calculate coordinates for green triangle
                        if(i<12 || i==26) {
                            //if on top row then y from 0 to triangleHeight
                            NextTriangleRect.set(x - tempHintWidth, YBaseTop, x +
                                    tempHintWidth, TriangleHeight+YBaseTop);
                        }
                        else{
                            //if on bottom row then y from height-triangleHeight to height
                            NextTriangleRect.set(x - tempHintWidth, Height-TriangleHeight,
                                    x + tempHintWidth, Height);
                        }
                        //draw green triangle
                        canvas.drawBitmap(currentNextImage, null, NextTriangleRect,
                                NextTriangleTransparentPaint);
                    }
                }
            }
            //check if there is moving chip
            if(MoveChipX!=-1 && MoveChipY!=-1){
                Paint localPaint=null;
                //find paint for chip
                if(MoveChipPlayer==1){
                    localPaint=WhiteChipPaint;
                }
                else{
                    localPaint=RedChipPaint;
                }
                canvas.drawCircle(MoveChipX + MoveChipSize*0.08F,
                        MoveChipY + MoveChipSize*0.12F, MoveChipSize/2, MoveChipShadowPaint);
                ChipRect.set(MoveChipX-MoveChipSize/2F, MoveChipY-MoveChipSize/2F,
                        MoveChipX+MoveChipSize/2F, MoveChipY+MoveChipSize/2F);
                drawLuxuryChip(canvas, ChipRect, MoveChipPlayer);
            }
            if(movePulseField!=-1){
                float pulseX=FieldCenterX[movePulseField];
                float pulseY=(movePulseField<12 || movePulseField==24 || movePulseField==26)
                        ? YBaseTop + Math.max(MoveChipSize, EndChipHeight)
                        : Height - Math.max(MoveChipSize, EndChipHeight);
                float radius=(MoveChipSize*0.45F) + (MoveChipSize*0.45F*movePulseProgress);
                int alpha=(int)(180F * (1F - movePulseProgress));
                MovePulsePaint.setColor(movePulseHit
                        ? Color.argb(alpha, 244, 176, 68)
                        : Color.argb(alpha, 136, 165, 183));
                MovePulsePaint.setStrokeWidth(Math.max(3F, MoveChipSize*0.08F));
                canvas.drawCircle(pulseX, pulseY, radius, MovePulsePaint);
            }
        }
    }

    //Method for checking which triangle is touched. (if none ret -1)
    public int triangleTouched(float touch_x, float touch_y){
        //check if coordinates are in top row of triangles
        if((touch_y<(TriangleHeight+YBaseTop)) && (touch_y>=YBaseTop)){
            //check if coordinates are in top right board
            if(touch_x>(RightX+XBaseLeft)){
                //check if end game position
                if(touch_x>Width){
                    float tempXStart=EndBoardMidX-(PaddingXLeft*0.35F);
                    float tempXEnd=EndBoardMidX+(PaddingXLeft*0.35F);
                    if((touch_x>=tempXStart) && (touch_x<=tempXEnd)){
                        return 26;
                    }
                }
                else {
                    //go through triangles and find touched one
                    float TriangleBorder = PaddingXRight + RightX + XBaseLeft;
                    int currTriangle = 6;
                    while (TriangleBorder < touch_x) {
                        currTriangle++;
                        TriangleBorder += PaddingXRight;
                    }
                    return currTriangle;
                }
            }
            else{
                //check if coordinates are in top left board
                if(touch_x<LeftX+XBaseLeft){
                    //go through triangles and find touched one
                    float TriangleBorder=PaddingXLeft+XBaseLeft;
                    int currTriangle=0;
                    while(TriangleBorder<touch_x){
                        currTriangle++;
                        TriangleBorder+=PaddingXLeft;
                    }
                    return currTriangle;
                }
                else{
                    float xChipStart = ((Width-XBaseLeft)/2)+ XBaseLeft - PaddingXLeft * 0.35f;
                    float xChipEnd = ((Width-XBaseLeft)/2) + XBaseLeft + PaddingXLeft * 0.35f;
                    if(touch_x>=xChipStart && touch_x<=xChipEnd){
                        return 24;
                    }
                }
            }
        }
        else{
            //check if coordinates are in bottom row of triangles
            if ((Height-TriangleHeight)<touch_y){
                //check if coordinates are in bottom right board
                if(touch_x>(RightX+XBaseLeft)){
                    //check if end game position
                    if(touch_x>Width){
                        float tempXStart=EndBoardMidX-(PaddingXLeft*0.35F);
                        float tempXEnd=EndBoardMidX+(PaddingXLeft*0.35F);
                        if((touch_x>=tempXStart) && (touch_x<=tempXEnd)){
                            return 27;
                        }
                    }
                    else {
                        //go through triangles and find touched one
                        float TriangleBorder = PaddingXRight + RightX + XBaseLeft;
                        int currTriangle = 18;
                        while (TriangleBorder < touch_x) {
                            currTriangle++;
                            TriangleBorder += PaddingXRight;
                        }
                        return currTriangle;
                    }
                }
                else{
                    //check if coordinates are in bottom left board
                    if(touch_x<(LeftX + XBaseLeft )){
                        //go through triangles and find touched one
                        float TriangleBorder=PaddingXLeft + XBaseLeft;
                        int currTriangle=12;
                        while(TriangleBorder<touch_x){
                            currTriangle++;
                            TriangleBorder+=PaddingXLeft;
                        }
                        return currTriangle;
                    }
                    else{
                        float xChipStart = ((Width-XBaseLeft)/2) + XBaseLeft - PaddingXLeft * 0.35f;
                        float xChipEnd = ((Width-XBaseLeft)/2) + XBaseLeft + PaddingXLeft * 0.35f;
                        if(touch_x>=xChipStart && touch_x<=xChipEnd){
                            return 25;
                        }
                    }
                }
            }
        }
        //return -1 if none of triangles are clicked
        return -1;
    }

    // Checks whether a checker was touched. The checker is drawn at its original
    // size, but the invisible touch target is larger for more responsive play.
    public synchronized boolean chipPTouched(int trianglePosition, float touchX,
                                             float touchY) {
        if (trianglePosition < 0 || trianglePosition > 25) {
            return false;
        }

        int numberOfChips = ChipMatrix[trianglePosition].getNumberOfChips();
        if (numberOfChips <= 0) {
            return false;
        }

        float chipSize;
        if (touchX > (RightX + XBaseLeft)) {
            chipSize = PaddingXRight * 0.7f;
        }
        else {
            chipSize = PaddingXLeft * 0.7f;
        }

        float touchTolerance = chipSize * 0.45f;
        float chipsHeight = numberOfChips * chipSize;

        boolean isTopTriangle =
                touchY >= (YBaseTop - touchTolerance)
                        && touchY <= (YBaseTop + TriangleHeight + touchTolerance);

        if (isTopTriangle) {
            float touchEnd = YBaseTop + chipsHeight + touchTolerance;
            if (touchY <= touchEnd) {
                MoveChipSize = chipSize;
                return true;
            }
        }

        boolean isBottomTriangle =
                touchY >= (Height - TriangleHeight - touchTolerance)
                        && touchY <= (Height + touchTolerance);

        if (isBottomTriangle) {
            float touchStart = Height - chipsHeight - touchTolerance;
            if (touchY >= touchStart) {
                MoveChipSize = chipSize;
                return true;
            }
        }

        return false;
    }
}


