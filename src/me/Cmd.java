package me;

import java.io.Console;

public class Cmd implements Runnable {

    private Console console = System.console();

    @Override
    public void run() {
        while (true) {
            try {
                // System.out.print("> ");
                // String cmd = br.readLine();
                String cmd = this.console.readLine("> ");
                cmdHandle(cmd);
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        }
    }

    private void cmdHandle(String cmd) {
        if (cmd.equalsIgnoreCase("exit")) {
            System.exit(0);
            return;
        }
        System.out.println("无法识别的命令: \"" + cmd + "\"");
    }
}
