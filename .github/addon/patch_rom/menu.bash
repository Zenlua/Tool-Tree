# Kakathic

MPAT="${0%/*}"
  echo '
  [[group.page.options]]
  type = "default"
  title = "@string/update_text"
  script = """
  '$MPAT'/index.bash update_addon
  """
  '
  