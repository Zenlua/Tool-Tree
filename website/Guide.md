# [Tool-Tree](https://zenlua.github.io/Tool-Tree)

**Application user guide**

- If you don't have root, you can see the following image so that **[MT Manager](https://mt2.cn/download)** can access the tool memory above.

**Input directory**

> `/sdcard/TREE`

- Place the input file in the following folder: `ROM` `APK`

**Output directory**

- No-Root: `/storage/xxxx-xxxx/data/files/home`
- Root: `/data/data/com.tool.tree/files/home`

- The home directory contains the TOOL and TREE folders

+ TOOL: Contains the decoded APK project
+ TREE: Contains the decoded ROM project

**Save folder**

- APK: `/sdcard/TREE/APK/out`
- ROM: `/sdcard/TREE/ROM/out`

+ Some special processes are stored elsewhere, so pay attention to the logs on the screen.

**Command in Terminal (root)**

- Use in MT Manager

```shell
source /data/data/com.tool.tree/files/root/executor.sh
apktool
```

**Instructions for granting file access permissions in No-Root mode**

- The following images are for no-root users only, to be able to access data in the tool-tree application

<img src="https://github.com/Zenlua/Tool-Tree/raw/refs/heads/main/.github/img/img1.jpg" alt="1" style="width:49%;"> <img src="https://github.com/Zenlua/Tool-Tree/raw/refs/heads/main/.github/img/img2.jpg" alt="2" style="width:49%;">
<img src="https://github.com/Zenlua/Tool-Tree/raw/refs/heads/main/.github/img/img3.jpg" alt="3" style="width:49%;"> <img src="https://github.com/Zenlua/Tool-Tree/raw/refs/heads/main/.github/img/img4.jpg" alt="4" style="width:49%;">
<img src="https://github.com/Zenlua/Tool-Tree/raw/refs/heads/main/.github/img/img5.jpg" alt="5" style="width:49%;"> <img src="https://github.com/Zenlua/Tool-Tree/raw/refs/heads/main/.github/img/img6.jpg" alt="6" style="width:49%;">
