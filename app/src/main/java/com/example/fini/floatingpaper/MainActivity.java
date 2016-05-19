package com.example.fini.floatingpaper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;

public class MainActivity extends Activity implements OnClickListener {
    private final static int OVERLAY_PERMISSION=0;      //
    private AlertDialog.Builder wantOverlayPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //메모지 on-off 버튼
        findViewById(R.id.onbutton).setOnClickListener(this);
        findViewById(R.id.offbutton).setOnClickListener(this);

        /*
        권한요청 대화상자
        요청에 응할 경우, 해당 권한 설정 페이지를 연다
        */
        wantOverlayPermission = new AlertDialog.Builder(MainActivity.this);
        wantOverlayPermission.setTitle("오버레이 권한 필요");
        wantOverlayPermission.setMessage("최상위 화면에 쪽지를 띄우기 위해 오버레이 권한을 허가해주세요.");
        wantOverlayPermission.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                requestOverlayPermission();
            }
        });
        wantOverlayPermission.setNegativeButton("NO", null);

    }

    //on-off 버튼 행동 정의
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.onbutton :
                //권한 없으면 달라고하고
                if( !checkOverlayPermission() )
                    wantOverlayPermission.show();
                //권한 있으면 서비스 시작
                else
                    startService(new Intent(this, FloatingPaperService.class));
            break;

            case R.id.offbutton :
                //서비스 종료
                stopService(new Intent(this, FloatingPaperService.class));
            break;
        }

    }

    //오버레이 권한을 가졌는지 여부를 리턴
    public boolean checkOverlayPermission() {
        //SDK 버전이 낮으면 매니페스트에 쓴 것으로 충분하므로 true를 리턴.
        if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M)
            return true;

        //권한 확보 여부를 검사하여 리턴
        return Settings.canDrawOverlays(this);
    }

    //권한 설정 페이지로 이동
    public void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, OVERLAY_PERMISSION);
    }

    //설정 페이지에서 되돌아왔을 때
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            //어떤 권한을 얻으려 했는가?
            case OVERLAY_PERMISSION:
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                {
                    //권한 획득이 안 되었으면 아무것도 하지 않는다
                    if (!Settings.canDrawOverlays(this))
                        ;
                    //권한을 얻었으면 서비스를 시작한다
                    else
                        startService(new Intent(this, FloatingPaperService.class));
                }
                break;
        }
    }
}
