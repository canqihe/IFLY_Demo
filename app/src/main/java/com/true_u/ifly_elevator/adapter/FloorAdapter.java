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
import com.true_u.ifly_elevator.bean.FloorBean;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.RequiresApi;
import androidx.cardview.widget.CardView;

/**
 * Created by Colin
 * on 2020/4/27
 * E-mail: hecanqi168@gmail.com
 */
public class FloorAdapter extends BaseAdapter {
    Context context;
    List<Integer> indexList;
    List<FloorBean.DataBean> dataBeanList;
    int Devflag;//0设备 1串口

    public FloorAdapter(Context context) {
        this.context = context;
    }

    public void updateData(List<FloorBean.DataBean> list, List<Integer> floorList, int flag) {
        this.dataBeanList = list;
        this.indexList = floorList;
        this.Devflag = flag;
    }


    @Override
    public int getCount() {
        if (dataBeanList != null)
            return dataBeanList.size();
        else return 0;
    }

    @Override
    public Object getItem(int i) {
        if (dataBeanList != null && dataBeanList.size() > i) {
            return dataBeanList.get(i);
        }
        return null;
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
        holder.text.setText(dataBeanList.get(position).getNumberFloor() + "");
        holder.text.setTextColor(Color.parseColor("#666666"));
        holder.cardView.setBackground(context.getDrawable(R.drawable.radius_font));

        if (indexList.size() != 0) {
            if (Devflag == 0) {
                for (int i = 0; i < indexList.size(); i++) {
                    if (indexList.get(i) == dataBeanList.get(position).getNumberFloor()) {
                        holder.text.setTextColor(Color.parseColor("#ff6510"));
                        holder.cardView.setBackground(context.getDrawable(R.drawable.radius_floor));
                    }
                }
            } else {
                for (int i = 0; i < indexList.size(); i++) {
                    if (indexList.get(i) == position) {
                        holder.text.setTextColor(Color.parseColor("#ff6510"));
                        holder.cardView.setBackground(context.getDrawable(R.drawable.radius_floor));
                    }
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
