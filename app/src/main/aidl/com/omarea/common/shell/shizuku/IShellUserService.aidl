package com.omarea.common.shell.shizuku;

interface IShellUserService {
    String execCommand(String cmd);
    void destroy() = 16777114;
}
