package com.example.rootfilepicker;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class FileExplorerAdapter extends RecyclerView.Adapter<FileExplorerAdapter.ViewHolder> {

    private File currentDir;
    private final List<File> files = new ArrayList<>();
    private final List<File> selected = new ArrayList<>();
    private final Consumer<List<File>> selectionListener;
    private final String[] extensions;

    public FileExplorerAdapter(File root, String[] filters, Consumer<List<File>> listener) {
        this.currentDir = root;
        this.extensions = filters;
        this.selectionListener = listener;
        loadFiles();
    }

    private void loadFiles() {
        files.clear();
        File[] list = currentDir.listFiles();
        if (list != null) {
            Arrays.sort(list, (f1, f2) -> Boolean.compare(f2.isDirectory(), f1.isDirectory()));
            for (File file : list) {
                if (file.isDirectory() || extensions == null || matchExtension(file)) {
                    files.add(file);
                }
            }
        }
        notifyDataSetChanged();
    }

    private boolean matchExtension(File file) {
        for (String ext : extensions) {
            if (file.getName().toLowerCase().endsWith(ext.trim().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public void filter(String ext) {
        if (ext != null && !ext.isEmpty()) {
            currentDir = currentDir; // giữ nguyên thư mục, chỉ lọc theo đuôi
            files.removeIf(f -> !f.getName().toLowerCase().endsWith(ext.toLowerCase()));
            notifyDataSetChanged();
        } else {
            loadFiles();
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView tv = new TextView(parent.getContext());
        tv.setPadding(32, 32, 32, 32);
        return new ViewHolder(tv);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        File file = files.get(position);
        TextView textView = (TextView) holder.itemView;
        textView.setText(file.getName());
        textView.setTypeface(null, file.isDirectory() ? Typeface.BOLD : Typeface.NORMAL);
        textView.setBackgroundColor(selected.contains(file) ? 0xFFE0E0E0 : 0x00000000);

        textView.setOnClickListener(v -> {
            if (file.isDirectory()) {
                currentDir = file;
                loadFiles();
            } else {
                toggleSelection(file);
            }
        });

        textView.setOnLongClickListener(v -> {
            toggleSelection(file);
            return true;
        });
    }

    private void toggleSelection(File file) {
        if (selected.contains(file)) selected.remove(file);
        else selected.add(file);
        notifyDataSetChanged();
        selectionListener.accept(selected);
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
