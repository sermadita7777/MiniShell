package minishell;

import java.io.File;
import java.util.List;
import java.util.Scanner;

import tokenizer.TCommand;
import tokenizer.TLine;
import tokenizer.Tokenizer;
import tokenizer.MissingFileException;

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
                    }
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

    public static void main(String[] args) {
        new MiniShell().run();
    }
}
