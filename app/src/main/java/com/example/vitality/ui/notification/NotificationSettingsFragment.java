package com.example.vitality.ui.notification;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.vitality.R;
import com.example.vitality.databinding.ItemReminderRowBinding;
import com.example.vitality.notification.NotificationController;
import com.example.vitality.notification.NotificationEvent.Group;
import com.example.vitality.notification.NotificationSettings;
import com.example.vitality.notification.ReminderScheduler;
import com.example.vitality.ui.main.PageFragment;
import java.io.IOException;

public class NotificationSettingsFragment extends PageFragment {
    private String pendingKey;
    private String feedback = "";
    private NotificationSettings displayed;
    private final ActivityResultLauncher<String> requestPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                String key = pendingKey;
                pendingKey = null;
                if (granted && key != null) {
                    save(key, true);
                    return;
                }
                feedback = granted ? "" : "Chưa được cấp quyền thông báo";
                if (granted) ReminderScheduler.schedule(requireContext());
                if (binding != null) render();
            });

    @Override public void onViewCreated(View view, Bundle state) {
        if (state != null) pendingKey = state.getString("notificationPermissionKey");
        heading("Thông báo", "Chọn cập nhật sức khỏe bạn muốn nhận.");
        NotificationController.createChannels(requireContext());
        render();
    }

    @Override public void onResume() {
        super.onResume();
        if (binding != null) render();
    }

    private void render() {
        clear();
        button("Quay lại", this::back);
        boolean allowed = NotificationController.systemAllows(requireContext());
        card(null, "Thông báo Android", null, null);
        text(allowed ? "Android đã cho phép thông báo." : "Android đang chặn thông báo của Vitality.");
        button("Quản lý thông báo hệ thống ↗", this::openSystemSettings);
        if (NotificationController.needsPermission(requireContext())) {
            button("Cấp quyền thông báo", () -> requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS));
        }
        endCard();
        if (!feedback.isEmpty()) status(feedback);
        try {
            displayed = NotificationSettings.read(requireContext());
        } catch (IOException | RuntimeException error) {
            displayed = null;
            status("Không thể đọc cài đặt thông báo");
            button("Thử lại", this::render);
            return;
        }

        card(null, "Thông báo trong ứng dụng", null, null);
        addSwitch("Cho phép thông báo", null, NotificationSettings.MASTER, true);
        endCard();
        boolean master = displayed.checked(NotificationSettings.MASTER);
        for (Group group : Group.values()) renderGroup(group, master);
    }

    private void renderGroup(Group group, boolean master) {
        card(null, group.title, group == Group.CYCLE ? "Mở lịch" : null,
                () -> navigate(R.id.habit));
        if (!NotificationController.channelAllows(requireContext(), group)) {
            text("Nhóm này đang bị chặn trong cài đặt Android.");
        }
        addSwitch("Cho phép thông báo nhóm này", group, group.enabledKey(), master);
        boolean enabled = master && displayed.checked(group.enabledKey());
        addSwitch(group.eventLabel, group, group.eventKey(), enabled);
        if (group.supportsPreview()) {
            addSwitch("Xem trước nội dung", group, group.previewKey(), enabled);
        }
        endCard();
    }

    private void addSwitch(String label, Group group, String key, boolean enabled) {
        ItemReminderRowBinding row = ItemReminderRowBinding.bind(widget(R.layout.item_reminder_row));
        row.remTitle.setText(label);
        row.remSub.setVisibility(View.GONE);
        row.remSwitch.setContentDescription(group == null ? label : group.title + ". " + label);
        row.remSwitch.setEnabled(enabled && pendingKey == null);
        row.remSwitch.setChecked(displayed.checked(key));
        row.remSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (checked && !key.endsWith(".preview") && NotificationController.needsPermission(requireContext())) {
                pendingKey = key;
                feedback = "";
                render();
                requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
            save(key, checked);
        });
    }

    private void save(String key, boolean checked) {
        try {
            NotificationSettings.save(requireContext(), key, checked);
            feedback = "";
        } catch (IOException | RuntimeException error) {
            feedback = "Không thể lưu cài đặt thông báo";
            if (binding != null) render();
            return;
        }
        if (!checked) {
            if (NotificationSettings.MASTER.equals(key)) NotificationController.cancelVisible(requireContext(), null);
            else for (Group group : Group.values()) {
                if (key.equals(group.enabledKey()) || key.equals(group.eventKey()) || key.equals(group.previewKey())) {
                    NotificationController.cancelVisible(requireContext(), group);
                    break;
                }
            }
        }
        ReminderScheduler.schedule(requireContext());
        if (binding != null) render();
    }

    private void status(String message) {
        TextView view = text(message);
        view.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    }

    private void openSystemSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", requireContext().getPackageName(), null));
        if (Build.VERSION.SDK_INT >= 26) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
        }
        startActivity(intent);
    }

    @Override public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("notificationPermissionKey", pendingKey);
    }
}
