import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
    String routineFooter = "END\r\n\r\n";
    String indent = "   ";
    boolean cDis = false;

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
            
            NodeList nodes = document.getElementsByTagName("rrs2_MainRoutine");
            for (int i = 0; i < nodes.getLength(); i++) {
                Node routineNode = nodes.item(i);
                
                StringBuilder sb = new StringBuilder();
                
                String routineHeader = doFileHead();
                sb.append(routineHeader);
                
                Node subRoutineBody = findSubNode("rrs2_RoutineBody", routineNode);
                sb.append(handleRoutineBody(subRoutineBody));
                
                sb.append(routineFooter);
                
                try {
                    writeFileLine(sb.toString());
                } catch (FileNotFoundException ex) {
                    ex.printStackTrace();
                } catch (UnsupportedEncodingException ex) {
                    ex.printStackTrace();
                } 
            }
            
            
            nodes = document.getElementsByTagName("rrs2_SubRoutine");
            for (int i = 0; i < nodes.getLength(); i++) {
                Node subRoutineNode = nodes.item(i);
                Node subRoutineNameNode = findSubNode("rrs2_SubRoutineName", subRoutineNode);
                
                StringBuilder sb = new StringBuilder();
                
                String subRoutineName = subRoutineNameNode.getAttributes().getNamedItem("value").getNodeValue();
                String routineHeader = "DEF " + subRoutineName.toUpperCase() + "()\r\n";
                sb.append(routineHeader);
                
                Node subRoutineBody = findSubNode("rrs2_RoutineBody", subRoutineNode);
                sb.append(handleRoutineBody(subRoutineBody));
                
                sb.append(routineFooter);
                
                try {
                    writeFileLine(sb.toString());
                } catch (FileNotFoundException ex) {
                    ex.printStackTrace();
                } catch (UnsupportedEncodingException ex) {
                    ex.printStackTrace();
                }                
            }
        }
    }

    private String handleRoutineBody(Node routineBody) {
        Element e = (Element) routineBody;
        
        StringBuilder sb = new StringBuilder();
        
        NodeList childNodes = routineBody.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node n = childNodes.item(i);
            
            if (n.getNodeName().equalsIgnoreCase("#text"))
                continue;
            
            if (n.getNodeName().equalsIgnoreCase("rrs2_PureMotionStatement")) {
                sb.append(indent);
                sb.append(getStringForPureMotionStatement(n));
                sb.append("\r\n");
            } else if (n.getNodeName().equalsIgnoreCase("rrs2_SetBinaryOutputStatement")) {
                sb.append(indent);
                sb.append(getStringForBinaryOutStatement(n));
                sb.append("\r\n");
            } else if (n.getNodeName().equalsIgnoreCase("rrs2_SubRoutineCallStatement")) {
                sb.append(indent);
                sb.append(getStringForCallStatement(n));
                sb.append("\r\n");
            } else if (n.getNodeName().equalsIgnoreCase("rrs2_WaitForBinaryInputStatement")) {
                sb.append(indent);
                sb.append(getStringForWaitForInputStatement(n));
                sb.append("\r\n");
            } else if (n.getNodeName().equalsIgnoreCase("rrs2_Delay")) {
                sb.append(indent);
                sb.append(getStringForDelayStatement(n));
                sb.append("\r\n");
            } else if (n.getNodeName().equalsIgnoreCase("DefineTool")) {
                sb.append(indent);
                sb.append(getStringForDefineToolStatement(n));
                sb.append("\r\n");
            } else if (n.getNodeName().equalsIgnoreCase("DefineBase")) {
                sb.append(indent);
                sb.append(getStringForDefineBaseStatement(n));
                sb.append("\r\n");
            } else {
                System.out.println(n.getNodeName());
            }
        }
        return sb.toString();
    }
    
    private String getStringForDefineBaseStatement(Node defineNode) {
        Node posNode = findSubNode("WorldPosition", defineNode);
        
        String pz = posNode.getAttributes().getNamedItem("pz").getNodeValue();
        String py = posNode.getAttributes().getNamedItem("py").getNodeValue();
        String px = posNode.getAttributes().getNamedItem("px").getNodeValue();

        String az = posNode.getAttributes().getNamedItem("az").getNodeValue();
        String nz = posNode.getAttributes().getNamedItem("nz").getNodeValue();
        String ny = posNode.getAttributes().getNamedItem("ny").getNodeValue();
        String nx = posNode.getAttributes().getNamedItem("nx").getNodeValue();
        String oz = posNode.getAttributes().getNamedItem("oz").getNodeValue();
        
        double yaw = Math.toDegrees(Math.atan2(Double.parseDouble(ny), Double.parseDouble(nx)));
        
        double azd = Double.parseDouble(az);
        double ozd = Double.parseDouble(oz);
        
        double pitch = Math.toDegrees(Math.atan2(-Double.parseDouble(nz), Math.sqrt(ozd*ozd + azd*azd)));
        double roll = Math.toDegrees(Math.atan2(ozd, azd));
        
        String lowCase = "$BASE = {x " + px + ",y " + py + ",z " + pz + ",a " + yaw + ",b " + pitch + ",c " + roll + "}";
        return lowCase.toUpperCase();
    }
    
    private String getStringForDefineToolStatement(Node defineNode) {
        Node posNode = findSubNode("Position", defineNode);
        String pz = posNode.getAttributes().getNamedItem("pz").getNodeValue();
        String py = posNode.getAttributes().getNamedItem("py").getNodeValue();
        String px = posNode.getAttributes().getNamedItem("px").getNodeValue();

        String az = posNode.getAttributes().getNamedItem("az").getNodeValue();
        String nz = posNode.getAttributes().getNamedItem("nz").getNodeValue();
        String ny = posNode.getAttributes().getNamedItem("ny").getNodeValue();
        String nx = posNode.getAttributes().getNamedItem("nx").getNodeValue();
        String oz = posNode.getAttributes().getNamedItem("oz").getNodeValue();
        
        double yaw = Math.toDegrees(Math.atan2(Double.parseDouble(ny), Double.parseDouble(nx)));
        
        double azd = Double.parseDouble(az);
        double ozd = Double.parseDouble(oz);
        
        double pitch = Math.toDegrees(Math.atan2(-Double.parseDouble(nz), Math.sqrt(ozd*ozd + azd*azd)));
        double roll = Math.toDegrees(Math.atan2(ozd, azd));
        
        String lowCase = "$TOOL = {x " + px + ",y " + py + ",z " + pz + ",a " + yaw + ",b " + pitch + ",c " + roll + "}";
        return lowCase.toUpperCase();
    }
    
    private String getStringForDelayStatement(Node delayNode) {
        Node durNode = findSubNode("rrs2_DelayDuration", delayNode);
        String dur = durNode.getAttributes().getNamedItem("value").getNodeValue();
        return "WAIT SEC " + dur;
    }
    
    private String getStringForWaitForInputStatement(Node waitNode) {
        Node numNode = findSubNode("rrs2_BinaryInputNumber", waitNode);
        String inNum = numNode.getAttributes().getNamedItem("value").getNodeValue();
        
        Node trigOnNode = findSubNode("rrs2_BinaryTriggerType", waitNode);
        int inVal = Integer.parseInt(trigOnNode.getAttributes().getNamedItem("value").getNodeValue());
        
        String codeLine = "WAIT FOR $IN[" + inNum + "]";
        
        if (inVal == 0)
            codeLine = codeLine + "==FALSE";
        
        return codeLine;
    }
    
    private String getStringForCallStatement(Node callNode) {
        Node nameNode = findSubNode("rrs2_SubRoutineName", callNode);
        String routineName = nameNode.getAttributes().getNamedItem("value").getNodeValue().toUpperCase();
        return routineName + "()";
    }
    
    private String getStringForBinaryOutStatement(Node binOutNode) {
        Node numberNode = findSubNode("rrs2_BinaryOutputNumber", binOutNode);
        Node outputNode = findSubNode("rrs2_BinaryOutputValue", binOutNode);
        String outNum = numberNode.getAttributes().getNamedItem("value").getNodeValue();
        String codeLine = "$OUT[" + outNum + "] = ";
        
        int outVal = Integer.parseInt(outputNode.getAttributes().getNamedItem("value").getNodeValue());
        if (outVal == 1)
            codeLine = codeLine + "TRUE";
        else
            codeLine = codeLine + "FALSE";

        return codeLine;
    }
    
    private String getStringForPureMotionStatement(Node pureMotionNode) {
        NodeList childNodes = pureMotionNode.getChildNodes();
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
        double pitch = Math.toDegrees(Math.atan2(-nz, Math.sqrt(oz * oz + az * az)));
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
            sb.append("C " + roll + "}");
            
            if (cDis) {
                sb.append(" C_DIS");
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
        }
        return sb.toString().toUpperCase();
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
        } else if (line.equalsIgnoreCase("@END@")) {
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
                + indent + "EXT BAS (BAS_COMMAND: IN, REAL:IN)\r\n"
                + indent + "DECL AXIS HOME\r\n\r\n"
                + indent + "BAS (#INITMOV, 0)\r\n\r\n"
                + indent + "HOME = {AXIS: A1 0, A2 -90, A3 90, A4 0, A5 0, A6 0}\r\n\r\n"
                + indent + "PTP HOME\r\n\r\n";
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
