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
					
					//Cmd exit.
					if(cmdName.equals("exit")) {
						System.out.println("Saliendo...");
						break;
					//Cmd cd
					} else if (cmdName.equals("cd")) {
						handleCd(cmd);
						
					} else {
						executeExternalCommand(cmd, line);
					}
					
					
				}
				
				System.out.println(line);
			} catch (MissingFileException e) {
				// TODO Auto-generated catch block
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
		
		//Prueba 
		System.out.println("Directorio actual: "+this.currentDirectory.getAbsolutePath());
	}
	
	
	private void executeExternalCommand(TCommand cmd, TLine line){
		try {
			
			//Filtar simbolos de redirección??
			List<String> cleanArgs=new ArrayList<>();
			
			for(String arg: cmd.getArgv()) {
				if(!arg.equals(">") && !arg.equals(">>") && !arg.equals("<") && !arg.equals("2>") && !arg.equals("2>>")) {
					cleanArgs.add(arg);
				}
			}
			
			List<String> command=new ArrayList<>();
			//VERIFICACIÓN SISTEMA OPERATIVO?? 
			
			/**List<String> args=cmd.getArgv();
			String os=System.getProperty("os.name").toLowerCase();
			
			if(os.contains("win")) {
				command.add("cmd.exe");
				command.add("/c");
				command.add(String.join(" ", args));
			} else {
				command.add(args.get(0));
			} if(args.size()>1) {
				command.addAll(args.subList(1, args.size()));
			}*/
			
			command.add("cmd.exe");
			command.add("/c");
			command.addAll(cleanArgs);
			
			
			ProcessBuilder pb=new ProcessBuilder(command);
			pb.directory(this.currentDirectory);
			pb.redirectErrorStream(false); //salida y error por separado, de momento
			
			//Redirección de entrada (>)
			if(line.getRedirectInput()!=null) {
				pb.redirectInput(new File(line.getRedirectInput()));
			}
			
			//Redirección de salida (> o >>)
			if(line.getRedirectOutput()!=null) {
				File outFile=new File(line.getRedirectOutput());
				if(line.isAppendOutput()) {
					pb.redirectOutput(ProcessBuilder.Redirect.appendTo(outFile));
				} else {
					pb.redirectOutput(ProcessBuilder.Redirect.to(outFile));
				}
			}
			
			//Redirección de error (2> o 2>>)
			if(line.getRedirectError()!=null) {
				File errFile=new File(line.getRedirectError());
				if(line.isAppendError()) {
					pb.redirectError(ProcessBuilder.Redirect.appendTo(errFile));
				} else {
					pb.redirectError(ProcessBuilder.Redirect.to(errFile));
				}
			}
				Process p=pb.start();
				
				//Si no hay redirección se muestra
				
				if(line.getRedirectOutput()==null) {
					try(BufferedReader br=new BufferedReader(new InputStreamReader(p.getInputStream()))) {
						String lineOut;
						while((lineOut=br.readLine()) !=null) {
							System.out.println(lineOut);
						}
					}
				}
				
				if(line.getRedirectError()==null) {
					try(BufferedReader br=new BufferedReader(new InputStreamReader(p.getInputStream()))) {
						String lineErr;
						while((lineErr=br.readLine()) !=null) {
							System.out.println(lineErr);
						}
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
