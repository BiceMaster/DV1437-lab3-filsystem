import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

public class FileManager {

    private FileSystem fileSystem;
    private ArrayList<String> workPath;

    public FileManager(FileSystem p_BlockDevice) {
        fileSystem = p_BlockDevice;
        format();
        
        // Setup stack
        workPath = new ArrayList(0);
        
        // Load example filestructure
        //System.out.println(read("default"));
        
    }

    public String format() {
        fileSystem.format();
        workPath = new ArrayList(0);
        return new String("Diskformat successful");
    }

    public String ls(String[] p_asPath) {
        return ls();
    }
    
    public String ls() {
        String result = "";    
        String[] fileNames   = fileSystem.getNonFolderNames(workPath.toArray(new String[0]));
        String[] folderNames = fileSystem.getFolderNames(workPath.toArray(new String[0]));
        
        // Append all folders to String
        result = "<" + folderNames.length + " folder(s)>";
        for(int i=0; i<folderNames.length; i++){
            result += folderNames[i] + "/\n";
        }
        
        // Append all files to String
        result = result + "\n<" + fileNames.length + " file(s)>";
        for(int i=0; i<fileNames.length; i++){
            result += fileNames[i] + "\n";
        }
        
        // If no files
        if(fileNames.length == 0 && folderNames.length == 0){
            result = "<empty>";
        }
        
        // Return
        return result;
    }
    
    // Loads a file and makes it into a file in the filesytem
    public String loadfile(String fileName) {
        String result = ""; 
        StringBuilder content = new StringBuilder();

        try {
            // Read content
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            String line = null;
            while ((line = in.readLine()) != null) {
                content.append(line).append(System.getProperty("line.separator"));
            }
            in.close();
            
            // Write content to new file
            String[] p_asPath = new String[1];
            p_asPath[0] = fileName;
            result = create(p_asPath, content.toString().getBytes());
        } catch (Exception e) {
            result = "File not found";
        }

        return result;
    }

    public String create(String[] p_asPath, byte[] data) {
        String result = ""; 
        String[] path = parsePath(workPath, p_asPath);
        String name = path[path.length-1];
        path = popStringArray(path);
        
        // Add "new line" to text to make it more readable in case we append two files.
        data[data.length-1] = '\n';
        
        // Create file
        if (fileSystem.touchFile(name, false, path) != -1) {
            result = "File created\n";

            // Write data to file
            if(fileSystem.writeToFile(name, data, path)){
                result += "Write succeeded";
            }else {
                result += "Write failed";
            }
            
        } else {
            result = "Name already exists / Folder in path doesn't exist";
        }
        
        return result;
    }

    public String cat(String[] p_asPath) {        
        String result = ""; 
        String[] path = parsePath(workPath, p_asPath);
        String name = path[path.length-1];
        path = popStringArray(path);
		
        return fileSystem.readTextFromFile(name, path);
    }
    
    /*
     * Save as a UNIX file. what is a UNIX file? Wikipeida says nothing. Our 
     * guess is that it is a file without a file extension. Thats how we choose 
     * to implement it.
     * 
     *  Mattias Liljeson
     */
    public String save(String p_sPath) {
        String result = "Saving blockdevice to file \"" + p_sPath+"\"";
        //result = "\nWriting file failed";
        
        try{
            FileOutputStream fs = new FileOutputStream(p_sPath);
            ObjectOutputStream os = new ObjectOutputStream(fs);
            os.writeObject(fileSystem);
            os.close();
            result += "\nFile written successfully";
        }
        catch(IOException ex) {
            result += "\nFailed to save file. IO error";
        }
        
        return result;
    }

    public String read(String p_sPath) {
        String result = "Reading file \"" + p_sPath + "\" to blockdevice";
        //result += "Loading file failed";
        
        try{
            FileInputStream fileStream = new FileInputStream(p_sPath);
            ObjectInputStream os = new ObjectInputStream(fileStream);
            format();
            fileSystem = (FileSystem)os.readObject();
            result += "\nFile loaded successfully";
        }
        catch(IOException ex) {
            result += "\nFile not found or other IO error";
            ex.printStackTrace();
        }
        catch(ClassNotFoundException ex) {
            result += "\nWrong type of file or from other version of program";
        }
        
        return result;
    }

    public String rm(String[] p_asPath) {
		String result = "File not found"; 
        String[] path = parsePath(workPath, p_asPath);
        String name = path[path.length-1];
        path = popStringArray(path);
        
        if (fileSystem.removeFile(name, path)) {
            result = "File removed";
        }
        
        return result;
    }

    public String copy(String[] p_asSource, String[] p_asDestination) {
        // Get the full source path
        String[] src = parsePath(workPath, p_asSource);
        // Get the full destination path
        String[] dst = parsePath(workPath, p_asDestination);
        
        // Copy paths
        return fileSystem.copy(src, dst);
    }

    public String append(String[] p_asSource, String[] p_asDestination) {
        // Get the full source path
        String[] src = parsePath(workPath, p_asSource);
        // Get the full destination path
        String[] dst = parsePath(workPath, p_asDestination);

        // Append files
        return fileSystem.mergeFiles(src, dst);
    }

    public String rename(String[] p_asSource, String[] p_asDestination) {	
		String result = ""; 
        String[] path = parsePath(workPath, p_asSource);
        String oldName = p_asSource[p_asSource.length-1];
		String newName = p_asDestination[p_asDestination.length-1];
        path = popStringArray(path);
		
        if(fileSystem.rename(oldName, newName, path)){
            result = "File renamed";
        }else{
            result = "Couldn't rename";
        }
        return result;
    }

    public String mkdir(String[] p_path) {
        String result = ""; 
        String[] path = parsePath(workPath, p_path);
        String name = path[path.length-1];
        path = popStringArray(path);
		
        if(fileSystem.touchFile(name, true, path) != -1){
            result = "Directory created";
        }
		else {
            result = "Name already exists";
        }
        return result;
    }
    
    public String cd(String[] p_path) {
        String result = ""; 
        String[] newPath = parsePath(workPath, p_path);

        if (fileSystem.isPathValid(newPath)) {
            workPath = new ArrayList(Arrays.asList(newPath));
            result = "Directory changed";

        }
		else {
            result = "No such directory";
        }

        // Return
        return result;
    }

    public String pwd() {
        // Print path
        String path = "~";
        for(String d : workPath){
            path = path+"/"+d;
        }
        
        return path;
    }

    private void dumpArray(String[] p_asArray) {
        for (int nIndex = 0; nIndex < p_asArray.length; nIndex++) {
            System.out.print(p_asArray[nIndex] + "=>");
        }
    }
    
     // Returns a stack with the combined workpath and addedPath
    private Stack<String> getAddedPathStack(String[] addedPath) {
        // Backup old workdir
         Stack<String> path = (Stack<String>) workPath.clone();
         
          // Manipulate workPath based on commands in path array
         for(String p : addedPath){
             if( p.equals("..") ){
                 if(path.size()>0){
                     path.pop();
                 }
             }else if( p.equals(".")){
                 // Nothing
             }else {
                 path.add(p);
             }
         }
         
         return path;
    }
    
    // Returns an array with the combined workpath and addedPath
    private String[] getAddedPath(String[] addedPath) {
        Stack<String> path = getAddedPathStack(addedPath);
        
         return path.toArray(new String[path.size()]);
    }
    
    private String[] parsePath(ArrayList<String> workPath, String[] appendee) {
        return parsePath(workPath.toArray(new String[0]), appendee);
    }
    
    private String[] parsePath(String[] workPath, String[] appendee) {
        ArrayList<String> tmp = new ArrayList(Arrays.asList(workPath));
		tmp.addAll(Arrays.asList(appendee));
        
        // if ".." is part of the path, remove one layer from the workPath part
		int indexOfTwoDots = findPhraseInStringArray(tmp.toArray(new String[0]), "..");
        if(indexOfTwoDots != -1) {
            tmp.remove(indexOfTwoDots);
			if(indexOfTwoDots-1 > -1)
				tmp.remove(indexOfTwoDots-1);
		}
		
		int indexOfOneDot = findPhraseInStringArray(tmp.toArray(new String[0]), ".");
		if(indexOfOneDot != -1) {
            tmp.remove(indexOfOneDot);
			if(indexOfOneDot-1 > -1)
				tmp.remove(indexOfOneDot-1);
		}
		
        return (tmp.toArray(new String[0]));
    }
    
    private String[] popStringArray(String[] arr){
        String[] tmpArr = new String[arr.length-1];
        System.arraycopy(arr, 0, tmpArr, 0, tmpArr.length);
        return tmpArr;
    }
    
    private int findPhraseInStringArray(String[] arr, String phrase) {
        int index = -1;
        
        for(int i=0; i<arr.length; i++)
            if(arr[i].equals(phrase))
                index = i;
        
        return index;
    }
}
