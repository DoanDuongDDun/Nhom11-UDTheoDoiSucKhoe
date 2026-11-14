package com.example.vitality.graphics;
import android.content.Context;
import android.util.AttributeSet;
import android.graphics.*;
import android.view.View;
import com.example.vitality.data.entity.BodyMeasurement;
public class BodyComparisonView extends View  {
    private BodyShapeParameters first,second;
    private boolean overlay;
    private int opacity=110;
    public BodyComparisonView(Context c,AttributeSet a) {
        super(c,a);
    }
    public void setMeasurements(BodyMeasurement a,BodyMeasurement b) {
        first=BodyShapeMapper.map(a);
        second=BodyShapeMapper.map(b);
        invalidate();
    }
    public void setOverlay(boolean value) {
        overlay=value;
        invalidate();
    }
    public void setOpacity(int value) {
        opacity=value;
        invalidate();
    }
    @Override protected void onDraw(Canvas c) {
        if(first==null||second==null)return;
        float scale=Math.min(getWidth()/(overlay?115f:210f),getHeight()/(Math.max(first.height,second.height)+12));
        float feet=(getHeight()+Math.max(first.height,second.height)*scale)/2;
        BodyShapeView.drawBody(c,second,overlay?getWidth()/2f:getWidth()*.75f,feet,scale,0xffA92F36,255);
        BodyShapeView.drawBody(c,first,overlay?getWidth()/2f:getWidth()*.25f,feet,scale,0xffEF9393,overlay?opacity:255);
    }
}
