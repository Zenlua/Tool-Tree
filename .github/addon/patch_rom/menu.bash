# Kakathic

MPAT="${0%/*}"
  echo '
  [[group.page.options]]
  type = "default"
  title = "@string/update_text add-on"
  auto-finish = true
  script = "'$MPAT'/index.bash update_addon"
  '
  