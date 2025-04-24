package com.example.fileselector;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.VH> {
    private Context context;
    private List<FileItem> fullList;
    private List<FileItem> filteredList;

    public FileAdapter(Context ctx, List<FileItem> data) {
        this.context = ctx;
        this.fullList = new ArrayList<>(data);
        this.filteredList = new ArrayList<>(data);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                    .inflate(R.layout.file_item, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        FileItem item = filteredList.get(position);
        holder.name.setText(item.getName());
        holder.icon.setImageResource(
            item.isDirectory() ? android.R.drawable.ic_menu_agenda
                               : android.R.drawable.ic_menu_info_details);
        holder.checkBox.setChecked(item.isSelected());
        holder.checkBox.setOnCheckedChangeListener((b, isChecked) -> {
            item.setSelected(isChecked);
        });
        holder.itemView.setOnClickListener(v -> {
            boolean newState = !item.isSelected();
            item.setSelected(newState);
            holder.checkBox.setChecked(newState);
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public List<FileItem> getSelectedItems() {
        List<FileItem> sel = new ArrayList<>();
        for (FileItem f : fullList) {
            if (f.isSelected()) sel.add(f);
        }
        return sel;
    }

    /**
     * Lọc theo tên và phần mở rộng
     */
    public void filter(String query, String extFilter) {
        filteredList.clear();
        String q = query == null ? "" : query.toLowerCase();
        String ex = extFilter == null ? "" : extFilter.toLowerCase();
        for (FileItem item : fullList) {
            boolean matchName = item.getName().toLowerCase().contains(q);
            boolean matchExt = ex.isEmpty() || item.getName().toLowerCase().endsWith(ex);
            if (matchName && matchExt) {
                filteredList.add(item);
            }
        }
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView name;
        ImageView icon;
        CheckBox checkBox;

        VH(View v) {
            super(v);
            name      = v.findViewById(R.id.fileName);
            icon      = v.findViewById(R.id.fileIcon);
            checkBox  = v.findViewById(R.id.fileCheck);
        }
    }
}
