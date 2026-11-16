package com.doloreshazze.onesecond;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.view.*;
import android.content.Context;
import java.util.*;

public class MainActivity extends Activity {
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().setStatusBarColor(Color.rgb(17,21,43));
    setContentView(new GameView(this));
  }

  static class GameView extends View {
    Paint p = new Paint(3); Paint stroke = new Paint(3);
    float downX, downY, x, y, vx, vy, density;
    int level = 1; boolean frozen = true, won = false, lost = false;
    long last; String banner = ""; ArrayList<Spark> sparks = new ArrayList<>();
    RectF restart = new RectF();

    GameView(Context c) { super(c); density=getResources().getDisplayMetrics().density; stroke.setStyle(Paint.Style.STROKE); stroke.setStrokeWidth(4*density); stroke.setColor(Color.WHITE); p.setTypeface(Typeface.create("sans", Typeface.BOLD)); reset(); }
    void reset() { frozen=true; won=false; lost=false; banner=""; sparks.clear(); x=.28f; y= level==1?.37f:.71f; vx=vy=0; last=System.currentTimeMillis(); invalidate(); }
    float W(){return getWidth();} float H(){return getHeight();} float X(float v){return v*W();} float Y(float v){return v*H();}
    void fill(Canvas c,int color,float l,float t,float r,float b,float rad){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRoundRect(X(l),Y(t),X(r),Y(b),rad*density,rad*density,p);}
    void text(Canvas c,String s,float size,int color,float cx,float cy){p.setTextSize(size*density);p.setColor(color);p.setTextAlign(Paint.Align.CENTER);p.setStyle(Paint.Style.FILL);c.drawText(s,X(cx),Y(cy),p);}
    @Override protected void onDraw(Canvas c) { super.onDraw(c); float w=W(),h=H();
      c.drawColor(level==1?Color.rgb(29,39,77):Color.rgb(35,25,69));
      // soft game-board panels
      fill(c, level==1?Color.rgb(52,78,125):Color.rgb(73,45,121), .035f,.12f,.965f,.91f,28);
      fill(c, Color.argb(95,255,255,255), .055f,.14f,.945f,.24f,18);
      text(c,"ONE SECOND!",24,Color.WHITE,.5f,.185f);
      text(c, "УРОВЕНЬ "+level+"  ·  "+(level==1?"Спаси вазу":"Не разбуди кота"),14,Color.rgb(215,225,255),.5f,.218f);
      drawScene(c);
      if(frozen && !won && !lost){ text(c,"ВРЕМЯ ЗАМОРОЖЕНО",17,Color.rgb(255,230,99),.5f,.30f); text(c,"Свайпни, чтобы задать направление",14,Color.WHITE,.5f,.335f); pulse(c); }
      if(!frozen && !won && !lost) text(c,"Смотри, что будет…",15,Color.WHITE,.5f,.30f);
      if(won||lost) overlay(c);
      if(!frozen&&!won&&!lost){ step(); postInvalidateOnAnimation(); }
      for(Spark s:sparks){s.draw(c,p);}
    }
    void pulse(Canvas c){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3*density);p.setColor(Color.argb(180,255,255,255));c.drawCircle(X(x),Y(y),31*density,p);p.setStyle(Paint.Style.FILL);}
    void drawScene(Canvas c){
      // floor
      fill(c,Color.rgb(25,29,52),.07f,.82f,.93f,.86f,8);
      if(level==1){
        // table, cat, vase and pillow target
        fill(c,Color.rgb(134,77,49),.10f,.58f,.90f,.62f,5); fill(c,Color.rgb(107,56,39),.17f,.62f,.22f,.82f,5);fill(c,Color.rgb(107,56,39),.78f,.62f,.83f,.82f,5);
        p.setColor(Color.rgb(255,165,83));c.drawCircle(X(.18f),Y(.51f),35*density,p);c.drawCircle(X(.14f),Y(.47f),15*density,p);c.drawCircle(X(.22f),Y(.47f),15*density,p);text(c,"=^.^=",13,Color.rgb(84,48,44),.18f,.525f);
        fill(c,Color.rgb(121,214,255),.72f,.47f,.79f,.59f,8); p.setColor(Color.WHITE);c.drawCircle(X(.755f),Y(.44f),13*density,p); text(c,"💐",20,Color.WHITE,.755f,.49f);
        fill(c,Color.rgb(255,112,157),.57f,.76f,.85f,.81f,28); text(c,"ПОДУШКА",10,Color.WHITE,.71f,.792f);
        text(c,"★",25,Color.rgb(255,224,97),.89f,.38f);
      } else {
        // cat nap, fishbowl and bouncy bell
        fill(c,Color.rgb(84,144,205),.12f,.39f,.88f,.43f,7); fill(c,Color.rgb(55,110,169),.16f,.43f,.22f,.66f,5); fill(c,Color.rgb(55,110,169),.78f,.43f,.84f,.66f,5);
        p.setColor(Color.rgb(96,221,238));c.drawOval(new RectF(X(.68f),Y(.52f),X(.85f),Y(.73f)),p); p.setColor(Color.WHITE);c.drawCircle(X(.765f),Y(.58f),6*density,p);text(c,"🐟",19,Color.WHITE,.765f,.66f);
        fill(c,Color.rgb(93,73,115),.15f,.69f,.55f,.78f,32);p.setColor(Color.rgb(188,174,205));c.drawCircle(X(.32f),Y(.65f),34*density,p);text(c,"Z z",14,Color.rgb(48,39,66),.32f,.66f);
        fill(c,Color.rgb(255,206,71),.55f,.69f,.64f,.77f,40);text(c,"!",22,Color.rgb(130,80,20),.595f,.745f);
        text(c,"НЕ ЗВОНИ!",11,Color.rgb(255,228,151),.595f,.81f);
      }
      // player orb / object being thrown
      p.setColor(Color.rgb(86,239,191)); c.drawCircle(X(x),Y(y),22*density,p);p.setColor(Color.WHITE);c.drawCircle(X(x-.007f),Y(y-.008f),6*density,p);text(c,level==1?"●":"★",18,Color.rgb(23,83,89),x,y+.01f);
    }
    void step(){ long now=System.currentTimeMillis(); float dt=Math.min(.035f,(now-last)/1000f);last=now; vy+=.70f*dt; x+=vx*dt; y+=vy*dt;
      if(x<.08f||x>.92f){vx=-vx*.7f; x=Math.max(.08f,Math.min(.92f,x));} if(y<.28f){vy=-vy*.7f;y=.28f;}
      if(level==1){ if(x>.57f&&x<.86f&&y>.71f&&y<.82f&&vy>0) finish(true,"Ваза спасена! ✨"); else if(y>.86f) finish(false,"БАБАХ! Ваза в щепки"); }
      else { if(x>.68f&&x<.86f&&y>.51f&&y<.74f) finish(true,"Рыбка в безопасности! 🐟"); else if(x>.51f&&x<.68f&&y>.65f&&y<.80f) finish(false,"ДЗЫНЬ! Кот проснулся"); else if(y>.86f) finish(false,"Плюх… слишком поздно"); }
    }
    void finish(boolean yes,String message){won=yes;lost=!yes;frozen=true;banner=message;for(int i=0;i<18;i++)sparks.add(new Spark(X(x),Y(y),yes)); invalidate();}
    void overlay(Canvas c){fill(c,Color.argb(218,12,16,37),.10f,.34f,.90f,.72f,30);text(c,banner,23,won?Color.rgb(105,255,193):Color.rgb(255,142,157),.5f,.445f);text(c,won?"Одна секунда. Идеально!":"Попробуй другой угол",15,Color.WHITE,.5f,.50f); restart.set(X(.23f),Y(.56f),X(.77f),Y(.65f));p.setColor(won?Color.rgb(60,196,146):Color.rgb(244,88,118));c.drawRoundRect(restart,20*density,20*density,p);text(c,won&&level==1?"СЛЕДУЮЩИЙ УРОВЕНЬ":"ЕЩЁ РАЗ",15,Color.WHITE,.5f,.615f);}
    @Override public boolean onTouchEvent(android.view.MotionEvent e){
      if(e.getAction()==MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();return true;}
      if(e.getAction()==MotionEvent.ACTION_UP){
        if((won||lost)&&restart.contains(e.getX(),e.getY())){if(won&&level==1)level=2;reset();return true;}
        if(frozen&&!won&&!lost){float dx=e.getX()-downX,dy=e.getY()-downY;float len=(float)Math.sqrt(dx*dx+dy*dy);if(len>20*density){vx=dx/len*.70f;vy=dy/len*.70f;frozen=false;last=System.currentTimeMillis();invalidate();}return true;}
      }return true;
    }
    static class Spark {float x,y,vx,vy;int color; Spark(float a,float b,boolean ok){x=a;y=b;double r=Math.random()*6.28;float s=(float)(2+Math.random()*6);vx=(float)Math.cos(r)*s;vy=(float)Math.sin(r)*s;color=ok?Color.rgb(255,225,80):Color.rgb(255,107,125);}void draw(Canvas c,Paint p){p.setColor(color);c.drawCircle(x+=vx,y+=vy,5,p);vy+=.2f;}}
  }
}
