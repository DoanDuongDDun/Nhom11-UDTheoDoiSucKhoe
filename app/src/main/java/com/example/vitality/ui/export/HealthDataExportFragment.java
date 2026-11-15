package com.example.vitality.ui.export;

import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
import android.os.Bundle;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.vitality.R;
import com.example.vitality.data.export.HealthDataExportFormat;
import com.example.vitality.data.export.HealthDataFilter;
import com.example.vitality.data.export.HealthDataFilter.Type;
import com.example.vitality.data.export.HealthDataRecord;
import com.example.vitality.databinding.FragmentHealthDataExportBinding;
import com.example.vitality.databinding.ItemHealthExportRecordBinding;
import com.example.vitality.utils.Formats;
import com.example.vitality.viewmodel.HealthDataExportViewModel;
import java.util.Calendar;

public class HealthDataExportFragment extends Fragment {
    private FragmentHealthDataExportBinding binding;
    private HealthDataExportViewModel model;
    private RecordAdapter adapter;
    private final ActivityResultLauncher<String> csvPicker = registerForActivityResult(
            new ActivityResultContracts.CreateDocument(HealthDataExportFormat.CSV.mimeType), this::documentChosen);
    private final ActivityResultLauncher<String> excelPicker = registerForActivityResult(
            new ActivityResultContracts.CreateDocument(HealthDataExportFormat.EXCEL.mimeType), this::documentChosen);

    private void documentChosen(Uri destination) {
        model.documentChosen(destination);
    }

    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                       @Nullable Bundle savedState) {
        model = new ViewModelProvider(this).get(HealthDataExportViewModel.class);
        binding = FragmentHealthDataExportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedState) {
        binding.healthDataBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        Type[] types = Type.values();
        String[] labels = new String[types.length];
        for (int i = 0; i < types.length; i++) labels[i] = types[i].label;
        ArrayAdapter<String> typesAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, labels);
        typesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.exportType.setAdapter(typesAdapter);
        binding.exportType.setSelection(model.filter().type.ordinal(), false);
        binding.exportType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View row, int position, long id) {
                model.setType(position);
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        binding.exportFrom.setOnClickListener(v -> chooseDate(true));
        binding.exportThrough.setOnClickListener(v -> chooseDate(false));
        binding.viewHealthData.setOnClickListener(v -> model.viewData());
        binding.exportHealthExcel.setOnClickListener(v -> model.export(HealthDataExportFormat.EXCEL));
        binding.exportHealthCsv.setOnClickListener(v -> model.export(HealthDataExportFormat.CSV));
        adapter = new RecordAdapter();
        binding.exportRecords.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.exportRecords.setAdapter(adapter);
        model.screen().observe(getViewLifecycleOwner(), this::render);
        model.chooseFile().observe(getViewLifecycleOwner(), format -> {
            if (format != null && model.claimPicker(format)) launchFilePicker(format);
        });
    }

    private void launchFilePicker(HealthDataExportFormat format) {
        try {
            ActivityResultLauncher<String> picker = format == HealthDataExportFormat.EXCEL ? excelPicker : csvPicker;
            picker.launch(format.fileName);
        } catch (ActivityNotFoundException | SecurityException error) {
            model.pickerUnavailable();
        }
    }

    private void chooseDate(boolean start) {
        HealthDataFilter filter = model.filter();
        Calendar selected = Calendar.getInstance();
        selected.setTimeInMillis(start ? filter.from : filter.through);
        DatePickerDialog picker = new DatePickerDialog(requireContext(), (view, year, month, day) -> {
            Calendar result = Calendar.getInstance();
            result.clear();
            result.set(year, month, day);
            model.setDate(start, result.getTimeInMillis());
        }, selected.get(Calendar.YEAR), selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH));
        picker.setTitle(start ? R.string.health_export_from : R.string.health_export_through);
        picker.show();
    }

    private void render(HealthDataExportViewModel.Screen screen) {
        HealthDataFilter filter = model.filter();
        binding.exportFrom.setText(getString(R.string.health_export_from_value, Formats.day(filter.from)));
        binding.exportThrough.setText(getString(R.string.health_export_through_value, Formats.day(filter.through)));
        binding.exportFrom.setEnabled(!screen.busy);
        binding.exportThrough.setEnabled(!screen.busy);
        binding.exportType.setEnabled(!screen.busy);
        binding.viewHealthData.setEnabled(!screen.busy);
        binding.exportHealthExcel.setEnabled(!screen.busy && screen.excelAvailable);
        binding.exportHealthCsv.setEnabled(!screen.busy && !screen.rows.isEmpty());
        binding.exportStatus.setText(screen.status);
        binding.exportStatus.setVisibility(screen.status.isEmpty() ? View.GONE : View.VISIBLE);
        binding.exportCount.setText(getString(R.string.health_export_record_count, screen.rows.size()));
        binding.exportCount.setVisibility(screen.rows.isEmpty() ? View.GONE : View.VISIBLE);
        adapter.submitList(screen.rows);
    }

    @Override public void onDestroyView() {
        binding.exportRecords.setAdapter(null);
        adapter = null;
        binding = null;
        super.onDestroyView();
    }

    private static final class RecordAdapter extends ListAdapter<HealthDataRecord, RecordHolder> {
        private RecordAdapter() {
            super(new DiffUtil.ItemCallback<HealthDataRecord>() {
                @Override public boolean areItemsTheSame(@NonNull HealthDataRecord oldRow,
                                                         @NonNull HealthDataRecord newRow) {
                    return oldRow.id == newRow.id && oldRow.category.equals(newRow.category)
                            && oldRow.metric.equals(newRow.metric);
                }

                @Override public boolean areContentsTheSame(@NonNull HealthDataRecord oldRow,
                                                            @NonNull HealthDataRecord newRow) {
                    return oldRow.time == newRow.time && oldRow.value.equals(newRow.value)
                            && oldRow.unit.equals(newRow.unit) && oldRow.note.equals(newRow.note);
                }
            });
        }

        @NonNull @Override public RecordHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            return new RecordHolder(ItemHealthExportRecordBinding.inflate(LayoutInflater.from(parent.getContext()),
                    parent, false));
        }

        @Override public void onBindViewHolder(@NonNull RecordHolder holder, int position) {
            HealthDataRecord row = getItem(position);
            holder.row.recordSource.setText(row.category + " • " + Formats.date(row.time));
            holder.row.recordValue.setText(row.metric + "  " + row.value + (row.unit.isEmpty() ? "" : " " + row.unit));
            holder.row.recordNote.setText(row.note);
            holder.row.recordNote.setVisibility(row.note.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private static final class RecordHolder extends RecyclerView.ViewHolder {
        private final ItemHealthExportRecordBinding row;

        private RecordHolder(ItemHealthExportRecordBinding row) {
            super(row.getRoot());
            this.row = row;
        }
    }
}
