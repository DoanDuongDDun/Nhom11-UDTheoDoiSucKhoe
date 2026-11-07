package com.example.vitality.graphics;
import android.content.Context;
import android.util.AttributeSet;
import android.graphics.*;
import android.view.View;
import android.animation.ValueAnimator;
import com.example.vitality.data.entity.BodyMeasurement;
public class BodyShapeView extends View  {
    protected BodyShapeParameters shape=new BodyShapeParameters(170,34,28,36,10,18,12);
    private ValueAnimator animator;
    public BodyShapeView(Context c,AttributeSet a) {
        super(c,a);
        setContentDescription("Mô hình cơ thể minh họa");
    }
    public void setInitialMeasurement(BodyMeasurement m) {
        if(animator!=null)animator.cancel();
        shape=BodyShapeMapper.map(m);
        invalidate();
    }
    public void setMeasurement(BodyMeasurement m) {
        BodyShapeParameters next=BodyShapeMapper.map(m);
        if(animator!=null)animator.cancel();
        BodyShapeParameters old=shape;
        animator=ValueAnimator.ofFloat(0,1);
        animator.setDuration(450);
        animator.addUpdateListener(a-> {
            float t=(float)a.getAnimatedValue();shape=new BodyShapeParameters(mix(old.height,next.height,t),mix(old.chest,next.chest,t),mix(old.waist,next.waist,t),mix(old.hip,next.hip,t),mix(old.arm,next.arm,t),mix(old.thigh,next.thigh,t),mix(old.calf,next.calf,t));invalidate();
        }
        );
        animator.start();
    }
    private float mix(float a,float b,float t) {
        return a+(b-a)*t;
    }
    @Override protected void onDetachedFromWindow() {
        if(animator!=null)animator.cancel();
        super.onDetachedFromWindow();
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float scale=Math.min(getWidth()/115f,getHeight()/260f);
        drawBody(c,shape,getWidth()/2f,getHeight()-12,scale,0xffD64545,255);
    }
    /** Decorative surface anatomy only: veins do not represent measured health data. */
    public static void drawBody(Canvas c, BodyShapeParameters p, float x, float feet,
                                float scale, int color, int alpha) {
        c.save();
        c.translate(x, feet-p.height*scale);
        c.scale(scale*p.height/180f, scale*p.height/180f);
        float factor=180f/p.height;
        float chest=Math.min(p.chest*factor/2,26), waist=Math.min(p.waist*factor/2,26);
        float hip=Math.min(p.hip*factor/2,28);
        float shoulder=Math.max(chest+2,Math.max(waist,hip)*.88f+3);
        float arm=Math.min(p.arm*factor/2,8);
        float thigh=Math.min(p.thigh*factor/2,hip*.47f);
        float calf=Math.min(p.calf*factor/2,thigh*.85f);
        float legCenter=Math.max(hip*.53f,thigh+1);
        Path silhouette=new Path();
        // Skull, jaw and neck flow directly into the trapezius and shoulders.
        silhouette.moveTo(0,0);
        silhouette.cubicTo(-9,-.5f,-10,8,-8,15);
        silhouette.cubicTo(-7.5f,20,-5,22,-4,23);
        silhouette.cubicTo(-4,28,-8,29,-shoulder+3,32);
        silhouette.cubicTo(-shoulder-2,33,-shoulder-1,39,-chest,46);
        silhouette.cubicTo(-chest,53,-waist,60,-waist,66);
        silhouette.cubicTo(-waist,74,-hip,78,-hip,87);
        silhouette.cubicTo(-hip,96,-hip*.6f,100,0,101);
        silhouette.cubicTo(hip*.6f,100,hip,96,hip,87);
        silhouette.cubicTo(hip,78,waist,74,waist,66);
        silhouette.cubicTo(waist,60,chest,53,chest,46);
        silhouette.cubicTo(shoulder+1,39,shoulder+2,33,shoulder-3,32);
        silhouette.cubicTo(8,29,4,28,4,23);
        silhouette.cubicTo(5,22,7.5f,20,8,15);
        silhouette.cubicTo(10,8,9,-.5f,0,0);
        silhouette.close();
        for(int side:new int[]{-1,1}) {
            Path limb=new Path();
            float elbow=shoulder+6, wrist=shoulder+12;
            limb.moveTo(shoulder-3,33);
            limb.cubicTo(shoulder+arm+2,32,shoulder+arm+2,45,elbow+arm*.6f,58);
            limb.cubicTo(elbow+arm*.8f,65,wrist+2,78,wrist+2,84);
            // Palm and a relaxed thumb; four rounded fingers are unioned below.
            limb.cubicTo(wrist+6,86,wrist+7,90,wrist+7,94);
            limb.lineTo(wrist+1,97);
            limb.quadTo(wrist-2,94,wrist-2,90);
            limb.quadTo(wrist-5,93,wrist-5,90);
            limb.lineTo(wrist-3,84);
            limb.cubicTo(wrist-4,77,elbow-arm*.65f,66,elbow-arm*.7f,58);
            limb.cubicTo(shoulder-2,49,shoulder-4,41,shoulder-3,33);
            limb.close();
            for(int finger=0;finger<4;finger++) {
                float fx=wrist-.4f+finger*1.8f;
                Path digit=new Path();
                digit.addRoundRect(fx,91,fx+1.7f,99+(finger==1?2:finger==2?1:0),.85f,.85f,Path.Direction.CW);
                limb.op(digit,Path.Op.UNION);
            }
            Path leg=new Path();
            leg.moveTo(hip,85);
            leg.cubicTo(legCenter+thigh,96,legCenter+thigh,111,legCenter+calf*.7f,125);
            leg.cubicTo(legCenter+calf,132,legCenter+calf,141,legCenter+3,163);
            leg.quadTo(legCenter+2.5f,169,legCenter+5,172);
            leg.cubicTo(legCenter+10,177,legCenter+8,180,legCenter+4,180);
            leg.cubicTo(legCenter,180,legCenter-4,176,legCenter-3,172);
            leg.lineTo(legCenter-2.5f,165);
            leg.cubicTo(legCenter-calf,146,legCenter-calf,136,legCenter-calf*.65f,126);
            leg.cubicTo(legCenter-thigh,116,1.4f,103,1.2f,96);
            leg.lineTo(0,91);
            leg.close();
            limb.op(leg,Path.Op.UNION);
            Matrix mirror=new Matrix();mirror.setScale(side,1);limb.transform(mirror);
            silhouette.op(limb,Path.Op.UNION);
        }
        // One composited layer keeps overlay opacity uniform even where limbs meet.
        int layer=c.saveLayerAlpha(-65,-3,65,183,alpha);
        Paint fill=new Paint(Paint.ANTI_ALIAS_FLAG);
        int light=blend(color,0xffffb0a6,.32f), dark=blend(color,0xff5b1026,.34f);
        fill.setShader(new LinearGradient(-35,0,35,0,new int[]{dark,color,light,color,dark},new float[]{0,.24f,.47f,.72f,1},Shader.TileMode.CLAMP));
        c.drawPath(silhouette,fill);
        fill.setShader(null);fill.setStyle(Paint.Style.STROKE);fill.setStrokeJoin(Paint.Join.ROUND);
        fill.setStrokeWidth(.45f);fill.setColor(dark);fill.setAlpha(180);c.drawPath(silhouette,fill);
        c.save();c.clipPath(silhouette);
        Paint detail=new Paint(Paint.ANTI_ALIAS_FLAG);detail.setStyle(Paint.Style.STROKE);
        detail.setStrokeCap(Paint.Cap.ROUND);detail.setStrokeJoin(Paint.Join.ROUND);
        detail.setColor(dark);detail.setAlpha(105);detail.setStrokeWidth(.38f);
        for(int side:new int[]{-1,1}) {
            c.save();c.scale(side,1);
            Path anatomy=new Path();
            anatomy.moveTo(1.8f,35);anatomy.cubicTo(5,32,9,34,shoulder-4,33);
            anatomy.moveTo(3,48);anatomy.cubicTo(chest*.55f,51,chest*.85f,49,chest*.91f,44);
            anatomy.moveTo(hip*.6f,87);anatomy.quadTo(hip*.4f,92,2,96);
            anatomy.moveTo(legCenter-2,124);anatomy.quadTo(legCenter-3,128,legCenter,130);
            anatomy.moveTo(legCenter+3,125);anatomy.quadTo(legCenter+4,128,legCenter+2,130);
            anatomy.moveTo(legCenter+1,166);anatomy.lineTo(legCenter+1,171);
            c.drawPath(anatomy,detail);
            // Fine red vessel accents, intentionally sparse and purely decorative.
            Paint vein=new Paint(detail);vein.setColor(0xffffb1aa);vein.setAlpha(85);vein.setStrokeWidth(.32f);
            Path vessels=new Path();float e=shoulder+6;
            vessels.moveTo(e+1,60);vessels.cubicTo(e+2,67,e+4,74,e+6,83);
            vessels.moveTo(e+3,71);vessels.quadTo(e,70,e-1,66);
            vessels.moveTo(e+5,78);vessels.quadTo(e+7,76,e+7,73);
            vessels.moveTo(legCenter+1,136);vessels.cubicTo(legCenter-1,143,legCenter+2,150,legCenter,159);
            vessels.moveTo(legCenter+1,146);vessels.quadTo(legCenter+4,144,legCenter+4,140);
            c.drawPath(vessels,vein);
            c.restore();
        }
        detail.setAlpha(95);Path center=new Path();center.moveTo(0,53);center.quadTo(-.6f,60,0,65);
        c.drawPath(center,detail);c.drawOval(-.5f,73,.5f,74,detail);
        c.restore();c.restoreToCount(layer);c.restore();
    }
    private static int blend(int a,int b,float t) {
        return Color.rgb((int)(Color.red(a)*(1-t)+Color.red(b)*t),
                (int)(Color.green(a)*(1-t)+Color.green(b)*t),
                (int)(Color.blue(a)*(1-t)+Color.blue(b)*t));
    }
}
