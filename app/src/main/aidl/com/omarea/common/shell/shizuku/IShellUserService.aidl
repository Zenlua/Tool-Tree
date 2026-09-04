package com.omarea.common.shell.shizuku;

interface IShellUserService {
    String execCommand(String cmd) = 1;
    void destroy() = 16777114;
}
