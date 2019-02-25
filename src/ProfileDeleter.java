
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Class with functionality to delete user profile folders from a computer
 * running Windows.
 * <p>
 * In Windows 7 and up when a user logs into a computer it stores a copy of
 * their profile in the (default) folder C:\Users.<br>
 * It also adds some data to the registry for the user in the registry keys:
 * HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows
 * NT\\CurrentVersion\\ProfileList.<br>
 * HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows
 * NT\\CurrentVersion\\ProfileGuid.
 * <p>
 * To remove a user account from a computer in Windows, whether to conserve hard
 * drive space or because the users profile is corrupt on the computer you need
 * to:<br>
 * <ol>
 * <li>Delete the users folder from the users directory.</li>
 * <li>Match the registry key in ProfileList to the user account and delete the
 * key
 * under ProfileList.</li>
 * <li>Match the registry key in ProfileGuid to the users registry key in
 * ProfileList and delete the matching key under ProfileGuid.</li>
 * </ol>
 * <p>
 * Doing this for more than one user at a time can be tedious and time
 * consuming.<br>
 * This class automates this process.
 * <p>
 * Can set an ActionListener on the class if creating a GUI that displays the
 * ProfileDeleter's log. ProfileDeleter will trigger a "LogWritten" ActionEvent
 * on the ActionListener to notify the ActionListener that the log has been
 * updated.
 */
public class ProfileDeleter {

    /**
     * Class attributes.
     */
    private String remote_computer;
    private String users_directory;
    private String local_data_directory;
    private volatile List<UserData> user_list;
    private volatile List<String> users_deleted;
    private List<String> cannot_delete_list;
    private List<String> should_not_delete_list;
    private volatile List<String> log_list;
    private String session_id;
    private String logs_location;
    private String pstools_location;
    private String reports_location;
    private String sessions_location;
    private String src_location;
    private AtomicInteger number_of_users_deleted;
    private int state_check_attempts;
    private int registry_check_attempts;
    private int folder_deletion_attempts;
    private int registry_sid_deletion_attempts;
    private int registry_guid_deletion_attempts;
    private int number_of_pooled_threads;
    private int intended_number_of_pooled_threads;
    private boolean size_check;
    private boolean state_check;
    private boolean registry_check;
    private boolean size_check_complete;
    private boolean state_check_complete;
    private boolean registry_check_complete;
    private boolean delete_all_users;
    private ActionListener log_updated;
    private ExecutorService thread_pool;

    /**
     * Severity level for logged messages.
     */
    public enum LOG_TYPE {
        INFO(0),
        WARNING(1),
        ERROR(2);

        private final int state;

        LOG_TYPE(int new_state) {
            state = new_state;
        }

        public int GetState() {
            return state;
        }
    }

    /**
     * Constructor for ProfileDeleter class.
     *
     * @throws UnrecoverableException if a configuration file cannot be loaded
     */
    public ProfileDeleter() throws UnrecoverableException {
        this(null);
    }

    /**
     * Constructor for ProfileDeleter class that allows an ActionListener to be
     * specified.
     * <p>
     * When the log list attribute is updated ProfileDeleter will trigger a
     * "LogWritten" ActionEvent on the ActionListener to notify it that the log
     * has been updated.<br>
     * This is intended to allow GUI classes to update any UI elements that
     * display the log.
     *
     * @param log_updated the ActonListener to notify that the log has been
     * updated
     *
     * @throws UnrecoverableException if a configuration file cannot be loaded
     */
    public ProfileDeleter(ActionListener log_updated) throws UnrecoverableException {
        remote_computer = "";
        users_directory = "";
        local_data_directory = "";
        user_list = Collections.synchronizedList(new ArrayList<UserData>());
        users_deleted = Collections.synchronizedList(new ArrayList<String>());
        log_list = Collections.synchronizedList(new ArrayList<String>());
        cannot_delete_list = new ArrayList<>();
        should_not_delete_list = new ArrayList<>();
        session_id = "";
        logs_location = "";
        pstools_location = "";
        reports_location = "";
        sessions_location = "";
        src_location = "";
        number_of_users_deleted = new AtomicInteger(0);
        state_check_attempts = 0;
        registry_check_attempts = 0;
        folder_deletion_attempts = 0;
        registry_sid_deletion_attempts = 0;
        registry_guid_deletion_attempts = 0;
        number_of_pooled_threads = 0;
        intended_number_of_pooled_threads = 0;
        size_check = false;
        state_check = false;
        registry_check = false;
        size_check_complete = false;
        state_check_complete = false;
        registry_check_complete = false;
        delete_all_users = false;
        this.log_updated = log_updated;
        thread_pool = null;
        try {
            loadConfigFile();
        } catch (UnrecoverableException e) {
            String message = e.getMessage() + ". Cannot recover from this error";
            throw new UnrecoverableException(message);
        }
    }

    /**
     * Sets the remote computer and users directory attributes.
     *
     * @param remote_computer the hostname or IP address of the target computer
     */
    public void setRemoteComputer(String remote_computer) {
        this.remote_computer = remote_computer;
        this.users_directory = "\\\\" + remote_computer + "\\c$\\users\\";
        logMessage("Remote computer set to " + remote_computer, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the value of the users directory attribute.
     *
     * @param users_directory the filepath to the target computers users
     * directory
     */
    public void setUsersDirectory(String users_directory) {
        this.users_directory = users_directory;
        logMessage("Users directory set to " + users_directory, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the value of the local data directory attribute
     *
     * @param local_data_directory the filepath to the directory on the local
     * computer for storing ProfileDeleter data
     */
    public void setLocalDataDirectory(String local_data_directory) {
        this.local_data_directory = local_data_directory;
        logMessage("Local data directory set to " + local_data_directory, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the logs location attribute.
     * <p>
     * Where log files should be stored.
     *
     * @param logs_location where log files should be stored
     */
    public void setLogsLocation(String logs_location) {
        this.logs_location = logs_location;
        logMessage("Logs location set to " + logs_location, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the pstools location attribute.
     * <p>
     * Where pstools files are stored.
     *
     * @param pstools_location where pstools files should be stored
     */
    public void setPstoolsLocation(String pstools_location) {
        this.pstools_location = pstools_location;
        logMessage("Pstools location set to " + pstools_location, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the reports location attribute.
     * <p>
     * Where reports files should be stored.
     *
     * @param reports_location where reports files should be stored.
     */
    public void setReportsLocation(String reports_location) {
        this.reports_location = reports_location;
        logMessage("Reports location set to " + reports_location, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the sessions location attribute.
     * <p>
     * Where sessions files should be stored.
     *
     * @param sessions_location where sessions files should be stored.
     */
    public void setSessionsLocation(String sessions_location) {
        this.sessions_location = sessions_location;
        logMessage("Sessions location set to " + sessions_location, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the src location attribute.
     * <p>
     * Where src files are stored.
     *
     * @param src_location where src files are stored.
     */
    public void setSrcLocation(String src_location) {
        this.src_location = src_location;
        logMessage("Src location set to " + src_location, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the state check attempts attribute.
     * <p>
     * The number of times a state check should be rerun before determining a
     * failure.
     *
     * @param state_check_attempts the number of times a state check should be
     * rerun before determining a failure
     */
    public void setStateCheckAttempts(int state_check_attempts) {
        this.state_check_attempts = state_check_attempts;
        logMessage("State check attempts set to " + state_check_attempts, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the registry check attempts attribute.
     * <p>
     * The number of times a registry query should be rerun before determining a
     * failure.
     *
     * @param registry_check_attempts the number of times a registry query
     * should be rerun before determining a failure
     */
    public void setRegistryCheckAttempts(int registry_check_attempts) {
        this.registry_check_attempts = registry_check_attempts;
        logMessage("Registry check attempts set to " + registry_check_attempts, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the folder deletion attempts attribute.
     * <p>
     * The number of times to attempt to delete a user folder before determining
     * a failure.
     *
     * @param folder_deletion_attempts the number of times to attempt to delete
     * a user folder before determining a failure
     */
    public void setFolderDeletionAttempts(int folder_deletion_attempts) {
        this.folder_deletion_attempts = folder_deletion_attempts;
        logMessage("Folder deletion attempts set to " + folder_deletion_attempts, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the registry sid deletion attempts attribute.
     * <p>
     * The number to attempt to delete a user registry sid before determining a
     * failure.
     *
     * @param registry_sid_deletion_attempts the number of times to attempt to
     * delete a user registry sid before determining a failure
     */
    public void setRegistrySidDeletionAttempts(int registry_sid_deletion_attempts) {
        this.registry_sid_deletion_attempts = registry_sid_deletion_attempts;
        logMessage("Registry SID deletion attempts set to " + registry_sid_deletion_attempts, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the registry guid deletion attempts attribute.
     * <p>
     * The number to attempt to delete a user registry guid before determining a
     * failure.
     *
     * @param registry_guid_deletion_attempts the number of times to attempt to
     * delete a user registry guid before determining a failure
     */
    public void setRegistryGuidDeletionAttempts(int registry_guid_deletion_attempts) {
        this.registry_guid_deletion_attempts = registry_guid_deletion_attempts;
        logMessage("Registry GUID deletion attempts set to " + registry_guid_deletion_attempts, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the number of pooled threads attribute.
     * <p>
     * The number of pooled threads to use for various lengthy processes.
     *
     * @param number_of_pooled_threads the number of pooled threads to use for
     * various lengthy processes
     */
    public void setNumberOfPooledThreads(int number_of_pooled_threads) {
        this.number_of_pooled_threads = number_of_pooled_threads;
        logMessage("Number of pooled threads set to " + number_of_pooled_threads, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the intended number of pooled threads attribute.
     * <p>
     * The number of pooled threads intended to be used for various lengthy
     * processes. The actual number of pooled threads used may be less than the
     * intended number as the program will adjust as necessary.
     *
     * @param intended_number_of_pooled_threads the intended number of pooled
     * threads to use for
     * various lengthy processes
     */
    public void setIntendedNumberOfPooledThreads(int intended_number_of_pooled_threads) {
        this.intended_number_of_pooled_threads = intended_number_of_pooled_threads;
        logMessage("Intended number of pooled threads set to " + intended_number_of_pooled_threads, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the size check attribute.
     * <p>
     * Determines whether a size check is done when checks are run.
     *
     * @param size_check whether to run a size check or not
     */
    public void setSizeCheck(boolean size_check) {
        this.size_check = size_check;
        logMessage("Size check set to " + size_check, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the state check attribute.
     * <p>
     * Determines whether a state check is done when checks are run.
     *
     * @param state_check whether to run a registry check or not
     */
    public void setStateCheck(boolean state_check) {
        this.state_check = state_check;
        logMessage("State check set to " + state_check, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the registry check attribute.
     * <p>
     * Determines whether a registry check is done when checks are run.
     *
     * @param registry_check whether to run a registry check or not
     */
    public void setRegistryCheck(boolean registry_check) {
        this.registry_check = registry_check;
        logMessage("Registry check set to " + registry_check, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the size check complete attribute.
     * <p>
     * Whether a size check has been completed or not.
     *
     * @param size_check_complete Whether a size check has been completed or not
     */
    public void setSizeCheckComplete(boolean size_check_complete) {
        this.size_check_complete = size_check_complete;
        logMessage("Size check complete set to " + size_check_complete, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the state check complete attribute.
     * <p>
     * Whether a state check has been completed or not.
     *
     * @param state_check_complete Whether a state check has been completed or
     * not
     */
    public void setStateCheckComplete(boolean state_check_complete) {
        this.state_check_complete = state_check_complete;
        logMessage("State check complete set to " + state_check_complete, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the registry check complete attribute.
     * <p>
     * Whether a registry check has been completed or not.
     *
     * @param registry_check_complete Whether a registry check has been
     * completed or not
     */
    public void setRegistryCheckComplete(boolean registry_check_complete) {
        this.registry_check_complete = registry_check_complete;
        logMessage("Registry check complete set to " + registry_check_complete, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the delete all users attribute.
     * <p>
     * Whether all users should be flagged for deletion or not.
     *
     * @param delete_all_users Whether to flag all users for deletion
     */
    public void setDeleteAllUsers(boolean delete_all_users) {
        this.delete_all_users = delete_all_users;
        if (user_list != null && !user_list.isEmpty() && state_check_complete) {
            for (UserData user : user_list) {
                if (!cannot_delete_list.contains(user.getName().toLowerCase()) && !should_not_delete_list.contains(user.getName().toLowerCase()) && user.getState().compareTo("Editable") == 0) {
                    user.setDelete(this.delete_all_users);
                }
            }
        }
        logMessage("Delete all users set to " + delete_all_users, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the user list attribute.
     *
     * @param user_list list of UserData that contain details for the users on
     * the target computer
     */
    public void setUserList(List<UserData> user_list) {
        this.user_list = Collections.synchronizedList(user_list);
    }

    /**
     * Sets the cannot delete list attribute.
     * <p>
     * The users in this list cannot be deleted, regardless of their editable
     * state.
     *
     * @param cannot_delete_list list of user account names that are not allowed
     * to be deleted
     */
    public void setCannotDeleteList(List<String> cannot_delete_list) {
        this.cannot_delete_list = cannot_delete_list;
    }

    /**
     * Sets the should not delete list attribute.
     * <p>
     * The users in this list will never be automatically flagged for deletion,
     * they must be manually flagged.
     *
     * @param should_not_delete_list list of user account names that are
     * recommended to not delete
     */
    public void setShouldNotDeleteList(List<String> should_not_delete_list) {
        this.should_not_delete_list = should_not_delete_list;
    }

    /**
     * Sets the log list attribute.
     *
     * @param log_list list of logged events
     */
    public void setLogList(List<String> log_list) {
        this.log_list = Collections.synchronizedList(log_list);
    }

    /**
     * Sets the log updated attribute.
     *
     * @param log_updated the ActionListener to notify when the log is updated
     */
    public void setLogUpdatedActionListener(ActionListener log_updated) {
        this.log_updated = log_updated;
    }

    /**
     * Gets the value of the remote computer attribute.
     *
     * @return the target computers hostname or IP address
     */
    public String getRemoteComputer() {
        return remote_computer;
    }

    /**
     * Gets the value of the users directory attribute.
     *
     * @return the filepath to the target computers users directory
     */
    public String getUsersDirectory() {
        return users_directory;
    }

    /**
     * Gets the value of the local data directory attribute
     *
     * @return the filepath to the directory on the local computer for storing
     * ProfileDeleter data
     */
    public String getLocalDataDirectory() {
        return local_data_directory;
    }

    /**
     * Gets the logs location attribute.
     * <p>
     * Where log files should be stored.
     *
     * @return where log files should be stored.
     */
    public String getLogsLocation() {
        return logs_location;
    }

    /**
     * Gets the pstools location attribute.
     * <p>
     * Where pstools files should be stored.
     *
     * @return where pstools are stored.
     */
    public String getPstoolsLocation() {
        return pstools_location;
    }

    /**
     * Gets the reports location attribute.
     * <p>
     * Where reports files should be stored.
     *
     * @return where reports files should be stored.
     */
    public String getReportsLocation() {
        return reports_location;
    }

    /**
     * Gets the sessions location attribute.
     * <p>
     * Where sessions files should be stored.
     *
     * @return where sessions files should be stored.
     */
    public String getSessionsLocation() {
        return sessions_location;
    }

    /**
     * Gets the src location attribute.
     * <p>
     * Where src files should be stored.
     *
     * @return where src files are stored.
     */
    public String getSrcLocation() {
        return src_location;
    }

    /**
     * Gets the number of users deleted attribute.
     * <p>
     * The number of users that have been deleted in the current run of
     * processDeletion.
     *
     * @return the number of users that have currently been deleted
     */
    public AtomicInteger getNumberOfUsersDeleted() {
        return number_of_users_deleted;
    }

    /**
     * Gets the state check attempts attribute.
     * <p>
     * The number of times a state check should be rerun before determining a
     * failure.
     *
     * @return the number of times a state check should be rerun before
     * determining a failure
     */
    public int getStateCheckAttempts() {
        return state_check_attempts;
    }

    /**
     * Gets the registry check attempts attribute.
     * <p>
     * The number of times a registry query should be rerun before determining a
     * failure.
     *
     * @return the number of times a registry query should be rerun before
     * determining a failure
     */
    public int getRegistryCheckAttempts() {
        return registry_check_attempts;
    }

    /**
     * Gets the folder deletion attempts attribute.
     * <p>
     * The number of times to attempt to delete a user folder before determining
     * a failure.
     *
     * @return the number of times to attempt to delete a user folder before
     * determining a failure
     */
    public int getFolderDeletionAttempts() {
        return folder_deletion_attempts;
    }

    /**
     * Gets the registry sid deletion attempts attribute.
     * <p>
     * The number to attempt to delete a user registry sid before determining a
     * failure.
     *
     * @return the number of times to attempt to delete a user registry sid
     * before determining a failure
     */
    public int getRegistrySidDeletionAttempts() {
        return registry_sid_deletion_attempts;
    }

    /**
     * Gets the registry guid deletion attempts attribute.
     * <p>
     * The number to attempt to delete a user registry guid before determining a
     * failure.
     *
     * @return the number of times to attempt to delete a user registry guid
     * before determining a failure
     */
    public int getRegistryGuidDeletionAttempts() {
        return registry_guid_deletion_attempts;
    }

    /**
     * Gets the number of pooled threads attribute.
     * <p>
     * The number of pooled threads to use for various lengthy processes.
     *
     * @return the number of pooled threads to use for various lengthy processes
     */
    public int getNumberOfPooledThreads() {
        return number_of_pooled_threads;
    }

    /**
     * Gets the intended number of pooled threads attribute.
     * <p>
     * The number of pooled threads intended to be used for various lengthy
     * processes. The actual number of pooled threads used may be less than the
     * intended number as the program will adjust as necessary.
     *
     * @return the intended number of pooled threads to use for various lengthy
     * processes
     */
    public int getIntendedNumberOfPooledThreads() {
        return intended_number_of_pooled_threads;
    }

    /**
     * Gets the size check attribute.
     *
     * @return whether to run a size check or not
     */
    public boolean getSizeCheck() {
        return size_check;
    }

    /**
     * Gets the state check attribute.
     *
     * @return whether to run a state check or not
     */
    public boolean getStateCheck() {
        return state_check;
    }

    /**
     * Gets the registry check attribute.
     *
     * @return whether to run a registry check or not
     */
    public boolean getRegistryCheck() {
        return registry_check;
    }

    /**
     * Gets the size check complete attribute.
     *
     * @return whether a size check has been completed
     */
    public boolean getSizeCheckComplete() {
        return size_check_complete;
    }

    /**
     * Gets the state check complete attribute.
     *
     * @return whether a state check has been completed
     */
    public boolean getStateCheckComplete() {
        return state_check_complete;
    }

    /**
     * Gets the registry check complete attribute.
     *
     * @return whether a registry check has been completed
     */
    public boolean getRegistryCheckComplete() {
        return registry_check_complete;
    }

    /**
     * Gets the delete all users attribute.
     *
     * @return whether to flag all users for deletion
     */
    public boolean getDeleteAllUsers() {
        return delete_all_users;
    }

    /**
     * Gets the user list attribute.
     *
     * @return list of UserData that contain details for the users on the target
     * computer
     */
    public List<UserData> getUserList() {
        return user_list;
    }

    /**
     * Gets the users deleted attribute.
     *
     * @return the list of users deleted by the processDeletion function
     */
    public List<String> getUsersDeleted() {
        return users_deleted;
    }

    /**
     * Gets the cannot delete list attribute.
     * <p>
     * The users in this list cannot be deleted, regardless of their editable
     * state.
     *
     * @return list of user account names that are not allowed to be deleted
     */
    public List<String> getCannotDeleteList() {
        return cannot_delete_list;
    }

    /**
     * Gets the should not delete list attribute.
     * <p>
     * The users in this list will never be automatically flagged for deletion,
     * they must be manually flagged.
     *
     * @return list of user account names that are recommended to not delete
     */
    public List<String> getShouldNotDeleteList() {
        return should_not_delete_list;
    }

    /**
     * Gets the log list attribute.
     *
     * @return list of logged events
     */
    public List<String> getLogList() {
        return log_list;
    }

    /**
     * Gets the log updated attribute.
     *
     * @return the ActionListener to notify when the log is updated
     */
    public ActionListener getLogUpdatedActionListener() {
        return log_updated;
    }

    /**
     * Converts the user list attribute into a 2D Object array so it can be
     * displayed in a JTable.
     *
     * @return 2D Object array of the user list attribute
     */
    public Object[][] convertUserListTo2DObjectArray() {
        logMessage("Converting user list to 2D Object array", LOG_TYPE.INFO, true, false);
        Object[][] object_array = new Object[user_list.size()][];
        for (int i = 0; i < user_list.size(); i++) {
            object_array[i] = user_list.get(i).toObjectArray();/*
            String last_updated_to_calendar = object_array[i][2].toString().replace(" ","/");
            last_updated_to_calendar = last_updated_to_calendar.replace(":","/");
            String[] calendar_fields = last_updated_to_calendar.split("/");
            Calendar last_updated_as_calendar = Calendar.getInstance();
            last_updated_as_calendar.set(Integer.parseInt(calendar_fields[2]), Integer.parseInt(calendar_fields[0]), Integer.parseInt(calendar_fields[1]), Integer.parseInt(calendar_fields[3]), Integer.parseInt(calendar_fields[4]), Integer.parseInt(calendar_fields[5]));
             */
            try {
                Date last_updated_to_date = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").parse(object_array[i][2].toString());
                object_array[i][2] = last_updated_to_date;
            } catch (ParseException e) {
                logMessage("Failed to convert user " + user_list.get(i).getName() + " last updated to date. Last updated value is " + user_list.get(i).getLastUpdated(), LOG_TYPE.WARNING, true, false);
            }
        }
        logMessage("Converted user list to 2D Object array", LOG_TYPE.INFO, true, false);
        return object_array;
    }

    /**
     * Process local Windows account deletion on target computer.
     * <p>
     * Before this can be done the user list attribute must be set and state
     * check + registry check must be set to true.<br>
     * These can be done manually or by running the generateUserList function to
     * create the user list and running the checking functions checkState,
     * checkRegistry or checkAll.<br>
     * A size check does not need to be done to process the deletion, this is an
     * optional check as it can take a very long time to complete.
     *
     * @return deletion deletion report detailing users deleted and any problems
     * deleting the user folder or registry keys
     * @throws NotInitialisedException user list has not been initialised or a
     * state and/or registry check has not been run
     * @throws InterruptedException the thread pool was interrupted before all
     * tasks could be completed
     */
    public List<String> processDeletion() throws NotInitialisedException, InterruptedException {
        logMessage("Attempting to run deletion on users list", LOG_TYPE.INFO, true);
        if (user_list != null && !user_list.isEmpty() && state_check_complete && registry_check_complete) {
            List<UserData> new_folders = new ArrayList<>();
            users_deleted = Collections.synchronizedList(new ArrayList<String>());
            number_of_users_deleted.set(0);
            double total_size_deleted = 0.0;
            users_deleted.add("User" + '\t' + "Successful?" + '\t' + "Folder Deleted?" + '\t' + "SID Deleted?" + '\t' + "GUID Deleted?" + '\t' + "SID" + '\t' + "GUID" + '\t' + "Size");
            //ExecutorService thread_pool = Executors.newFixedThreadPool(number_of_pooled_threads);
            logMessage("Pooling user deletions for each flagged user", LOG_TYPE.INFO, true);
            List<delete_user_process> delete_user_process_list = new ArrayList<delete_user_process>();
            for (UserData user : user_list) {
                if (user.getDelete()) {
                    logMessage("User " + user.getName() + " is flagged for deletion", LOG_TYPE.INFO, true);
                    users_deleted.add(user.getName());
                    //thread_pool.submit(new delete_user_process(user, this, users_deleted));
                    delete_user_process_list.add(new delete_user_process(user, this, users_deleted, number_of_users_deleted));
                } else {
                    new_folders.add(user);
                }
            }
            logMessage("All tasks have been scheduled, awaiting task completion", LOG_TYPE.INFO, true);
            /*thread_pool.shutdown();
            boolean thread_pool_terminated = false;
            while (!thread_pool_terminated) {
                thread_pool_terminated = thread_pool.isTerminated();
            }*/
            try {
                thread_pool.invokeAll(delete_user_process_list);
                logMessage("All tasks completed", LOG_TYPE.INFO, true);
                if (users_deleted.size() > 1) {
                    for (int i = 1; i < users_deleted.size(); i++) {
                        String[] deleted_user = users_deleted.get(i).split("\t");
                        try {
                            total_size_deleted = Double.parseDouble(deleted_user[7]);
                        } catch (NumberFormatException e) {
                        }
                    }
                }
            } catch (InterruptedException e) {
                logMessage("Failed to run pooled delete user tasks, thread pool was interrupted. Error is: " + e.getMessage(), LOG_TYPE.ERROR, true);
                throw e;
            }
            user_list = Collections.synchronizedList(new_folders);
            if (intended_number_of_pooled_threads > 0) {
                if (intended_number_of_pooled_threads > user_list.size()) {
                    number_of_pooled_threads = user_list.size();
                } else {
                    number_of_pooled_threads = intended_number_of_pooled_threads;
                }
                if (thread_pool != null) {
                    thread_pool.shutdown();
                    while (!thread_pool.isShutdown()) {
                    }
                    thread_pool = null;
                }
                thread_pool = Executors.newFixedThreadPool(number_of_pooled_threads);
            }
            logMessage("Completed deletions", LOG_TYPE.INFO, true);
            if (users_deleted.size() > 1) {
                try {
                    List<String> formatted_report = new ArrayList<String>();
                    formatted_report.add("Deletion Report");
                    formatted_report.add("Computer: " + remote_computer);
                    formatted_report.add("Total Size Deleted: " + Long.toString(Math.round(total_size_deleted)));
                    for (String deleted_folder : users_deleted) {
                        formatted_report.add(deleted_folder);
                    }
                    writeToFile(reports_location + "\\" + remote_computer + "_deletion_report_" + session_id + ".txt", formatted_report);
                    logMessage("Deletion report written to file " + reports_location + "\\" + remote_computer + "_deletion_report_" + session_id + ".txt", LOG_TYPE.INFO, true);
                } catch (IOException e) {
                    logMessage("Failed to write deletion report to file " + reports_location + "\\" + remote_computer + "_deletion_report_" + session_id + ".txt. Error is: " + e.getMessage(), LOG_TYPE.ERROR, true);
                }
            }
            return users_deleted;
        } else {
            String message = "Either user list has not been initialised or a state and/or registry check has not been run";
            logMessage(message, LOG_TYPE.WARNING, true);
            throw new NotInitialisedException(message);
        }
    }

    /**
     * Backs up ProfileList and ProfileGuid registry keys on the target computer
     * and copies the files to the local computer.
     * <p>
     * Local data directory and remote data directory attributes need to be
     * initialised before this function can be run.<br>
     * ProfileList registry key =
     * HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows
     * NT\\CurrentVersion\\ProfileList.<br>
     * ProfileGuid registry key =
     * HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows
     * NT\\CurrentVersion\\ProfileGuid.
     *
     * @throws IOException thrown if any IO errors are received running registry
     * backup and file copy commands on target and local computer
     * @throws InterruptedException thrown if any of the process threads used to
     * run registry backup and file copy commands on the target and local
     * computer become interrupted
     * @throws CannotEditException unable to create required folders or files on
     * target or local computer
     * @throws NotInitialisedException local or remote data directory attribute
     * has not been initialised
     */
    public void backupAndCopyRegistry() throws IOException, InterruptedException, CannotEditException, NotInitialisedException {
        logMessage("Attempting to backup profilelist and profileguid registry keys on remote computer", LOG_TYPE.INFO, true);
        if (local_data_directory.compareTo("") == 0) {
            String message = "Local data directory has not been initialised";
            logMessage(message, LOG_TYPE.WARNING, true);
            throw new NotInitialisedException(message);
        } else {
            String filename_friendly_computer = remote_computer.replace('.', '_');
            int count = 1;
            boolean run = true;
            while (run) {
                try {
                    registryQuery(remote_computer, "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProfileList", local_data_directory + "\\" + filename_friendly_computer + "_ProfileList.txt");
                    run = false;
                } catch (IOException | InterruptedException | CannotEditException e) {
                    if (count >= registry_check_attempts) {
                        throw e;
                    } else {
                        logMessage("Attempt " + Integer.toString(count) + " at backing up registry key failed", LOG_TYPE.WARNING, true);
                        count++;
                    }
                }
            }
            run = true;
            count = 1;
            while (run) {
                try {
                    registryQuery(remote_computer, "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProfileGuid", local_data_directory + "\\" + filename_friendly_computer + "_ProfileGuid.txt");
                    run = false;
                } catch (IOException | InterruptedException | CannotEditException e) {
                    if (count >= registry_check_attempts) {
                        throw e;
                    } else {
                        logMessage("Attempt " + Integer.toString(count) + " at backing up registry key failed", LOG_TYPE.WARNING, true);
                        count++;
                    }
                }
            }
        }
    }

    /**
     * Processes ProfileSid and ProfileGuid registry data obtained from target
     * computer and assigns the values to the correct user in the user list
     * attribute.
     * <p>
     * backupAndCopyRegistry function must be run before this function can be
     * run, or the needed files need to be manually created.<br>
     * Local data directory attribute must be initialised before this function
     * can be run.
     *
     * @throws IOException thrown if IO errors are received when trying to open
     * needed files
     * @throws NotInitialisedException local data directory attribute has not
     * been initialised
     */
    public void findSIDAndGUID() throws IOException, NotInitialisedException {
        logMessage("Attempting to compile SID and GUID data from registry backups", LOG_TYPE.INFO, true);
        if (local_data_directory.compareTo("") != 0) {
            List<String> regkeys_profile_list;
            List<String> regkeys_profile_guid;
            String filename_friendly_computer = remote_computer.replace('.', '_');
            try {
                logMessage("Loading file " + local_data_directory + "\\" + filename_friendly_computer + "_ProfileList.txt", LOG_TYPE.INFO, true);
                regkeys_profile_list = readFromFile(local_data_directory + "\\" + filename_friendly_computer + "_ProfileList.txt");
                logMessage("Loading file " + local_data_directory + "\\" + filename_friendly_computer + "_ProfileGuid.txt", LOG_TYPE.INFO, true);
                regkeys_profile_guid = readFromFile(local_data_directory + "\\" + filename_friendly_computer + "_ProfileGuid.txt");
                if (regkeys_profile_list != null && !regkeys_profile_list.isEmpty() && regkeys_profile_guid != null && !regkeys_profile_guid.isEmpty()) {
                    String current_sid = "";
                    String profile_path = "";
                    String profile_guid = "";
                    boolean found_profile_path = false;
                    int count = 0;
                    logMessage("Processing file " + local_data_directory + "\\" + filename_friendly_computer + "_ProfileList.txt", LOG_TYPE.INFO, true);
                    for (String line : regkeys_profile_list) {
                        line = line.replace(" ", "");
                        line = line.replace("\t", "");
                        if (line.startsWith("HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\WindowsNT\\CurrentVersion\\ProfileList\\") || count == regkeys_profile_list.size() - 1) {
                            String new_sid = line.replace("HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\WindowsNT\\CurrentVersion\\ProfileList\\", "");
                            logMessage("Found new SID " + new_sid, LOG_TYPE.INFO, true);
                            if (!profile_path.isEmpty()) {
                                logMessage("Processing details for found profile " + profile_path, LOG_TYPE.INFO, true);
                                boolean found_user = false;
                                for (UserData user : user_list) {
                                    if (user.getName().toLowerCase().compareTo(profile_path.toLowerCase().replace("c:\\users\\", "")) == 0) {
                                        found_user = true;
                                        logMessage("Found matching user account", LOG_TYPE.INFO, true);
                                        if (!user.getSid().isEmpty()) {
                                            logMessage("SID already exists for user, resolving conflict", LOG_TYPE.INFO, true);
                                            boolean found_guid = false;
                                            for (String guid : regkeys_profile_guid) {
                                                if (guid.contains("HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProfileGuid\\")) {
                                                    guid = guid.replace("HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProfileGuid\\", "");
                                                    if (guid.compareTo(profile_guid) == 0) {
                                                        logMessage("Found matching GUID from " + local_data_directory + "\\" + filename_friendly_computer + "_ProfileGuid.txt. Checking SID for match", LOG_TYPE.INFO, true);
                                                        found_guid = true;
                                                    }
                                                } else if (found_guid) {
                                                    String sid = guid.replace(" ", "");
                                                    sid = sid.replace("\t", "");
                                                    sid = sid.replace("SidStringREG_SZ", "");
                                                    if (sid.compareTo(current_sid) == 0) {
                                                        logMessage("New SID details match SID details for GUID, replacing user details with new details. SID set to " + current_sid + " and GUID to " + profile_guid, LOG_TYPE.INFO, true);
                                                        user_list.get(user_list.indexOf(user)).setSid(current_sid);
                                                        user_list.get(user_list.indexOf(user)).setGuid(profile_guid);
                                                        break;
                                                    }
                                                }
                                            }
                                            logMessage("No match found, discarding new details found for SID " + current_sid, LOG_TYPE.INFO, true);
                                        } else {
                                            logMessage("Set SID for user " + profile_path + " to " + current_sid + " and GUID to " + profile_guid, LOG_TYPE.INFO, true);
                                            user_list.get(user_list.indexOf(user)).setSid(current_sid);
                                            user_list.get(user_list.indexOf(user)).setGuid(profile_guid);
                                        }
                                        break;
                                    }
                                }
                                if (!found_user) {
                                    logMessage("No matching user found for profile " + profile_path, LOG_TYPE.INFO, true);
                                }
                                current_sid = new_sid;
                                profile_path = "";
                                profile_guid = "";
                            } else {
                                current_sid = new_sid;
                                logMessage("SID is " + current_sid, LOG_TYPE.INFO, true);
                            }
                        } else if (line.startsWith("ProfileImagePathREG_EXPAND_SZ")) {
                            profile_path = line.replace("ProfileImagePathREG_EXPAND_SZ", "");
                            logMessage("Found profile_path " + profile_path, LOG_TYPE.INFO, true);
                        } else if (line.startsWith("GuidREG_SZ")) {
                            profile_guid = line.replace("GuidREG_SZ", "");
                            logMessage("Found GUID " + profile_guid, LOG_TYPE.INFO, true);
                        }
                        count++;
                    }
                    registry_check_complete = true;
                    logMessage("Successfully compiled SID and GUID data from registry backups", LOG_TYPE.INFO, true);
                } else {
                    String message = "File " + local_data_directory + "\\" + filename_friendly_computer + "_ProfileList.txt or " + local_data_directory + "\\" + filename_friendly_computer + "_ProfileGuid.txt is either empty or corrupt";
                    logMessage(message, LOG_TYPE.ERROR, true);
                    throw new NotInitialisedException(message);
                }
            } catch (IOException e) {
                logMessage("Unable to read file " + local_data_directory + "\\" + filename_friendly_computer + "_ProfileList.txt. File may not exist. Error is " + e.getMessage(), LOG_TYPE.ERROR, true);
                throw e;
            }
        }
    }

    /**
     * Populates the user list attribute with data from the target computers
     * users directory.
     * <p>
     * Gets the folder name and last updated date for each user.<br>
     * Sets delete to true unless the folder name is in the cannot delete list.
     *
     * @throws IOException an IO error has occurred when running the powershell
     * script to get the user list on the target computer
     */
    public void generateUserList() throws IOException {
        logMessage("Attempting to build users directory " + users_directory, LOG_TYPE.INFO, true);
        if (users_directory.compareTo("") != 0) {
            try {
                user_list = Collections.synchronizedList(new ArrayList<UserData>());
                String command = "Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process | powershell.exe -File \"" + src_location + "\\GetDirectoryList.ps1\" - directory " + users_directory;
                ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe", "-Command", command);
                builder.redirectErrorStream(true);
                Process power_shell_process = builder.start();
                try (BufferedReader powershell_process_output_stream = new BufferedReader(new InputStreamReader(power_shell_process.getInputStream()))) {
                    String output = "";
                    String line = "";
                    while ((line = powershell_process_output_stream.readLine()).compareTo("EndOfScriptGetDirectoryList") != 0) {
                        if (!line.isEmpty()) {
                            logMessage("Discovered folder details " + line, LOG_TYPE.INFO, true);
                            String[] line_split = line.split("\\t");
                            UserData user = new UserData(false, line_split[0], line_split[1], "", "", "", "");
                            user_list.add(user);
                        }
                    }
                }
                power_shell_process.destroy();
                if (intended_number_of_pooled_threads > 0) {
                    if (intended_number_of_pooled_threads > user_list.size()) {
                        number_of_pooled_threads = user_list.size();
                    } else {
                        number_of_pooled_threads = intended_number_of_pooled_threads;
                    }
                    if (thread_pool != null) {
                        thread_pool.shutdown();
                        while (!thread_pool.isShutdown()) {
                        }
                        thread_pool = null;
                    }
                    thread_pool = Executors.newFixedThreadPool(number_of_pooled_threads);
                }
                logMessage("Successfully built users directory " + users_directory, LOG_TYPE.INFO, true);
            } catch (IOException e) {
                logMessage("Failed to build users directory " + users_directory, LOG_TYPE.ERROR, true);
                logMessage(e.getMessage(), LOG_TYPE.ERROR, true);
                throw e;
            }
        } else {
            logMessage("Computer name has not been specified. Building users directory has been aborted", LOG_TYPE.WARNING, true);
        }
    }

    /**
     * Checks the size of each user folder on the target computer.
     * <p>
     * This check can take a very long time depending on the size of the users
     * directory on the target computer.<br>
     * This check is not required to run a deletion.
     *
     * @throws InterruptedException the thread pool was interrupted before all
     * tasks could be completed
     */
    public void checkSize() throws InterruptedException {
        logMessage("Calcuting size of directory list", LOG_TYPE.INFO, true);
        if (user_list.size() > 0 && users_directory.compareTo("") != 0) {
            //ExecutorService thread_pool = Executors.newFixedThreadPool(number_of_pooled_threads);
            logMessage("Pooling size check tasks for each user", LOG_TYPE.INFO, true);
            List<size_check_process> size_check_process_list = new ArrayList<size_check_process>();
            for (int i = 0; i < user_list.size(); i++) {
                //thread_pool.submit(new size_check_process(i, this));
                size_check_process_list.add(new size_check_process(i, this));
            }
            logMessage("All tasks have been scheduled, awaiting task completion", LOG_TYPE.INFO, true);
            /*thread_pool.shutdown();
            boolean thread_pool_terminated = false;
            while (!thread_pool_terminated) {
                thread_pool_terminated = thread_pool.isTerminated();
            }*/
            try {
                thread_pool.invokeAll(size_check_process_list);
                logMessage("All tasks completed", LOG_TYPE.INFO, true);
                size_check_complete = true;
            } catch (InterruptedException e) {
                logMessage("Failed to run pooled size check tasks, thread pool was interrupted. Error is: " + e.getMessage(), LOG_TYPE.ERROR, true);
                throw e;
            }
            logMessage("Finished calculating size of directory list", LOG_TYPE.INFO, true);
        } else {
            logMessage("Directory list is empty, aborting size calculation", LOG_TYPE.WARNING, true);
        }
    }

    /**
     * Checks the state of each user folder on the target computer to determine
     * if the folder can be edited and therefore deleted.
     * <p>
     * This check is required before a deletion can be run.
     *
     * @throws InterruptedException the thread pool was interrupted before all
     * tasks could be completed
     */
    public void checkState() throws InterruptedException {
        logMessage("Checking editable state of directory list", LOG_TYPE.INFO, true);
        if (user_list.size() > 0 && users_directory.compareTo("") != 0) {
            //ExecutorService thread_pool = Executors.newFixedThreadPool(number_of_pooled_threads);
            logMessage("Pooling state check tasks for each user", LOG_TYPE.INFO, true);
            List<state_check_process> state_check_process_list = new ArrayList<state_check_process>();
            for (int i = 0; i < user_list.size(); i++) {
                //thread_pool.submit(new state_check_process(i, this));
                state_check_process_list.add(new state_check_process(i, this));
            }
            logMessage("All tasks have been scheduled, awaiting task completion", LOG_TYPE.INFO, true);
            /*thread_pool.shutdown();
            boolean thread_pool_terminated = false;
            while (!thread_pool_terminated) {
                thread_pool_terminated = thread_pool.isTerminated();
            }*/
            try {
                thread_pool.invokeAll(state_check_process_list);
                logMessage("All tasks completed", LOG_TYPE.INFO, true);
                state_check_complete = true;
            } catch (InterruptedException e) {
                logMessage("Failed to run pooled state check tasks, thread pool was interrupted. Error is: " + e.getMessage(), LOG_TYPE.ERROR, true);
                throw e;
            }
            logMessage("Finished checking editable state of directory list", LOG_TYPE.INFO, true);
        } else {
            logMessage("Directory list is empty, aborting editable state check", LOG_TYPE.WARNING, true);
        }
    }

    /**
     * Gets the registry sid and guid data for each user account on the target
     * computer.
     * <p>
     * This check is required before a deletion can be run.
     */
    public void checkRegistry() {
        logMessage("Getting registry SID and GUID values for user list", LOG_TYPE.INFO, true);
        generateSessionID();
        try {
            generateLocalSessionFolder();
            try {
                backupAndCopyRegistry();
                try {
                    findSIDAndGUID();
                } catch (IOException | NotInitialisedException e) {
                    logMessage("Unable to process SID and GUID registry data, error is: " + e.getMessage(), LOG_TYPE.ERROR, true);
                }
            } catch (IOException | CannotEditException | NotInitialisedException | InterruptedException e) {
                logMessage("Unable to backup registry files, error is: " + e.getMessage(), LOG_TYPE.ERROR, true);
            }
        } catch (IOException | CannotEditException | NotInitialisedException e) {
            logMessage("Unable to create session folders, error is: " + e.getMessage(), LOG_TYPE.ERROR, true);
        }
    }

    /**
     * Runs the size check, state check and registry check if their
     * corresponding boolean attribute is set to true.
     * <p>
     * Set the corresponding boolean attribute for each check using the
     * setSizeCheck, setStateCheck and setRegistryCheck functions.
     *
     * @throws IOException an IO error occurs when trying to check the editable
     * state of users in user list attribute
     * @throws InterruptedException the cmd process thread for checkState was
     * interrupted
     */
    public void checkAll() throws IOException, InterruptedException {
        logMessage("Running all enabled checks", LOG_TYPE.INFO, true);
        if (size_check) {
            checkSize();
        } else {
            logMessage("Size check is turned off, skipping size check", LOG_TYPE.INFO, true);
        }
        if (state_check) {
            checkState();
        } else {
            logMessage("State check is turned off, skipping state check", LOG_TYPE.INFO, true);
        }
        if (registry_check) {
            checkRegistry();
        } else {
            logMessage("Registry check is turned off, skipping registry check", LOG_TYPE.INFO, true);
        }
        logMessage("Running enabled checks complete", LOG_TYPE.INFO, true);
    }

    /**
     * Generates a session ID for uniquely naming folders and files related to
     * the particular deletion.
     * <p>
     * The session ID is generated using the generateDateString function.
     */
    public void generateSessionID() {
        session_id = generateDateString();
        logMessage("Session ID has been set to " + session_id, LOG_TYPE.INFO, true);
    }

    /**
     * Creates the local session folder for the deletion to run.
     * <p>
     * Creates a folder on the local computer in the sessions folder.<br>
     * Session ID attribute must be set. The folder is named using the session
     * ID so that it is unique.<br>
     * Remote computer must be set as this is used in the name of the folder.
     *
     * @throws NotInitialisedException thrown if the session ID or remote
     * computer attribute are not set
     * @throws IOException an IO error occurs when trying to create the needed
     * folder
     * @throws CannotEditException unable to create the needed folder on the
     * local computer
     */
    public void generateLocalSessionFolder() throws NotInitialisedException, IOException, CannotEditException {
        logMessage("Attempting to create local session folder", LOG_TYPE.INFO, true);
        if (session_id.compareTo("") != 0 && remote_computer.compareTo("") != 0) {
            try {
                directoryCreate(sessions_location + "\\" + remote_computer + "_" + session_id);
                local_data_directory = sessions_location + "\\" + remote_computer + "_" + session_id;
            } catch (IOException | CannotEditException | InterruptedException e) {
                String message = "Unable to create local data directory " + sessions_location + "\\" + remote_computer + "_" + session_id;
                logMessage(message, LOG_TYPE.ERROR, true);
                throw new CannotEditException(message);
            }
            logMessage("Successfully created local session folder", LOG_TYPE.INFO, true);
        } else {
            String message = "";
            if (session_id.compareTo("") == 0) {
                message += "Session ID has not been created";
            }
            if (remote_computer.compareTo("") == 0) {
                if (message.compareTo("") != 0) {
                    message += " and ";
                }
                message += "computer has not been initialised";
            }
            message += ". Please Initialise before running generateLocalSessionFolder";
            logMessage(message, LOG_TYPE.ERROR, true);
            throw new NotInitialisedException(message);
        }
    }

    /**
     * Uses pstools to rename a folder.
     *
     * @param computer the computer the folder is on
     * @param directory the directory containing the folder to rename
     * @param folder the name of the folder to rename
     * @param folder_renamed the name to rename the folder to
     * @throws IOException an IO error occurs when trying to rename the folder
     * @throws CannotEditException unable to rename the folder
     * @throws InterruptedException the pstools process thread was interrupted
     */
    public void directoryRename(String computer, String directory, String folder, String folder_renamed) throws IOException, CannotEditException, InterruptedException {
        try {
            logMessage("Attempting to rename folder " + directory + folder + " to " + folder_renamed, LOG_TYPE.INFO, true);
            String line = "";
            String error = "";
            String command = pstools_location + "\\psexec /accepteula \\\\" + computer + " cmd /c REN \"" + directory + folder + "\" \"" + folder_renamed + "\" && echo editable|| echo uneditable";
            ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", command);
            builder.redirectErrorStream(true);
            Process pstools_process = builder.start();
            try (BufferedReader pstools_process_output_stream = new BufferedReader(new InputStreamReader(pstools_process.getInputStream()))) {
                while ((line = pstools_process_output_stream.readLine()) != null) {
                    error = line;
                }
            }
            pstools_process.waitFor();
            if (!error.equals("editable")) {
                String message = "Unable to rename folder " + directory + folder + ". Error is: " + error;
                throw new CannotEditException(message);
            }
            logMessage("Successfully renamed folder " + directory + folder + " to " + folder_renamed, LOG_TYPE.INFO, true);
        } catch (CannotEditException | IOException | InterruptedException e) {
            logMessage("Could not rename directory " + directory + folder, LOG_TYPE.WARNING, true);
            logMessage(e.getMessage(), LOG_TYPE.WARNING, true);
            throw e;
        }
    }

    /**
     * Calculates the filesize of a user folder on the target computer.
     * <p>
     * Uses powershell script GetFolderSize.ps1.
     *
     * @param user the name of the user folder to calculate the size of on the
     * target computer
     * @return the size of the folder
     * @throws NonNumericException the size calculated is not a number
     * @throws IOException an IO error has occurred when trying to calculate the
     * size of access and run the powershell script
     */
    public String findFolderSize(String user) throws NonNumericException, IOException {
        try {
            logMessage("Calculating filesize for folder " + users_directory + user, LOG_TYPE.INFO, true);
            String command = "Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process | powershell.exe -File \"" + src_location + "\\GetFolderSize.ps1\" - directory " + users_directory + user;
            ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe", "-command", command);
            builder.redirectErrorStream(true);
            Process power_shell_process = builder.start();
            String output;
            try (BufferedReader powershell_process_output_stream = new BufferedReader(new InputStreamReader(power_shell_process.getInputStream()))) {
                output = "";
                String line = "";
                while ((line = powershell_process_output_stream.readLine()).compareTo("EndOfScriptGetFolderSize") != 0) {
                    if (!line.isEmpty()) {
                        output = line;
                    }
                }
            }
            power_shell_process.destroy();
            if (Pattern.matches("[0-9]+", output)) {
                logMessage("Successfully calculated filesize for folder " + users_directory + user + ": " + output, LOG_TYPE.INFO, true);
                return output;
            } else {
                String message = "Size calculated is not a number. Ensure powershell script " + src_location + "\\GetFolderSize.ps1 is correct";
                logMessage(message, LOG_TYPE.ERROR, true);
                throw new NonNumericException(message);
            }
        } catch (NonNumericException | IOException e) {
            logMessage("Could not calculate size of folder " + users_directory + user, LOG_TYPE.ERROR, true);
            logMessage(e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        }
    }

    /**
     * Creates a folder.
     * <p>
     * Can be used to create folders on remote computers using \\computername.
     *
     * @param directory the path + name of the folder to create
     * @throws IOException an IO error has occurred when running process to
     * create the folder
     * @throws CannotEditException unable to create the folder
     * @throws InterruptedException the cmd process thread was interrupted
     */
    public void directoryCreate(String directory) throws IOException, CannotEditException, InterruptedException {
        try {
            logMessage("Attempting to create folder " + directory, LOG_TYPE.INFO, true);
            String line = "";
            String error = "";
            String command = "MKDIR \"" + directory + "\"";
            ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", command);
            builder.redirectErrorStream(true);
            Process cmd_process = builder.start();
            try (BufferedReader cmd_process_output_stream = new BufferedReader(new InputStreamReader(cmd_process.getInputStream()))) {
                while ((line = cmd_process_output_stream.readLine()) != null) {
                    error = line;
                }
            }
            cmd_process.waitFor();
            if (error.compareTo("") != 0) {
                String message = "Folder " + directory + " already exists. Error is: " + error;
                logMessage(message, LOG_TYPE.WARNING, true);
                throw new CannotEditException(message);
            }
            logMessage("Successfully created folder " + directory, LOG_TYPE.INFO, true);
        } catch (IOException | InterruptedException e) {
            logMessage("Could not create folder " + directory, LOG_TYPE.ERROR, true);
            logMessage(e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        } catch (CannotEditException e) {
            throw e;
        }
    }

    /**
     * Deletes a folder.
     * <p>
     * Can be used to delete folders on remote computers using \\computername.
     *
     * @param directory the path + name of the folder to delete
     * @throws IOException an IO error has occurred when running process to
     * delete the folder
     * @throws CannotEditException unable to delete the folder
     * @throws InterruptedException the cmd process thread was interrupted
     */
    public void directoryDelete(String directory) throws IOException, CannotEditException, InterruptedException {
        try {
            logMessage("Attempting to delete folder " + directory, LOG_TYPE.INFO, true);
            String line = "";
            String error = "";
            String command = "RMDIR /S /Q \"" + directory + "\"";
            ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", command);
            builder.redirectErrorStream(true);
            Process cmd_process = builder.start();
            try (BufferedReader cmd_process_output_stream = new BufferedReader(new InputStreamReader(cmd_process.getInputStream()))) {
                while ((line = cmd_process_output_stream.readLine()) != null) {
                    error = line;
                }
            }
            cmd_process.waitFor();
            if (error.compareTo("") != 0) {
                String message = "Unable to delete folder " + directory + ". Error is: " + error;
                logMessage(message, LOG_TYPE.ERROR, true);
                throw new CannotEditException(message);
            }
            logMessage("Successfully deleted folder " + directory, LOG_TYPE.INFO, true);
        } catch (CannotEditException | IOException | InterruptedException e) {
            logMessage("Could not delete folder " + directory, LOG_TYPE.ERROR, true);
            logMessage(e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        }
    }

    /**
     * Deletes a list of files in a folder.
     * <p>
     * Can be used to delete files on remote computers using \\computername.
     *
     * @param directory the path + name of the folder to delete the files in
     * @param files the list of files to delete
     * @param do_not_delete files to not delete, can be used if the list of
     * files to delete is not filtered previously
     * @throws IOException an IO error occurred when attempting to delete the
     * files
     * @throws CannotEditException unable to delete files
     * @throws InterruptedException the cmd process thread was interrupted
     */
    public void directoryDeleteFiles(String directory, List<String> files, List<String> do_not_delete) throws IOException, CannotEditException, InterruptedException {
        try {
            logMessage("Attempting to delete list of files in directory " + directory, LOG_TYPE.INFO, true);
            for (String file : files) {
                boolean delete = true;
                if (do_not_delete != null) {
                    for (String exclude_file : do_not_delete) {
                        if (file.compareTo(exclude_file) == 0) {
                            delete = false;
                        }
                    }
                }
                if (delete) {
                    String command = "del \"" + directory + "\\" + file + "\"";
                    String line = "";
                    String error = "";
                    ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", command);
                    builder.redirectErrorStream(true);
                    Process cmd_process = builder.start();
                    try (BufferedReader cmd_process_output_stream = new BufferedReader(new InputStreamReader(cmd_process.getInputStream()))) {
                        while ((line = cmd_process_output_stream.readLine()) != null) {
                            error = line;
                        }

                    }
                    cmd_process.waitFor();
                    if (error.compareTo("") != 0) {
                        String message = "Unable to delete file " + directory + "\\" + file + ". Error is: " + error;
                        logMessage(message, LOG_TYPE.ERROR, true);
                        throw new CannotEditException(message);
                    }
                    logMessage("Successfully deleted file " + directory + "\\" + file, LOG_TYPE.INFO, true);
                } else {
                    logMessage("File " + directory + "\\" + file + " is in do not delete list. It has not been deleted", LOG_TYPE.INFO, true);
                }
            }
        } catch (CannotEditException | IOException | InterruptedException e) {
            logMessage("Failed to delete all requested files in directory " + directory, LOG_TYPE.ERROR, true);
            logMessage(e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        }
    }

    /**
     * Gets a list of files in a folder
     *
     * @param directory the path + folder name of the folder to get the files
     * list from
     * @return the list of files in the designated folder
     * @throws IOException an IO error occurred when getting the list of files
     * @throws CannotEditException unable to read filenames from the designated
     * folder
     * @throws InterruptedException the cmd process thread was interrupted
     */
    public List<String> directoryListFiles(String directory) throws IOException, CannotEditException, InterruptedException {
        try {
            logMessage("Attempting to get list of files in directory " + directory, LOG_TYPE.INFO, true);
            List<String> files = new ArrayList<>();
            String line = "";
            String error = "";
            String command = "dir /b /a-d \"" + directory + "\"";
            ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", command);
            builder.redirectErrorStream(true);
            Process cmd_process = builder.start();
            try (BufferedReader cmd_process_output_stream = new BufferedReader(new InputStreamReader(cmd_process.getInputStream()))) {
                while ((line = cmd_process_output_stream.readLine()) != null) {
                    if (line.compareTo("") != 0) {
                        files.add(line);
                    }
                    error = line;
                }
            }
            cmd_process.waitFor();
            if (error.compareTo("") != 0) {
                String message = "Unable to get list of files in diectory " + directory + ". Error is: " + error;
                logMessage(message, LOG_TYPE.ERROR, true);
                throw new CannotEditException(message);
            } else {
                logMessage("Successfully got list of files in directory " + directory, LOG_TYPE.INFO, true);
                return files;
            }
        } catch (CannotEditException | IOException | InterruptedException e) {
            logMessage("Could not get list of files in directory " + directory, LOG_TYPE.ERROR, true);
            logMessage(e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        }
    }

    /**
     * Copies a single file
     *
     * @param old_full_file_name the path + name of the file to copy. Can be
     * copied from a remote computer using \\computername
     * @param new_directory the folder to copy the file to
     * @throws IOException an IO error occurred when copying the file
     * @throws CannotEditException unable to copy the file
     * @throws InterruptedException the cmd process thread was interrupted
     */
    public void fileCopy(String old_full_file_name, String new_directory) throws IOException, CannotEditException, InterruptedException {
        try {
            logMessage("Attempting to copy file " + old_full_file_name + " to new directory " + new_directory, LOG_TYPE.INFO, true);
            String line = "";
            String error = "";
            String command = "copy \"" + old_full_file_name + "\" \"" + new_directory + "\"";
            ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", command);
            builder.redirectErrorStream(true);
            Process cmd_process = builder.start();
            try (BufferedReader cmd_process_output_stream = new BufferedReader(new InputStreamReader(cmd_process.getInputStream()))) {
                while ((line = cmd_process_output_stream.readLine()) != null) {
                    error = line;
                }
            }
            cmd_process.waitFor();
            if (!error.contains("file(s) copied")) {
                String message = "Unable to copy file " + old_full_file_name + " to folder " + new_directory + ". Error is: " + error;
                logMessage(message, LOG_TYPE.ERROR, true);
                throw new CannotEditException(message);
            }
            logMessage("Successfully copied file " + old_full_file_name + " to new directory " + new_directory, LOG_TYPE.INFO, true);
        } catch (CannotEditException | IOException | InterruptedException e) {
            logMessage("Could not copy file " + old_full_file_name + " to new directory " + new_directory, LOG_TYPE.ERROR, true);
            logMessage(e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        }
    }

    /**
     * Creates a registry backup using REG QUERY.
     *
     * @param computer the computer to create the backup on
     * @param reg_key the registry key to backup
     * @param full_file_name the path + filename of the backup file to create.
     * Include the file extension
     * @throws IOException an IO error occurred when backing up the registry or
     * writing the file
     * @throws CannotEditException unable to access the registry key or unable
     * to create the backup file
     * @throws InterruptedException the cmd process thread was interrupted
     */
    public void registryQuery(String computer, String reg_key, String full_file_name) throws IOException, CannotEditException, InterruptedException {
        try {
            logMessage("Attempting to save registry key " + reg_key + " on computer " + computer + " to folder " + full_file_name, LOG_TYPE.INFO, true);
            String line = "";
            String error = "";
            boolean run = true;
            String command = "REG QUERY \"\\\\" + computer + "\\" + reg_key + "\" /s > \"" + full_file_name + "\"";
            ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", command);
            builder.redirectErrorStream(true);
            Process cmd_process = builder.start();
            try (BufferedReader cmd_process_output_stream = new BufferedReader(new InputStreamReader(cmd_process.getInputStream()))) {
                while ((line = cmd_process_output_stream.readLine()) != null && run) {
                    error = line;
                }
            }
            cmd_process.waitFor();
            if (error.compareTo("") != 0) {
                String message = "Could not save registry key " + reg_key + " on computer " + computer + " to folder " + full_file_name;
                logMessage(message, LOG_TYPE.ERROR, true);
                throw new CannotEditException(message);
            }
            logMessage("Successfully saved registry key " + reg_key + " on computer " + computer + " to folder " + full_file_name, LOG_TYPE.INFO, true);
        } catch (IOException | InterruptedException e) {
            logMessage("Could not save registry key " + reg_key + " on computer " + computer + " to folder " + full_file_name + ". Error is " + e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        }
    }

    /**
     * Deletes a registry key using pstools.
     *
     * @param computer the computer to delete the registry key from
     * @param reg_key the registry key to delete
     * @throws IOException an IO error occurred when trying to delete the
     * registry key
     * @throws InterruptedException the pstools process thread was interrupted
     */
    public void registryDelete(String computer, String reg_key) throws IOException, CannotEditException, InterruptedException {
        try {
            logMessage("Attempting to delete registry key " + reg_key + " from computer " + computer, LOG_TYPE.INFO, true);
            String line = "";
            String error = "";
            String command = pstools_location + "\\psexec /accepteula \\\\" + computer + " REG DELETE \"" + reg_key + "\" /f";
            ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", command);
            builder.redirectErrorStream(true);
            Process pstools_process = builder.start();
            try (BufferedReader cmd_process_output_stream = new BufferedReader(new InputStreamReader(pstools_process.getInputStream()))) {
                while ((line = cmd_process_output_stream.readLine()) != null) {
                    error = line;
                }
            }
            if (!error.contains("error code 0")) {
                String message = "Could not delete registry key " + reg_key + " on computer " + computer;
                logMessage(message, LOG_TYPE.ERROR, true);
                throw new CannotEditException(message);
            }
            pstools_process.waitFor();
            logMessage("Successfully deleted registry key " + reg_key + " from computer " + computer, LOG_TYPE.INFO, true);
        } catch (IOException | CannotEditException | InterruptedException e) {
            logMessage("Could not delete registry key " + reg_key + " from computer " + computer, LOG_TYPE.ERROR, true);
            logMessage(e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        }
    }

    /**
     * Compiles the user list into a single readable String.
     * <p>
     * The String includes \n and \t characters to aid formatting.
     *
     * @return readable String containing the user list data
     */
    public String printUserList() {
        logMessage("Compiling user list into readable String", LOG_TYPE.INFO, true, false);
        String output = "";
        Double total_size = 0.0;
        output += UserData.headingsToString();
        if (user_list.size() > 0) {
            output += '\n';
        }
        for (int i = 0; i < user_list.size(); i++) {
            output += user_list.get(i).toString();
            if (Pattern.matches("[-+]?[0-9]*\\.?[0-9]+", user_list.get(i).getSize())) {
                total_size += Double.parseDouble(user_list.get(i).getSize());
            }
            if (i != user_list.size() - 1) {
                output += '\n';
            }
        }
        if (user_list.size() > 0) {
            Double size_in_megabytes = total_size / (1024.0 * 1024.0);
            long size_in_megaytes_long = Math.round(size_in_megabytes);
            String size_in_megabytes_long_string = Long.toString(size_in_megaytes_long);
            String size_in_megabytes_string = "";
            int count = 0;
            for (int i = size_in_megabytes_long_string.length() - 1; i < 0; i--) {
                if (count == 2) {
                    size_in_megabytes_string += ",";
                    count = 0;
                }
                size_in_megabytes_string += size_in_megabytes_long_string.charAt(i);
                count++;
            }
            output += '\n' + "Total size:" + '\t' + (size_in_megabytes_string + " MB");
        }
        logMessage("Successfully compiled user list into readable String", LOG_TYPE.INFO, true, false);
        return output;
    }

    /**
     * Generates a String using the current date/time.
     *
     * @return the generated String based on the current date/time
     */
    public String generateDateString() {
        String output = generateDateString("");
        return output;
    }

    /**
     * Generates a String using the current date/time.
     * <p>
     * Can supply a prefix to add to the front of the generated String.
     *
     * @param prefix the prefix to add to the front of the generated String.
     * @return the generated String based on the current date/time and prefix
     * supplied
     */
    public String generateDateString(String prefix) {
        logMessage("Generating date/time String with prefix " + prefix, LOG_TYPE.INFO, true, false);
        TimeZone timezone = TimeZone.getTimeZone("UTC");
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat filename_utc = new SimpleDateFormat("yyMMddHHmmss");
        String current_date = filename_utc.format(calendar.getTime());
        logMessage("Generated date/time String " + prefix + current_date, LOG_TYPE.INFO, true, false);
        return prefix + current_date;
    }

    /**
     * Adds a message to the log.
     * <p>
     * Requires a severity using the LOG_TYPE enum and can choose to include a
     * timestamp.
     *
     * @param message the message to add to the log
     * @param severity the severity LOG_TYPE of the message
     * @param include_timestamp whether to include a timestamp or not
     */
    public void logMessage(String message, LOG_TYPE severity, boolean include_timestamp) {
        logMessage(message, severity, include_timestamp, true);
    }

    /**
     * Adds a message to the log.
     * <p>
     * Requires a severity using the LOG_TYPE enum and can choose to include a
     * timestamp.<br>
     * If an ActionListener has been specified on the ProfileDeleter class it
     * will trigger a "LogWritten" ActionEvent on the ActionListener. This is
     * intended to allow any GUI classes to update any elements used to display
     * the log as it is updated.
     *
     * @param message the message to add to the log
     * @param severity the severity LOG_TYPE of the message
     * @param include_timestamp whether to include a timestamp or not
     * @param display_to_gui triggers a "LogWritten" action event if an
     * ActionListener has been specified on the ProfileDeleter class
     */
    public void logMessage(String message, LOG_TYPE severity, boolean include_timestamp, boolean display_to_gui) {
        String log_message = "";
        if (null != severity) {
            switch (severity) {
                case INFO:
                    log_message += "Info: ";
                    break;
                case WARNING:
                    log_message += "Warning: ";
                    break;
                case ERROR:
                    log_message += "ERROR: ";
                    break;
                default:
                    break;
            }
        }

        if (include_timestamp) {
            SimpleDateFormat human_readable_timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
            Date timestamp = new Date();
            String format_timestamp = human_readable_timestamp.format(timestamp);
            log_message += "[" + format_timestamp + "] ";
        }

        log_message += message;

        log_list.add(log_message);
        if (display_to_gui && log_updated != null) {
            log_updated.actionPerformed(new java.awt.event.ActionEvent(this, 0, "LogWritten"));
        }
    }

    /**
     * Dumps the list of logged messages to a text file.
     *
     * @return the filename of the created text file
     * @throws IOException an IO error occurred when trying to create the text
     * file
     * @throws NotInitialisedException the log list attribute has not had
     * anything logged to it
     */
    public String writeLog() throws IOException, NotInitialisedException {
        if (!log_list.isEmpty()) {
            try {
                String filename = logs_location + "\\Profile_Deleter_Log_" + remote_computer + "_" + generateDateString() + ".txt";
                writeToFile(filename, log_list);
                return filename;
            } catch (IOException e) {
                throw e;
            }
        } else {
            throw new NotInitialisedException("Nothing has been logged");
        }
    }

    /**
     * Loads the profiledeleter.config file.
     * <p>
     * This file contains configuration information for the ProfileDeleter
     * class.<br>
     * It specifies where the various data folders are located and populates the
     * cannot delete list and should not delete list attributes.<br>
     * If the profiledeleter.config file is missing one of the folder location
     * paths it will instead attempt to run the profiledeleter.config.default
     * file.<br>
     * If the profiledeleter.config.default file is missing one of the folder
     * location paths it will attempt to recreate the file.<br>
     * If it fails to recreate the profiledeleter.config.default file it will
     * throw an UnrecoverableException.
     *
     *
     * @throws UnrecoverableException if the profiledeleter.config file cannot
     * be loaded or the profiledeleter.config.default file cannot be loaded or
     * cannot be recreated in the case it is incomplete
     */
    public void loadConfigFile() throws UnrecoverableException {
        logMessage("Attempting to load a configuration file", LOG_TYPE.INFO, true);
        List<String> config_file = new ArrayList<>();
        boolean failed_to_load_config = false;
        boolean failed_to_load_config_default = false;
        boolean attempting_to_load_config = true;
        while (attempting_to_load_config) {
            logs_location = "";
            pstools_location = "";
            reports_location = "";
            sessions_location = "";
            src_location = "";
            size_check = false;
            state_check = false;
            registry_check = false;
            delete_all_users = false;
            state_check_attempts = 0;
            registry_check_attempts = 0;
            number_of_pooled_threads = 0;
            cannot_delete_list = new ArrayList<>();
            should_not_delete_list = new ArrayList<>();
            try {
                if (!failed_to_load_config) {
                    logMessage("Attempting to load profiledeleter.config", LOG_TYPE.INFO, true);
                    config_file = readFromFile("profiledeleter.config");
                } else {
                    logMessage("Attempting to load profiledeleter.config.default", LOG_TYPE.INFO, true);
                    config_file = readFromFile("profiledeleter.config.default");
                }
                for (String line : config_file) {
                    if (line.startsWith("logs=")) {
                        logs_location = line.replace("logs=", "");
                    } else if (line.startsWith("pstools=")) {
                        pstools_location = line.replace("pstools=", "");
                    } else if (line.startsWith("reports=")) {
                        reports_location = line.replace("reports=", "");
                    } else if (line.startsWith("sessions=")) {
                        sessions_location = line.replace("sessions=", "");
                    } else if (line.startsWith("src=")) {
                        src_location = line.replace("src=", "");
                    } else if (line.startsWith("size_check_default=")) {
                        size_check = (Boolean.parseBoolean(line.replace("size_check_default=", "")));
                    } else if (line.startsWith("state_check_default=")) {
                        state_check = (Boolean.parseBoolean(line.replace("state_check_default=", "")));
                    } else if (line.startsWith("registry_check_default=")) {
                        registry_check = (Boolean.parseBoolean(line.replace("registry_check_default=", "")));
                    } else if (line.startsWith("delete_all_users_default=")) {
                        delete_all_users = (Boolean.parseBoolean(line.replace("delete_all_users_default=", "")));
                    } else if (line.startsWith("state_check_attempts=")) {
                        state_check_attempts = (Integer.parseInt(line.replace("state_check_attempts=", "")));
                        if (state_check_attempts < 1) {
                            throw new NonNumericException("state_check_attempts must be greater than 0");
                        }
                    } else if (line.startsWith("registry_check_attempts=")) {
                        registry_check_attempts = (Integer.parseInt(line.replace("registry_check_attempts=", "")));
                        if (registry_check_attempts < 1) {
                            throw new NonNumericException("registry_check_attempts must be greater than 0");
                        }
                    } else if (line.startsWith("folder_deletion_attempts=")) {
                        folder_deletion_attempts = (Integer.parseInt(line.replace("folder_deletion_attempts=", "")));
                        if (folder_deletion_attempts < 1) {
                            throw new NonNumericException("folder_deletion_attempts must be greater than 0");
                        }
                    } else if (line.startsWith("registry_sid_deletion_attempts=")) {
                        registry_sid_deletion_attempts = (Integer.parseInt(line.replace("registry_sid_deletion_attempts=", "")));
                        if (registry_sid_deletion_attempts < 1) {
                            throw new NonNumericException("registry_sid_deletion_attempts must be greater than 0");
                        }
                    } else if (line.startsWith("registry_guid_deletion_attempts=")) {
                        registry_guid_deletion_attempts = (Integer.parseInt(line.replace("registry_guid_deletion_attempts=", "")));
                        if (registry_guid_deletion_attempts < 1) {
                            throw new NonNumericException("registry_guid_deletion_attempts must be greater than 0");
                        }
                    } else if (line.startsWith("number_of_pooled_threads=")) {
                        if (line.replace("number_of_pooled_threads=", "").equals("max")) {
                            intended_number_of_pooled_threads = 2147483647;
                        } else {
                            intended_number_of_pooled_threads = (Integer.parseInt(line.replace("number_of_pooled_threads=", "")));
                            if (intended_number_of_pooled_threads < 1) {
                                throw new NonNumericException("number_of_pooled_threads must be greater than 0");
                            }
                        }
                    } else if (line.startsWith("cannot_delete_list=")) {
                        cannot_delete_list.add(line.replace("cannot_delete_list=", ""));
                    } else if (line.startsWith("should_not_delete_list=")) {
                        should_not_delete_list.add(line.replace("should_not_delete_list=", ""));
                    }
                }

                if (logs_location == null || logs_location.isEmpty() || pstools_location == null || pstools_location.isEmpty() || reports_location == null || reports_location.isEmpty() || sessions_location == null || sessions_location.isEmpty() || src_location == null || src_location.isEmpty() || state_check_attempts <= 0 || registry_check_attempts <= 0 || folder_deletion_attempts <= 0 || registry_sid_deletion_attempts <= 0 || registry_guid_deletion_attempts <= 0 || intended_number_of_pooled_threads <= 0) {
                    if (!failed_to_load_config) {
                        logMessage("Profiledeleter.config file is incomplete, will attempt to load profiledeleter.config.default instead", LOG_TYPE.ERROR, true);
                        failed_to_load_config = true;
                    } else {
                        if (!failed_to_load_config_default) {
                            logMessage("Profiledeleter.config.default file is incomplete", LOG_TYPE.ERROR, true);
                            logMessage("Attempting to recreate profiledeleter.config.default", LOG_TYPE.INFO, true);
                            failed_to_load_config_default = true;
                            List<String> profile_deleter_config_default = generateProfileDeleterConfigDefault();
                            writeToFile("profiledeleter.config.default", profile_deleter_config_default, false);
                            logMessage("Successfully recreated profiledeleter.config.default", LOG_TYPE.INFO, true);
                        } else {
                            logMessage("Profiledeleter.config.default is incomplete, program cannot start without a working config file", LOG_TYPE.ERROR, true);
                            throw new UnrecoverableException("profiledeleter.config and profiledeleter.config.default files are incomplete");
                        }
                    }
                } else {
                    logMessage("Successfully loaded config file", LOG_TYPE.INFO, true);
                    attempting_to_load_config = false;
                }
            } catch (IOException | NumberFormatException | NonNumericException e) {
                if (!failed_to_load_config) {
                    logMessage("Failed to load profiledeleter.config, will attempt to load profiledeleter.config.default instead", LOG_TYPE.ERROR, true);
                    failed_to_load_config = true;
                } else {
                    if (!failed_to_load_config_default) {
                        logMessage("Failed to load profiledeleter.config.default", LOG_TYPE.ERROR, true);
                        logMessage("Attempting to recreate profiledeleter.config.default", LOG_TYPE.INFO, true);
                        failed_to_load_config_default = true;
                        List<String> profile_deleter_config_default = generateProfileDeleterConfigDefault();
                        try {
                            writeToFile("profiledeleter.config.default", profile_deleter_config_default, false);
                            logMessage("Successfully recreated profiledeleter.config.default", LOG_TYPE.INFO, true);
                        } catch (IOException e2) {
                            logMessage("Failed to recreate profiledeleter.config.default", LOG_TYPE.ERROR, true);
                        }
                    } else {
                        String message = "Unable to load any config file, program cannot start";
                        logMessage(message, LOG_TYPE.ERROR, true);
                        throw new UnrecoverableException(message);
                    }
                }
            }
        }
    }

    /**
     * Generates a List containing String values for the
     * profiledeleter.config.default file.
     *
     * @return a List containing String values for the
     * profiledeleter.config.default file
     */
    public List<String> generateProfileDeleterConfigDefault() {
        List<String> profile_deleter_config_default = new ArrayList<>();
        profile_deleter_config_default.add("* default configuration settings for ProfileDeleter program. DO NOT EDIT THIS FILE. If you want to change the configuration settings edit profiledeleter.config instead");
        profile_deleter_config_default.add("* locations for folders used by the program");
        profile_deleter_config_default.add("logs=logs");
        profile_deleter_config_default.add("pstools=pstools");
        profile_deleter_config_default.add("reports=reports");
        profile_deleter_config_default.add("sessions=sessions");
        profile_deleter_config_default.add("src=src");
        profile_deleter_config_default.add("help=help");
        profile_deleter_config_default.add("* whether specific toggles should default to true or false");
        profile_deleter_config_default.add("size_check_default=false");
        profile_deleter_config_default.add("state_check_default=true");
        profile_deleter_config_default.add("registry_check_default=true");
        profile_deleter_config_default.add("delete_all_users_default=false");
        profile_deleter_config_default.add("show_tooltips=false");
        profile_deleter_config_default.add("* how long (in milliseconds) to wait before displaying tooltips and dismissing tooltips once displayed");
        profile_deleter_config_default.add("tooltip_delay_timer=0");
        profile_deleter_config_default.add("tooltip_dismiss_timer=60000");
        profile_deleter_config_default.add("* the number of times to repeat specfic checks and processes before registering a fail");
        profile_deleter_config_default.add("state_check_attempts=10");
        profile_deleter_config_default.add("registry_check_attempts=30");
        profile_deleter_config_default.add("folder_deletion_attempts=10");
        profile_deleter_config_default.add("registry_sid_deletion_attempts=10");
        profile_deleter_config_default.add("registry_guid_deletion_attempts=10");
        profile_deleter_config_default.add("* number of concurrent threads to use for size check and deletion process");
        profile_deleter_config_default.add("number_of_pooled_threads=10");
        profile_deleter_config_default.add("* cannot delete list. Users in this list cannot be deleted by the program. Add users to the list by including a new line with cannot_delete_list=<username>");
        profile_deleter_config_default.add("cannot_delete_list=public");
        profile_deleter_config_default.add("cannot_delete_list=default");
        profile_deleter_config_default.add("* should not delete list. Users in this list will not be flagged for deletion automatically by the program and must be flagged manually. Add users to the list by including a new line with should_not_delete_list=<username>");
        profile_deleter_config_default.add("should_not_delete_list=administrator");
        profile_deleter_config_default.add("should_not_delete_list=intranet");
        return profile_deleter_config_default;
    }

    /**
     * Reads all lines in a file and adds them to a String list.
     *
     * @param filename the path + filename of the file to read
     * @return the contents of the file compiled into a String list
     * @throws IOException an IO error occurred when trying to read the file
     */
    public List<String> readFromFile(String filename) throws IOException {
        List<String> read_data = new ArrayList<>();
        try {
            File file = new File(filename);
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                for (String line; (line = br.readLine()) != null;) {
                    read_data.add(line);
                }
            }
        } catch (IOException e) {
            throw e;
        }
        return read_data;
    }

    /**
     * Write all lines in a String list to a file.
     * <p>
     * Appends to the end of the file.
     *
     * @param filename the path + filename of the file to write to
     * @param write_to_file the String list to write to the file
     * @throws IOException an IO error occurred when trying to write to the file
     */
    public void writeToFile(String filename, List<String> write_to_file) throws IOException {
        writeToFile(filename, write_to_file, true);
    }

    /**
     * Write all lines in a String list to a file.
     * <p>
     * Can specify whether to append to the end of the file or write from the
     * beginning.
     *
     * @param filename the path + filename of the file to write to
     * @param write_to_file the String list to write to the file
     * @param append whether to append to the end of the file or write from the
     * beginning
     * @throws IOException an IO error occurred when trying to write to the file
     */
    public void writeToFile(String filename, List<String> write_to_file, boolean append) throws IOException {
        try {
            int count = 0;
            File file = new File(filename);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, append))) {
                for (String string_line : write_to_file) {
                    if (count > 0) {
                        writer.newLine();
                    }
                    writer.write(string_line);
                    count++;
                }
            }
        } catch (IOException e) {
            throw e;
        }
    }

    /**
     * Pings a computer to see if it is reachable on the network.
     * <p>
     * Can be supplied a hostname or IP address.
     *
     * @param PC the hostname or IP address of the computer to ping
     * @return whether the computer is reachable on the network or not
     * @throws IOException an IO error occurred when trying to run the process
     * to ping the computer
     * @throws InterruptedException the cmd process thread was interrupted
     */
    public boolean pingPC(String PC) throws IOException, InterruptedException {
        logMessage("Pinging PC " + PC + " to ensure it exists and is reachable on the network", LOG_TYPE.INFO, true);
        boolean pc_online = false;
        try {
            String command = "ping " + PC + " -n 1";
            ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", command);
            builder.redirectErrorStream(true);
            Process p = builder.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            p.waitFor();

            boolean search = true;
            String detail = "";
            while (search) {
                detail = r.readLine();
                if (detail == null) {
                    pc_online = false;
                    search = false;
                } else if (detail.contains("Received = 1")) {
                    pc_online = true;
                    search = false;
                }
            }
        } catch (IOException | InterruptedException e) {
            logMessage("Ping check has failed with error " + e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        }
        logMessage("Ping check has completed, result is " + pc_online, LOG_TYPE.INFO, true);
        return pc_online;
    }
}

class size_check_process implements Callable<Object> {

    private int index;
    private ProfileDeleter profile_deleter;

    size_check_process(int index, ProfileDeleter profile_deleter) {
        this.index = index;
        this.profile_deleter = profile_deleter;
    }

    @Override
    public Object call() {
        String folder = profile_deleter.getUserList().get(index).getName();
        String folder_size = "";
        try {
            folder_size = profile_deleter.findFolderSize(folder);
            profile_deleter.logMessage("Calculated size " + folder_size + " for folder " + folder, ProfileDeleter.LOG_TYPE.INFO, true);
        } catch (NonNumericException | IOException e) {
            folder_size = "Could not calculate size";
            profile_deleter.logMessage(folder_size + " for folder " + folder, ProfileDeleter.LOG_TYPE.WARNING, true);
            profile_deleter.logMessage(e.getMessage(), ProfileDeleter.LOG_TYPE.ERROR, true);
        }
        profile_deleter.getUserList().get(index).setSize(folder_size);
        return null;
    }
}

class state_check_process implements Callable<Object> {

    private int index;
    private ProfileDeleter profile_deleter;

    state_check_process(int index, ProfileDeleter profile_deleter) {
        this.index = index;
        this.profile_deleter = profile_deleter;
    }

    @Override
    public Object call() {
        String user = profile_deleter.getUserList().get(index).getName();
        String folder_size = "";
        profile_deleter.logMessage("Checking editable state of folder " + user, ProfileDeleter.LOG_TYPE.INFO, true);
        try {
            if (!profile_deleter.getCannotDeleteList().contains(user.toLowerCase())) {
                int count = 1;
                boolean run = true;
                while (run) {
                    if (count > 1) {
                        profile_deleter.logMessage("Attempt " + count + " at checking state for user " + user, ProfileDeleter.LOG_TYPE.INFO, true);
                    }
                    try {
                        profile_deleter.directoryRename(profile_deleter.getRemoteComputer(), "C:\\users\\", user, user);
                        profile_deleter.getUserList().get(index).setState("Editable");
                        if (profile_deleter.getDeleteAllUsers() && !profile_deleter.getShouldNotDeleteList().contains(user.toLowerCase())) {
                            profile_deleter.getUserList().get(index).setDelete(true);
                        }
                        run = false;
                        profile_deleter.logMessage("User " + user + " determined to be editable", ProfileDeleter.LOG_TYPE.INFO, true);
                    } catch (CannotEditException e) {
                        if (count >= profile_deleter.getStateCheckAttempts()) {
                            profile_deleter.getUserList().get(index).setDelete(false);
                            profile_deleter.logMessage("User " + user + " determined to be uneditable, all attempts have failed, state set to uneditable", ProfileDeleter.LOG_TYPE.INFO, true);
                            run = false;
                            throw e;
                        } else {
                            count++;
                            profile_deleter.logMessage("User " + user + " determined to be uneditable, running state check again", ProfileDeleter.LOG_TYPE.INFO, true);
                        }
                    }
                }
            } else {
                profile_deleter.getUserList().get(index).setState("Uneditable");
                profile_deleter.getUserList().get(index).setDelete(false);
                profile_deleter.logMessage("User is in the cannot delete list, skipping check for this user", ProfileDeleter.LOG_TYPE.INFO, true);
            }
        } catch (CannotEditException e) {
            String message = "Uneditable";
            profile_deleter.logMessage(message + ". User may be logged in or PC may need to be restarted", ProfileDeleter.LOG_TYPE.WARNING, true);
            profile_deleter.getUserList().get(index).setState(message);
            profile_deleter.getUserList().get(index).setDelete(false);
        } catch (IOException | InterruptedException e) {
            profile_deleter.logMessage("Editable state check has failed, you may not have permission to rename folders in the user directory or PC may be offline", ProfileDeleter.LOG_TYPE.ERROR, true);
            profile_deleter.logMessage(e.getMessage(), ProfileDeleter.LOG_TYPE.ERROR, true);
        }
        return null;
    }
}

class delete_user_process implements Callable<Object> {

    private UserData user;
    private ProfileDeleter profile_deleter;
    private List<String> deleted_folders;
    private AtomicInteger number_of_users_deleted;

    delete_user_process(UserData user, ProfileDeleter profile_deleter, List<String> deleted_folders, AtomicInteger number_of_users_deleted) {
        this.user = user;
        this.profile_deleter = profile_deleter;
        this.deleted_folders = deleted_folders;
        this.number_of_users_deleted = number_of_users_deleted;
    }

    @Override
    public Object call() {
        profile_deleter.logMessage("User " + user.getName() + " is flagged for deletion", ProfileDeleter.LOG_TYPE.INFO, true);
        boolean folder_delete = false;
        boolean sid_delete = false;
        boolean guid_delete = false;
        int error_count = 0;
        String deleted_user_success = "";
        String deleted_user_folder_success = "";
        String deleted_user_sid_success = "";
        String deleted_user_guid_success = "";
        while (!folder_delete && error_count < profile_deleter.getFolderDeletionAttempts()) {
            try {
                profile_deleter.directoryDelete(profile_deleter.getUsersDirectory() + user.getName());
                deleted_user_folder_success = "Yes";
                folder_delete = true;
                profile_deleter.logMessage("Successfully deleted user directory for " + user.getName(), ProfileDeleter.LOG_TYPE.INFO, true);
            } catch (IOException | CannotEditException | InterruptedException e) {
                if (error_count >= profile_deleter.getFolderDeletionAttempts() - 1) {
                    String message = "Failed to delete user directory " + user.getName() + ". Error is " + e.getMessage();
                    deleted_user_folder_success = message;
                    profile_deleter.logMessage(message, ProfileDeleter.LOG_TYPE.ERROR, true);
                } else {
                    profile_deleter.logMessage("Failed to delete user directory " + user.getName() + " on attempt " + (error_count + 1) + ". Will try again", ProfileDeleter.LOG_TYPE.WARNING, true);
                }
                error_count++;
            }
        }
        error_count = 0;
        while (!sid_delete && error_count < profile_deleter.getRegistrySidDeletionAttempts()) {
            try {
                if (user.getSid().compareTo("") != 0) {
                    profile_deleter.registryDelete(profile_deleter.getRemoteComputer(), "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProfileList\\" + user.getSid());
                    deleted_user_sid_success = "Yes";
                    profile_deleter.logMessage("Successfully deleted SID " + user.getSid() + " for user " + user.getName(), ProfileDeleter.LOG_TYPE.INFO, true);
                } else {
                    deleted_user_sid_success = "SID is blank";
                    profile_deleter.logMessage("SID for user " + user.getName() + " is blank", ProfileDeleter.LOG_TYPE.WARNING, true);
                }
                sid_delete = true;
            } catch (IOException | CannotEditException | InterruptedException e) {
                if (error_count >= profile_deleter.getRegistrySidDeletionAttempts() - 1) {
                    String message = "Failed to delete user SID " + user.getSid() + " from registry. Error is " + e.getMessage();
                    deleted_user_sid_success = message;
                    profile_deleter.logMessage(message, ProfileDeleter.LOG_TYPE.ERROR, true);
                } else {
                    profile_deleter.logMessage("Failed to delete user SID " + user.getSid() + " on attempt " + (error_count + 1) + ". Will try again", ProfileDeleter.LOG_TYPE.WARNING, true);
                }
                error_count++;
            }
        }
        error_count = 0;
        while (!guid_delete && error_count < profile_deleter.getRegistryGuidDeletionAttempts()) {
            try {
                if (user.getGuid().compareTo("") != 0) {
                    profile_deleter.registryDelete(profile_deleter.getRemoteComputer(), "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProfileGuid\\" + user.getGuid());
                    deleted_user_guid_success = "Yes";
                    profile_deleter.logMessage("Successfully deleted GUID " + user.getGuid() + " for user " + user.getName(), ProfileDeleter.LOG_TYPE.INFO, true);
                } else {
                    deleted_user_guid_success = "GUID is blank";
                    profile_deleter.logMessage("GUID for user " + user.getName() + " is blank", ProfileDeleter.LOG_TYPE.WARNING, true);
                }
                guid_delete = true;
            } catch (IOException | CannotEditException | InterruptedException e) {
                if (error_count >= profile_deleter.getRegistryGuidDeletionAttempts() - 1) {
                    String message = "Failed to delete user GUID " + user.getGuid() + " from registry. Error is " + e.getMessage();
                    deleted_user_guid_success = message;
                    profile_deleter.logMessage(message, ProfileDeleter.LOG_TYPE.ERROR, true);
                } else {
                    profile_deleter.logMessage("Failed to delete user GUID " + user.getGuid() + " on attempt " + (error_count + 1) + ". Will try again", ProfileDeleter.LOG_TYPE.WARNING, true);
                }
                error_count++;
            }
            if (folder_delete && sid_delete && guid_delete) {
                deleted_user_success = "Yes";
            } else {
                deleted_user_success = "No";
            }
        }
        deleted_folders.set(deleted_folders.indexOf(user.getName()), user.getName() + '\t' + deleted_user_success + '\t' + deleted_user_folder_success + '\t' + deleted_user_sid_success + '\t' + deleted_user_guid_success + '\t' + user.getSid() + '\t' + user.getGuid() + '\t' + user.getSize());
        number_of_users_deleted.incrementAndGet();
        return null;
    }
}
