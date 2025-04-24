package com.example.fileselector;

import com.example.fileselector.R;


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
    private SelectionListener selectionListener;

    public interface SelectionListener {
        void onSelectionChanged(List<FileItem> selected);
    }

    public FileAdapter(List<FileItem> list, Context ctx, SelectionListener listener) {
        this.originalList = new ArrayList<>(list);
        this.filteredList = list;
        this.context = ctx;
        this.selectionListener = listener;
    }

    @NonNull
    @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.file_item, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH holder, int position) {
        FileItem item = filteredList.get(position);
        holder.name.setText(item.getName());
        holder.checkBox.setChecked(item.isSelected());
        holder.icon.setImageResource(item.isDirectory() ? android.R.drawable.ic_menu_agenda : android.R.drawable.ic_menu_save);

        holder.itemView.setOnClickListener(v -> {
            if (item.isDirectory()) {
                // open folder
                filteredList = FileUtils.listFiles(item.getPath(), true);
                notifyDataSetChanged();
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            item.setSelected(!item.isSelected());
            notifyItemChanged(position);
            selectionListener.onSelectionChanged(null);
            selectionListener.onSelectionChanged(null);
            return true;
        });
    }

    @Override public int getItemCount() { return filteredList.size(); }

    public List<FileItem> getItems() { return filteredList; }

    public void filter(String query, String ext) {
        List<FileItem> temp = new ArrayList<>();
        for (FileItem item : originalList) {
            boolean matchesQuery = item.getName().toLowerCase().contains(query.toLowerCase());
            boolean matchesExt = ext.isEmpty() || item.getName().toLowerCase().endsWith("." + ext.toLowerCase());
            if (matchesQuery && matchesExt) temp.add(item);
        }
        filteredList = temp;
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView name;
        ImageView icon;
        CheckBox checkBox;
        VH(View v) {
            super(v);
            name = v.findViewById(R.id.fileName);
            icon = v.findViewById(R.id.fileIcon);
            checkBox = v.findViewById(R.id.fileCheck);
        }
    }
}
