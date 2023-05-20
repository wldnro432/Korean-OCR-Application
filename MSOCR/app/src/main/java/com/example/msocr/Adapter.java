package com.example.msocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class Adapter extends BaseAdapter {
    private LayoutInflater inflater;
    private ArrayList<SaveItem> data; //Item 목록을 담을 배열
    private int layout;
    boolean checkarray[];

    public Adapter(Context context, int layout, ArrayList<SaveItem> data) {
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.data = data;
        this.layout = layout;

        if (data.size()>0) {
            checkarray = new boolean[data.size()];
        }
    }

    @Override
    public int getCount() { //리스트 안 Item의 개수를 센다.
        return data.size();
    }

    @Override
    public String getItem(int position) {
        return data.get(position).getText1();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public boolean getChecked (int position) {
        return checkarray[position];
    }

    public void setCheck(int position) {
        checkarray[position] = !checkarray[position];
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(layout, parent, false);
        }
        SaveItem saveItem = data.get(position);

        //이미지 파일 연동
        ImageView profile = (ImageView) convertView.findViewById(R.id.image_item);

        byte[] b = saveItem.getImage();
        Bitmap get_save_image = BitmapFactory.decodeByteArray(b,0,b.length);
        profile.setImageBitmap(get_save_image);

        //이름 등 정보 연동
        TextView info = (TextView) convertView.findViewById(R.id.text_item1);
        info.setText(saveItem.getText1());

        //전화번호 연동
        TextView phone = (TextView) convertView.findViewById(R.id.text_item2);
        phone.setText(saveItem.getText2());

        return convertView;
    }
}