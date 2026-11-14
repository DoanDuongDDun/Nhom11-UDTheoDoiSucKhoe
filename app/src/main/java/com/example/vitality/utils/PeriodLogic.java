package com.example.vitality.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.example.vitality.data.entity.PeriodEntry;

public final class PeriodLogic {
    private PeriodLogic() {}

    /** Uses up to six recent plausible intervals. At least two recorded starts are required. */
    public static Long nextStart(List<PeriodEntry> entries) {
        if (entries == null || entries.size() < 2) return null;
        List<Long> starts = new ArrayList<>();
        for (PeriodEntry e : entries) starts.add(e.startDay);
        Collections.sort(starts);
        List<Integer> intervals = new ArrayList<>();
        for (int i = starts.size() - 1; i > 0 && intervals.size() < 6; i--) {
            int days = calendarDays(starts.get(i - 1), starts.get(i));
            if (days >= 15 && days <= 60) intervals.add(days);
        }
        if (intervals.isEmpty()) return null;
        int sum = 0;
        for (int days : intervals) sum += days;
        int average = Math.round((float) sum / intervals.size());
        long next = Formats.addDays(starts.get(starts.size() - 1), average);
        long today = Formats.dayStart(System.currentTimeMillis());
        while (next < today) next = Formats.addDays(next, average);
        return next;
    }

    public static int calendarDays(long a, long b) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(a);
        int yearA = c.get(java.util.Calendar.YEAR);
        int dayA = c.get(java.util.Calendar.DAY_OF_YEAR);
        c.setTimeInMillis(b);
        int yearB = c.get(java.util.Calendar.YEAR);
        int dayB = c.get(java.util.Calendar.DAY_OF_YEAR);
        int days = dayB - dayA;
        for (int y = yearA; y < yearB; y++) days += (y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)) ? 366 : 365;
        return days;
    }
}
