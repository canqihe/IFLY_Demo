package com.true_u.ifly_elevator.adapter;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.true_u.ifly_elevator.R;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.RequiresApi;
import androidx.cardview.widget.CardView;

/**
 * Created by Colin
 * on 2020/4/27
 * E-mail: hecanqi168@gmail.com
 * Copyright (C) 2018 SSZB, Inc.
 */
public class FloorAdapter extends BaseAdapter {
    private Context context;
    private List<Integer> mList = new ArrayList();
    private int floors;

    public FloorAdapter(Context context, List<Integer> list, int floors) {
        this.context = context;
        this.mList = list;
        this.floors = floors;
    }


    @Override
    public int getCount() {
        return floors;
    }

    @Override
    public Object getItem(int i) {
        return i;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        ViewHolder holder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_floor, null);
            holder = new ViewHolder();
            holder.text = convertView.findViewById(R.id.floor);
            holder.cardView = convertView.findViewById(R.id.cardview);
            holder.relativeLayout = convertView.findViewById(R.id.rel);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.text.setText((position + 1) + "");
        holder.cardView.setBackground(context.getDrawable(R.drawable.radius_font));
        if (mList.size() != 0) {
            for (int i = 0; i < mList.size(); i++) {
                if (mList.get(i) - 1 == position) {
                    holder.text.setTextColor(Color.parseColor("#ff6510"));
                    holder.cardView.setBackground(context.getDrawable(R.drawable.radius_floor));
//                    holder.text.setShadowLayer(5, 0, 0, Color.parseColor("#ff6510"));
                }
            }
        }
        return convertView;
    }

    class ViewHolder {
        TextView text;
        RelativeLayout relativeLayout;
        CardView cardView;
    }
}
