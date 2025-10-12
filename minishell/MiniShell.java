package minishell;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import tokenizer.MissingFileException;
import tokenizer.TCommand;
import tokenizer.TLine;
import tokenizer.Tokenizer;

public class MiniShell {
    private static final String prompt="ms$";
    private File currentDirectory= new File(System.getProperty("user.dir"));

    public void run() {
        Scanner sc=new Scanner(System.in);

        while(true) {
            System.out.print(this.prompt+" ");

            if(!sc.hasNextLine()) {
                System.out.println();
                break;
            }

            String input=sc.nextLine().trim();
            if(input.isEmpty()) continue;

            try {
                TLine line=Tokenizer.tokenize(input);

                if(line.getNcommands() > 0) {
                    TCommand cmd = line.getCommands().get(0);
                    String cmdName= cmd.getArgv().get(0);

                    if(cmdName.equals("exit")) {
                        System.out.println("Saliendo...");
                        break;
                    } else if (cmdName.equals("cd")) {
                        handleCd(cmd);
                    } else {
                        executeExternalCommand(cmd);
                    }

                    System.out.println(line);
                }
            } catch (MissingFileException e) {
                e.printStackTrace();
            }
        }

        sc.close();
    }

    private void handleCd(TCommand cmd) {
        List<String> args=cmd.getArgv();

        if(args.size() < 2) {
            this.currentDirectory=new File(System.getProperty("user.home"));
        } else {
            File targetDir=new File(args.get(1));
            if(!targetDir.isAbsolute()) {
                targetDir=new File(this.currentDirectory, args.get(1));
            }
            if(targetDir.exists() && targetDir.isDirectory()) {
                this.currentDirectory=targetDir;
            } else {
                System.err.println("No existe el directorio: "+args.get(1));
            }
        }

        System.out.println("Directorio actual: "+this.currentDirectory.getAbsolutePath());
    }

    private void executeExternalCommand(TCommand cmd){
        try {
            List<String> command=new ArrayList<>();
            command.add("cmd.exe");
            command.add("/c");
            command.addAll(cmd.getArgv());

            ProcessBuilder pb=new ProcessBuilder(command);
            pb.directory(this.currentDirectory);
            pb.redirectErrorStream(true);

            Process p=pb.start();

            try (BufferedReader br=new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while((line=br.readLine()) !=null) {
                    System.out.println(line);
                }
            }

            int exitCode=p.waitFor();
            System.out.println("Finalizado con código: "+exitCode);
        } catch (IOException e) {
            System.out.println("Error al ejecutrar comando: "+e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Ejecución interrumpida");
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        new MiniShell().run();
    }
}
