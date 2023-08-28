import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;


public class Main {
    public static void main(String[] args) {
        if (args.length<1){
            throw new RuntimeException("no file or folder Specified");
        }else if (args.length>1){
            throw new RuntimeException("too many arguments");
        }
        File input = new File(args[0]);

        //check if target exits
        if (!input.exists()){
            throw new RuntimeException("Target does not exist");
        }

        // if target is Directory -> readAll .vm files and create Bootstrap Code
        // else read file
        if (input.isDirectory()) {
            File[] files = input.listFiles();
            if (files!=null) {
                System.out.println("found the following .jack files:\n--------------------------------");
                Arrays.stream(files).filter((File file) -> file.getName().endsWith(".jack"))
                        .forEach((File file) -> System.out.println("\t" + file.getName()));
                System.out.println("--------------------------------");
                for (File file : files) {
                    if (file.getName().endsWith(".jack")) {
                        compileFile(file);
                    }
                }
            }

        } else if (input.isFile() && input.getName().endsWith(".jack")) {
            compileFile(input);
        }

        System.out.println("--------------------------------");
        System.out.println("done");


    }
    private static void compileFile (File input){
        System.out.println("--------------------------------");
        System.out.println(input.getName());
        //System.out.println("--------------------------------");
        //read String from inputFile
        String inputString = readInstructionsFromFileToString(input);
        System.out.println("\tFile read successfully");
        ArrayList<Token> tokenList;
        TokenReader tokenReader= new TokenReader();
        //convert String to Tokens
        tokenList = tokenReader.readTokesFromString(inputString);
        CompilationEngine compilationEngine = new CompilationEngine(tokenList,false);

        String tokenString = compilationEngine.getTokenXML();
        writeOutputToFile(input,tokenString,"T.xml");
        System.out.println("\tsuccessfully tokenized");

        try {

            compilationEngine.compile();
            String xmlString = compilationEngine.getXmlString();
            String vmInstructionString= compilationEngine.getInstructionString();
            writeOutputToFile(input,xmlString,".xml");
            writeOutputToFile(input,vmInstructionString,".vm");
            System.out.println("\tsuccessfully compiled");
        }catch (RuntimeException e){
            System.out.println("\t\u001B[31m"+e.getMessage()+"\u001B[0m");
            //e.printStackTrace();
        }


    }

    private static String readInstructionsFromFileToString(File input) {
        StringBuilder outputStingBuilder =new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(input));
            String line;
            while ((line = reader.readLine()) != null) {
                outputStingBuilder.append(line).append("\n");
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStingBuilder.toString();
    }


    private static void writeOutputToFile(File input, String outputString, String suffix){
        //Write OutputFile
        String inputName = input.getName();
        String outputName;

        if (inputName.contains(".")) {
            outputName = input.getParent()+File.separator+inputName.substring(0, inputName.lastIndexOf('.')) + suffix;
        } else {
            outputName = inputName + suffix;
        }

        File output = new File(outputName);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(output));
            writer.write(outputString);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }






}