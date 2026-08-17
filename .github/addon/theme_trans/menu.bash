# Kakathic

source $MPAT/Add-on.bash

  echo '
    [[group.page.options]]
    link = "https://aistudio.google.com/api-keys"
    title = "'$generate_text'"
    silent = true
    
    [[group.page.options]]
    title = "'$check_key_text'"
    script = "MPAT='$MPAT' '$MPAT'/widget.bash check"
  '
