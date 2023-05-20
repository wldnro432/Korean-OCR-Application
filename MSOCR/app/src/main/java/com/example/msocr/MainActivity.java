package com.example.msocr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity{

    final int REQUEST_PERMISSION = 9;
    final int SAVE_TO_MAIN_PAGE = 333;
    final int CAMERA_OPEN = 1;
    final int ALBUM_OPEN = 2;
    private ArrayList<SaveItem> save_data = null;

    Button button_camera, button_album, button_delete;
    ImageView imageView_main;
    ListView listView;
    Adapter adapter;

    @Override
    public void onResume() {
        super.onResume();
        checkPermission();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button_camera = findViewById(R.id.button_camera);
        button_album = findViewById(R.id.button_album);

        imageView_main = findViewById(R.id.imageView_main);
        listView = findViewById(R.id.listView);

        try {
            Intent intent = getIntent();
            int number = intent.getIntExtra("number",0);
            if(number == SAVE_TO_MAIN_PAGE) {
                byte[] b = intent.getByteArrayExtra("image");

                String get_save_text = intent.getStringExtra("text1");
                String get_trans_text = intent.getStringExtra("text2");

                save_data = new ArrayList<>();
                SaveItem save = new SaveItem(b, get_save_text, get_trans_text);
                save_data.add(save);
                adapter = new Adapter(this, R.layout.item, save_data);
                listView.setAdapter(adapter);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), SaveDetail.class);
                intent.putExtra("image", save_data.get(position).getImage());
                intent.putExtra("text1", save_data.get(position).getText1());
                intent.putExtra("text2", save_data.get(position).getText2());
                adapter.setCheck(position);
                adapter.notifyDataSetChanged();
                startActivity(intent);

            }
        });

        button_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ImagePreprocessing.class);
                intent.putExtra("number",CAMERA_OPEN);
                startActivity(intent);
            }
        });

        button_album.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ImagePreprocessing.class);
                intent.putExtra("number",ALBUM_OPEN);
                startActivity(intent);
            }
        });
    }

    //  권한 확인 --------------------------------------------------------------------------------
    public void checkPermission() {
        int permissionCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);                 // 카메라권한
        int permissionRead = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);    // 외부저장소 읽기 권한
        int permissionWrite = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);  // 외부저장소 쓰기 권한

        //권한이 없으면 권한 요청
        if (permissionCamera != PackageManager.PERMISSION_GRANTED
                || permissionRead != PackageManager.PERMISSION_GRANTED
                || permissionWrite != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                Toast.makeText(this, "이 앱을 실행하기 위해 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(this, "권한 확인", Toast.LENGTH_LONG).show();

                } else {
                    Toast.makeText(this, "권한 없음", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }
}