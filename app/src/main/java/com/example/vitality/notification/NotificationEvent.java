package com.example.vitality.notification;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import com.example.vitality.R;

public final class NotificationEvent {
    public enum Group {
        CYCLE("cycle", "Chu kỳ sinh hoạt", "Báo khi lịch sinh hoạt đến hạn"),
        GOALS("goals", "Mục tiêu", "Báo khi đạt mục tiêu"),
        ACTIVITY("activity", "Hoạt động", "Báo khi ghi nhận hoạt động"),
        SLEEP("sleep", "Giám sát giấc ngủ", "Báo khi có kết quả giấc ngủ mới");

        final String key;
        public final String title;
        public final String eventLabel;

        Group(String key, String title, String eventLabel) {
            this.key = key;
            this.title = title;
            this.eventLabel = eventLabel;
        }

        public String enabledKey() { return key + ".enabled"; }
        public String eventKey() { return key + ".event"; }
        public String previewKey() { return key + ".preview"; }
        public boolean supportsPreview() { return this != SLEEP; }
    }

    public enum Type {
        HABIT_DUE(Group.CYCLE, R.id.habit, "Có lịch sinh hoạt đến hạn."),
        GOAL_REACHED(Group.GOALS, R.id.goals, "Bạn đã hoàn thành một mục tiêu."),
        PROFILE_GOAL_REACHED(Group.GOALS, R.id.profile, "Bạn đã hoàn thành một mục tiêu."),
        ACTIVITY_SAVED(Group.ACTIVITY, R.id.activity, "Một hoạt động đã được ghi nhận."),
        SLEEP_FINISHED(Group.SLEEP, R.id.sleep, "Bạn có kết quả giấc ngủ mới."),
        PERIOD_REMINDER(null, R.id.period, "Sắp đến ngày bắt đầu kỳ kinh dự kiến.");

        @Nullable public final Group group;
        @IdRes public final int destination;
        final String generalMessage;

        Type(@Nullable Group group, @IdRes int destination, String generalMessage) {
            this.group = group;
            this.destination = destination;
            this.generalMessage = generalMessage;
        }
    }

    final Type type;
    final String key;
    final long recordId;
    final String detail;

    public NotificationEvent(Type type, String eventId, long recordId, String detail) {
        this.type = type;
        this.key = type.name() + ":" + eventId;
        this.recordId = recordId;
        this.detail = detail;
    }
}
