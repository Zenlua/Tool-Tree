
// FileListAdapter.java
package com.example.rootfilepicker;

import android.content.Context;
import android.view.*;
import android.widget.*;
import java.io.File;
import java.util.List;

public class FileListAdapter extends BaseAdapter {
    private final Context context;
    private final List<FileItem> items;
    private final List<FileItem> selected;

    public FileListAdapter(Context context, List<FileItem> items, List<FileItem> selected) {
        this.context = context;
        this.items = items;
        this.selected = selected;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int i) {
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_file, viewGroup, false);
        }

        TextView name = view.findViewById(R.id.name);
        ImageView icon = view.findViewById(R.id.icon);
        ImageView check = view.findViewById(R.id.check);

        File file = items.get(i).getFile();
        name.setText(file.getName());
        icon.setImageResource(file.isDirectory() ? android.R.drawable.ic_menu_agenda : android.R.drawable.ic_menu_save);
        check.setVisibility(selected.contains(items.get(i)) ? View.VISIBLE : View.INVISIBLE);

        return view;
    }
}
