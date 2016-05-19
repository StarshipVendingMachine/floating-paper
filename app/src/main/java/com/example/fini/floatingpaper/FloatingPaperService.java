package com.example.fini.floatingpaper;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.*;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.*;
import android.view.View.OnTouchListener;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import java.util.ArrayList;

public class FloatingPaperService extends Service {
    private RelativeLayout paperLayout;                 //화면에 띄울 종이
    private View resetbutton, edge;                    //초기화버튼, 크기조절 버튼
    private WindowManager.LayoutParams paperParams;     //layout params 객체. 뷰의 위치 및 크기를 지정하는 객체
    private WindowManager paperWindowManeger;		   //윈도우 매니저
    private SeekBar paperSeekBar;                       //투명도 조절 seek bar
    private DisplayMetrics matrix;                       //화면 크기를 구하기 위해 사용

    private FreeLine linedata;                          //낙서 내용을 저장할 클래스
    private ArrayList<Vertex> arVertex;


    private float START_X, START_Y;				//움직이기 위해 터치한 시작 점
    private int PREV_X, PREV_Y;					//움직이기 이전에 뷰가 위치한 점
    private int MAX_X, MAX_Y;					//뷰의 위치 최대 값
    private float LIMIT_X, LIMIT_Y;               //크기 제한
    private static final int MIN_SIZE=400;      //뷰의 최소 크기 지정
    private static final int MIN_OPACITY = 10;  //투명도 최소값 지정

   public void onCreate() {
        super.onCreate();

       linedata = new FreeLine(this);
       arVertex = new ArrayList<Vertex>();
       paperWindowManeger = (WindowManager) getSystemService(WINDOW_SERVICE);

       //XML에서 다 만들어둔 것을 꺼내다가 리스너를 붙이고 사용하자
       LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
       paperLayout = (RelativeLayout) inflater.inflate(R.layout.floatingpaper, null);

       //앞에서부터 child를 꺼낸다
       paperSeekBar = (SeekBar) paperLayout.getChildAt(0);
       paperLayout.removeViewAt(0);

       resetbutton = (View) paperLayout.getChildAt(0);
       paperLayout.removeViewAt(0);

       edge = (View) paperLayout.getChildAt(0);
       paperLayout.removeViewAt(0);

       //투명도 조절기 설정
       settingOpacityController();

       //이동 리스너 등록
       paperLayout.setOnTouchListener(PaperMoveListener);

       //초기화 리스너 등록
       resetbutton.setOnClickListener(PaperReset);

       //화면을 다 덮지 못하게 크기 제한
       setLimitSize();
       //크기 조절 리스너 등록
       edge.setOnTouchListener(PaperSizeChanger);

       //세팅이 끝난 뷰들을 재조립
       paperLayout.addView(linedata);
       paperLayout.addView(paperSeekBar);
       paperLayout.addView(edge);
       paperLayout.addView(resetbutton);

       //최상위 윈도우에 넣기 위한 설정
       paperParams = new WindowManager.LayoutParams(
               MIN_SIZE,
               MIN_SIZE,
               WindowManager.LayoutParams.TYPE_PHONE,					//항상 최 상위에 있게. 터치 이벤트 받을 수 있음.
               WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,		//자기 영역 밖터치는 인식 안하도록
               PixelFormat.TRANSLUCENT);								//투명
       paperParams.gravity = Gravity.LEFT | Gravity.TOP;			//왼쪽 상단에 위치하게 함.

       //최상위 윈도우에 뷰 넣기
       paperWindowManeger.addView(paperLayout, paperParams);

       //화면 밖으로 이동하지 않도록 이동 가능한 최대좌표 설정
       setMaxPosition();
    }

    //점들의 좌표
    public class Vertex {
        Vertex(float inx, float iny, boolean ind) {
            x = inx;
            y = iny;
            Draw = ind;
        }
        float x;
        float y;
        boolean Draw;
    }

    //낙서장
    protected class FreeLine extends View {
        Paint freeLinePainter;

        public FreeLine(Context context) {
            super(context);

            // Paint 객체 미리 초기화
            freeLinePainter = new Paint();
            freeLinePainter.setColor(Color.BLACK);
            freeLinePainter.setStrokeWidth(3);
            freeLinePainter.setAntiAlias(true);
        }

        public void onDraw(Canvas canvas) {
            canvas.drawColor(Color.LTGRAY);

            // 점들을 이어 그린다
            for (int i=0;i<arVertex.size();i++) {
                if (arVertex.get(i).Draw) {
                    canvas.drawLine(arVertex.get(i-1).x, arVertex.get(i-1).y,
                            arVertex.get(i).x, arVertex.get(i).y, freeLinePainter);
                }
            }
        }

        // 터치 이동시마다 점 추가
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                arVertex.add(new Vertex(event.getX(), event.getY(), false));
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                arVertex.add(new Vertex(event.getX(), event.getY(), true));
                invalidate();
                return true;
            }
            return false;
        }
    }

    //이동 처리 리스너
    private OnTouchListener PaperMoveListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch(event.getAction()) {
                case MotionEvent.ACTION_DOWN:				//사용자 터치 다운이면
                    START_X = event.getRawX();					//터치 시작 점
                    START_Y = event.getRawY();					//터치 시작 점
                    PREV_X = paperParams.x;					//뷰의 시작 점
                    PREV_Y = paperParams.y;					//뷰의 시작 점
                    break;
                case MotionEvent.ACTION_MOVE:
                    int x = (int)(event.getRawX() - START_X);	//이동한 거리
                    int y = (int)(event.getRawY() - START_Y);	//이동한 거리

                    //터치해서 이동한 만큼 이동 시킨다
                    paperParams.x = PREV_X + x;
                    paperParams.y = PREV_Y + y;

                    bindInBoundOfScreen();		//뷰의 위치 확인
                    paperWindowManeger.updateViewLayout(paperLayout, paperParams);	//뷰 업데이트
                    break;
            }

            return true;
        }
    };

    //크기 갱신 리스너
    private OnTouchListener PaperSizeChanger = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch(event.getAction()) {
                case MotionEvent.ACTION_DOWN:				//사용자 터치 다운이면
                    START_X = event.getRawX();					//터치 시작 점
                    START_Y = event.getRawY();					//터치 시작 점
                    PREV_X = paperParams.width;				//뷰의 너비
                    PREV_Y = paperParams.height;				//뷰의 높이
                    break;

                case MotionEvent.ACTION_MOVE:
                    int x = (int)(event.getRawX() - START_X);	//이동한 거리
                    int y = (int)(event.getRawY() - START_Y);	//이동한 거리

                    //터치해서 이동한 만큼 크기 조정
                    paperParams.width = PREV_X + x;
                    paperParams.height = PREV_Y + y;

                    //일정 크기 아래로는 작아지지 않는다.
                    protectMinimumSize();
                    //화면을 다 덮을만큼 커지지 않는다
                    limitMaximumSize();

                    //뷰 업데이트
                    paperWindowManeger.updateViewLayout(paperLayout, paperParams);
                    setMaxPosition();
                    break;
            }
            return true;
        }
    };

    //쪽지 초기화 리스너
    private View.OnClickListener PaperReset = new View.OnClickListener() {
        public void onClick(View v){
            switch (v.getId()) {
                case R.id.resetbutton:
                    arVertex = new ArrayList<Vertex>();
                    linedata.invalidate();
                    break;
            }
        }
    };


    @Override
    public IBinder onBind(Intent arg0) { return null; }

    //뷰의 위치가 화면 밖에 나가지 않도록 위치 정보 제한
    private void setMaxPosition()
    {
        MAX_X = matrix.widthPixels - paperLayout.getWidth();
        MAX_Y = matrix.heightPixels - paperLayout.getHeight();
    }

    //쪽지가 화면 밖으로 나간 경우, 안으로 끌고 온다
    private void bindInBoundOfScreen()
    {
        if(paperParams.x > MAX_X) paperParams.x = MAX_X;
        if(paperParams.y > MAX_Y) paperParams.y = MAX_Y;
        if(paperParams.x < 0) paperParams.x = 0;
        if(paperParams.y < 0) paperParams.y = 0;
    }

    //쪽지의 가로세로 길이의 최소값 제한
    private void protectMinimumSize()
    {
        if(paperParams.width < MIN_SIZE) paperParams.width = MIN_SIZE;
        if(paperParams.height < MIN_SIZE) paperParams.height = MIN_SIZE;
    }

    //쪽지의 최대크기 제한
    private void setLimitSize()
    {
        matrix = new DisplayMetrics();

        //화면 정보를 얻음
        paperWindowManeger.getDefaultDisplay().getMetrics(matrix);

        //화면크기를 기준으로 쪽지의 제한크기 설정
        LIMIT_X = matrix.widthPixels * 0.9f;
        LIMIT_Y = matrix.heightPixels * 0.9f;
    }

    //크기 제한 체크
    private void limitMaximumSize()
    {
        if(paperParams.height > LIMIT_Y)
            paperParams.height = (int)LIMIT_Y;

        if(paperParams.width > LIMIT_X)
            paperParams.width = (int)LIMIT_X;
    }


    // 투명도 조절 컨트롤러를 설정
    private void settingOpacityController() {
        paperSeekBar.setMax(100);					//최대값 설정.
        paperSeekBar.setProgress(100);			//현재 투명도 설정. 100:불투명, 0은 완전 투명
        paperSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override public void onProgressChanged(SeekBar seekBar, int progress,	boolean fromUser) {
                //최소 투명도 제한
                if(progress < MIN_OPACITY)
                {
                    progress = MIN_OPACITY;
                    seekBar.setProgress(MIN_OPACITY);
                }
                //알파값 설정하여 적용
                paperParams.alpha = progress / 100.0f;
                paperWindowManeger.updateViewLayout(paperLayout, paperParams);
            }
        });
    }

    //장비 회전시 다시 그리기
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        //최대값 다시 설정
        setLimitSize();

        //새로운 최대값에 맞게 크기 재설정
        limitMaximumSize();

        //초기위치인 좌상단으로 옮김
        paperParams.gravity = Gravity.LEFT | Gravity.TOP;

        //뷰 업데이트
        paperWindowManeger.updateViewLayout(paperLayout, paperParams);
    }

    @Override
    public void onDestroy() {
        //서비스 종료시 뷰 제거
        if(paperWindowManeger != null) {
            if(paperLayout != null) paperWindowManeger.removeView(paperLayout);
        }
        super.onDestroy();
    }
}