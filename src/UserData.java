
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Essentially a data structure class containing data for a single user account
 * in Windows.
 * <p>
 * Contains the name of the users folder, the last time the folder was updated,
 * the size and editable state of the folder and the SID and GUID value of the
 * corresponding registry keys.<br>
 * Also has an attribute to tell the ProfileDeleter class whether this
 * particular user folder should be deleted.<br>
 * Has some functions for compiling the data into Strings or Object arrays for
 * use with various GUI elements.
 */
public class UserData {

    /**
     * Heading names for use in GUI elements.
     */
    public static final List<String> HEADINGS = Arrays.asList("Delete?", "Name", "Last Updated", "Size", "State", "SID", "GUID");

    /**
     * Class attributes.
     */
    private boolean delete;
    private String name;
    private String last_updated;
    private String size;
    private String state;
    private String sid;
    private String guid;

    /**
     * Constructor for UserData class.
     */
    public UserData() {
        delete = false;
        name = "";
        last_updated = "";
        size = "";
        state = "";
        sid = "";
        guid = "";
    }

    /**
     * Constructor for UserData class with values for initialisation.
     *
     * @param delete whether the user should be flagged for deletion
     * @param name the username of the user
     * @param last_updated the last time the users folder in Windows was updated
     * @param size the size of the users folder in Windows
     * @param state whether the users folder in Windows can be edited
     * @param sid the ProfileList SID value for the user in the registry
     * @param guid the ProfileGuid GUID value for the user in the registry
     */
    public UserData(boolean delete, String name, String last_updated, String size, String state, String sid, String guid) {
        this.delete = delete;
        this.name = name;
        this.last_updated = last_updated;
        this.size = size;
        this.state = state;
        this.sid = sid;
        this.guid = guid;
    }

    /**
     * Sets the delete attribute
     *
     * @param delete whether the user should be flagged for deletion
     */
    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    /**
     * Sets the name attribute
     *
     * @param name the username of the user
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the last updated attribute
     *
     * @param last_updated the last time the users folder in Windows was updated
     */
    public void setLastUpdated(String last_updated) {
        this.last_updated = last_updated;
    }

    /**
     * Sets the size attribute
     *
     * @param size the size of the users folder in Windows
     */
    public void setSize(String size) {
        this.size = size;
    }

    /**
     * Sets the state attribute
     *
     * @param state whether the users folder in Windows can be edited
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * Sets the sid attribute
     *
     * @param sid the ProfileList SID value for the user in the registry
     */
    public void setSid(String sid) {
        this.sid = sid;
    }

    /**
     * Sets the guid attribute
     *
     * @param guid the ProfileGuid GUID value for the user in the registry
     */
    public void setGuid(String guid) {
        this.guid = guid;
    }

    /**
     * Gets the delete attribute
     *
     * @return whether the user should be flagged for deletion
     */
    public boolean getDelete() {
        return delete;
    }

    /**
     * Gets the name attribute
     *
     * @return the username of the user
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the last updated attribute
     *
     * @return the last time the users folder in Windows was updated
     */
    public String getLastUpdated() {
        return last_updated;
    }

    /**
     * Gets the size attribute
     *
     * @return the size of the users folder in Windows
     */
    public String getSize() {
        return size;
    }

    /**
     * Gets the state attribute
     *
     * @return whether the users folder in Windows can be edited
     */
    public String getState() {
        return state;
    }

    /**
     * Gets the sid attribute
     *
     * @return the ProfileList SID value for the user in the registry
     */
    public String getSid() {
        return sid;
    }

    /**
     * Gets the guid attribute
     *
     * @return the ProfileGuid GUID value for the user in the registry
     */
    public String getGuid() {
        return guid;
    }

    /**
     * Converts the HEADINGS attribute into a String array.
     * <p>
     * Designed to be used for GUI elements.
     *
     * @return the HEADINGS attribute as a String array
     */
    public static String[] headingsToStringArray() {
        String[] string_array = new String[HEADINGS.size()];
        for (int i = 0; i < HEADINGS.size(); i++) {
            string_array[i] = HEADINGS.get(i);
        }
        return string_array;
    }

    /**
     * Converts the HEADINGS attribute into a tab delimited String.
     * <p>
     * Designed to be used for GUI elements.
     *
     * @return the HEADINGS attribute as a tab delimited String
     */
    public static String headingsToString() {
        String output = "";
        for (int i = 0; i < HEADINGS.size(); i++) {
            output += HEADINGS.get(i);
            if (i != HEADINGS.size() - 1) {
                output += '\t';
            }
        }
        return output;
    }

    /**
     * Converts the attributes of the UserData to an Object array.
     * <p>
     * Designed to be used for GUI elements.
     *
     * @return the attribute of the UserData as an Object array
     */
    public Object[] toObjectArray() {
        Object[] object_array = {delete, name, last_updated, size, state, sid, guid};
        return object_array;
    }

    /**
     * Converts the attributes of the UserData into a tab delimited String.
     * <p>
     * Designed to be used for GUI elements.
     *
     * @return the attribute of the UserData as a tab delimited String
     */
    public String toString() {
        String output = "";
        if (delete) {
            output += "Yes" + '\t';
        } else {
            output += "No" + '\t';
        }
        output += name + '\t';
        output += last_updated + '\t';
        if (Pattern.matches("[-+]?[0-9]*\\.?[0-9]+", size)) {
            Double size_in_bytes = Double.parseDouble(size);
            Double size_in_megabytes = size_in_bytes / (1024.0 * 1024.0);
            output += (size_in_megabytes + " MB") + '\t';
        } else {
            if (size.compareTo("") != 0) {
                output += "Could not calculate size" + '\t';
            }
        }
        output += state + '\t';
        output += sid + '\t';
        output += guid;
        return output;
    }
}
