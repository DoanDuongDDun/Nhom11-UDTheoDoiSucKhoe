package com.example.vitality.graphics;
import android.content.Context;
import android.util.AttributeSet;
import android.graphics.*;
import android.view.View;
import java.util.*;
public class TrendView extends View  {
    private final List<Long> times=new ArrayList<>();
    private final List<Double> values=new ArrayList<>();
    private final Paint p=new Paint(3);
    public TrendView(Context c,AttributeSet a) {
        super(c,a);
    }
    public void setData(List<Long> t,List<Double> v) {
        times.clear();
        values.clear();
        // Measurement history arrives newest first; draw every series oldest first.
        List<Integer> order=new ArrayList<>();
        for(int i=0;i<t.size();i++)order.add(i);
        order.sort(Comparator.comparingLong(t::get));
        for(int index:order) {
            times.add(t.get(index));
            values.add(v.get(index));
        }
        invalidate();
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        p.setTextSize(30);
        p.setColor(0xff747B88);
        if(values.isEmpty()) {
            c.drawText("Chưa ghi nhận trong khoảng này",16,50,p);
            return;
        }
        double min=Collections.min(values),max=Collections.max(values);
        long start=Collections.min(times),end=Collections.max(times);
        float left=70,bottom=getHeight()-38,w=getWidth()-90,h=getHeight()-70;
        p.setStrokeWidth(2);
        c.drawLine(left,20,left,bottom,p);
        c.drawLine(left,bottom,getWidth()-15,bottom,p);
        p.setTextSize(24);
        c.drawText(String.format(Locale.getDefault(),"%.0f",max),0,30,p);
        c.drawText(String.format(Locale.getDefault(),"%.0f",min),0,bottom,p);
        p.setTextSize(20);
        c.drawText(com.example.vitality.utils.Formats.day(start),left,getHeight()-8,p);
        if(end!=start) {
            String endLabel=com.example.vitality.utils.Formats.day(end);
            c.drawText(endLabel,getWidth()-p.measureText(endLabel)-4,getHeight()-8,p);
        }
        p.setColor(0xffD64545);
        Path trend=new Path();
        float[] xs=new float[values.size()],ys=new float[values.size()];
        for(int i=0;i<values.size();i++) {
            float x=left+(end==start?w/2:(times.get(i)-start)*w/(end-start));
            float y=bottom-(max==min?h/2:(float)((values.get(i)-min)*h/(max-min)));
            xs[i]=x;
            ys[i]=y;
            if(i==0)trend.moveTo(x,y);
            else trend.lineTo(x,y);
        }
        float density=getResources().getDisplayMetrics().density;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2*density);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeCap(Paint.Cap.ROUND);
        c.drawPath(trend,p);
        p.setStyle(Paint.Style.FILL);
        for(int i=0;i<xs.length;i++) {
            p.setColor(Color.WHITE);
            c.drawCircle(xs[i],ys[i],4*density,p);
            p.setColor(0xffD64545);
            c.drawCircle(xs[i],ys[i],2.7f*density,p);
        }
    }
}
