# Kakathic

MPAT="${0%/*}"
  echo '
  [[group.page.options]]
  type = "default"
  title = "@string/update_text"
  script = """
  check_add "'$MPAT'"
  echo
  echo "English:"
  cat '$MPAT'/changelog.txt
  echo
  echo "$LANGUAGE-$COUNTRY"
  cat '$MPAT'/changelog.txt | trans $LANGUAGE-$COUNTRY
  """
  '
  