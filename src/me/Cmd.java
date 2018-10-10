package me;

//import java.io.BufferedReader;
//import java.io.Console;
//import java.io.IOException;
//import java.io.InputStreamReader;

public class Cmd implements Runnable {

    // private InputStreamReader input = new InputStreamReader(System.in);
    // private BufferedReader br = new BufferedReader(input);

    @Override
    public void run() {
        while (true) {
            try {
                // System.out.print("> ");
                // String cmd = br.readLine();
                String cmd = Server.console.readLine("> ");
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
