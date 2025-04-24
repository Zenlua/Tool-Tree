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
    private List<FileItem> originalList, filteredList;
    private Context context;
    private SelectionListener listener;

    public interface SelectionListener {
        void onSelectionChanged(List<FileItem> selected);
    }

    public FileAdapter(List<FileItem> list, Context ctx, SelectionListener listener) {
        this.originalList = new ArrayList<>(list);
        this.filteredList = new ArrayList<>(list);
        this.context = ctx;
        this.listener = listener;
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.file_item, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH holder, int position) {
        FileItem item = filteredList.get(position);
        holder.name.setText(item.getName());
        holder.checkBox.setChecked(item.isSelected());
        holder.icon.setImageResource(item.isDirectory()? android.R.drawable.ic_menu_agenda 
                                                         : android.R.drawable.ic_menu_save);

        holder.itemView.setOnClickListener(v -> {
            if (item.isDirectory()) {
                filteredList = FileUtils.listFiles(item.getPath(), true);
                originalList = new ArrayList<>(filteredList);
                notifyDataSetChanged();
                listener.onSelectionChanged(new ArrayList<>());
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            item.setSelected(!item.isSelected());
            notifyItemChanged(position);
            List<FileItem> sel = new ArrayList<>();
            for (FileItem fi: originalList) if (fi.isSelected()) sel.add(fi);
            listener.onSelectionChanged(sel);
            return true;
        });
    }

    @Override public int getItemCount() { return filteredList.size(); }

    public void filter(String query, String ext) {
        filteredList.clear();
        for (FileItem item: originalList) {
            boolean ok = item.getName().toLowerCase().contains(query.toLowerCase()) &&
                         (ext.isEmpty() || item.getName().toLowerCase().endsWith("."+ext.toLowerCase()));
            if (ok) filteredList.add(item);
        }
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView name; ImageView icon; CheckBox checkBox;
        VH(View v) {
            super(v);
            name = v.findViewById(R.id.fileName);
            icon = v.findViewById(R.id.fileIcon);
            checkBox = v.findViewById(R.id.fileCheck);
        }
    }
}
