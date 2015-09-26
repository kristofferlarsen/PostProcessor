package com.Magic;

import javax.swing.*;
import java.io.*;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class PostProcessor {
    String targetPath = "";
    String sourcePath = "";

    public PostProcessor(File from, File to)
    {
        sourcePath = from.getPath();
        targetPath = to.getPath();
        System.out.print("File to open: " + sourcePath + ", ResultFile: " + targetPath);
        writeFile(targetPath);
    }

    private void createFile(String filetocreate)
    {
        File tmp = new File(filetocreate);
    }

    private void writeFile(String targetPath)
    {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(targetPath,"UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if(writer != null){
            writer.write(doFileHead());
            writer.close();
        }
    }

    /**
     * Creates the standard text head for the src file
     * @return The standard head
     */
    private String doFileHead(){
        String head = "";
        head = head + "DEF PROGRAMNAME()\n\n" +
                "EXT BAS (BAS_COMMAND: IN, REAL:IN)\n" +
                "DECL AXIS HOME\n\n" +
                "BAS (#INITMOV, 0)\n\n" +
                "HOME = {AXIS: A1 0, A2 -90, A3 90, A4 0, A5 0, A6 0}\n" +
                "PTP HOME\n";
        return head;
    }

    /**
     * Main entrypoint for PostProcessor. Can be run in 3 different ways:
     *  - no arguments: presents the user with a file chooser to select the .RSI file to convert to .SRC
     *  - one input argument: the file to convert from .RSI to .SRC, this file must be a .RSI file.
     *  - two input arguments: source .RSI file, target .SRC file
     * @param args
     */
    public static void main(String[] args) {

        String currentDir = System.getProperty("user.dir");
        if(!new File("Resorces").exists())
        {
            new File("Resorces").mkdir();
            System.out.println("created Resorces dir");
        }
        if(!new File("Resorces\\source").exists())
        {
            new File("Resorces\\source").mkdir();
            System.out.println("created source dir");
        }
        if(!new File("Resorces\\result").exists())
        {
            new File("Resorces\\result").mkdir();
            System.out.println("created result dir");
        }
        File fileToOpen;
        String[] tmps;
        switch (args.length) {
            case 0:
                //no input arguments, load gui
                JFileChooser fc = new JFileChooser(new File(currentDir));
                int returnVal = fc.showOpenDialog(null);
                if(returnVal == JFileChooser.APPROVE_OPTION)
                {
                    fileToOpen = fc.getSelectedFile();
                    tmps = fileToOpen.getName().split("[.]");
                    if(!tmps[1].equalsIgnoreCase("RSI"))
                    {
                        //not an RSI file. GTFO!
                        System.exit(0);
                    }
                    try {
                        Files.copy(fileToOpen.toPath(),new File("Resorces\\source\\"+tmps[0]+"."+tmps[1]).toPath(), StandardCopyOption.REPLACE_EXISTING);
                        PostProcessor p = new PostProcessor(new File("Resorces\\source\\"+tmps[0]+"."+tmps[1]),new File("Resorces\\result\\"+tmps[0]+"."+"src"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else
                {
                    System.exit(0);
                }
                break;
            case 1:
                //one input argument, should be input file, output is in current location
                fileToOpen = new File(args[0]);
                tmps = fileToOpen.getName().split("[.]");
                if(!tmps[1].equalsIgnoreCase("RSI"))
                {
                    //not an RSI file!
                    System.exit(0);
                }
                try {
                    Files.copy(fileToOpen.toPath(), new File("Resorces\\source\\" + tmps[0] + "." + tmps[1]).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    PostProcessor p = new PostProcessor(new File("Resorces\\source\\"+tmps[0]+"."+tmps[1]),new File("Resorces\\result\\"+tmps[0]+"."+"src"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 2:
                //two input arguments, input file and output file/location
                fileToOpen = new File(args[0]);
                File dest = new File(args[1]);
                String[] desttmps = dest.getName().split("[.]");
                if(!desttmps[1].equalsIgnoreCase("SRC"))
                {
                    //target is not a src-file, GTFO
                    System.exit(0);
                }
                tmps = fileToOpen.getName().split("[.]");
                if(!tmps[1].equalsIgnoreCase("RSI"))
                {
                    //not an RSI file!
                    System.exit(0);
                }

                try {
                    Files.copy(fileToOpen.toPath(),new File("Resorces\\source\\"+tmps[0]+"."+tmps[1]).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    PostProcessor p = new PostProcessor(new File("Resorces\\source\\"+tmps[0]+"."+tmps[1]),new File("Resorces\\result\\"+desttmps[0]+"."+"src"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }
    }
}
