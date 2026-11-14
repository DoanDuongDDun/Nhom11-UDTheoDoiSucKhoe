package com.example.vitality.graphics;
import android.content.Context;
import android.util.AttributeSet;
import android.graphics.*;
import android.view.View;
import android.animation.ValueAnimator;
import android.util.LruCache;
import com.example.vitality.data.entity.BodyMeasurement;
public class BodyShapeView extends View  {
    protected BodyShapeParameters shape=new BodyShapeParameters(170,34,28,36,10,18,12);
    private ValueAnimator animator;
    private BodyShapeParameters target;
    public BodyShapeView(Context c,AttributeSet a) {
        super(c,a);
        setContentDescription("Mô hình cơ thể minh họa");
    }
    public void setInitialMeasurement(BodyMeasurement m) {
        if(animator!=null)animator.cancel();
        shape=target=BodyShapeMapper.map(m);
        invalidate();
    }
    public void setMeasurement(BodyMeasurement m) {
        BodyShapeParameters next=BodyShapeMapper.map(m);
        if(next.equals(target))return;
        target=next;
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
        if(animator!=null) {
            animator.cancel();
            if(target!=null)shape=target;
        }
        super.onDetachedFromWindow();
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float scale=Math.min(getWidth()/115f,getHeight()/260f);
        drawBody(c,shape,getWidth()/2f,getHeight()-12,scale,0xffD64545,255);
    }
    /** Hình học đã dựng (đường viền và các nét chi tiết) cho một bộ thông số; chỉ phụ thuộc vào thông số, không phụ thuộc vị trí/tỷ lệ/màu. */
    private static final class Geometry {
        final Path silhouette=new Path(),anatomy=new Path(),vessels=new Path(),center=new Path();
    }
    // Dựng đường viền (kể cả Path.op UNION cho tay, ngón tay, chân) khá tốn: lưu lại để các lần vẽ lại cùng thông số
    // (kéo thanh độ đậm, xoay màn hình, vẽ hai hình so sánh...) không phải dựng lại.
    private static final LruCache<BodyShapeParameters,Geometry> GEOMETRY=new LruCache<>(8);
    // Chỉ dùng trên luồng giao diện, nên dùng chung được thay vì tạo Paint mới ở mỗi khung hình.
    private static final Paint FILL=new Paint(Paint.ANTI_ALIAS_FLAG),DETAIL=new Paint(Paint.ANTI_ALIAS_FLAG),VEIN=new Paint(Paint.ANTI_ALIAS_FLAG);
    private static Geometry geometry(BodyShapeParameters p) {
        Geometry g=GEOMETRY.get(p);
        if(g==null) {
            g=buildGeometry(p);
            GEOMETRY.put(p,g);
        }
        return g;
    }
    private static Geometry buildGeometry(BodyShapeParameters p) {
        Geometry g=new Geometry();
        float factor=180f/p.height;
        float chest=Math.min(p.chest*factor/2,26), waist=Math.min(p.waist*factor/2,26);
        float hip=Math.min(p.hip*factor/2,28);
        float shoulder=Math.max(chest+2,Math.max(waist,hip)*.88f+3);
        float arm=Math.min(p.arm*factor/2,8);
        float thigh=Math.min(p.thigh*factor/2,hip*.47f);
        float calf=Math.min(p.calf*factor/2,thigh*.85f);
        float legCenter=Math.max(hip*.53f,thigh+1);
        Path silhouette=g.silhouette;
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
        // Hai tay + hai chân dựng một lần cho bên phải rồi lật gương; các ngón tay dùng chung một Path.
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
        Path digits=new Path();
        for(int finger=0;finger<4;finger++) {
            float fx=wrist-.4f+finger*1.8f;
            digits.addRoundRect(fx,91,fx+1.7f,99+(finger==1?2:finger==2?1:0),.85f,.85f,Path.Direction.CW);
        }
        limb.op(digits,Path.Op.UNION);
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
        silhouette.op(limb,Path.Op.UNION);
        Path mirrored=new Path();
        Matrix mirror=new Matrix();
        mirror.setScale(-1,1);
        limb.transform(mirror,mirrored);
        silhouette.op(mirrored,Path.Op.UNION);
        // Surface details are identical for both sides (drawn mirrored), so they are built once.
        Path anatomy=g.anatomy;
        anatomy.moveTo(1.8f,35);anatomy.cubicTo(5,32,9,34,shoulder-4,33);
        anatomy.moveTo(3,48);anatomy.cubicTo(chest*.55f,51,chest*.85f,49,chest*.91f,44);
        anatomy.moveTo(hip*.6f,87);anatomy.quadTo(hip*.4f,92,2,96);
        anatomy.moveTo(legCenter-2,124);anatomy.quadTo(legCenter-3,128,legCenter,130);
        anatomy.moveTo(legCenter+3,125);anatomy.quadTo(legCenter+4,128,legCenter+2,130);
        anatomy.moveTo(legCenter+1,166);anatomy.lineTo(legCenter+1,171);
        // Fine red vessel accents, intentionally sparse and purely decorative.
        Path vessels=g.vessels;
        float e=shoulder+6;
        vessels.moveTo(e+1,60);vessels.cubicTo(e+2,67,e+4,74,e+6,83);
        vessels.moveTo(e+3,71);vessels.quadTo(e,70,e-1,66);
        vessels.moveTo(e+5,78);vessels.quadTo(e+7,76,e+7,73);
        vessels.moveTo(legCenter+1,136);vessels.cubicTo(legCenter-1,143,legCenter+2,150,legCenter,159);
        vessels.moveTo(legCenter+1,146);vessels.quadTo(legCenter+4,144,legCenter+4,140);
        g.center.moveTo(0,53);g.center.quadTo(-.6f,60,0,65);
        return g;
    }
    /** Decorative surface anatomy only: veins do not represent measured health data. */
    public static void drawBody(Canvas c, BodyShapeParameters p, float x, float feet,
                                float scale, int color, int alpha) {
        Geometry g=geometry(p);
        c.save();
        c.translate(x, feet-p.height*scale);
        c.scale(scale*p.height/180f, scale*p.height/180f);
        // One composited layer keeps overlay opacity uniform even where limbs meet.
        int layer=c.saveLayerAlpha(-65,-3,65,183,alpha);
        int light=blend(color,0xffffb0a6,.32f), dark=blend(color,0xff5b1026,.34f);
        FILL.reset();FILL.setAntiAlias(true);
        FILL.setShader(new LinearGradient(-35,0,35,0,new int[]{dark,color,light,color,dark},new float[]{0,.24f,.47f,.72f,1},Shader.TileMode.CLAMP));
        c.drawPath(g.silhouette,FILL);
        FILL.setShader(null);FILL.setStyle(Paint.Style.STROKE);FILL.setStrokeJoin(Paint.Join.ROUND);
        FILL.setStrokeWidth(.45f);FILL.setColor(dark);FILL.setAlpha(180);c.drawPath(g.silhouette,FILL);
        c.save();c.clipPath(g.silhouette);
        DETAIL.reset();DETAIL.setAntiAlias(true);DETAIL.setStyle(Paint.Style.STROKE);
        DETAIL.setStrokeCap(Paint.Cap.ROUND);DETAIL.setStrokeJoin(Paint.Join.ROUND);
        DETAIL.setColor(dark);DETAIL.setAlpha(105);DETAIL.setStrokeWidth(.38f);
        VEIN.set(DETAIL);VEIN.setColor(0xffffb1aa);VEIN.setAlpha(85);VEIN.setStrokeWidth(.32f);
        for(int side=-1;side<=1;side+=2) {
            c.save();c.scale(side,1);
            c.drawPath(g.anatomy,DETAIL);
            c.drawPath(g.vessels,VEIN);
            c.restore();
        }
        DETAIL.setAlpha(95);
        c.drawPath(g.center,DETAIL);c.drawOval(-.5f,73,.5f,74,DETAIL);
        c.restore();c.restoreToCount(layer);c.restore();
    }
    private static int blend(int a,int b,float t) {
        return Color.rgb((int)(Color.red(a)*(1-t)+Color.red(b)*t),
                (int)(Color.green(a)*(1-t)+Color.green(b)*t),
                (int)(Color.blue(a)*(1-t)+Color.blue(b)*t));
    }
}
