package com.omarea.krscript.executor

import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.omarea.krscript.model.RunnableNode
import com.omarea.krscript.model.ShellHandlerBase
import java.io.DataOutputStream
import java.util.Objects

/**
 * Created by Hello on 2018/04/01.
 */
class ShellExecutor {
    private var started = false
    private val sessionTag = "pio_" + System.currentTimeMillis()

    private fun killProcess(context: Context) {
        ScriptEnvironmen.executeResultRoot(
            context,
            String.format("shell_progres='%s' killtree", sessionTag),
            null
        )
        // KeepShellPublic.INSTANCE.doCmdSync(String.format("kill -s 1 `pgrep -f %s`", sessionTag));
    }

    /**
     * 执行脚本
     */
    fun execute(
        context: Context,
        nodeInfo: RunnableNode?,
        cmds: String,
        onExit: Runnable?,
        params: HashMap<String, String>?,
        shellHandlerBase: ShellHandlerBase
    ): Process? {
        if (started) {
            return null
        }

        val process = ScriptEnvironmen.getRuntime()
        if (process == null) {
            Toast.makeText(context, "Failed to start command line process", Toast.LENGTH_SHORT).show()
            onExit?.run()
        } else {
            val forceStopRunnable: Runnable? =
                if (nodeInfo != null && (nodeInfo.interruptable || nodeInfo.shell == RunnableNode.shellModeBgTask)) {
                    Runnable {
                        /*
                        // 没啥用，这个pid和在shell创建的子进程不是父子关系，杀死此进程对shell里创建的进程毫无影响
                        int pid = -1;
                        if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
                            try {
                                Class cl = process.getClass();
                                Field field = cl.getDeclaredField("pid");
                                field.setAccessible(true);
                                Object pidObject = field.get(process);
                                pid = (Integer) pidObject;
                            } catch (Exception ignored) {}
                        }
                        */
                        killProcess(context)

                        try {
                            process.inputStream.close()
                        } catch (ignored: Exception) {
                        }
                        try {
                            process.outputStream.close()
                        } catch (ignored: Exception) {
                        }
                        try {
                            process.errorStream.close()
                        } catch (ignored: Exception) {
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            try {
                                process.destroyForcibly()
                            } catch (ex: Exception) {
                                Log.e("KrScriptError", ex.message ?: ex.toString())
                            }
                        } else {
                            try {
                                process.destroy()
                            } catch (ex: Exception) {
                                Log.e("KrScriptError", ex.message ?: ex.toString())
                            }
                        }
                    }
                } else null
            SimpleShellWatcher().setHandler(context, process, shellHandlerBase, onExit)

            val outputStream = process.outputStream
            val dataOutputStream = DataOutputStream(outputStream)
            // Gắn stdin của process vào shellHandlerBase để ô nhập liệu trên UI (DialogLogFragment)
            // có thể ghi trực tiếp dữ liệu người dùng gõ vào trong lúc script đang chạy.
            shellHandlerBase.bindStdin(dataOutputStream)
            try {
                shellHandlerBase.sendMessage(shellHandlerBase.obtainMessage(ShellHandlerBase.EVENT_START, "shell@android:\n"))
                shellHandlerBase.sendMessage(shellHandlerBase.obtainMessage(ShellHandlerBase.EVENT_START, "$cmds\n\n"))
                shellHandlerBase.onStart(forceStopRunnable)
                dataOutputStream.writeBytes("sleep 0.2;\n")

                val needInput = nodeInfo != null && nodeInfo.needInput
                ScriptEnvironmen.executeShell(context, dataOutputStream, cmds, params, nodeInfo, sessionTag, needInput)
            } catch (ex: Exception) {
                process.destroy()
            }
            started = true
        }
        return process
    }
}
