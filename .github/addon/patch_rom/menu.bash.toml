# Kakathic

MPAT="${0%/*}"
echo '
  [[group.page.options]]
  type = "refresh"
  title = "@string/refresh_text"

  [[group.page.options]]
  key = "123"
  type = "default"
  title = "@string/update_text add-on"
  auto-finish = true
  script = """
  [ "$menu_id" == "123" ] && '$MPAT'/index.bash update_addon
  """
  '
