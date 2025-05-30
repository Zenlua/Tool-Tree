# [Tool-Tree](https://zenlua.github.io/Tool-Tree)

Add-on file format

- Is a compressed file in .zip or .7z format

- After compression is complete, rename the extension to file.add

Internal structure of add-on file

```
file.add
└── (contents inside the file)
    ├── addon.sh
    ├── icon.png (256×256)
    ├── menu.sh|menu.xml
    ├── index.sh|index.xml
    ├── install.sh
    └── addon/
```

Contents of addon.sh file

```
# is a shell script file

id=test
author=Kakathic
version=1.0
versionCode=100
web=https://zenlua.github.io/Tool-Tree/Instruct.html

langen(){
name="Test"
description="Test add-on 1"
}

langvi(){
name="Thử nghiệm"
description="Mẫu thử nghiệm 1"
}

case "$LANGUAGE" in
    "vi")
    langvi;
    ;;
    *)
    langen;
    ;;
esac
```

- Both for containing language and language data

Add-on icon

- The icon.png file is an image file that can be 100x100 ~ 500x500 in size

- Can also rename icon_true.png, icon_false.png, true is dark mode, false is light mode.

Content inside menu.sh, menu.xml

- Mainly use page to move to index

- [See details](https://github.com/helloklf/kr-scripts/blob/master/docs/Page.md)

Content inside index.sh, index.xml

- There are many things that are difficult to say that can only be found out by yourself.

- [See details](https://github.com/helloklf/kr-scripts)

If there is an install.sh file in the add-on

- After unzipping the add-on file start running install.sh, there is actually no need to use this script.

**Download:** [Sample 1](https://github.com/Zenlua/Tool-Tree/raw/refs/heads/main/add-on/Test/Test1.add)








