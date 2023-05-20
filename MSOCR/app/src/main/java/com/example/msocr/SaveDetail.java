package com.example.msocr;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

public class SaveDetail extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_detail);

        Intent intent = getIntent();

        ImageView detail_image = (ImageView) findViewById(R.id.detail_image);
        TextView detail_text1=(TextView) findViewById(R.id.detail_text_1);
        TextView detail_text2=(TextView) findViewById(R.id.detail_text_2);

        byte[] b = intent.getByteArrayExtra("image");
        Bitmap get_save_image = BitmapFactory.decodeByteArray(b,0,b.length);

        detail_image.setImageBitmap(get_save_image);
        detail_text1.setText(intent.getStringExtra("text1"));
        detail_text2.setText(intent.getStringExtra("text2"));
    }
}