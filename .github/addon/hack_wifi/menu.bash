# Kakathic

MPAT="${0%/*}"

  echo '
  [[group.page.options]]
  type = "default"
  title = "@string/update_text"
  auto-off = true
  reload = true
  interruptible = false
  script = """
  taive "https://raw.githubusercontent.com/anbuinfosec/wipwn/refs/heads/main/main.py" '$MPAT'/main.py 2>&1
  chmod 755 '$MPAT'/main.py
  """

  [[group.page.options]]
  type = "refresh"
  style = "fab"
  icon = "'$ETC'/icon/Loading.png"
  '
