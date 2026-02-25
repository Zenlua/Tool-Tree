package com.tool.tree;

import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Point;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.webkit.MimeTypeMap;

import java.io.*;
import java.util.*;

public class DocumentsProvider extends android.provider.DocumentsProvider {

    private static final String ALL_MIME_TYPES = "*/*";
    private static final int MAX_SEARCH_RESULTS = 100;

    private File baseDir;

    @Override
    public boolean onCreate() {
        baseDir = getContext().getFilesDir();
        return true;
    }

    // ================= ROOT =================

    @Override
    public Cursor queryRoots(String[] projection) {

        MatrixCursor result = new MatrixCursor(
                projection != null ? projection :
                        new String[]{
                                Root.COLUMN_ROOT_ID,
                                Root.COLUMN_DOCUMENT_ID,
                                Root.COLUMN_TITLE,
                                Root.COLUMN_FLAGS,
                                Root.COLUMN_MIME_TYPES,
                                Root.COLUMN_AVAILABLE_BYTES
                        });

        String appName = getContext()
                .getApplicationInfo()
                .loadLabel(getContext().getPackageManager())
                .toString();

        MatrixCursor.RowBuilder row = result.newRow();
        row.add(Root.COLUMN_ROOT_ID, getDocId(baseDir));
        row.add(Root.COLUMN_DOCUMENT_ID, getDocId(baseDir));
        row.add(Root.COLUMN_TITLE, appName);
        row.add(Root.COLUMN_FLAGS,
                Root.FLAG_SUPPORTS_CREATE |
                Root.FLAG_SUPPORTS_SEARCH |
                Root.FLAG_SUPPORTS_IS_CHILD);
        row.add(Root.COLUMN_MIME_TYPES, ALL_MIME_TYPES);
        row.add(Root.COLUMN_AVAILABLE_BYTES, baseDir.getFreeSpace());

        return result;
    }

    // ================= QUERY =================

    @Override
    public Cursor queryDocument(String documentId, String[] projection)
            throws FileNotFoundException {

        MatrixCursor result = new MatrixCursor(
                projection != null ? projection : defaultDocProjection());

        includeFile(result, documentId, null);
        return result;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId,
                                      String[] projection,
                                      String sortOrder)
            throws FileNotFoundException {

        MatrixCursor result = new MatrixCursor(
                projection != null ? projection : defaultDocProjection());

        File parent = getFile(parentDocumentId);
        File[] files = parent.listFiles();

        if (files != null) {
            Arrays.sort(files, Comparator.comparing(File::getName));
            for (File f : files) {
                includeFile(result, null, f);
            }
        }

        return result;
    }

    // ================= OPEN =================

    @Override
    public ParcelFileDescriptor openDocument(String documentId,
                                             String mode,
                                             CancellationSignal signal)
            throws FileNotFoundException {

        File file = getFile(documentId);
        return ParcelFileDescriptor.open(
                file,
                ParcelFileDescriptor.parseMode(mode));
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String documentId,
                                                     Point sizeHint,
                                                     CancellationSignal signal)
            throws FileNotFoundException {

        File file = getFile(documentId);
        ParcelFileDescriptor pfd =
                ParcelFileDescriptor.open(file,
                        ParcelFileDescriptor.MODE_READ_ONLY);

        return new AssetFileDescriptor(pfd, 0, file.length());
    }

    // ================= CREATE =================

    @Override
    public String createDocument(String parentDocumentId,
                                 String mimeType,
                                 String displayName)
            throws FileNotFoundException {

        File parent = getFile(parentDocumentId);
        File file = resolveNameConflict(new File(parent, displayName));

        try {
            if (Document.MIME_TYPE_DIR.equals(mimeType)) {
                if (!file.mkdir())
                    throw new IOException();
            } else {
                if (!file.createNewFile())
                    throw new IOException();
            }
        } catch (IOException e) {
            throw new FileNotFoundException("Create failed");
        }

        return getDocId(file);
    }

    // ================= DELETE =================

    @Override
    public void deleteDocument(String documentId)
            throws FileNotFoundException {

        File file = getFile(documentId);
        deleteRecursive(file);
    }

    private void deleteRecursive(File file)
            throws FileNotFoundException {

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File c : children) {
                    deleteRecursive(c);
                }
            }
        }

        if (!file.delete())
            throw new FileNotFoundException("Delete failed");
    }

    // ================= RENAME =================

    @Override
    public String renameDocument(String documentId,
                                 String displayName)
            throws FileNotFoundException {

        File file = getFile(documentId);
        File newFile = resolveNameConflict(
                new File(file.getParentFile(), displayName));

        if (!file.renameTo(newFile))
            throw new FileNotFoundException("Rename failed");

        return getDocId(newFile);
    }

    // ================= COPY =================

    @Override
    public String copyDocument(String sourceDocumentId,
                               String targetParentDocumentId)
            throws FileNotFoundException {

        File source = getFile(sourceDocumentId);
        File targetParent = getFile(targetParentDocumentId);

        File target = resolveNameConflict(
                new File(targetParent, source.getName()));

        copyRecursive(source, target);

        return getDocId(target);
    }

    private void copyRecursive(File source, File target)
            throws FileNotFoundException {

        try {
            if (source.isDirectory()) {
                if (!target.mkdir())
                    throw new IOException();

                File[] children = source.listFiles();
                if (children != null) {
                    for (File child : children) {
                        copyRecursive(child,
                                new File(target, child.getName()));
                    }
                }
            } else {
                try (InputStream in = new FileInputStream(source);
                     OutputStream out = new FileOutputStream(target)) {

                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }
            }
        } catch (IOException e) {
            throw new FileNotFoundException("Copy failed");
        }
    }

    // ================= MOVE =================

    @Override
    public String moveDocument(String sourceDocumentId,
                               String sourceParentDocumentId,
                               String targetParentDocumentId)
            throws FileNotFoundException {

        File source = getFile(sourceDocumentId);
        File targetParent = getFile(targetParentDocumentId);

        File target = resolveNameConflict(
                new File(targetParent, source.getName()));

        if (!source.renameTo(target)) {
            copyRecursive(source, target);
            deleteRecursive(source);
        }

        return getDocId(target);
    }

    // ================= SEARCH =================

    @Override
    public Cursor querySearchDocuments(String rootId,
                                       String query,
                                       String[] projection)
            throws FileNotFoundException {

        MatrixCursor result = new MatrixCursor(
                projection != null ? projection : defaultDocProjection());

        LinkedList<File> queue = new LinkedList<>();
        queue.add(getFile(rootId));

        while (!queue.isEmpty() &&
                result.getCount() < MAX_SEARCH_RESULTS) {

            File file = queue.removeFirst();

            if (file.isDirectory()) {
                File[] children = file.listFiles();
                if (children != null)
                    Collections.addAll(queue, children);
            }

            if (file.getName().toLowerCase()
                    .contains(query.toLowerCase())) {
                includeFile(result, null, file);
            }
        }

        return result;
    }

    // ================= UTILS =================

    private String[] defaultDocProjection() {
        return new String[]{
                Document.COLUMN_DOCUMENT_ID,
                Document.COLUMN_DISPLAY_NAME,
                Document.COLUMN_SIZE,
                Document.COLUMN_MIME_TYPE,
                Document.COLUMN_LAST_MODIFIED,
                Document.COLUMN_FLAGS
        };
    }

    private String getDocId(File file) {
        return file.getAbsolutePath();
    }

    private File getFile(String docId)
            throws FileNotFoundException {

        File file = new File(docId);

        try {
            if (!file.getCanonicalPath()
                    .startsWith(baseDir.getCanonicalPath()))
                throw new FileNotFoundException("Access denied");
        } catch (IOException e) {
            throw new FileNotFoundException("Path error");
        }

        if (!file.exists())
            throw new FileNotFoundException("Not found");

        return file;
    }

    private File resolveNameConflict(File file) {

        if (!file.exists()) return file;

        String name = file.getName();
        String base = name;
        String ext = "";

        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            base = name.substring(0, dot);
            ext = name.substring(dot);
        }

        int i = 2;
        while (file.exists()) {
            file = new File(file.getParent(),
                    base + " (" + i++ + ")" + ext);
        }

        return file;
    }

    private String getMimeType(File file) {
        if (file.isDirectory())
            return Document.MIME_TYPE_DIR;

        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            String ext = name.substring(dot + 1);
            String mime = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(ext);
            if (mime != null) return mime;
        }
        return "application/octet-stream";
    }

    private void includeFile(MatrixCursor result,
                             String docId,
                             File file)
            throws FileNotFoundException {

        if (docId == null)
            docId = getDocId(file);
        else
            file = getFile(docId);

        int flags = Document.FLAG_SUPPORTS_DELETE |
                    Document.FLAG_SUPPORTS_RENAME |
                    Document.FLAG_SUPPORTS_COPY |
                    Document.FLAG_SUPPORTS_MOVE;

        if (file.isDirectory())
            flags |= Document.FLAG_DIR_SUPPORTS_CREATE;

        if (file.canWrite())
            flags |= Document.FLAG_SUPPORTS_WRITE;

        if (getMimeType(file).startsWith("image/"))
            flags |= Document.FLAG_SUPPORTS_THUMBNAIL;

        MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, docId);
        row.add(Document.COLUMN_DISPLAY_NAME, file.getName());
        row.add(Document.COLUMN_SIZE, file.length());
        row.add(Document.COLUMN_MIME_TYPE, getMimeType(file));
        row.add(Document.COLUMN_LAST_MODIFIED, file.lastModified());
        row.add(Document.COLUMN_FLAGS, flags);
    }
}