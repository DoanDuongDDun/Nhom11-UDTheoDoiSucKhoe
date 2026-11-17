package com.example.vitality.utils;
import java.text.Normalizer;import java.util.*;import com.example.vitality.data.entity.*;
public final class GoalProgress {
 public static double target(List<Goal> goals,String kind,double fallback){Goal latest=null;if(goals!=null)for(Goal g:goals)if(kind.equals(g.kind)&&(g.keyword==null||g.keyword.isEmpty())&&(latest==null||g.id>latest.id))latest=g;return latest==null?fallback:latest.target;}
 public static int category(Goal g){return body(g)?0:g.kind.equals("nutrition")||g.kind.equals("water")?1:g.kind.equals("activity")?2:3;}
 public static boolean body(Goal g){return g.kind.equals("weight")||g.kind.equals("waist");}
 public static String unit(Goal g){return g.kind.equals("weight")?"kg":g.kind.equals("waist")?"cm":g.kind.equals("activity")?"phút":g.kind.equals("water")?"ml":g.kind.equals("sleep")?"giờ":"lần";}
 private static String norm(String s){return Normalizer.normalize(s==null?"":s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replace('đ','d').replaceAll("\\s+"," ").trim();}
 public static double current(Goal g,List<JournalEntry> journal,List<BodyMeasurement> body,long day){
 if(body(g)){BodyMeasurement latest=null;if(body!=null)for(BodyMeasurement m:body)if(latest==null||m.measuredAt>latest.measuredAt||m.measuredAt==latest.measuredAt&&m.id>latest.id)latest=m;return latest==null?g.baseline:g.kind.equals("weight")?latest.weightKg:latest.waistCm;}
 long start=Formats.dayStart(day),stop=Formats.addDays(start,1),now=System.currentTimeMillis();
 String keyword=norm(g.keyword);double sum=0;
 if(journal!=null)for(JournalEntry e:journal){long time=g.kind.equals("sleep")?(e.endedAt==null?0:e.endedAt):e.startedAt;if(!g.kind.equals(e.kind)||time<start||time>=stop||time>now)continue;
 String text=(e.title==null?"":e.title)+" "+(e.note==null?"":e.note);if(!keyword.isEmpty()&&!norm(text).contains(keyword))continue;
 sum+=g.kind.equals("nutrition")?1:g.kind.equals("sleep")?HealthMath.sleepMinutesOrZero(e.startedAt,e.endedAt,e.awakeMinutes)/60:e.amount;}
 return sum;}
 public static int percent(Goal g,double current){double ratio;if(body(g)){double distance=g.target-g.baseline;ratio=distance==0?(Math.abs(current-g.target)<.1?1:0):(current-g.baseline)/distance;}else ratio=g.target>0?current/g.target:0;return (int)Math.round(Math.max(0,Math.min(1,ratio))*100);}
 public static int daily(List<Goal> goals,List<JournalEntry> entries,List<BodyMeasurement> measures,long day){int sum=0,n=0;if(goals!=null)for(Goal g:goals)if(!body(g)&&Formats.dayStart(g.createdAt)<=Formats.dayStart(day)){sum+=percent(g,current(g,entries,measures,day));n++;}return n==0?0:Math.round(sum/(float)n);}
}
