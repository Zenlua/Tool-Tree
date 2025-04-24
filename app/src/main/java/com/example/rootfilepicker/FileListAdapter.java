public class FileListAdapter extends BaseAdapter implements Filterable {
    private List<FileItem> originalList;
    private List<FileItem> filteredList;
    private final Context context;

    public FileListAdapter(Context context, List<FileItem> data) {
        this.context = context;
        this.originalList = data;
        this.filteredList = new ArrayList<>(data);
    }

    @Override
    public int getCount() {
        return filteredList.size();
    }

    @Override
    public Object getItem(int position) {
        return filteredList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // inflate and bind view...
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<FileItem> results = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    results.addAll(originalList);
                } else {
                    String query = constraint.toString().toLowerCase();
                    for (FileItem item : originalList) {
                        if (item.getFile().getName().toLowerCase().contains(query)) {
                            results.add(item);
                        }
                    }
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = results;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredList.clear();
                filteredList.addAll((List<FileItem>) results.values);
                notifyDataSetChanged();
            }
        };
    }
}
