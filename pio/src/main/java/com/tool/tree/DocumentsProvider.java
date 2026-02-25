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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;

public class DocumentsProvider extends android.provider.DocumentsProvider {

    private static final String ALL_MIME_TYPES = "*/*";
    private static final int MAX_SEARCH_RESULTS = 50;

    private File baseDir;

    private static final String[] DEFAULT_ROOT_PROJECTION = new String[]{
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_MIME_TYPES,
            Root.COLUMN_FLAGS,
            Root.COLUMN_ICON,
            Root.COLUMN_TITLE,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_AVAILABLE_BYTES
    };

    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[]{
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_FLAGS,
            Document.COLUMN_SIZE
    };

    @Override
    public boolean onCreate() {
        baseDir = getContext().getFilesDir();
        return true;
    }

    // ---------------- ROOT ----------------

    @Override
    public Cursor queryRoots(String[] projection) {
    
        MatrixCursor result = new MatrixCursor(
                projection != null ? projection : DEFAULT_ROOT_PROJECTION);
    
        String appName = getContext()
                .getApplicationInfo()
                .loadLabel(getContext().getPackageManager())
                .toString();
    
        MatrixCursor.RowBuilder row = result.newRow();
        row.add(Root.COLUMN_ROOT_ID, getDocIdForFile(baseDir));
        row.add(Root.COLUMN_DOCUMENT_ID, getDocIdForFile(baseDir));
        row.add(Root.COLUMN_TITLE, appName);
        row.add(Root.COLUMN_FLAGS,
                Root.FLAG_SUPPORTS_CREATE |
                Root.FLAG_SUPPORTS_SEARCH |
                Root.FLAG_SUPPORTS_IS_CHILD);
        row.add(Root.COLUMN_MIME_TYPES, "*/*");
        row.add(Root.COLUMN_AVAILABLE_BYTES, baseDir.getFreeSpace());
    
        return result;
    }

    // ---------------- DOCUMENT ----------------

    @Override
    public Cursor queryDocument(String documentId, String[] projection)
            throws FileNotFoundException {

        MatrixCursor result = new MatrixCursor(
                projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION);

        includeFile(result, documentId, null);
        return result;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId,
                                      String[] projection,
                                      String sortOrder)
            throws FileNotFoundException {

        MatrixCursor result = new MatrixCursor(
                projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION);

        File parent = getFileForDocId(parentDocumentId);
        File[] files = parent.listFiles();
        if (files != null) {
            for (File file : files) {
                includeFile(result, null, file);
            }
        }
        return result;
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId,
                                             String mode,
                                             CancellationSignal signal)
            throws FileNotFoundException {

        File file = getFileForDocId(documentId);
        int accessMode = ParcelFileDescriptor.parseMode(mode);
        return ParcelFileDescriptor.open(file, accessMode);
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String documentId,
                                                     Point sizeHint,
                                                     CancellationSignal signal)
            throws FileNotFoundException {

        File file = getFileForDocId(documentId);
        ParcelFileDescriptor pfd =
                ParcelFileDescriptor.open(file,
                        ParcelFileDescriptor.MODE_READ_ONLY);

        return new AssetFileDescriptor(pfd, 0, file.length());
    }

    @Override
    public String createDocument(String parentDocumentId,
                                 String mimeType,
                                 String displayName)
            throws FileNotFoundException {

        File parent = getFileForDocId(parentDocumentId);
        File newFile = new File(parent, displayName);

        int noConflictId = 2;
        while (newFile.exists()) {
            newFile = new File(parent,
                    displayName + " (" + noConflictId++ + ")");
        }

        try {
            boolean success;
            if (Document.MIME_TYPE_DIR.equals(mimeType)) {
                success = newFile.mkdir();
            } else {
                success = newFile.createNewFile();
            }

            if (!success) {
                throw new FileNotFoundException("Create failed");
            }

        } catch (IOException e) {
            throw new FileNotFoundException("Create failed");
        }

        return getDocIdForFile(newFile);
    }

    @Override
    public void deleteDocument(String documentId)
            throws FileNotFoundException {

        File file = getFileForDocId(documentId);
        if (!file.delete()) {
            throw new FileNotFoundException("Delete failed");
        }
    }

    @Override
    public String getDocumentType(String documentId)
            throws FileNotFoundException {

        return getMimeType(getFileForDocId(documentId));
    }

    // ---------------- SEARCH ----------------

    @Override
    public Cursor querySearchDocuments(String rootId,
                                       String query,
                                       String[] projection)
            throws FileNotFoundException {

        MatrixCursor result = new MatrixCursor(
                projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION);

        File parent = getFileForDocId(rootId);
        LinkedList<File> pending = new LinkedList<>();
        pending.add(parent);

        while (!pending.isEmpty() &&
                result.getCount() < MAX_SEARCH_RESULTS) {

            File file = pending.removeFirst();

            try {
                if (!file.getCanonicalPath()
                        .startsWith(baseDir.getCanonicalPath())) {
                    continue;
                }
            } catch (IOException ignored) {}

            if (file.isDirectory()) {
                File[] children = file.listFiles();
                if (children != null) {
                    Collections.addAll(pending, children);
                }
            } else if (file.getName()
                    .toLowerCase()
                    .contains(query.toLowerCase())) {

                includeFile(result, null, file);
            }
        }

        return result;
    }

    @Override
    public boolean isChildDocument(String parentDocumentId,
                                   String documentId) {
        try {
            File parent = getFileForDocId(parentDocumentId);
            File child = getFileForDocId(documentId);

            return child.getCanonicalPath()
                    .startsWith(parent.getCanonicalPath());
        } catch (Exception e) {
            return false;
        }
    }

    // ---------------- UTILS ----------------

    private String getDocIdForFile(File file) {
        return file.getAbsolutePath();
    }

    private File getFileForDocId(String docId)
            throws FileNotFoundException {

        File file = new File(docId);

        if (!file.exists())
            throw new FileNotFoundException(docId);

        try {
            if (!file.getCanonicalPath()
                    .startsWith(baseDir.getCanonicalPath())) {
                throw new FileNotFoundException("Access denied");
            }
        } catch (IOException e) {
            throw new FileNotFoundException("Path error");
        }

        return file;
    }

    private String getMimeType(File file) {
        if (file.isDirectory()) {
            return Document.MIME_TYPE_DIR;
        }

        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            String ext = name.substring(lastDot + 1).toLowerCase();
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

        if (docId == null) {
            docId = getDocIdForFile(file);
        } else {
            file = getFileForDocId(docId);
        }

        int flags = 0;

        if (file.isDirectory()) {
            if (file.canWrite())
                flags |= Document.FLAG_DIR_SUPPORTS_CREATE;
        } else if (file.canWrite()) {
            flags |= Document.FLAG_SUPPORTS_WRITE;
        }

        if (file.getParentFile() != null &&
                file.getParentFile().canWrite()) {
            flags |= Document.FLAG_SUPPORTS_DELETE;
        }

        if (getMimeType(file).startsWith("image/")) {
            flags |= Document.FLAG_SUPPORTS_THUMBNAIL;
        }

        MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, docId);
        row.add(Document.COLUMN_DISPLAY_NAME, file.getName());
        row.add(Document.COLUMN_SIZE, file.length());
        row.add(Document.COLUMN_MIME_TYPE, getMimeType(file));
        row.add(Document.COLUMN_LAST_MODIFIED, file.lastModified());
        row.add(Document.COLUMN_FLAGS, flags);
    }
}