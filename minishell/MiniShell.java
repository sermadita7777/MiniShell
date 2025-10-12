package minishell;

import java.util.Scanner;

public class MiniShell {
    private static final String prompt = "ms$";

    public void run() {
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.print(this.prompt + " ");

            if (!sc.hasNextLine()) {
                System.out.println();
                break;
            }

            String input = sc.nextLine().trim();

            if (input.isEmpty()) continue;

            if (input.equals("exit")) {
                System.out.println("Saliendo...");
                break;
            }

            System.out.println("Comando ingresado: " + input);
        }

        sc.close();
    }

    public static void main(String[] args) {
        new MiniShell().run();
    }
}
