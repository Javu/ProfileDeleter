import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;



public class UserAccount {
    public static final List<String> HEADINGS = Arrays.asList("Delete?", "Name", "Last Updated", "Size", "State", "SID", "GUID");
    
    public boolean delete;
    public String name;
    public String last_updated;
    public String size;
    public String state;
    public String sid;
    public String guid;
    
    public UserAccount() {
        delete = false;
        name = "";
        last_updated = "";
        size = "";
        state = "";
        sid = "";
        guid = "";
    }
    
    public UserAccount(boolean delete, String name, String last_updated, String size, String state, String sid, String guid) {
        this.delete = delete;
        this.name = name;
        this.last_updated = last_updated;
        this.size = size;
        this.state = state;
        this.sid = sid;
        this.guid = guid;
    }
    public static String[] HeadingsToStringArray() {
        String[] string_array = new String[HEADINGS.size()];
        for(int i=0;i<HEADINGS.size();i++) {
            string_array[i] = HEADINGS.get(i);
        }
        return string_array;
    }
    
    public Object[] ToObjectArray() {
        Object[] object_array = {delete, name, last_updated, size, state, sid, guid};
        return object_array;
    }
    
    public String ToString() {
        String output = "";
        if(delete) {
            output += "Yes" + '\t';
        } else {
            output += "No" + '\t';
        }
        output += name + '\t';
        output += last_updated + '\t';
        if(Pattern.matches("[-+]?[0-9]*\\.?[0-9]+", size)) {
            Double size_in_bytes = Double.parseDouble(size);
            Double size_in_megabytes = size_in_bytes / (1024.0 * 1024.0);
            output += (size_in_megabytes + " MB") + '\t';
        } else {
            if(size.compareTo("") != 0) {
                output += "Could not calculate size" + '\t';
            }
        }
        output += state + '\t';
        output += sid + '\t';
        output += guid;
        return output;
    }
    
    public static String HeadingsToString() {
        String output = "";
        for(int i = 0;i < HEADINGS.size();i++) {
            output += HEADINGS.get(i);
            if(i != HEADINGS.size()-1) {
                output += '\t';
            }
        }
        return output;
    }
}
