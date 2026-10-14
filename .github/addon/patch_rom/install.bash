# Kakathic

if [ -z "$(glog ime_color_dark)" ]; then
  slog ime_dimen '<dimen name="input_method_seek_bar_margin">6.5999756dp</dimen>
<dimen name="input_bottom_height">45.599976dp</dimen>
<dimen name="input_bottom_button_height">28.5dp</dimen>
<dimen name="input_bottom_button_margin_top">2.5dp</dimen>'
  slog ime_app com.google.android.inputmethod.latin
  slog ime_color '#f0f3f8'
  slog ime_color_dark '#1e1f21'
fi
