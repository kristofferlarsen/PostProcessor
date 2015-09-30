package postprocessor;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class PostProcessor {

    String targetPath = "";
    String sourcePath = "";
    PrintWriter writer = null;
    String positionType = "";

    public PostProcessor(File from, File to, String positionType) {
        this.positionType = positionType;
        sourcePath = from.getPath();
        targetPath = to.getPath();
        System.out.print("File to open: " + sourcePath + ", ResultFile: " + targetPath + "\n");
        try {
            writeFileLine("@START@");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        processXML(positionType);
        try {
            writeFileLine("@END@");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * This is where the magic happens, read the xml, generate .SRC code lines
     * based on application launch arguments [-a,-c]
     *
     * @param positionType
     */
    private void processXML(String positionType) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        Document document = null;
        try {
            db = dbf.newDocumentBuilder();
            document = db.parse(new File(sourcePath));
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (document != null) {
            //successfully created a document
            NodeList nodes = document.getElementsByTagName("rrs2_PureMotionStatement");
            for (int i = 0; i < nodes.getLength(); i++) {
                NodeList childNodes = nodes.item(i).getChildNodes();
                Node targetFrame = childNodes.item(1);
                Node targetParameters = findSubNode("TargetParameters", targetFrame);
                Node localJoints = findSubNode("LocalJoints", targetFrame);
                Node motionParamters = findSubNode("MotionParameters", targetFrame);
                Node pointData = targetParameters.getChildNodes().item(5);
                Node motionData = motionParamters.getChildNodes().item(1);
                Node axis1Data = localJoints.getChildNodes().item(1);
                Node axis2Data = localJoints.getChildNodes().item(3);
                Node axis3Data = localJoints.getChildNodes().item(5);
                Node axis4Data = localJoints.getChildNodes().item(7);
                Node axis5Data = localJoints.getChildNodes().item(9);
                Node axis6Data = localJoints.getChildNodes().item(11);

                double a1 = Double.parseDouble(axis1Data.getAttributes().getNamedItem("value").getNodeValue());
                double a2 = Double.parseDouble(axis2Data.getAttributes().getNamedItem("value").getNodeValue());
                double a3 = Double.parseDouble(axis3Data.getAttributes().getNamedItem("value").getNodeValue());
                double a4 = Double.parseDouble(axis4Data.getAttributes().getNamedItem("value").getNodeValue());
                double a5 = Double.parseDouble(axis5Data.getAttributes().getNamedItem("value").getNodeValue());
                double a6 = Double.parseDouble(axis6Data.getAttributes().getNamedItem("value").getNodeValue());

                double px = Double.parseDouble(pointData.getAttributes().getNamedItem("px").getNodeValue());
                double py = Double.parseDouble(pointData.getAttributes().getNamedItem("py").getNodeValue());
                double pz = Double.parseDouble(pointData.getAttributes().getNamedItem("pz").getNodeValue());
                double nx = Double.parseDouble(pointData.getAttributes().getNamedItem("nx").getNodeValue());
                double ny = Double.parseDouble(pointData.getAttributes().getNamedItem("ny").getNodeValue());
                double nz = Double.parseDouble(pointData.getAttributes().getNamedItem("nz").getNodeValue());
                double ox = Double.parseDouble(pointData.getAttributes().getNamedItem("ox").getNodeValue());
                double oy = Double.parseDouble(pointData.getAttributes().getNamedItem("oy").getNodeValue());
                double oz = Double.parseDouble(pointData.getAttributes().getNamedItem("oz").getNodeValue());
                double ax = Double.parseDouble(pointData.getAttributes().getNamedItem("ax").getNodeValue());
                double ay = Double.parseDouble(pointData.getAttributes().getNamedItem("ay").getNodeValue());
                double az = Double.parseDouble(pointData.getAttributes().getNamedItem("az").getNodeValue());
                double yaw = Math.toDegrees(Math.atan2(ny, nx));
                double pitch = Math.toDegrees(Math.atan2(-nz, Math.sqrt(oz*oz + az*az)));
                double roll = Math.toDegrees(Math.atan2(oz, az));
                String motionType = motionData.getAttributes().getNamedItem("value").getNodeValue();

                StringBuffer sb = new StringBuffer();
                if (motionType.equalsIgnoreCase("LINEAR")) {
                    sb.append("LIN {");
                } else {
                    sb.append("PTP {");
                }
                if (positionType.equalsIgnoreCase("-c")) {
                    sb.append("X " + px + ",");
                    sb.append("Y " + py + ",");
                    sb.append("Z " + pz + ",");
                    sb.append("A " + yaw + ",");
                    sb.append("B " + pitch + ",");
                    sb.append("C " + roll + "} C_DIS");
                    try {
                        writeFileLine(sb.toString());
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
                if (positionType.equalsIgnoreCase("-a")) {
                    sb.append("AXIS: ");
                    sb.append("A1 " + a1 + ",");
                    sb.append("A2 " + a2 + ",");
                    sb.append("A3 " + a3 + ",");
                    sb.append("A4 " + a4 + ",");
                    sb.append("A5 " + a5 + ",");
                    sb.append("A6 " + a6 + ",");
                    sb.append("}");
                    try {
                        writeFileLine(sb.toString());
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public Node findSubNode(String name, Node node) {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            System.err.println("Error: Search node not of element type");
            System.exit(22);
        }

        if (!node.hasChildNodes()) {
            return null;
        }

        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node subnode = list.item(i);
            if (subnode.getNodeType() == Node.ELEMENT_NODE) {
                if (subnode.getNodeName().equals(name)) {
                    return subnode;
                }
            }
        }
        return null;
    }

    /**
     * Writes a line in the .src file
     *
     * @param line
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    private void writeFileLine(String line) throws FileNotFoundException, UnsupportedEncodingException {
        if (line.equalsIgnoreCase("@START@")) {
            writer = new PrintWriter(targetPath, "UTF-8");
            writer.write(doFileHead() + "\r\n");
        } else if (line.equalsIgnoreCase("@END@")) {
            writer.write("END");
            writer.close();
        } else {
            writer.write(line + "\r\n");
        }
    }

    /**
     * Creates the standard text head for the src file
     *
     * @return The standard head
     */
    private String doFileHead() {
        String head = "";
        head = head + "DEF PROGRAMNAME()\r\n\r\n"
                + "EXT BAS (BAS_COMMAND: IN, REAL:IN)\r\n"
                + "DECL AXIS HOME\r\n\r\n"
                + "BAS (#INITMOV, 0)\r\n\r\n"
                + "HOME = {AXIS: A1 0, A2 -90, A3 90, A4 0, A5 0, A6 0}\r\n\r\n"
                + "PTP HOME\r\n\r\n";
        return head;
    }

    /**
     * Main entrypoint for PostProcessor. Can be run in 3 different ways: - no
     * arguments: presents the user with a file chooser to select the .RSI file
     * to convert to .SRC, Th output file will use the -c parameter - one input
     * argument: presents the user with a file chooser to select the .RSI file
     * to convert to .SRC, Th output file will be set by the arguement [-a,-c] -
     * two input arguments: code type argument [-a,-c] and file to open, the
     * result file will be named the same as the target, and placed in the
     * result folder.
     *
     * @param args
     */
    public static void main(String[] args) {
        String[] arguments = args;

        String currentDir = System.getProperty("user.dir");
        if (!new File("Resorces").exists()) {
            new File("Resorces").mkdir();
            System.out.println("created Resorces dir");
        }
        if (!new File("Resorces\\source").exists()) {
            new File("Resorces\\source").mkdir();
            System.out.println("created source dir");
        }
        if (!new File("Resorces\\result").exists()) {
            new File("Resorces\\result").mkdir();
            System.out.println("created result dir");
        }
        File fileToOpen;
        String[] tmps;
        File dest;
        String[] desttmps;
        JFileChooser fc;
        int returnVal;
        switch (args.length) {
            case 0:
                //no input arguments, load gui
                fc = new JFileChooser(new File(currentDir));
                returnVal = fc.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    fileToOpen = fc.getSelectedFile();
                    tmps = fileToOpen.getName().split("[.]");
                    if (!tmps[1].equalsIgnoreCase("RSL")) {
                        //not an RSI file. GTFO!
                        System.exit(0);
                    }
                    try {
                        Files.copy(fileToOpen.toPath(), new File("Resorces\\source\\" + tmps[0] + "." + tmps[1]).toPath(), StandardCopyOption.REPLACE_EXISTING);
                        PostProcessor p = new PostProcessor(new File("Resorces\\source\\" + tmps[0] + "." + tmps[1]), new File("Resorces\\result\\" + tmps[0] + "." + "src"), "-c");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.exit(0);
                }
                break;
            case 1:
                //no input arguments, load gui
                fc = new JFileChooser(new File(currentDir));
                returnVal = fc.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    fileToOpen = fc.getSelectedFile();
                    tmps = fileToOpen.getName().split("[.]");
                    if (!tmps[1].equalsIgnoreCase("RSL")) {
                        //not an RSI file. GTFO!
                        System.exit(0);
                    }
                    try {
                        Files.copy(fileToOpen.toPath(), new File("Resorces\\source\\" + tmps[0] + "." + tmps[1]).toPath(), StandardCopyOption.REPLACE_EXISTING);
                        PostProcessor p = new PostProcessor(new File("Resorces\\source\\" + tmps[0] + "." + tmps[1]), new File("Resorces\\result\\" + tmps[0] + "." + "src"), args[0]);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.exit(0);
                }
                break;
            case 2:
                //one input argument, should be input file, output is in current location
                fileToOpen = new File(args[1]);
                tmps = fileToOpen.getName().split("[.]");
                if (!tmps[1].equalsIgnoreCase("RSL")) {
                    //not an RSI file!
                    System.exit(0);
                }
                try {
                    Files.copy(fileToOpen.toPath(), new File("Resorces\\source\\" + tmps[0] + "." + tmps[1]).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    PostProcessor p = new PostProcessor(new File("Resorces\\source\\" + tmps[0] + "." + tmps[1]), new File("Resorces\\result\\" + tmps[0] + "." + "src"), args[0]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            /**
             * case 3: //thre input arguments, input file and output
             * file/location fileToOpen = new File(args[1]); dest = new
             * File(args[2]); desttmps = dest.getName().split("[.]");
             * if(!desttmps[1].equalsIgnoreCase("SRC")) { //target is not a
             * src-file, GTFO System.exit(0); } tmps =
             * fileToOpen.getName().split("[.]");
             * if(!tmps[1].equalsIgnoreCase("RSI")) { //not an RSI file!
             * System.exit(0); }
             *
             * try { Files.copy(fileToOpen.toPath(),new
             * File("Resorces\\source\\"+tmps[0]+"."+tmps[1]).toPath(),
             * StandardCopyOption.REPLACE_EXISTING); PostProcessor p = new
             * PostProcessor(new
             * File("Resorces\\source\\"+tmps[0]+"."+tmps[1]),new
             * File("Resorces\\result\\"+desttmps[0]+"."+"src"),args[0]); }
             * catch (IOException e) { e.printStackTrace(); } break;
             *
             */
        }
    }
}
