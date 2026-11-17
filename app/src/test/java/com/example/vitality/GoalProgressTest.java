package com.example.vitality;
import org.junit.Test;import static org.junit.Assert.*;import java.util.*;import com.example.vitality.data.entity.*;import com.example.vitality.utils.*;
public class GoalProgressTest {
 @Test public void weightLossAndGain(){Goal g=new Goal();g.kind="weight";g.baseline=80;g.target=70;assertEquals(50,GoalProgress.percent(g,75));assertEquals(0,GoalProgress.percent(g,85));assertEquals(100,GoalProgress.percent(g,65));g.target=90;assertEquals(50,GoalProgress.percent(g,85));}
 @Test public void dailyOnlyMatchesActivityAndDate(){Goal g=new Goal();g.keyword="chạy";g.target=30;JournalEntry run=new JournalEntry();run.kind="activity";run.note="Chay bo";run.amount=15;JournalEntry walk=new JournalEntry();walk.kind="activity";walk.note="Đi bộ";walk.amount=40;JournalEntry old=new JournalEntry();old.kind="activity";old.note="Chạy";old.amount=100;old.startedAt=Formats.dayStart(System.currentTimeMillis())-1;double current=GoalProgress.current(g,Arrays.asList(run,walk,old),null,System.currentTimeMillis());assertEquals(15,current,0);assertEquals(50,GoalProgress.percent(g,current));}
 @Test public void mealMatching(){Goal g=new Goal();g.kind="nutrition";g.keyword="rau";JournalEntry e=new JournalEntry();e.kind="nutrition";e.note="Cơm và rau luộc";assertEquals(1,GoalProgress.current(g,Arrays.asList(e),null,System.currentTimeMillis()),0);}
 @Test public void nutritionCountsMatchingMealsOnceFromNewAndLegacyRecords() {
     long today = Formats.dayStart(System.currentTimeMillis());
     Goal goal = new Goal();
     goal.kind = "nutrition";
     goal.keyword = "rau luộc";
     goal.target = 2;

     JournalEntry meal = new JournalEntry();
     meal.kind = "nutrition";
     meal.title = "Rau   luộc";
     meal.calories = 200.0;
     meal.startedAt = today;
     JournalEntry legacy = new JournalEntry();
     legacy.kind = "nutrition";
     legacy.note = "Cơm và rau luộc";
     legacy.startedAt = today;
     JournalEntry otherMeal = new JournalEntry();
     otherMeal.kind = "nutrition";
     otherMeal.title = "Cơm gà";
     otherMeal.startedAt = today;
     JournalEntry old = new JournalEntry();
     old.kind = "nutrition";
     old.title = "Rau luộc";
     old.startedAt = today - 1;

     List<JournalEntry> journal = Arrays.asList(meal, legacy, otherMeal, old);
     assertEquals(2, GoalProgress.current(goal, journal, null, today), 0);
     assertEquals(100, GoalProgress.percent(goal, GoalProgress.current(goal, journal, null, today)));
     goal.keyword = null;
     assertEquals(3, GoalProgress.current(goal, journal, null, today), 0);
 }
 @Test public void overallCapsEachGoalAndExcludesBody(){Goal a=new Goal();a.target=10;Goal b=new Goal();b.kind="water";b.target=1000;Goal body=new Goal();body.kind="weight";JournalEntry e=new JournalEntry();e.kind="activity";e.amount=50;assertEquals(50,GoalProgress.daily(Arrays.asList(a,b,body),Arrays.asList(e),null,System.currentTimeMillis()));}
 @Test public void emptyDayStartsAtZero(){Goal g=new Goal();assertEquals(0,GoalProgress.daily(Arrays.asList(g),Collections.emptyList(),null,System.currentTimeMillis()));}
 @Test public void waterUsesSharedTargetAndJournalAmounts(){Goal g=new Goal();g.kind="water";g.target=2000;JournalEntry e=new JournalEntry();e.kind="water";e.amount=1000;List<Goal> goals=Arrays.asList(g);assertEquals(2000,GoalProgress.target(goals,"water",1500),0);assertEquals(50,GoalProgress.percent(g,GoalProgress.current(g,Arrays.asList(e),null,System.currentTimeMillis())));assertEquals(50,GoalProgress.daily(goals,Arrays.asList(e),null,System.currentTimeMillis()));assertEquals(0,GoalProgress.daily(goals,Collections.emptyList(),null,System.currentTimeMillis()));}
}
