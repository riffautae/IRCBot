package us.rddt.IRCBot;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Settings for tweaking silly extras.
 * @author milton
 *
 */
public class SillyConfiguration {

	// silly stuff
    private static List<String[]> sub_vic_aliases;
    private static List<String> join_wrapper;
    private static List<String> part_wrapper;
    private static List<String> chatter_wrapper;
	
	public static List<String[]> getSubVicAliases() {
		return sub_vic_aliases;
	}

	public static List<String> getJoinWrapper() {
		return join_wrapper;
	}

	public static List<String> getPartWrapper() {
		return part_wrapper;
	}

	public static List<String> getChatterWrapper() {
		return chatter_wrapper;
	}

    /**
     * Loads the configuration provided via various files.
     * @throws FileNotFoundException if the properties file does not exist
     * @throws IOException if an exception is raised reading the properties file
     */
    public static void loadConfiguration() throws FileNotFoundException, IOException {
    	loadSillyAliases();
    }

    /**
     * Load silly reply strings
     * @throws IOException
     */
    private static void loadSillyAliases() throws IOException {
    	BufferedReader file = new BufferedReader(new FileReader("silly_aliases.txt"));
    	HashMap<String, List<String>> map = _readSections(file);
    	
    	if( map.containsKey("Submitter/Victim Aliases") ) {
    		List<String[]> sec = new LinkedList<String[]>();
    		String[] set;
    		for( String s : map.get("Submitter/Victim Aliases") ) {
    			set = s.split(",");
    			for(int i=0; i<2; i++) {
    				set[i] = set[i].trim();
    			}
    			sec.add(set);
    		}
    		sub_vic_aliases = sec;
    	}
    	
    	if( map.containsKey("Join Wrapper") ) {
    		join_wrapper = map.get("Join Wrapper");
    	}
    	if( map.containsKey("Part Wrapper") ) {
    		part_wrapper = map.get("Part Wrapper");
    	}
    	if( map.containsKey("Chatter Wrapper") ) {
    		chatter_wrapper = map.get("Chatter Wrapper");
    	}
    }
    
    /**
     * Read a file with an ini like syntax
     * @param reader
     * @return
     * @throws IOException
     */
    private static HashMap<String, List<String>> _readSections(BufferedReader reader) throws IOException {
    	HashMap<String, List<String>> map = new HashMap<String, List<String>>();
    	String secname = null;
    	List<String> section = null;
    	String line;
    	while ( (line = reader.readLine())!= null ) {
    		if(line.length() == 0 || line.startsWith("#")) continue;
    		
    		if(line.length() > 2 && 
    				line.charAt(0) == '[' && 
    				line.charAt(line.length()-1) == ']') {
    			
    			if( section != null ) map.put(secname, section);
    			section = new LinkedList<String>();
    			secname = line.substring(1, line.length()-1);
    		} else if ( section != null ) {
    			section.add(line);
    		}
    	}
    	
    	return map;
    }
}
