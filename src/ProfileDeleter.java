import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.LineBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

public class ProfileDeleter extends JFrame implements TableModelListener, ActionListener
{
    private String computer;
    private String users_directory;
    private String remote_data_directory;
    private String local_data_directory;
    private List<UserData> user_list;
    private List<String> log_list;
    private String session_id;
    private boolean size_check;
    private boolean state_check;
    private boolean registry_check;
    private boolean size_check_complete;
    private boolean state_check_complete;
    private boolean registry_check_complete;
    public BufferedReader console_in;
    
    private JScrollPane results_scroll_pane;
    private JTable results_table;
    private GridBagConstraints results_gc;
    private JScrollPane system_console_scroll_pane;
    private JTextArea system_console_text_area;
    private int system_console_scrollbar_previous_maximum;
    private GridBagConstraints system_console_gc;
    private JTextField computer_name_text_field;
    private GridBagConstraints computer_name_gc;
    private JButton set_computer_button;
    private GridBagConstraints set_computer_gc;
    private JButton rerun_checks_button;
    private GridBagConstraints rerun_checks_gc;
    private JButton run_deletion_button;
    private GridBagConstraints run_deletion_gc;
    private JButton write_log_button;
    private GridBagConstraints write_log_gc;
    private JButton exit_button;
    private GridBagConstraints exit_gc;
    private JCheckBox size_check_checkbox;
    private GridBagConstraints size_check_gc;
    private JCheckBox state_check_checkbox;
    private GridBagConstraints state_check_gc;
    private JCheckBox registry_check_checkbox;
    private GridBagConstraints registry_check_gc;

    private SetComputerThread set_computer_thread;
    private RerunChecksThread rerun_checks_thread;
    private RunDeletionThread run_deletion_thread;
    private WriteLogThread write_log_thread;

    public enum LOG_TYPE {
        INFO(0),
        WARNING(1),
        ERROR(2);

        private int state;

        LOG_TYPE(int new_state) {
                state = new_state;
        }

        public int GetState() {
                return state;
        }
    }

    public static void main(String args[]) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new ProfileDeleter();
            }
        });
    }

    public ProfileDeleter() {
        super("Profile Deleter");
        
        computer = "";
        users_directory = "";
        remote_data_directory = "";
        local_data_directory = "";
        user_list = new ArrayList<>();
        log_list = new ArrayList<>();
        session_id = "";
        size_check = false;
        state_check = true;
        registry_check = true;
        size_check_complete = false;
        state_check_complete = false;
        registry_check_complete = false;
        console_in = new BufferedReader(new InputStreamReader(System.in));
        
        setMinimumSize(new Dimension(950, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(new GridBagLayout());
        
        String[] columnToolTips = {
            "Cannot delete if state is not Editable and cannot delete the Public account",
            null,
            null,
            null,
            null,
            null,
            null
        };
        results_table = new JTable(new DefaultTableModel()) {
           public String getToolTipText(MouseEvent e) {
               String tip = null;
               java.awt.Point p = e.getPoint();
               int rowIndex = rowAtPoint(p);
               int colIndex = columnAtPoint(p);
               int realColumnIndex = convertColumnIndexToModel(colIndex);
               int realRowIndex = convertRowIndexToModel(rowIndex);
               
               if(realColumnIndex == 0) {
                   TableModel model = getModel();
                   String editable = (String)model.getValueAt(realRowIndex, 4);
                   String name = (String)model.getValueAt(realRowIndex, 1);
                   if(editable.compareTo("Editable") != 0 || name.compareTo("Public") == 0) {
                       tip = "Cannot delete if state is not Editable and cannot delete the Public account";
                   }
               }
               return tip;
           }
           protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        String tip = null;
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        return columnToolTips[realIndex];
                    }
                };
            }
        };
        UpdateTableData();
        results_scroll_pane = new JScrollPane(results_table);
        results_scroll_pane.setBorder(new LineBorder(Color.BLACK, 2));
        results_gc = new GridBagConstraints();
        results_gc.fill = GridBagConstraints.BOTH;
        results_gc.gridx = 0;
        results_gc.gridy = 1;
        results_gc.gridwidth = GridBagConstraints.REMAINDER;
        results_gc.weighty = 1;
        
        system_console_text_area = new JTextArea("System Console");
        system_console_text_area.setEditable(false);
        system_console_text_area.setBackground(Color.BLACK);
        system_console_text_area.setForeground(Color.WHITE);
        system_console_text_area.setSelectedTextColor(Color.YELLOW);
        system_console_text_area.setMargin(new Insets(0,2,0,0));
        system_console_scroll_pane = new JScrollPane(system_console_text_area);
        system_console_scroll_pane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                if(system_console_scrollbar_previous_maximum - (system_console_scroll_pane.getViewport().getViewPosition().y + system_console_scroll_pane.getViewport().getViewRect().height) <= 1) {
                    e.getAdjustable().setValue(e.getAdjustable().getMaximum());
                }
                system_console_scrollbar_previous_maximum = e.getAdjustable().getMaximum();
            }
        });
        system_console_scroll_pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        system_console_scroll_pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        system_console_scroll_pane.getVerticalScrollBar().setUnitIncrement(16);
        system_console_scroll_pane.getVerticalScrollBar().setBlockIncrement(16);
        system_console_scrollbar_previous_maximum = system_console_scroll_pane.getVerticalScrollBar().getMaximum();
        system_console_gc = new GridBagConstraints();
        system_console_gc.fill = GridBagConstraints.BOTH;
        system_console_gc.gridx = 0;
        system_console_gc.gridy = 2;
        system_console_gc.gridwidth = GridBagConstraints.REMAINDER;
        system_console_scroll_pane.setPreferredSize(new Dimension(100,100));
        
        computer_name_text_field = new JTextField();
        computer_name_gc = new GridBagConstraints();
        computer_name_gc.fill = GridBagConstraints.BOTH;
        computer_name_gc.gridx = 0;
        computer_name_gc.gridy = 0;
        computer_name_gc.gridwidth = 1;
        computer_name_gc.gridheight = 1;
        computer_name_gc.weightx = 1;
        computer_name_gc.insets = new Insets(2,2,2,2);
        
        set_computer_button = new JButton("Set Computer");
        set_computer_button.setActionCommand("SetComputer");
        set_computer_button.addActionListener(this);
        set_computer_gc = new GridBagConstraints();
        set_computer_gc.fill = GridBagConstraints.BOTH;
        set_computer_gc.gridx = 1;
        set_computer_gc.gridy = 0;
        set_computer_gc.gridwidth = 1;
        set_computer_gc.gridheight = 1;
        
        size_check_checkbox = new JCheckBox();
        size_check_checkbox.setSelected(false);
        size_check_checkbox.setText("Size Check");
        size_check_checkbox.setActionCommand("SizeCheckToggle");
        size_check_checkbox.addActionListener(this);
        size_check_gc = new GridBagConstraints();
        size_check_gc.fill = GridBagConstraints.BOTH;
        size_check_gc.gridx = 2;
        size_check_gc.gridy = 0;
        size_check_gc.gridwidth = 1;
        size_check_gc.gridheight = 1;
        
        state_check_checkbox = new JCheckBox();
        state_check_checkbox.setSelected(true);
        state_check_checkbox.setText("State Check");
        state_check_checkbox.setActionCommand("StateCheckToggle");
        state_check_checkbox.addActionListener(this);
        state_check_gc = new GridBagConstraints();
        state_check_gc.fill = GridBagConstraints.BOTH;
        state_check_gc.gridx = 3;
        state_check_gc.gridy = 0;
        state_check_gc.gridwidth = 1;
        state_check_gc.gridheight = 1;
        
        registry_check_checkbox = new JCheckBox();
        registry_check_checkbox.setSelected(true);
        registry_check_checkbox.setText("Registry Check");
        registry_check_checkbox.setActionCommand("RegistryCheckToggle");
        registry_check_checkbox.addActionListener(this);
        registry_check_gc = new GridBagConstraints();
        registry_check_gc.fill = GridBagConstraints.BOTH;
        registry_check_gc.gridx = 4;
        registry_check_gc.gridy = 0;
        registry_check_gc.gridwidth = 1;
        registry_check_gc.gridheight = 1;
        
        rerun_checks_button = new JButton("Rerun Checks");
        rerun_checks_button.setActionCommand("RerunChecks");
        rerun_checks_button.addActionListener(this);
        rerun_checks_button.setEnabled(false);
        rerun_checks_gc = new GridBagConstraints();
        rerun_checks_gc.fill = GridBagConstraints.BOTH;
        rerun_checks_gc.gridx = 5;
        rerun_checks_gc.gridy = 0;
        rerun_checks_gc.gridwidth = 1;
        rerun_checks_gc.gridheight = 1;
        
        run_deletion_button = new JButton("Run Deletion");
        run_deletion_button.setActionCommand("RunDeletion");
        run_deletion_button.addActionListener(this);
        run_deletion_button.setEnabled(false);
        run_deletion_gc = new GridBagConstraints();
        run_deletion_gc.fill = GridBagConstraints.BOTH;
        run_deletion_gc.gridx = 6;
        run_deletion_gc.gridy = 0;
        run_deletion_gc.gridwidth = 1;
        run_deletion_gc.gridheight = 1;
        
        write_log_button = new JButton("Write Log");
        write_log_button.setActionCommand("WriteLog");
        write_log_button.addActionListener(this);
        write_log_gc = new GridBagConstraints();
        write_log_gc.fill = GridBagConstraints.BOTH;
        write_log_gc.gridx = 7;
        write_log_gc.gridy = 0;
        write_log_gc.gridwidth = 1;
        write_log_gc.gridheight = 1;
        
        exit_button = new JButton("Exit");
        exit_button.setActionCommand("Exit");
        exit_button.addActionListener(this);
        exit_gc = new GridBagConstraints();
        exit_gc.fill = GridBagConstraints.BOTH;
        exit_gc.gridx = 8;
        exit_gc.gridy = 0;
        exit_gc.gridwidth = 1;
        exit_gc.gridheight = 1;
        
        getContentPane().add(computer_name_text_field, computer_name_gc);
        getContentPane().add(set_computer_button, set_computer_gc);
        getContentPane().add(size_check_checkbox, size_check_gc);
        getContentPane().add(state_check_checkbox, state_check_gc);
        getContentPane().add(registry_check_checkbox, registry_check_gc);
        getContentPane().add(rerun_checks_button, rerun_checks_gc);
        getContentPane().add(run_deletion_button, run_deletion_gc);
        getContentPane().add(write_log_button, write_log_gc);
        getContentPane().add(exit_button, exit_gc);
        getContentPane().add(results_scroll_pane, results_gc);
        getContentPane().add(system_console_scroll_pane, system_console_gc);
        pack();
        setVisible(true);
    }

    /**
     * Sets the computer and users directory attributes.
     * 
     * @param computer the hostname or IP address of the target computer
     */
    public void SetComputer(String computer) {
        this.computer = computer;
        this.users_directory = "\\\\" + computer + "\\c$\\users\\";
        LogMessage("Remote computer set to " + computer, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the value of the users directory attribute.
     * 
     * @return the filepath to the target computers users directory
     */
    public void SetUsersDirectory(String users_directory) {
        this.users_directory = users_directory;
        LogMessage("Users directory set to " + users_directory, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the value of the remote data directory attribute
     * 
     * @return the filepath to the directory on the target computer for storing ProfileDeleter data
     */
    public void SetRemoteDataDirectory(String remote_data_directory) {
        this.remote_data_directory = remote_data_directory;
        LogMessage("Remote data directory set to " + remote_data_directory, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the value of the local data directory attribute
     * 
     * @return the filepath to the directory on the local computer for storing ProfileDeleter data
     */
    public void SetLocalDataDirectory(String local_data_directory) {
        this.local_data_directory = local_data_directory;
        LogMessage("Local data directory set to " + local_data_directory, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the size check attribute.
     * Determines whether a size check is done when checks are run.
     * 
     * @param size_check whether to run a size check or not
     */
    public void SetSizeCheck(boolean size_check) {
        this.size_check = size_check;
        LogMessage("Size check set to " + size_check, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the state check attribute.
     * Determines whether a state check is done when checks are run.
     * 
     * @param state_check whether to run a state check or not
     */
    public void SetStateCheck(boolean state_check) {
        this.state_check = state_check;
        LogMessage("State check set to " + state_check, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the registry check attribute.
     * Determines whether a registry check is done when checks are run.
     * 
     * @param registry_check whether to run a registry check or not
     */
    public void SetRegistryCheck(boolean registry_check) {
        this.registry_check = registry_check;
        LogMessage("Registry check set to " + registry_check, LOG_TYPE.INFO, true);
    }

    /**
     * Sets the user list attribute.
     * 
     * @param user_list list of UserData that contain details for the users on the target computer
     */
    public void SetUserList(List<UserData> user_list) {
        this.user_list = user_list;
    }

    /**
     * Gets the value of the computer attribute.
     * 
     * @return the target computer hostname or IP address
     */
    public String GetComputer() {
        return computer;
    }

    /**
     * Gets the value of the users directory attribute.
     * 
     * @return the filepath to the target computers users directory
     */
    public String GetUsersDirectory() {
        return users_directory;
    }

    /**
     * Gets the value of the remote data directory attribute
     * 
     * @return the filepath to the directory on the target computer for storing ProfileDeleter data
     */
    public String GetRemoteDataDirectory() {
        return remote_data_directory;
    }

    /**
     * Gets the value of the local data directory attribute
     * 
     * @return the filepath to the directory on the local computer for storing ProfileDeleter data
     */
    public String GetLocalDataDirectory() {
        return local_data_directory;
    }

    /**
     * Gets the size check attribute.
     * 
     * @return whether to run a size check or not
     */
    public boolean GetSizeCheck() {
        return size_check;
    }

    /**
     * Gets the state check attribute.
     * 
     * @return whether to run a state check or not
     */
    public boolean GetStateCheck() {
        return state_check;
    }

    /**
     * Gets the registry check attribute.
     * 
     * @return whether to run a registry check or not
     */
    public boolean GetRegistryCheck() {
        return registry_check;
    }

    /**
     * Sets the user list attribute.
     * 
     * @return list of UserData that contain details for the users on the target computer
     */
    public List<UserData> GetUserList() {
        return user_list;
    }

    public Object[][] ConvertFoldersTo2DObjectArray() {
        Object[][] object_array = new Object[user_list.size()][];
        for(int i=0;i<user_list.size();i++) {
            object_array[i] = user_list.get(i).ToObjectArray();
        }
        return object_array;
    }
    
    public List<String> ProcessDeletion() throws NotInitialisedException {
        LogMessage("Attempting to run deletion on users list", LOG_TYPE.INFO, true);
        if(user_list != null && !user_list.isEmpty() && state_check_complete && registry_check_complete) {
            ArrayList<UserData> new_folders = new ArrayList<UserData>();
            ArrayList<String> deleted_folders = new ArrayList<String>();
            deleted_folders.add("User" + '\t' + "Folder Deleted?" + '\t' + "Registry SID Deleted?" + '\t' + "Registry GUID Deleted?");
            for(UserData user : user_list) {
                if(user.delete) {
                    LogMessage("User " + user.name + " is flagged for deletion", LOG_TYPE.INFO, true);
                    String deleted_user = user.name + '\t';
                    try{
                        DeleteDirectory(users_directory + user.name);
                        deleted_user += "Yes" + '\t';
                        LogMessage("Successfully deleted user directory for " + user.name, LOG_TYPE.INFO, true);
                    } catch(IOException | CannotEditException e) {
                        String message = "Failed to delete user directory " + user.name + ". Error is " + e.getMessage();
                        deleted_user += message + '\t';
                        LogMessage(message, LOG_TYPE.ERROR, true);
                    }
                    try{
                        if(user.sid.compareTo("") != 0) {
                            DeleteRegistry(computer, "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProfileList\\" + user.sid);
                            deleted_user += "Yes " + user.sid + '\t';
                            LogMessage("Successfully deleted SID " + user.sid + " for user " + user.name, LOG_TYPE.INFO, true);
                        } else {
                            deleted_user += "SID is blank" + '\t';
                            LogMessage("SID for user " + user.name + " is blank", LOG_TYPE.WARNING, true);
                        }
                    } catch(IOException | InterruptedException e) {
                        String message = "Failed to delete user SID " + user.sid + " from registry. Error is " + e.getMessage();
                        deleted_user += message + '\t';
                        LogMessage(message, LOG_TYPE.ERROR, true);
                    }
                    try{
                        if(user.guid.compareTo("") != 0) {
                            DeleteRegistry(computer, "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProfileGuid\\" + user.guid);
                            deleted_user += "Yes " + user.guid;
                            LogMessage("Successfully deleted GUID " + user.guid + " for user " + user.name, LOG_TYPE.INFO, true);
                        } else {
                            deleted_user += "GUID is blank";
                            LogMessage("GUID for user " + user.name + " is blank", LOG_TYPE.WARNING, true);
                        }
                    } catch(IOException | InterruptedException e) {
                        String message = "Failed to delete user GUID " + user.guid + " from registry. Error is " + e.getMessage();
                        deleted_user += message;
                        LogMessage(message, LOG_TYPE.ERROR, true);
                    }
                    deleted_folders.add(deleted_user);
                } else {
                    new_folders.add(user);
                }
            }
            user_list = new_folders;
            LogMessage("Successfully completed deletions", LOG_TYPE.INFO, true);
            return deleted_folders;
        } else {
            String message = "Either user list has not been initialised or an SID + GUID registry check has not been run";
            LogMessage(message, LOG_TYPE.WARNING, true);
            throw new NotInitialisedException(message);
        }
    }
    
    public void UpdateTableData() {
        results_table.getModel().removeTableModelListener(this);
        results_table.setModel(new AbstractTableModel () {
            private String[] columnNames = UserData.HeadingsToStringArray();
            private Object[][] rowData = ConvertFoldersTo2DObjectArray();
            
            public String getColumnName(int col) {
                return columnNames[col].toString();
            }
            public int getRowCount() { return rowData.length; }
            public int getColumnCount() { return columnNames.length; }
            public Object getValueAt(int row, int col) {
                return rowData[row][col];
            }
            public Class getColumnClass(int col) {
                if(col == 0) {
                    try {
                        return Class.forName("java.lang.Boolean");
                    } catch (ClassNotFoundException ex) {
                        System.out.println("Couldn't find class");
                        try {
                            //return getValueAt(0, col).getClass();
                            return Class.forName("java.lang.String");
                        } catch (ClassNotFoundException ex1) {
                            System.out.println("Couldn't find class");
                        }
                    }
                } else {
                    try {
                        //return getValueAt(0, col).getClass();
                        return Class.forName("java.lang.String");
                    } catch (ClassNotFoundException ex) {
                        System.out.println("Couldn't find class");
                    }
                }
                return null;
            }
            public boolean isCellEditable(int row, int col)
            {
                
                if(col == 0 && getValueAt(row, 4) == "Editable" && getValueAt(row, 1) != "Public") {
                    return true;
                } else {
                    return false;
                }
            }
            public void setValueAt(Object value, int row, int col) {
                rowData[row][col] = value;
                fireTableCellUpdated(row, col);
            }
            public void setRowData(Object[][] row_data) {
                rowData = row_data;
            }
        });
        results_table.setAutoCreateRowSorter(true);
        results_table.getModel().addTableModelListener(this);
        
        /*
        ((DeleterTableModel)results_table.getModel()).setRowData(ConvertFoldersTo2DObjectArray());
        results_table.setAutoCreateRowSorter(true);
        ((DeleterTableModel)results_table.getModel()).fireTableDataChanged();*/
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if ("SetComputer" == e.getActionCommand()) {
            SetComputerButton();
        } else if ("RerunChecks" == e.getActionCommand()) {
            RerunChecksButton();
        } else if ("RunDeletion" == e.getActionCommand()) {
            RunDeletionButton();
        } else if ("WriteLog" == e.getActionCommand()) {
            WriteLogButton();
        } else if ("SizeCheckToggle" == e.getActionCommand()) {
            SizeCheckCheckbox();
        } else if ("StateCheckToggle" == e.getActionCommand()) {
            StateCheckCheckbox();
        } else if ("RegistryCheckToggle" == e.getActionCommand()) {
            RegistryCheckCheckbox();
        } else if ("Exit" == e.getActionCommand()) {
            ExitButton();
        }
    }
    
    public void SetComputerButton() {
        computer_name_text_field.setEnabled(false);
        set_computer_button.setEnabled(false);
        size_check_checkbox.setEnabled(false);
        state_check_checkbox.setEnabled(false);
        registry_check_checkbox.setEnabled(false);
        rerun_checks_button.setEnabled(false);
        run_deletion_button.setEnabled(false);
        write_log_button.setEnabled(false);
        results_table.setEnabled(false);
        (set_computer_thread = new SetComputerThread()).execute();
    }
    
    public void RerunChecksButton() {
        computer_name_text_field.setEnabled(false);
        set_computer_button.setEnabled(false);
        size_check_checkbox.setEnabled(false);
        state_check_checkbox.setEnabled(false);
        registry_check_checkbox.setEnabled(false);
        rerun_checks_button.setEnabled(false);
        run_deletion_button.setEnabled(false);
        write_log_button.setEnabled(false);
        results_table.setEnabled(false);
        (rerun_checks_thread = new RerunChecksThread()).execute();
    }
    
    public void RunDeletionButton() {
        computer_name_text_field.setEnabled(false);
        set_computer_button.setEnabled(false);
        size_check_checkbox.setEnabled(false);
        state_check_checkbox.setEnabled(false);
        registry_check_checkbox.setEnabled(false);
        rerun_checks_button.setEnabled(false);
        run_deletion_button.setEnabled(false);
        write_log_button.setEnabled(false);
        results_table.setEnabled(false);
        (run_deletion_thread = new RunDeletionThread()).execute();
    }
    
    public void WriteLogButton() {
        computer_name_text_field.setEnabled(false);
        set_computer_button.setEnabled(false);
        size_check_checkbox.setEnabled(false);
        state_check_checkbox.setEnabled(false);
        registry_check_checkbox.setEnabled(false);
        rerun_checks_button.setEnabled(false);
        run_deletion_button.setEnabled(false);
        write_log_button.setEnabled(false);
        results_table.setEnabled(false);
        (write_log_thread = new WriteLogThread()).execute();
    }
    
    public void SizeCheckCheckbox() {
        size_check = size_check_checkbox.isSelected();
    }
    
    public void StateCheckCheckbox() {
        state_check = state_check_checkbox.isSelected();
    }
    
    public void RegistryCheckCheckbox() {
        registry_check = registry_check_checkbox.isSelected();
    }
    
    public void ExitButton() {
        System.exit(0);
    }
    
    public void BackupAndCopyRegistry() throws IOException, InterruptedException, CannotEditException, NotInitialisedException {
        LogMessage("Attempting to backup profilelist and profileguid registry keys on remote computer", LOG_TYPE.INFO, true);
        if(local_data_directory.compareTo("") == 0 || remote_data_directory.compareTo("") == 0) {
            String message = "Local or remote data directory has not been initialised";
            LogMessage(message, LOG_TYPE.WARNING, true);
            throw new NotInitialisedException(message);
        } else {
            String filename_friendly_computer = computer.replace('.', '_');
            int count = 0;
            boolean run = true;
            while(run) {
                try {
                    BackupRegistry(computer, "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProfileList", "C:\\temp\\Profile_Deleter\\" + session_id + "\\" + filename_friendly_computer + "_ProfileList.reg");
                    run = false;
                } catch(IOException | CannotEditException e) {
                    if(count > 29) {
                        /*
                        LogMessage("Back up of registry key has failed too many times. You may not have permission to backup the registry, you may not have permissions to create user_list and files in \\\\" + computer + "\\C$\\temp, or the drive may not have a couple of MB free to create the registry backups needed. Check permissions or delete some files from the remote PC. Awaiting prompt from user", LOG_TYPE.WARNING, true);
                        System.out.println("Failed to backup registry file \"HKEY_LOCAL_MACHINE\\\\SOFTWARE\\\\Microsoft\\\\Windows NT\\\\CurrentVersion\\\\ProfileList\". You may not have permission to backup the registry, you may not have permissions to create user_list and files in \\\\" + computer + "\\C$\\temp, or the drive may not have a couple of MB free to create the registry backups needed.");
                        System.out.println("1. Retry (it is recommended you manually delete a few MB of files before retrying)");
                        System.out.println("2. Delete all files in \\\\" + computer + "\\C$\\temp except BGInfo.bmp and retry");
                        System.out.println("3. Stop running registry backup");
                        System.out.println("Please enter an option (1-3):");
                        String option = console_in.readLine();
                        if(option.compareTo("1") == 0) {
                            LogMessage("User has selected to retry registry backup", LOG_TYPE.INFO, true);
                            count = 0;
                        } else if(option.compareTo("2") == 0) {
                            LogMessage("User has selected to delete all files in \\\\" + computer + "\\C$\\temp except BGInfo.bmp and retry", LOG_TYPE.INFO, true);
                            count = 0;
                            List<String> exclude_files = new ArrayList<String>();
                            exclude_files.add("BGInfo.bmp");
                            exclude_files.add("bginfo.bmp");
                            List<String> files_to_delete = GetFileList("\\\\" + computer + "\\C$\\temp");
                            DeleteFilesInDirectory("\\\\" + computer + "\\C$\\temp", files_to_delete, exclude_files);
                        } else if(option.compareTo("3") == 0) {
                            LogMessage("User has selected to stop running registry backup", LOG_TYPE.INFO, true);
                            throw e;
                        } else {
                            System.out.println("Invalid option");
                            System.out.println("");
                        }*/
                        throw e;
                    } else {
                        LogMessage("Attempt " + Integer.toString(count+1) + " at backing up registry key failed", LOG_TYPE.WARNING, true);
                        count++;
                    }
                }
            }
            run = true;
            count = 0;
            while(run) {
                try {
                    BackupRegistry(computer, "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProfileGuid", "C:\\temp\\Profile_Deleter\\" + session_id + "\\" + filename_friendly_computer + "_ProfileGuid.reg");
                    run = false;
                } catch(IOException | CannotEditException e) {
                    if(count > 29) {/*
                        LogMessage("Back up of registry key has failed too many times. You may not have permission to backup the registry, you may not have permissions to create user_list and files in \\\\" + computer + "\\C$\\temp, or the drive may not have a couple of MB free to create the registry backups needed. Check permissions or delete some files from the remote PC. Awaiting prompt from user", LOG_TYPE.WARNING, true);
                        System.out.println("Failed to backup registry file \"HKEY_LOCAL_MACHINE\\\\SOFTWARE\\\\Microsoft\\\\Windows NT\\\\CurrentVersion\\\\ProfileGuid\". You may not have permission to backup the registry, you may not have permissions to create user_list and files in \\\\" + computer + "\\C$\\temp, or the drive may not have a couple of MB free to create the registry backups needed.");
                        System.out.println("1. Retry (it is recommended you manually delete a few MB of files before retrying)");
                        System.out.println("2. Delete all files in \\\\" + computer + "\\C$\\temp except BGInfo.bmp and retry");
                        System.out.println("3. Stop running registry backup");
                        System.out.println("Please enter an option (1-3):");
                        String option = console_in.readLine();
                        if(option.compareTo("1") == 0) {
                            LogMessage("User has selected to retry registry backup", LOG_TYPE.INFO, true);
                            count = 0;
                        } else if(option.compareTo("2") == 0) {
                            LogMessage("User has selected to delete all files in \\\\" + computer + "\\C$\\temp except BGInfo.bmp and retry", LOG_TYPE.INFO, true);
                            count = 0;
                            List<String> exclude_files = new ArrayList<String>();
                            exclude_files.add("BGInfo.bmp");
                            exclude_files.add("bginfo.bmp");
                            List<String> files_to_delete = GetFileList("\\\\" + computer + "\\C$\\temp");
                            DeleteFilesInDirectory("\\\\" + computer + "\\C$\\temp", files_to_delete, exclude_files);
                        } else if(option.compareTo("3") == 0) {
                            LogMessage("User has selected to stop running registry backup", LOG_TYPE.INFO, true);
                            throw e;
                        } else {
                            System.out.println("Invalid option");
                            System.out.println("");
                        }*/
                        throw e;
                    } else {
                        LogMessage("Attempt " + Integer.toString(count+1) + " at backing up registry key failed", LOG_TYPE.WARNING, true);
                        count++;
                    }
                }
            }
            try {
                CopyFile(remote_data_directory + "\\" + filename_friendly_computer + "_ProfileList.reg", local_data_directory);
            } catch(IOException |  CannotEditException e) {
                throw e;
            }
            try {
                CopyFile(remote_data_directory + "\\" + filename_friendly_computer + "_ProfileGuid.reg", local_data_directory);
            } catch(IOException | CannotEditException e) {
                throw e;
            }
        }
    }
    
    public void FindSIDAndGUID() throws IOException, NotInitialisedException {
        LogMessage("Attempting to compile SID and GUID data from registry backups", LOG_TYPE.INFO, true);
        if(local_data_directory.compareTo("") != 0) {
            List<String> regkeys_profile_list;
            List<String> regkeys_profile_guid;
            String filename_friendly_computer = computer.replace('.', '_');
            try {
                LogMessage("Loading file " + local_data_directory + "\\" + filename_friendly_computer + "_ProfileList.reg", LOG_TYPE.INFO, true);
                regkeys_profile_list = ReadFromFile(local_data_directory + "\\" + filename_friendly_computer + "_ProfileList.reg");
                LogMessage("Loading file " + local_data_directory + "\\" + filename_friendly_computer + "_ProfileGuid.reg", LOG_TYPE.INFO, true);
                regkeys_profile_guid = ReadFromFile(local_data_directory + "\\" + filename_friendly_computer + "_ProfileGuid.reg");
                if(regkeys_profile_list != null && !regkeys_profile_list.isEmpty() && regkeys_profile_guid != null && !regkeys_profile_guid.isEmpty()) {
                    LogMessage("Cleaning data from file " + local_data_directory + "\\" + filename_friendly_computer + "_ProfileList.reg", LOG_TYPE.INFO, true);
                    List<String> cleaned_regkeys_profile_list = new ArrayList<String>();
                    List<String> cleaned_regkeys_profile_guid = new ArrayList<String>();
                    for(int i=0;i<regkeys_profile_list.size();i++) {
                        if((i % 2) == 0) {
                            String cleaned_string = "";
                            for(int j=0;j<regkeys_profile_list.get(i).length();j++) {
                                if((j % 2) != 0) {
                                    cleaned_string += regkeys_profile_list.get(i).charAt(j);
                                }
                            }
                            cleaned_regkeys_profile_list.add(cleaned_string);
                        }
                    }
                    regkeys_profile_list = cleaned_regkeys_profile_list;
                    LogMessage("Cleaning data from file " + local_data_directory + "\\" + filename_friendly_computer + "_ProfileGuid.reg", LOG_TYPE.INFO, true);
                    for(int i=0;i<regkeys_profile_guid.size();i++) {
                        if((i % 2) == 0) {
                            String cleaned_string = "";
                            for(int j=0;j<regkeys_profile_guid.get(i).length();j++) {
                                if((j % 2) != 0) {
                                    cleaned_string += regkeys_profile_guid.get(i).charAt(j);
                                }
                            }
                            cleaned_regkeys_profile_guid.add(cleaned_string);
                        }
                    }
                    regkeys_profile_guid = cleaned_regkeys_profile_guid;
                    String current_sid = "";
                    String profile_path = "";
                    String profile_guid = "";
                    boolean found_profile_path = false;
                    int count = 0;
                    LogMessage("Processing file " + local_data_directory + "\\" + filename_friendly_computer + "_ProfileList.reg", LOG_TYPE.INFO, true);
                    for(String line : regkeys_profile_list) {
                        if(line.startsWith("[HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProfileList\\") || count == regkeys_profile_list.size()-1) {
                            String new_sid = line.replace("[HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProfileList\\", "");
                            new_sid = new_sid.replaceAll("]", "");
                            new_sid = new_sid.trim();
                            LogMessage("Found new SID " + new_sid, LOG_TYPE.INFO, true);
                            if(!profile_path.isEmpty()) {
                                LogMessage("Processing details for found profile " + profile_path, LOG_TYPE.INFO, true);
                                boolean found_user = false;
                                for(UserData user : user_list) {
                                    if(user.name.compareTo(profile_path) == 0) {
                                        found_user = true;
                                        LogMessage("Found matching user account", LOG_TYPE.INFO, true);
                                        if(!user.sid.isEmpty()) {
                                            LogMessage("SID already exists for user, resolving conflict", LOG_TYPE.INFO, true);
                                            boolean found_guid = false;
                                            for(String guid : regkeys_profile_guid) {
                                                if(guid.contains("[HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProfileGuid\\")) {
                                                    String trimmed_guid = guid.replace("[HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProfileGuid\\", "");
                                                    trimmed_guid = trimmed_guid.replaceAll("]", "");
                                                    if(trimmed_guid.compareTo(profile_guid) == 0) {
                                                        LogMessage("Found matching GUID from " + local_data_directory + "\\" + filename_friendly_computer + "_ProfileGuid.reg. Checking SID for match", LOG_TYPE.INFO, true);
                                                        found_guid = true;
                                                    }
                                                } else if(found_guid) {
                                                    String sid = guid.replace("\"SidString\"=\"", "");
                                                    sid = sid.replaceAll("\"", "");
                                                    if(sid.compareTo(current_sid) == 0) {
                                                        LogMessage("New SID details match SID details for GUID, replacing user details with new details. SID set to " + current_sid + " and GUID to " + profile_guid, LOG_TYPE.INFO, true);
                                                        user_list.get(user_list.indexOf(user)).sid = current_sid;
                                                        user_list.get(user_list.indexOf(user)).guid = profile_guid;
                                                        break;
                                                    }
                                                }
                                            }
                                            LogMessage("No match found, discarding new details found", LOG_TYPE.INFO, true);
                                        } else {
                                            LogMessage("Set SID for user " + profile_path + " to " + current_sid + " and GUID to " + profile_guid, LOG_TYPE.INFO, true);
                                            user_list.get(user_list.indexOf(user)).sid = current_sid;
                                            user_list.get(user_list.indexOf(user)).guid = profile_guid;
                                        }
                                        break;
                                    }
                                }
                                if(!found_user) {
                                    LogMessage("No matching user found for profile " + profile_path, LOG_TYPE.INFO, true);
                                }
                                current_sid = new_sid;
                                profile_path = "";
                                profile_guid = "";
                            } else {
                                current_sid = new_sid;
                                LogMessage("SID is " + current_sid, LOG_TYPE.INFO, true);
                            }
                        } else if(line.startsWith("\"ProfileImagePath\"")) {
                            LogMessage("User directory exists in SID, processing details", LOG_TYPE.INFO, true);
                            profile_path = line;
                            found_profile_path = true;
                        } else if(found_profile_path) {
                            if(line.startsWith("  ")) {
                                profile_path += line;
                            } else {
                                profile_path = profile_path.replace("\"ProfileImagePath\"=hex(2):", "");
                                profile_path = profile_path.replaceAll("00,", "");
                                profile_path = profile_path.replaceAll("\\\\", "");
                                profile_path = profile_path.replaceAll(" ", "");
                                profile_path = profile_path.replaceAll("\\n", "");
                                profile_path = profile_path.replaceAll("\\r", "");
                                String[] profile_path_hex = profile_path.split(",");
                                String profile_path_hex_to_string = "";
                                for(String hex : profile_path_hex) {
                                    int decimal = Integer.parseInt(hex, 16);
                                    profile_path_hex_to_string += (char) decimal;
                                }
                                profile_path = profile_path_hex_to_string.replace("C:\\Users\\", "");
                                profile_path = profile_path.trim();
                                found_profile_path = false;
                                LogMessage("Found user directory " + profile_path, LOG_TYPE.INFO, true);
                            }
                        } else if(line.startsWith("\"Guid\"")) {
                            profile_guid = line.replace("\"Guid\"=\"", "");
                            profile_guid = profile_guid.replaceAll("\"", "");
                            profile_guid = profile_guid.trim();
                            LogMessage("Found GUID " + profile_guid, LOG_TYPE.INFO, true);
                        }
                        count++;
                    }
                    registry_check_complete = true;
                    LogMessage("Successfully compiled SID and GUID data from registry backups", LOG_TYPE.INFO, true);
                } else {
                    String message = "File " + local_data_directory + "\\" + filename_friendly_computer + "_ProfileList.reg or " + local_data_directory + "\\" + filename_friendly_computer + "_ProfileGuid.reg is either empty or corrupt";
                    LogMessage(message, LOG_TYPE.ERROR, true);
                    throw new NotInitialisedException(message);
                }
            } catch(IOException e) {
                LogMessage("Unable to read file " + local_data_directory + "\\" + filename_friendly_computer + "_ProfileList.reg. File may not exist. Error is " + e.getMessage(), LOG_TYPE.ERROR, true);
                throw e;
            }
        }
    }

    public void BuildDirectory() throws IOException {
        LogMessage("Attempting to build users directory " + users_directory, LOG_TYPE.INFO, true);
        if(users_directory.compareTo("") != 0) {
            try{
                    user_list = new ArrayList<>();
                    String command = "Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process | powershell.exe -File \".\\src\\GetDirectoryList.ps1\" - directory " + users_directory;
                    ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe", "-Command", command);
                    builder.redirectErrorStream(true);
                    Process power_shell_process = builder.start();
                    BufferedReader powershell_process_output_stream = new BufferedReader(new InputStreamReader(power_shell_process.getInputStream()));
                    String output = "";
                    String line = "";
                    while((line = powershell_process_output_stream.readLine()).compareTo("EndOfScriptGetDirectoryList") != 0) {
                            if(!line.isEmpty()) {
                                    LogMessage("Discovered folder details " + line, LOG_TYPE.INFO, true);
                                    String[] line_split = line.split("\\t");
                                    UserData folder = new UserData();
                                    if(line_split[0].compareTo("Public") == 0) {
                                        folder.delete = false;
                                    } else {
                                        folder.delete = true;
                                    }
                                    folder.name = line_split[0];
                                    folder.last_updated = line_split[1];
                                    folder.size = "";
                                    folder.state = "";
                                    folder.sid = "";
                                    folder.guid = "";
                                    user_list.add(folder);
                            }
                    }
                    powershell_process_output_stream.close();
                    power_shell_process.destroy();
                    setTitle("Profile Deleter - " + computer);
                    LogMessage("Successfully built users directory " + users_directory, LOG_TYPE.INFO, true);
            } catch(IOException e) {
                LogMessage("Failed to build users directory " + users_directory, LOG_TYPE.ERROR, true);
                LogMessage(e.getMessage(), LOG_TYPE.ERROR, true);
                throw e;
            }
        } else {
            LogMessage("Computer name has not been specified. Building users directory has been aborted", LOG_TYPE.WARNING, true);
        }
    }

    public void CheckState() throws IOException {
        LogMessage("Checking editable state of directory list", LOG_TYPE.INFO, true);
        if(user_list.size() > 0 && users_directory.compareTo("") != 0) {
            for(int i=0;i<user_list.size();i++) {
                String folder = user_list.get(i).name;
                LogMessage("Checking editable state of folder " + folder, LOG_TYPE.INFO, true);
                try {
                    RenameDirectory(computer, "C:\\users\\", folder, folder);
                    user_list.get(i).state = "Editable";
                    user_list.get(i).delete = true;
                } catch(CannotEditException e) {
                    String message = "Uneditable. User may be logged in or PC may need to be restarted";
                    LogMessage(message, LOG_TYPE.WARNING, true);
                    user_list.get(i).state = message;
                    user_list.get(i).delete = false;
                } catch(IOException e) {
                    LogMessage("Editable state check has failed", LOG_TYPE.ERROR, true);
                    LogMessage(e.getMessage(), LOG_TYPE.ERROR, true);
                    throw e;
                }
            }
            state_check_complete = true;
            LogMessage("Finished checking editable state of directory list", LOG_TYPE.INFO, true);
        } else {
            LogMessage("Directory list is empty, aborting editable state check", LOG_TYPE.WARNING, true);
        }
    }

    public void CheckSize() {
        LogMessage("Calcuting size of directory list", LOG_TYPE.INFO, true);
        if(user_list.size() > 0 && users_directory.compareTo("") != 0) {
            Double total_size = 0.0;
            for(int i=0;i<user_list.size();i++) {
                String folder = user_list.get(i).name;
                String folder_size = "";
                try {
                    folder_size = FolderSize(folder);
                    total_size += Double.parseDouble(folder_size);
                    LogMessage("Calculated size " + folder_size + " for folder " + folder, LOG_TYPE.INFO, true);
                } catch(NonNumericException | IOException e) {
                    folder_size = "Could not calculate size";
                    LogMessage(folder_size + " for folder " + folder, LOG_TYPE.WARNING, true);
                    LogMessage(e.getMessage(), LOG_TYPE.ERROR, true);
                }
                user_list.get(i).size = folder_size;
            }
            //Double size_in_megabytes = total_size / (1024.0 * 1024.0);
            setTitle("Profile Deleter - " + computer + " - Total Users Size: " + Math.round(total_size / (1024.0 * 1024.0)) + "MB");
            size_check_complete = true;
            LogMessage("Finished calculating size of directory list", LOG_TYPE.INFO, true);
        } else {
            LogMessage("Directory list is empty, aborting size calculation", LOG_TYPE.WARNING, true);
        }
    }

    public void GenerateSessionID() {
        session_id = GenerateDateString();
        LogMessage("Session ID has been set to " + session_id, LOG_TYPE.INFO, true);
    }

    public void CreateSessionFolders() throws NotInitialisedException, IOException, CannotEditException {
        LogMessage("Attempting to create session user_list", LOG_TYPE.INFO, true);
        if(session_id.compareTo("") != 0 && computer.compareTo("") != 0) {
            try {
                MakeDirectory("\\\\" + computer + "\\c$\\temp\\Profile_Deleter");
            } catch(IOException e) {
                throw e;
            } catch(CannotEditException e) {}
            try {
                MakeDirectory("\\\\" + computer + "\\c$\\temp\\Profile_Deleter\\" + session_id);
                remote_data_directory = "\\\\" + computer + "\\c$\\temp\\Profile_Deleter\\" + session_id;
            } catch(IOException | CannotEditException e) {
                String message = "Unable to create remote data directory " + "\\\\" + computer + "\\c$\\temp\\Profile_Deleter\\" + session_id;
                LogMessage(message, LOG_TYPE.ERROR, true);
                throw new CannotEditException(message);
            }
            try {
                MakeDirectory(".\\sessions\\" + computer + "_" + session_id);
                local_data_directory = ".\\sessions\\" + computer + "_" + session_id;
            } catch(IOException | CannotEditException e) {
                String message = "Unable to create local data directory " + ".\\sessions\\" + computer + "_" + session_id;
                LogMessage(message, LOG_TYPE.ERROR, true);
                throw new CannotEditException(message);
            }
            LogMessage("Successfully created session user_list", LOG_TYPE.INFO, true);
        } else {
            String message = "";
            if(session_id.compareTo("") == 0) {
                message += "Session ID has not been created";
            }
            if(computer.compareTo("") == 0) {
                if(message.compareTo("") != 0) {
                    message += " and ";
                }
                message += "computer has not been initialised";
            }
            message += ". Please Initialise before running CreateSessionFolders";
            LogMessage(message, LOG_TYPE.ERROR, true);
            throw new NotInitialisedException(message);
        }
    }

    public void RenameDirectory(String computer, String directory, String folder, String folder_renamed) throws IOException, CannotEditException {
        try{
            LogMessage("Attempting to rename folder " + directory + folder + " to " + folder_renamed , LOG_TYPE.INFO, true);
            String command = ".\\pstools\\psexec \\\\" + computer + " cmd /c REN \"" + directory + folder + "\" \"" + folder_renamed + "\" && echo editable|| echo uneditable";
            ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", command);
            builder.redirectErrorStream(true);	
            Process cmd_process = builder.start();
            BufferedReader cmd_process_output_stream = new BufferedReader(new InputStreamReader(cmd_process.getInputStream()));
            String line = "";
            String error = "";
            while((line = cmd_process_output_stream.readLine()) != null) {
                error = line;
            }
            if(error.compareTo("editable") != 0) {
                String message = "Unable to rename folder " + directory + folder + ". Error is: " + error;
                throw new CannotEditException(message);
            }
            LogMessage("Successfully renamed folder " + directory + folder + " to " + folder_renamed , LOG_TYPE.INFO, true);
        } catch(CannotEditException | IOException e) {
            LogMessage("Could not rename directory " + directory + folder, LOG_TYPE.WARNING, true);
            LogMessage(e.getMessage(), LOG_TYPE.WARNING, true);
            throw e;
        }
    }

    public String FolderSize(String folder) throws NonNumericException, IOException {
        try{
            LogMessage("Calculating filesize for folder " + users_directory + folder, LOG_TYPE.INFO, true);
            String command = "Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process | powershell.exe -File \".\\src\\GetFolderSize.ps1\" - directory " + users_directory + folder;
            ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe", "-command", command);
            builder.redirectErrorStream(true);	
            Process power_shell_process = builder.start();
            BufferedReader powershell_process_output_stream = new BufferedReader(new InputStreamReader(power_shell_process.getInputStream()));
            String output = "";
            String line = "";
            while((line = powershell_process_output_stream.readLine()).compareTo("EndOfScriptGetFolderSize") != 0) {
                    if(!line.isEmpty()) {
                            output = line;
                    }
            }
            powershell_process_output_stream.close();
            power_shell_process.destroy();
            if(Pattern.matches("[0-9]+", output)) {
                /*Long size_in_bytes = Long.parseLong(output);
                Long size_in_megabytes = size_in_bytes / (1024L * 1024L);
                return Long.toString(size_in_megabytes) + " MB";*/
                LogMessage("Successfully calculated filesize for folder " + users_directory + folder + ": " + output, LOG_TYPE.INFO, true);
                return output;
            } else {
                String message = "Size calculated is not a number. Ensure powershell script .\\src\\GetFolderSize.ps1 is correct";
                LogMessage(message, LOG_TYPE.ERROR, true);
                throw new NonNumericException(message);
            }
        } catch(NonNumericException | IOException e) {
            LogMessage("Could not calculate size of folder " + users_directory + folder, LOG_TYPE.ERROR, true);
            LogMessage(e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        }
    }

    public void MakeDirectory(String directory) throws IOException, CannotEditException {
        try{
            LogMessage("Attempting to create folder " + directory, LOG_TYPE.INFO, true);
            String command = "MKDIR \"" + directory + "\"";
            ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", command);
            builder.redirectErrorStream(true);	
            Process cmd_process = builder.start();
            BufferedReader cmd_process_output_stream = new BufferedReader(new InputStreamReader(cmd_process.getInputStream()));
            String line = "";
            String error = "";
            while((line = cmd_process_output_stream.readLine()) != null) {
                error = line;
            }
            if(error.compareTo("") != 0) {
                String message = "Folder " + directory + " already exists. Error is: " + error;
                LogMessage(message, LOG_TYPE.WARNING, true);
                throw new CannotEditException(message);
            }
            LogMessage("Successfully created folder " + directory, LOG_TYPE.INFO, true);
        } catch(IOException e) {
            LogMessage("Could not create folder " + directory, LOG_TYPE.ERROR, true);
            LogMessage(e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        } catch(CannotEditException e) {
            throw e;
        }
    }

    public void DeleteDirectory(String directory) throws IOException, CannotEditException {
        try{
            LogMessage("Attempting to delete folder " + directory, LOG_TYPE.INFO, true);
            String command = "RMDIR /S /Q \"" + directory + "\"";
            ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", command);
            builder.redirectErrorStream(true);	
            Process cmd_process = builder.start();
            BufferedReader cmd_process_output_stream = new BufferedReader(new InputStreamReader(cmd_process.getInputStream()));
            String line = "";
            String error = "";
            while((line = cmd_process_output_stream.readLine()) != null) {
                error = line;
            }
            if(error.compareTo("") != 0) {
                String message = "Unable to delete folder " + directory + ". Error is: " + error;
                LogMessage(message, LOG_TYPE.ERROR, true);
                throw new CannotEditException(message);
            }
            LogMessage("Successfully deleted folder " + directory, LOG_TYPE.INFO, true);
        } catch(CannotEditException | IOException e) {
            LogMessage("Could not delete folder " + directory, LOG_TYPE.ERROR, true);
            LogMessage(e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        }
    }
    
    public void DeleteFilesInDirectory(String directory, List<String> files, List<String> do_not_delete) throws IOException, CannotEditException {
        try{
            LogMessage("Attempting to delete list of files in directory " + directory, LOG_TYPE.INFO, true);
            for(String file : files) {
                boolean delete = true;
                if(do_not_delete != null) {
                    for(String exclude_file : do_not_delete){ 
                        if(file.compareTo(exclude_file) == 0) {
                            delete = false;
                        }
                    }
                }
                if(delete) {
                    String command = "del \"" + directory + "\\" + file + "\"";
                    ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", command);
                    builder.redirectErrorStream(true);	
                    Process cmd_process = builder.start();
                    BufferedReader cmd_process_output_stream = new BufferedReader(new InputStreamReader(cmd_process.getInputStream()));
                    String line = "";
                    String error = "";
                    while((line = cmd_process_output_stream.readLine()) != null) {
                        error = line;
                    }
                    if(error.compareTo("") != 0) {
                        String message = "Unable to delete file " + directory + "\\" + file + ". Error is: " + error;
                        LogMessage(message, LOG_TYPE.ERROR, true);
                        throw new CannotEditException(message);
                    }
                    LogMessage("Successfully deleted file " + directory + "\\" + file, LOG_TYPE.INFO, true);
                } else {
                    LogMessage("File " + directory + "\\" + file + " is in do not delete list. It has not been deleted", LOG_TYPE.INFO, true);
                }
            }
        } catch(CannotEditException | IOException e) {
            LogMessage("Failed to delete all requested files in directory " + directory, LOG_TYPE.ERROR, true);
            LogMessage(e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        }
    }
    
    public List<String> GetFileList(String directory) throws IOException, CannotEditException {
        try{
            LogMessage("Attempting to get list of files in directory " + directory, LOG_TYPE.INFO, true);
            String command = "dir /b /a-d \"" + directory + "\"";
            ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", command);
            builder.redirectErrorStream(true);	
            Process cmd_process = builder.start();
            BufferedReader cmd_process_output_stream = new BufferedReader(new InputStreamReader(cmd_process.getInputStream()));
            List<String> files = new ArrayList<String>();
            String line = "";
            String error = "";
            while((line = cmd_process_output_stream.readLine()) != null) {
                if(line.compareTo("") != 0) {
                    files.add(line);
                }
                error = line;
            }
            if(error.compareTo("") != 0) {
                String message = "Unable to get list of files in diectory " + directory + ". Error is: " + error;
                LogMessage(message, LOG_TYPE.ERROR, true);
                throw new CannotEditException(message);
            } else {
                LogMessage("Successfully got list of files in directory " + directory, LOG_TYPE.INFO, true);
                return files;
            }
        } catch(CannotEditException | IOException e) {
            LogMessage("Could not get list of files in directory " + directory, LOG_TYPE.ERROR, true);
            LogMessage(e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        }
    }
    
    
    public void DeleteFile(String full_file_name) throws IOException, CannotEditException {
        try{
            LogMessage("Attempting to delete file " + full_file_name, LOG_TYPE.INFO, true);
            String command = "del \"" + full_file_name + "\"";
            ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", command);
            builder.redirectErrorStream(true);	
            Process cmd_process = builder.start();
            BufferedReader cmd_process_output_stream = new BufferedReader(new InputStreamReader(cmd_process.getInputStream()));
            String line = "";
            String error = "";
            while((line = cmd_process_output_stream.readLine()) != null) {
                error = line;
            }
            if(error.compareTo("") != 0) {
                String message = "Unable to delete file " + full_file_name + ". Error is: " + error;
                LogMessage(message, LOG_TYPE.ERROR, true);
                throw new CannotEditException(message);
            }
            LogMessage("Successfully deleted file " + full_file_name, LOG_TYPE.INFO, true);
        } catch(CannotEditException | IOException e) {
            LogMessage("Could not delete file " + full_file_name, LOG_TYPE.ERROR, true);
            LogMessage(e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        }
    }

    public void CopyFile(String old_full_file_name, String new_directory) throws IOException, CannotEditException {
        try{
            LogMessage("Attempting to copy file " + old_full_file_name + " to new directory " + new_directory, LOG_TYPE.INFO, true);
            String command = "copy \"" + old_full_file_name + "\" \"" + new_directory + "\"";
            ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", command);
            builder.redirectErrorStream(true);	
            Process cmd_process = builder.start();
            BufferedReader cmd_process_output_stream = new BufferedReader(new InputStreamReader(cmd_process.getInputStream()));
            String line = "";
            String error = "";
            while((line = cmd_process_output_stream.readLine()) != null) {
                error = line;
            }
            if(!error.contains("file(s) copied")) {
                String message = "Unable to copy file " + old_full_file_name + " to folder " + new_directory + ". Error is: " + error;
                LogMessage(message, LOG_TYPE.ERROR, true);
                throw new CannotEditException(message);
            }
            LogMessage("Successfully copied file " + old_full_file_name + " to new directory " + new_directory, LOG_TYPE.INFO, true);
        } catch(CannotEditException | IOException e) {
            LogMessage("Could not copy file " + old_full_file_name + " to new directory " + new_directory, LOG_TYPE.ERROR, true);
            LogMessage(e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        }
    }

    public void BackupRegistry(String computer, String reg_key, String full_file_name) throws IOException, CannotEditException, InterruptedException {
        try{
            LogMessage("Attempting to backup registry key " + reg_key + " on computer " + computer + " to folder " + full_file_name, LOG_TYPE.INFO, true);
            String command = ".\\pstools\\psexec \\\\" + computer + " REG EXPORT \"" + reg_key + "\" \"" + full_file_name + "\" /y";
            ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", command);
            builder.redirectErrorStream(true);	
            Process cmd_process = builder.start();
            BufferedReader cmd_process_output_stream = new BufferedReader(new InputStreamReader(cmd_process.getInputStream()));
            String line = "";
            String error = "";
            boolean run = true;
            while((line = cmd_process_output_stream.readLine()) != null && run) {
                error = line;
                if(error.contains("REG exited")) {
                    run = false;
                }
            }
            cmd_process.waitFor();
            if(!error.contains("with error code 0")) {
                String message = "Could not backup registry key " + reg_key + " on computer " + computer + " to folder " + full_file_name;
                LogMessage(message, LOG_TYPE.ERROR, true);
                throw new CannotEditException(message);
            }
            LogMessage("Successfully backed up registry key " + reg_key + " on computer " + computer + " to folder " + full_file_name, LOG_TYPE.INFO, true);
        } catch(IOException | InterruptedException e) {
            LogMessage("Could not backup registry key " + reg_key + " on computer " + computer + " to folder " + full_file_name + ". Error is " + e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        }
    }

    public void DeleteRegistry(String computer, String reg_key) throws IOException, InterruptedException {
        try{
            LogMessage("Attempting to delete registry key " + reg_key + " from computer " + computer, LOG_TYPE.INFO, true);
            String command = ".\\pstools\\psexec \\\\" + computer + " REG DELETE \"" + reg_key + "\" /f";
            ProcessBuilder builder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", command);
            builder.redirectErrorStream(true);	
            Process cmd_process = builder.start();
            cmd_process.waitFor();
            LogMessage("Successfully deleted registry key " + reg_key + " from computer " + computer, LOG_TYPE.INFO, true);
        } catch(IOException | InterruptedException e) {
            LogMessage("Could not delete registry key " + reg_key + " from computer " + computer, LOG_TYPE.ERROR, true);
            LogMessage(e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        }
    }

    public String PrintDirectory() {
        LogMessage("Compiling directory list into readable String", LOG_TYPE.INFO, true, false);
        String output = "";
        Double total_size = 0.0;
        output += UserData.HeadingsToString();
        if(user_list.size() > 0) {
            output += '\n';
        }
        for(int i = 0;i < user_list.size();i++) {
            output += user_list.get(i).ToString();
            if(Pattern.matches("[-+]?[0-9]*\\.?[0-9]+", user_list.get(i).size)) {
                total_size += Double.parseDouble(user_list.get(i).size);
            }
            if(i != user_list.size()-1) {
                output += '\n';
            }
        }
        if(user_list.size() > 0) {
            Double size_in_megabytes = total_size / (1024.0 * 1024.0);
            output += '\n' + "Total size:" + '\t' + (size_in_megabytes + " MB"); 
        }
        LogMessage("Successfully compiled directory list into readable String", LOG_TYPE.INFO, true, false);
        return output;
    }

    public String GenerateDateString() {
        String output = GenerateDateString("");
        return output;
    }

    public String GenerateDateString(String prefix) {
        LogMessage("Generating date/time String with prefix " + prefix, LOG_TYPE.INFO, true, false);
        TimeZone timezone = TimeZone.getTimeZone("UTC");
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat filename_utc = new SimpleDateFormat("yyMMddHHmmss");
        String current_date = filename_utc.format(calendar.getTime());
        LogMessage("Generated date/time String " + prefix + current_date, LOG_TYPE.INFO, true, false);
        return prefix + current_date;
    }

    public String LogMessage(String message, LOG_TYPE state, boolean include_timestamp) {
        String log_message = LogMessage(message, state, include_timestamp, true);
        return log_message;
    }
    
    public String LogMessage(String message, LOG_TYPE state, boolean include_timestamp, boolean display_to_console) {
            String log_message = "";
            if(null != state) switch (state) {
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

            if(include_timestamp) {
                    SimpleDateFormat human_readable_timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
                    Date timestamp = new Date();
                    String format_timestamp = human_readable_timestamp.format(timestamp);
                    log_message += "[" + format_timestamp + "] ";
            }

            log_message += message;

            log_list.add(log_message);
            if(display_to_console) {
                system_console_text_area.append('\n' + message);
            }
            return log_message;
    }

    public String WriteLog() throws IOException, NotInitialisedException {
        if(!log_list.isEmpty()) {
            try {
                String filename = "logs\\Profile_Deleter_Log_" + GenerateDateString() + ".txt";
                WriteToFile(filename, log_list);
                return filename;
            } catch(IOException e) {
                throw e;
            }
        } else {
            throw new NotInitialisedException("Nothing has been logged");
        }
    }

    //Read from file function
    private List<String> ReadFromFile(String filename) throws IOException {
        List<String> read_data = new ArrayList<String>();
        try {
                File file = new File(filename);
                try(BufferedReader br = new BufferedReader(new FileReader(file))) {
                        for(String line; (line = br.readLine()) != null; ) {
                                read_data.add(line);
                        }
                }
        } catch(IOException e) {
                throw e;
        }
        return read_data;
    }

    private void WriteToFile(String filename, List<String> write_to_file) throws IOException {
        try {
            int count = 0;
            File file = new File(filename);
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
            for (String string_line : write_to_file) {
                if (count > 0) {
                    writer.newLine();
                }
                writer.write(string_line);
                count++;
            }
            writer.close();
        } catch (IOException e) {
            throw e;
        }
    }

    public void clearConsole() throws UnrecoverableException{
            try {
                    new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } catch(Exception e) {
                    throw new UnrecoverableException();
            }
    }

    public boolean pingPC(String PC) throws IOException, InterruptedException {
        LogMessage("Pinging PC " + PC + " to ensure it exists and is reachable on the network", LOG_TYPE.INFO, true);
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
            LogMessage("Ping check has failed with error " + e.getMessage(), LOG_TYPE.ERROR, true);
            throw e;
        }
        LogMessage("Ping check has completed successfully, result is " + pc_online, LOG_TYPE.INFO, true);
        return pc_online;
    }
    
    @Override
    public void tableChanged(TableModelEvent e) {
        int row = e.getFirstRow();
        int column = e.getColumn();
        TableModel model = (TableModel)e.getSource();
        Object data = model.getValueAt(row, column);
        
        user_list.get(row).delete = Boolean.parseBoolean(data.toString());
    }
    
    private class SetComputerThread extends SwingWorker<Object, Object> {
        boolean ping_success = false;
                
        @Override
        protected Object doInBackground() throws Exception {
            ping_success = pingPC(computer_name_text_field.getText());
            if(ping_success) {
                size_check_complete = false;
                state_check_complete = false;
                registry_check_complete = false;
                SetComputer(computer_name_text_field.getText());
                BuildDirectory();
                if(size_check) {
                    CheckSize();
                }
                if(state_check) {
                    CheckState();
                }
                if(registry_check) {
                    GenerateSessionID();
                    try {
                        CreateSessionFolders();
                        try {
                            BackupAndCopyRegistry();
                            try {
                                FindSIDAndGUID();
                            } catch(IOException | NotInitialisedException e) {
                            }
                        } catch(IOException | CannotEditException | NotInitialisedException | InterruptedException e) {
                            LogMessage("Unable to backup registry files, error is: " + e.getMessage(), LOG_TYPE.ERROR, true);
                        }
                    } catch(IOException | CannotEditException | NotInitialisedException e) {
                    }
                }
                UpdateTableData();
            } else {
                LogMessage("Unable to ping computer, computer not set", LOG_TYPE.WARNING, true);
            }
            return new Object();
        }
        
        @Override
        public void done() {
            if(ping_success || (computer != null && !computer.isEmpty())) {
                rerun_checks_button.setEnabled(true);
            }
            if(state_check_complete && registry_check_complete) {
                run_deletion_button.setEnabled(true);
            }
            computer_name_text_field.setEnabled(true);
            set_computer_button.setEnabled(true);
            size_check_checkbox.setEnabled(true);
            state_check_checkbox.setEnabled(true);
            registry_check_checkbox.setEnabled(true);
            write_log_button.setEnabled(true);
            results_table.setEnabled(true);
        }
    }
    
    private class RerunChecksThread extends SwingWorker<Object, Object> {
        @Override
        protected Object doInBackground() throws Exception {
            if(computer != null && !computer.isEmpty()) {
                if(size_check) {
                    CheckSize();
                }
                if(state_check) {
                    CheckState();
                }
                if(registry_check) {
                    GenerateSessionID();
                    try {
                        CreateSessionFolders();
                        try {
                            BackupAndCopyRegistry();
                            try {
                                FindSIDAndGUID();
                            } catch(IOException | NotInitialisedException e) {
                            }
                        } catch(IOException | CannotEditException | NotInitialisedException | InterruptedException e) {
                            LogMessage("Unable to backup registry files, error is: " + e.getMessage(), LOG_TYPE.ERROR, true);
                        }
                    } catch(IOException | CannotEditException | NotInitialisedException e) {
                    }
                }
                UpdateTableData();
            }
            return new Object();
        }
        
        @Override
        public void done() {
            if(state_check_complete && registry_check_complete) {
                run_deletion_button.setEnabled(true);
            }
            rerun_checks_button.setEnabled(true);
            computer_name_text_field.setEnabled(true);
            set_computer_button.setEnabled(true);
            size_check_checkbox.setEnabled(true);
            state_check_checkbox.setEnabled(true);
            registry_check_checkbox.setEnabled(true);
            write_log_button.setEnabled(true);
            results_table.setEnabled(true);
        }
    }
    
    private class WriteLogThread extends SwingWorker<Object, Object> {
        @Override
        protected Object doInBackground() throws Exception {
            try {
                LogMessage("Successfully wrote log to file " + WriteLog(), LOG_TYPE.INFO, true);
            } catch (IOException | NotInitialisedException e) {
                LogMessage("Failed to write log to file. Error is " + e.getMessage(), LOG_TYPE.ERROR, true);
            }
            return new Object();
        }
        
        @Override
        public void done() {
            if(state_check_complete && registry_check_complete) {
                run_deletion_button.setEnabled(true);
            }
            rerun_checks_button.setEnabled(true);
            computer_name_text_field.setEnabled(true);
            set_computer_button.setEnabled(true);
            size_check_checkbox.setEnabled(true);
            state_check_checkbox.setEnabled(true);
            registry_check_checkbox.setEnabled(true);
            write_log_button.setEnabled(true);
            results_table.setEnabled(true);
        }
    }
    
    private class RunDeletionThread extends SwingWorker<Object, Object> {
        @Override
        protected Object doInBackground() throws Exception {
            try {
                List<String> deleted_users = ProcessDeletion();
                deleted_users.add(0, "Deletion report:");
                if(deleted_users.size() > 2) {
                    for(String deleted_user : deleted_users) {
                        system_console_text_area.append('\n' + deleted_user);
                    }
                    String suffix = GenerateDateString();
                    WriteToFile("reports\\" + computer + "_deletion_report_" + suffix + ".txt", deleted_users);
                    LogMessage("Deletion report written to file reports\\" + computer + "_deletion_report_" + suffix + ".txt", LOG_TYPE.INFO, true);
                    UpdateTableData();
                    if(size_check_complete) {
                        double total_size = 0.0;
                        for(UserData user : user_list) {
                            total_size += Double.parseDouble(user.size);
                        }
                        setTitle("Profile Deleter - " + computer + " - Total Users Size: " + Math.round(total_size / (1024.0 * 1024.0)) + "MB");
                    }
                } else {
                    LogMessage("Nothing was flagged for deletion", LOG_TYPE.WARNING, true);
                }
            } catch(NotInitialisedException | IOException e) {
            }
            return new Object();
        }
        
        @Override
        public void done() {
            if(state_check_complete && registry_check_complete) {
                run_deletion_button.setEnabled(true);
            }
            rerun_checks_button.setEnabled(true);
            computer_name_text_field.setEnabled(true);
            set_computer_button.setEnabled(true);
            size_check_checkbox.setEnabled(true);
            state_check_checkbox.setEnabled(true);
            registry_check_checkbox.setEnabled(true);
            write_log_button.setEnabled(true);
            results_table.setEnabled(true);
        }
    }
}