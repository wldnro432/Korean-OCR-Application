package com.example.msocr;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.content.CursorLoader;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImagePreprocessing extends AppCompatActivity {

    final int REQUEST_IMAGE_CODE = 2;
    final int REQUEST_CAMERA_CODE = 3;
    final int CAMERA_OPEN = 1;
    final int ALBUM_OPEN = 2;
    final int SEND_TO_RESULT_PAGE = 143;

    ImageView imageview_processing;
    Button button_processing, button_back;
    Uri selectedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preprocessing);

        imageview_processing = findViewById(R.id.imageview_processing);
        button_processing = findViewById(R.id.button_processing);
        button_back = findViewById(R.id.button_back);

        Intent intent1 = getIntent();
        int number = intent1.getIntExtra("number",0);

        if(number == CAMERA_OPEN) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, REQUEST_CAMERA_CODE);
        }
        else if(number == ALBUM_OPEN) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Get Album"), REQUEST_IMAGE_CODE);
        }

        imageview_processing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSelectImageClick(imageview_processing);
            }
        });

        button_processing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Bitmap c = ((GlideBitmapDrawable) imageview_processing.getDrawable().getCurrent()).getBitmap();
                    Intent intent = new Intent(ImagePreprocessing.this, ResultActivity.class);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    c.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byte[] byteArray = stream.toByteArray();

                    intent.putExtra("image", byteArray);
                    intent.putExtra("number", SEND_TO_RESULT_PAGE);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        button_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent2 = new Intent(ImagePreprocessing.this, MainActivity.class);
                startActivity(intent2);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CODE ) {
            if (resultCode == RESULT_OK) {

                if (data == null) {
                    return;
                }
                selectedImage = data.getData();
                try {
                    Glide.with(getApplicationContext()).load(selectedImage).into(imageview_processing);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "사진 선택 취소", Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == REQUEST_CAMERA_CODE) {
            if (resultCode == RESULT_OK) {
                try {
                    selectedImage = data.getData();
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap)extras.get("data");
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                    String path = MediaStore.Images.Media.insertImage(getContentResolver(), imageBitmap, "Title", null);
                    Glide.with(getApplicationContext()).load(Uri.parse(path)).into(imageview_processing);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "오류", Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Glide.with(getApplicationContext()).load(result.getUri()).override(500,500).into(imageview_processing);
            }
        }

    }

    public void onSelectImageClick(View view) {
        Bitmap c = ((GlideBitmapDrawable)imageview_processing.getDrawable().getCurrent()).getBitmap();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        c.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), c, "Title", null);
        CropImage.activity(Uri.parse(path)).setGuidelines(CropImageView.Guidelines.ON).start(this);
    }
}