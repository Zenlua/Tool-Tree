# Kakathic

MPAT="${0%/*}"
path_modun="/data/adb/modules/Tool-Tree"
path_modun2="/data/adb/modules_update/Tool-Tree"

  echo '
  [[group.page.options]]
  type = "default"
  title = "@string/remove_text Module"
  reload = "true"
  script = """
  [ -d '$path_modun' ] && touch '$path_modun'/remove
  [ -d '$path_modun2' ] && rm -fr '$path_modun2'
  echo "The module will be removed after the device restarts." | trans -b $LANGUAGE
  """
  '
  