package com.example.msocr;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.content.CursorLoader;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.request.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;


public class ResultActivity extends AppCompatActivity {

    final int REQUEST_IMAGE_CODE = 2;
    final int REQUEST_RESULT_CODE = 4;
    final int SAVE_TO_MAIN_PAGE = 333;
    final int SEND_TO_RESULT_PAGE = 143;
    final String text_down_url = "https://${your_server_url}/east";

    private Bitmap static_send_image;
    public static Bitmap bitmap_new;

    private static final String TAG = ResultActivity.class.getSimpleName();

    Button button_save, button_next, button_run, button_trans;
    ImageView imageView_result;
    TextView  textView_result, textView_trans;

    String clientId = "clientId";//애플리케이션 클라이언트 아이디값";
    String clientSecret = "clientSecret";//애플리케이션 클라이언트 시크릿값";
    String apiURL = "apiURL";
    String text;
    String getresult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        button_save = findViewById(R.id.button_save);
        button_next = findViewById(R.id.button_next);
        button_run = findViewById(R.id.button_run);
        button_trans = findViewById(R.id.button_trans);

        imageView_result = findViewById(R.id.imageView_result);
        textView_result = findViewById(R.id.textView_result);
        textView_trans = findViewById(R.id.textView_trans);

        try {
            Intent intent = getIntent();
            int number = intent.getIntExtra("number",0);
            if(number == SEND_TO_RESULT_PAGE) {
                byte[] byteArray = intent.getByteArrayExtra("image");
                Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                imageView_result.setImageBitmap(bitmap);
                Bitmap bitmap2 = ((BitmapDrawable) imageView_result.getDrawable()).getBitmap();
                static_send_image = bitmap2;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        button_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(ResultActivity.this, MainActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        button_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    BitmapDrawable c = (BitmapDrawable) ((ImageView) findViewById(R.id.imageView_result)).getDrawable();

                    Intent intent = new Intent(ResultActivity.this, MainActivity.class);

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    Bitmap bitmap = c.getBitmap();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byte[] byteArray = stream.toByteArray();

                    intent.putExtra("image", byteArray); // bitmap image -> bytes로 변환후 (문자열이 됌) 전송 후 다시 받기
                    intent.putExtra("text1", textView_result.getText().toString()); // 스트링이니 그냥 전송후 setText
                    intent.putExtra("text2", textView_trans.getText().toString());
                    intent.putExtra("number", SAVE_TO_MAIN_PAGE);
                    startActivity(intent);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        button_run.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadPicture(getStringImage(static_send_image));
            }
        });

        button_trans.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ResultActivity.Translate translate = new ResultActivity.Translate();
                translate.execute(); //버튼 클릭시 ASYNC 사용
            }
        });
    }

    private void uploadPicture(final String send_to_server_image)
    {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading...");
        progressDialog.show();
        progressDialog.setCancelable(true);

        JSONObject postBody = new JSONObject();

        try {
            postBody.put("image", send_to_server_image);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonRequest = new JsonObjectRequest (Request.Method.POST, text_down_url, postBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response)
            {
                Log.e(TAG, response.toString());
                try {
                    String response_image = response.getString("img");
                    byte[] encodeByte = Base64.decode(response_image, Base64.DEFAULT);
                    bitmap_new = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
                    imageView_result.setImageBitmap(bitmap_new);

                    String response_text = response.getString("text");
                    textView_result.setText(response_text);

                    progressDialog.dismiss();
                    Toast.makeText(ResultActivity.this, "Success!", Toast.LENGTH_SHORT).show();

                } catch (JSONException e) {
                    e.printStackTrace();
                    progressDialog.dismiss();
                    Toast.makeText(ResultActivity.this, "Try Again! error : " + e.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(com.android.volley.error.VolleyError error) {
                progressDialog.dismiss();
                Toast.makeText(ResultActivity.this, "Error : " + error.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        jsonRequest.setShouldCache(false);
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonRequest);
    }

    public String getStringImage(Bitmap bitmap)
    {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] imageByteArray = byteArrayOutputStream.toByteArray();
        String encodedImage = Base64.encodeToString(imageByteArray, Base64.DEFAULT);
        return encodedImage;
    }

    class Translate extends AsyncTask<String ,Void, String > {


        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override

        protected String doInBackground(String... strings) {

            try {
                text = URLEncoder.encode(textView_result.getText().toString(), "UTF-8");  /// 번역할 문장 Edittext  입력

                URL url = new URL(apiURL);
                HttpURLConnection con = (HttpURLConnection)url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("X-Naver-Client-Id", clientId);
                con.setRequestProperty("X-Naver-Client-Secret", clientSecret);
                String postParams = "source=ko&target=en&text=" + text;

                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(postParams);
                wr.flush();
                wr.close();
                int responseCode = con.getResponseCode();
                BufferedReader br;
                if(responseCode==200) { // 정상 호출
                    br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                } else {  // 에러 발생
                    br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                }
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = br.readLine()) != null) {
                    response.append(inputLine);
                }
                br.close();
                System.out.println(response.toString());
                getresult = response.toString();

                textView_trans.setText(getresult);

            } catch (Exception e) {
                System.out.println(e);
            }
            return null;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CODE) {
            if (resultCode == RESULT_OK) {
                if (data == null) {
                    return;
                }
                Uri selectedImage = data.getData();
                try {
                    InputStream in = getContentResolver().openInputStream(selectedImage);
                    Bitmap img = BitmapFactory.decodeStream(in);

                    img = GetRotatedBitmap(img, getOrientation(getPath(selectedImage)));
                    in.close();
                    imageView_result.setImageBitmap(img);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "사진 선택 취소", Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == REQUEST_RESULT_CODE) {
            if (resultCode == RESULT_OK) {
                try {
                    Bitmap image = (Bitmap) data.getExtras().get("image");
                    imageView_result.setImageBitmap(image);

                } catch (Exception e) {

                }
            }
        }
    }

    public int getOrientation(String path) {
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(path);
        } catch(IOException e){
            e.printStackTrace();
        }

        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return(90);

            case ExifInterface.ORIENTATION_ROTATE_180:
                return(180);

            case ExifInterface.ORIENTATION_ROTATE_270:
                return(270);
        }
        return 0;
    }


    private String getPath(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };

        CursorLoader cursorLoader = new CursorLoader(this, contentUri, proj, null, null, null);
        Cursor cursor = cursorLoader.loadInBackground();

        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }


    public synchronized static Bitmap GetRotatedBitmap(Bitmap bitmap, int degrees) {
        if (degrees != 0 && bitmap != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);
            //          받아온 degrees 만큼 회전 -> 90도 회전시켜서 보여줌
            //          px, py 기준으로 회전
            try {
                Bitmap b2 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                //
                if (bitmap != b2) {
                    // b2와 다르면 , b2를 비트맵이미지에 덮어씌움
                    // bitmap = 받아온 이미지 GetRotatedBitmap(bitmap)
                    bitmap = b2;
                }
            } catch (OutOfMemoryError ex) {
                ex.printStackTrace();
            }
        }
        return bitmap;
    }
}