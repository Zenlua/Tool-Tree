# [Tool-Tree](https://zenlua.github.io/Tool-Tree)

Add-on file format

- Is a compressed file in .zip or .7z format

- After compression is complete, rename the extension to file.add

Internal structure of add-on file

```
file.add
└── (contents inside the file)
    ├── addon.prop
    ├── icon.png (256×256)
    ├── menu.sh
    ├── index.sh|index.xml
    ├── early_start.sh
    └── language.sh
```

Contents of addon.prop file

```
# is a shell script file

id=test
id=Test
name=Test add-on
author=Kakathic
description=Short description
version=1.0
versionCode=100
root=false
```

Add-on icon

- The icon.png file is an image file that can be 100x100 ~ 500x500 in size

- Can also rename icon_true.png, icon_false.png, true is dark mode, false is light mode.

Content inside index.sh, index.xml

- There are many things that are difficult to say that can only be found out by yourself.

- [See details](https://github.com/helloklf/kr-scripts)

**Download:** [Sample](https://github.com/Zenlua/Tool-Tree/raw/refs/heads/main/add-on/Test)








