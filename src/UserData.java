import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class UserData {
    public static final List<String> HEADINGS = Arrays.asList("Delete?", "Name", "Last Updated", "Size", "State", "SID", "GUID");
    
    private boolean delete;
    private String name;
    private String last_updated;
    private String size;
    private String state;
    private String sid;
    private String guid;
    
    public UserData() {
        delete = false;
        name = "";
        last_updated = "";
        size = "";
        state = "";
        sid = "";
        guid = "";
    }
    
    public UserData(boolean delete, String name, String last_updated, String size, String state, String sid, String guid) {
        this.delete = delete;
        this.name = name;
        this.last_updated = last_updated;
        this.size = size;
        this.state = state;
        this.sid = sid;
        this.guid = guid;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLastUpdated(String last_updated) {
        this.last_updated = last_updated;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public boolean getDelete() {
        return delete;
    }

    public String getName() {
        return name;
    }

    public String getLastUpdated() {
        return last_updated;
    }

    public String getSize() {
        return size;
    }

    public String getState() {
        return state;
    }

    public String getSid() {
        return sid;
    }

    public String getGuid() {
        return guid;
    }

    public static String[] headingsToStringArray() {
        String[] string_array = new String[HEADINGS.size()];
        for(int i=0;i<HEADINGS.size();i++) {
            string_array[i] = HEADINGS.get(i);
        }
        return string_array;
    }
    
    public Object[] toObjectArray() {
        Object[] object_array = {delete, name, last_updated, size, state, sid, guid};
        return object_array;
    }
    
    public String toString() {
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
    
    public static String headingsToString() {
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
