package com.example.vitality.ui.activity;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.vitality.data.entity.JournalEntry;
import com.example.vitality.viewmodel.VitalityViewModel;

public final class StepSessionViewModel extends ViewModel {
    static final class PendingSave {
        final JournalEntry entry;
        final boolean saving;

        PendingSave(JournalEntry entry, boolean saving) {
            this.entry = entry;
            this.saving = saving;
        }
    }

    private final MutableLiveData<PendingSave> pending = new MutableLiveData<>();

    LiveData<PendingSave> pending() {
        return pending;
    }

    void save(VitalityViewModel vm, JournalEntry entry) {
        pending.setValue(new PendingSave(entry, true));
        vm.run(() -> {
            try {
                vm.wellness.saveJournal(entry);
            } catch (RuntimeException failure) {
                pending.postValue(new PendingSave(entry, false));
                throw failure;
            }
            pending.postValue(null);
        }, true);
    }
}
