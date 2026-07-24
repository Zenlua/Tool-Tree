package com.tool.tree.ui;

import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.omarea.common.ui.DialogHelper;
import com.omarea.common.ui.ProgressBarDialog;
import com.tool.tree.R;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public class AdapterFileSelector extends BaseAdapter {
    private File[] fileArray;
    private Runnable fileSelected;
    private File currentDir;
    private File selectedFile;
    private final Handler handler = new Handler();
    private ProgressBarDialog progressBarDialog;
    // Danh sách đuôi file được phép (đã có dấu chấm ở đầu, chữ thường), null/rỗng = không giới hạn
    private String[] extensions;
    private boolean hasParent = false; // 是否还有父级
    private String rootDir = "/"; // 根目录
    private final boolean leaveRootDir = true; // 是否允许离开设定的rootDir到更父级的目录去
    private boolean folderChooserMode = false; // 是否是目录选择模式（目录选择模式下不显示文件，长按目录选中）

    // Chế độ chọn nhiều mục (nhiều file, hoặc nhiều thư mục)
    private boolean multipleMode = false;
    // Giữ thứ tự đã chọn
    private final LinkedHashSet<File> selectedFiles = new LinkedHashSet<>();
    // Được gọi mỗi khi danh sách đã chọn thay đổi (để activity cập nhật nút "Xong"/số lượng đã chọn)
    private SelectionChangedListener selectionChangedListener;

    public interface SelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    private AdapterFileSelector(File rootDir, Runnable fileSelected, ProgressBarDialog progressBarDialog, String extension) {
        init(rootDir, fileSelected, progressBarDialog, extension);
    }

    public static AdapterFileSelector FolderChooser(File rootDir, Runnable fileSelected, ProgressBarDialog progressBarDialog) {
        AdapterFileSelector adapterFileSelector = new AdapterFileSelector(rootDir, fileSelected, progressBarDialog, null);
        adapterFileSelector.folderChooserMode = true;
        return adapterFileSelector;
    }

    public static AdapterFileSelector FolderChooser(File rootDir, Runnable fileSelected, ProgressBarDialog progressBarDialog, boolean multiple) {
        AdapterFileSelector adapterFileSelector = FolderChooser(rootDir, fileSelected, progressBarDialog);
        adapterFileSelector.multipleMode = multiple;
        return adapterFileSelector;
    }

    public static AdapterFileSelector FileChooser(File rootDir, Runnable fileSelected, ProgressBarDialog progressBarDialog, String extension) {
        AdapterFileSelector adapterFileSelector = new AdapterFileSelector(rootDir, fileSelected, progressBarDialog, extension);
        adapterFileSelector.folderChooserMode = false;
        return adapterFileSelector;
    }

    public static AdapterFileSelector FileChooser(File rootDir, Runnable fileSelected, ProgressBarDialog progressBarDialog, String extension, boolean multiple) {
        AdapterFileSelector adapterFileSelector = FileChooser(rootDir, fileSelected, progressBarDialog, extension);
        adapterFileSelector.multipleMode = multiple;
        return adapterFileSelector;
    }

    private void init(File rootDir, Runnable fileSelected, ProgressBarDialog progressBarDialog, String extension) {
        this.rootDir = rootDir.getAbsolutePath();
        this.fileSelected = fileSelected;
        this.progressBarDialog = progressBarDialog;
        // Hỗ trợ nhiều đuôi file, phân cách bằng dấu phẩy, ví dụ: "zip,apk,7z"
        if (extension != null && !extension.trim().isEmpty()) {
            String[] parts = extension.split(",");
            ArrayList<String> list = new ArrayList<>();
            for (String part : parts) {
                String trimmed = part.trim().toLowerCase(Locale.getDefault());
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (!trimmed.startsWith(".")) {
                    trimmed = "." + trimmed;
                }
                list.add(trimmed);
            }
            this.extensions = list.toArray(new String[0]);
        } else {
            this.extensions = null;
        }
        loadDir(rootDir);
    }

    private boolean matchesExtension(File file) {
        if (extensions == null || extensions.length == 0) {
            return true;
        }
        String name = file.getName().toLowerCase(Locale.getDefault());
        for (String ext : extensions) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private void loadDir(final File dir) {
        // progressBarDialog.showDialog("Loading...");
        new Thread(() -> {
            File parent = dir.getParentFile();
            if (parent != null) {
                String parentPath = parent.getAbsolutePath();
                hasParent = parent.exists() && parent.canRead() && (leaveRootDir || !(rootDir.startsWith(parentPath) && rootDir.length() > parentPath.length()));
            } else {
                hasParent = false;
            }

            if (dir.exists() && dir.canRead()) {
                File[] files = dir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File fileItem) {
                        if (folderChooserMode) {
                            return fileItem.isDirectory();
                        } else {
                            return fileItem.exists() && (fileItem.isDirectory() || matchesExtension(fileItem));
                        }
                    }
                });

                // 文件排序
                for (int i = 0; i < files.length; i++) {
                    for (int j = i + 1; j < files.length; j++) {
                        if ((files[j].isDirectory() && files[i].isFile())) {
                            File t = files[i];
                            files[i] = files[j];
                            files[j] = t;
                        } else if (files[j].isDirectory() == files[i].isDirectory() && (files[j].getName().toLowerCase().compareTo(files[i].getName().toLowerCase()) < 0)) {
                            File t = files[i];
                            files[i] = files[j];
                            files[j] = t;
                        }
                    }
                }
                fileArray = files;
            }
            currentDir = dir;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                    progressBarDialog.hideDialog();
                    if (selectionChangedListener != null) {
                        selectionChangedListener.onSelectionChanged(selectedFiles.size());
                    }
                }
            });
        }).start();
    }

    public boolean goParent() {
        if (hasParent) {
            loadDir(new File(currentDir.getParent()));
            return true;
        }
        return false;
    }

    @Override
    public int getCount() {
        if (hasParent) {
            if (fileArray == null) {
                return 1;
            }
            return fileArray.length + 1;
        } else {
            if (fileArray == null) {
                return 0;
            }
            return fileArray.length;
        }
    }

    public void refresh() {
        if (this.currentDir != null) {
            this.loadDir(currentDir);
        }
    }

    @Override
    public Object getItem(int position) {
        if (hasParent) {
            if (position == 0) {
                return new File(currentDir.getParent());
            } else {
                return fileArray[position - 1];
            }
        } else {
            return fileArray[position];
        }
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    private void toggleSelection(File file) {
        if (selectedFiles.contains(file)) {
            selectedFiles.remove(file);
        } else {
            selectedFiles.add(file);
        }
        notifyDataSetChanged();
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(selectedFiles.size());
        }
    }

    // Một tệp/thư mục có phải là đối tượng "có thể chọn" (hiện checkbox) trong danh sách hiện tại không.
    // Ở chế độ chọn thư mục: mọi mục trong fileArray đều là thư mục -> có thể chọn.
    // Ở chế độ chọn tệp: chỉ tệp mới có thể chọn, thư mục chỉ dùng để điều hướng.
    private boolean isSelectable(File file) {
        return folderChooserMode || !file.isDirectory();
    }

    // Thư mục hiện tại đã được chọn hết (mọi mục có thể chọn) hay chưa - dùng để đồng bộ checkbox "Chọn tất cả".
    public boolean isAllCurrentDirSelected() {
        if (fileArray == null || fileArray.length == 0) {
            return false;
        }
        boolean hasSelectable = false;
        for (File file : fileArray) {
            if (isSelectable(file)) {
                hasSelectable = true;
                if (!selectedFiles.contains(file)) {
                    return false;
                }
            }
        }
        return hasSelectable;
    }

    // Chọn/bỏ chọn tất cả các mục có thể chọn trong thư mục đang hiển thị.
    public void setSelectAllState(boolean selectAll) {
        if (fileArray == null) {
            return;
        }
        for (File file : fileArray) {
            if (!isSelectable(file)) {
                continue;
            }
            if (selectAll) {
                selectedFiles.add(file);
            } else {
                selectedFiles.remove(file);
            }
        }
        notifyDataSetChanged();
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(selectedFiles.size());
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view;
        if (hasParent && position == 0) {
            view = View.inflate(parent.getContext(), R.layout.list_item_dir, null);
            ((TextView) (view.findViewById(R.id.ItemTitle))).setText("...");
            View checkBox = view.findViewById(R.id.ItemCheckBox);
            if (checkBox != null) {
                checkBox.setVisibility(View.GONE);
            }
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    goParent();
                }
            });
            return view;
        } else {
            final File file = (File) getItem(position);
            if (file.isDirectory()) {
                view = View.inflate(parent.getContext(), R.layout.list_item_dir, null);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!file.exists()) {
                            Toast.makeText(view.getContext(), "The selected file has been deleted. Please select again!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        File[] files = file.listFiles();
                        if (files != null && files.length > 0) {
                            loadDir(file);
                        } else {
                            Snackbar.make(view, view.getContext().getString(R.string.no_files_in_directory), Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });
                if (folderChooserMode) {
                    final CheckBox checkBox = view.findViewById(R.id.ItemCheckBox);
                    if (multipleMode) {
                        if (checkBox != null) {
                            checkBox.setVisibility(View.VISIBLE);
                            checkBox.setChecked(selectedFiles.contains(file));
                            checkBox.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (!file.exists()) {
                                        Toast.makeText(view.getContext(), "The selected directory has been deleted. Please select another one!", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    toggleSelection(file);
                                }
                            });
                        }
                        // Nhấn giữ vẫn dùng để chọn nhanh 1 thư mục (giữ hành vi cũ, thêm vào danh sách đã chọn)
                        view.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                if (!file.exists()) {
                                    Toast.makeText(view.getContext(), "The selected directory has been deleted. Please select another one!", Toast.LENGTH_SHORT).show();
                                    return true;
                                }
                                toggleSelection(file);
                                return true;
                            }
                        });
                    } else {
                        if (checkBox != null) {
                            checkBox.setVisibility(View.GONE);
                        }
                        view.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                DialogHelper.Companion.confirm(view.getContext(), view.getContext().getString(R.string.dialog_title_select_directory), file.getAbsolutePath(), new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!file.exists()) {
                                            Toast.makeText(view.getContext(), "The selected directory has been deleted. Please select another one!", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        selectedFile = file;
                                        fileSelected.run();
                                    }
                                }, new Runnable() {
                                    @Override
                                    public void run() {

                                    }
                                });
                                return true;
                            }
                        });
                    }
                } else {
                    View checkBox = view.findViewById(R.id.ItemCheckBox);
                    if (checkBox != null) {
                        checkBox.setVisibility(View.GONE);
                    }
                }
            } else {
                view = View.inflate(parent.getContext(), R.layout.list_item_file, null);
                long fileLength = file.length();
                String fileSize;
                if (fileLength < 1024) {
                    fileSize = fileLength + "B";
                } else if (fileLength < 1048576) {
                    fileSize = String.format("%sKB", String.format("%.2f", (file.length() / 1024.0)));
                } else if (fileLength < 1073741824) {
                    fileSize = String.format("%sMB", String.format("%.2f", (file.length() / 1048576.0)));
                } else {
                    fileSize = String.format("%sGB", String.format("%.2f", (file.length() / 1073741824.0)));
                }

                ((TextView) (view.findViewById(R.id.ItemText))).setText(fileSize);

                final CheckBox checkBox = view.findViewById(R.id.ItemCheckBox);
                if (multipleMode) {
                    if (checkBox != null) {
                        checkBox.setVisibility(View.VISIBLE);
                        checkBox.setChecked(selectedFiles.contains(file));
                    }
                    View.OnClickListener toggleListener = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (!file.exists()) {
                                Toast.makeText(view.getContext(), "The selected file has been deleted. Please select again!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            toggleSelection(file);
                        }
                    };
                    view.setOnClickListener(toggleListener);
                    if (checkBox != null) {
                        checkBox.setOnClickListener(toggleListener);
                    }
                } else {
                    if (checkBox != null) {
                        checkBox.setVisibility(View.GONE);
                    }
                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            DialogHelper.Companion.confirm(view.getContext(), view.getContext().getString(R.string.dialog_title_select_file), file.getAbsolutePath(), new Runnable() {
                                @Override
                                public void run() {
                                    if (!file.exists()) {
                                        Toast.makeText(view.getContext(), "The selected file has been deleted. Please select again!", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    selectedFile = file;
                                    fileSelected.run();
                                }
                            }, new Runnable() {
                                @Override
                                public void run() {

                                }
                            });
                        }
                    });
                }
            }
            ((TextView) (view.findViewById(R.id.ItemTitle))).setText(file.getName());
            return view;
        }
    }

    public File getSelectedFile() {
        return this.selectedFile;
    }

    public boolean isMultipleMode() {
        return multipleMode;
    }

    public List<File> getSelectedFiles() {
        return new ArrayList<>(selectedFiles);
    }

    public int getSelectedCount() {
        return selectedFiles.size();
    }

    public void setSelectionChangedListener(SelectionChangedListener listener) {
        this.selectionChangedListener = listener;
    }
}
