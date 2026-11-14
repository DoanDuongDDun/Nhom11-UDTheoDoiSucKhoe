package com.example.vitality.graphics;
import android.content.Context;
import android.util.AttributeSet;
import android.graphics.*;
import android.view.View;
import java.util.*;
import com.example.vitality.utils.Formats;
public class TrendView extends View  {
    private static final int AXIS=0xff747B88,LINE=0xff2E8C68;
    // Dữ liệu đã sắp theo thời gian (cũ → mới) và các giá trị dẫn xuất tính một lần trong setData, không tính lại ở mỗi lần vẽ.
    private long[] times=new long[0];
    private double[] values=new double[0];
    private double min,max;
    private long start,end;
    private String maxLabel="",minLabel="",startLabel="",endLabel="";
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Path trend=new Path();
    private final float density;
    private float[] xs=new float[0],ys=new float[0];
    public TrendView(Context c,AttributeSet a) {
        super(c,a);
        density=getResources().getDisplayMetrics().density;
    }
    public void setData(List<Long> t,List<Double> v) {
        int n=t.size();
        // Measurement history arrives newest first; draw every series oldest first.
        Integer[] order=new Integer[n];
        for(int i=0;i<n;i++)order[i]=i;
        Arrays.sort(order,Comparator.comparingLong(t::get));
        times=new long[n];
        values=new double[n];
        xs=new float[n];
        ys=new float[n];
        for(int i=0;i<n;i++) {
            times[i]=t.get(order[i]);
            values[i]=v.get(order[i]);
        }
        if(n>0) {
            min=Double.POSITIVE_INFINITY;
            max=Double.NEGATIVE_INFINITY;
            for(double x:values) {
                if(x<min)min=x;
                if(x>max)max=x;
            }
            start=times[0];
            end=times[n-1];
            maxLabel=String.format(Locale.getDefault(),"%.0f",max);
            minLabel=String.format(Locale.getDefault(),"%.0f",min);
            startLabel=Formats.day(start);
            endLabel=end!=start?Formats.day(end):"";
        }
        invalidate();
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        p.setStyle(Paint.Style.FILL);
        p.setTextSize(30);
        p.setColor(AXIS);
        if(values.length==0) {
            c.drawText("Chưa ghi nhận trong khoảng này",16,50,p);
            return;
        }
        float left=70,bottom=getHeight()-38,w=getWidth()-90,h=getHeight()-70;
        p.setStrokeWidth(2);
        c.drawLine(left,20,left,bottom,p);
        c.drawLine(left,bottom,getWidth()-15,bottom,p);
        p.setTextSize(24);
        c.drawText(maxLabel,0,30,p);
        c.drawText(minLabel,0,bottom,p);
        p.setTextSize(20);
        c.drawText(startLabel,left,getHeight()-8,p);
        if(end!=start)c.drawText(endLabel,getWidth()-p.measureText(endLabel)-4,getHeight()-8,p);
        p.setColor(LINE);
        trend.rewind();
        long span=end-start;
        double range=max-min;
        for(int i=0;i<values.length;i++) {
            float x=left+(span==0?w/2:(times[i]-start)*w/span);
            float y=bottom-(range==0?h/2:(float)((values[i]-min)*h/range));
            xs[i]=x;
            ys[i]=y;
            if(i==0)trend.moveTo(x,y);
            else trend.lineTo(x,y);
        }
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2*density);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeCap(Paint.Cap.ROUND);
        c.drawPath(trend,p);
        p.setStyle(Paint.Style.FILL);
        for(int i=0;i<xs.length;i++) {
            p.setColor(Color.WHITE);
            c.drawCircle(xs[i],ys[i],4*density,p);
            p.setColor(LINE);
            c.drawCircle(xs[i],ys[i],2.7f*density,p);
        }
    }
}
