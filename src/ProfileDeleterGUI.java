
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.text.html.HTMLEditorKit;

/**
 * Implementation of a Swing GUI for the ProfileDeleter class.
 */
public class ProfileDeleterGUI extends JFrame implements TableModelListener, ActionListener {

    // The ProfileDeleter class that handles all the logic of the application.
    private ProfileDeleter profile_deleter;

    // Class attributes
    boolean show_tooltips;
    int tooltip_delay_timer;
    int tooltip_dismiss_timer;
    String help_location;
    String help_text;
    Color uneditable_color;
    String deletion_report_string;

    /**
     * Swing GUI elements.
     */
    private JScrollPane results_scroll_pane;
    private JTable results_table;
    private GridBagConstraints results_table_gc;
    private JScrollPane system_console_scroll_pane;
    private JTextArea system_console_text_area;
    private int system_console_scrollbar_previous_maximum;
    private GridBagConstraints system_console_gc;
    private JTextField computer_name_text_field;
    private GridBagConstraints computer_name_text_field_gc;
    private JButton set_computer_button;
    private GridBagConstraints set_computer_button_gc;
    private JButton rerun_checks_button;
    private GridBagConstraints rerun_checks_button_gc;
    private JButton run_deletion_button;
    private GridBagConstraints run_deletion_button_gc;
    private JButton write_log_button;
    private GridBagConstraints write_log_button_gc;
    private JButton help_button;
    private GridBagConstraints help_button_gc;
    private JButton exit_button;
    private GridBagConstraints exit_button_gc;
    private JCheckBox size_check_checkbox;
    private GridBagConstraints size_check_checkbox_gc;
    private JCheckBox state_check_checkbox;
    private GridBagConstraints state_check_checkbox_gc;
    private JCheckBox registry_check_checkbox;
    private GridBagConstraints registry_check_checkbox_gc;
    private JCheckBox delete_all_users_checkbox;
    private GridBagConstraints delete_all_users_checkbox_gc;
    private JCheckBox tooltips_checkbox;
    private GridBagConstraints tooltips_checkbox_gc;
    private JFrame help_frame;
    private JEditorPane help_frame_editor_pane;
    private JScrollPane help_frame_scroll_pane;
    private HTMLEditorKit help_frame_html_editor_kit;
    private JFrame deletion_report_frame;
    private JLabel deletion_report_frame_heading_label;
    private GridBagConstraints deletion_report_frame_heading_label_gc;
    private JLabel deletion_report_frame_computer_label;
    private GridBagConstraints deletion_report_frame_computer_label_gc;
    private JButton deletion_report_frame_copy_to_clipboard_button;
    private GridBagConstraints deletion_report_frame_copy_to_clipboard_button_gc;
    private JTable deletion_report_frame_table;
    private JScrollPane deletion_report_frame_scroll_pane;
    private GridBagConstraints deletion_report_frame_scroll_pane_gc;

    /**
     * SwingWorker threads for GUI.
     */
    private setComputerThread set_computer_thread;
    private rerunChecksThread rerun_checks_thread;
    private runDeletionThread run_deletion_thread;
    private writeLogThread write_log_thread;

    public static void main(String args[]) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    new ProfileDeleterGUI();
                } catch (UnrecoverableException e) {
                    JFrame fatal_error = new JFrame("Fatal Error");
                    fatal_error.setPreferredSize(new Dimension(250, 250));
                    fatal_error.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    JLabel error_message = new JLabel("<html>" + e.getMessage() + "</html>");
                    error_message.setBorder(new EmptyBorder(20, 20, 20, 20));
                    fatal_error.getContentPane().add(error_message);
                    fatal_error.pack();
                    fatal_error.setVisible(true);
                }
            }
        });
    }

    public ProfileDeleterGUI() throws UnrecoverableException {
        super("Profile Deleter");

        // The ProfileDeleter class that handles all the logic of the application.
        profile_deleter = new ProfileDeleter(this);
        show_tooltips = true;
        tooltip_delay_timer = 0;
        tooltip_dismiss_timer = 60000;
        help_location = "";
        uneditable_color = new Color(235, 235, 235);
        deletion_report_string = "";

        // Loads the GUI Configuration settings from the profiledeleter.config file.
        List<String> config = new ArrayList<>();
        try {
            config = profile_deleter.readFromFile("profiledeleter.config");
            for (String line : config) {
                if (line.startsWith("show_tooltips=")) {
                    show_tooltips = Boolean.parseBoolean(line.replace("show_tooltips=", ""));
                } else if (line.startsWith("tooltip_delay_timer=")) {
                    tooltip_delay_timer = Integer.parseInt(line.replace("tooltip_delay_timer=", ""));
                } else if (line.startsWith("tooltip_dismiss_timer=")) {
                    tooltip_dismiss_timer = Integer.parseInt(line.replace("tooltip_dismiss_timer=", ""));
                } else if (line.startsWith("help=")) {
                    help_location = line.replace("help=", "");
                    try {
                        List<String> help_text_array = profile_deleter.readFromFile(help_location + "\\profile_deleter_help.html");
                        int count = 1;
                        for (String help_text_line : help_text_array) {
                            help_text += help_text_line;
                            if (count < help_text_array.size()) {
                                help_text += '\n';
                            }
                            count++;
                        }
                    } catch (IOException e) {
                        help_text = "Failed to load help. Check help folder " + help_location + " for help documents";
                    }
                }
            }
        } catch (IOException e) {
        }

        // Configuration of tooltip settings
        ToolTipManager.sharedInstance().setEnabled(show_tooltips);
        ToolTipManager.sharedInstance().setInitialDelay(tooltip_delay_timer);
        ToolTipManager.sharedInstance().setDismissDelay(tooltip_dismiss_timer);

        // Configurations for the top level JFrame of the GUI.
        setMinimumSize(new Dimension(1150, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(new GridBagLayout());
        setLocationRelativeTo(null);

        // Column header tooltips in the results table JTable GUI element.
        final String[] columnToolTips = {
            "Whether user is marked for deletion. Cannot delete if state is not Editable and cannot delete users on the cannot delete list. Users in the should not delete list will not be automatically flagged for deletion and must be flagged for deletion manually",
            "Name of user folder",
            "Last time the user folder was updated",
            "Size in megabytes of the user folder. Run a size check to populate this column",
            "Whether user folder can be edited. If Uneditable user may be logged in or PC may need to be restarted. User may also be on the cannot delete list. Run a state check to populate this column",
            "SID value found in registry for user folder. Run a registry check to populate this column",
            "GUID value found in registry for user folder. Run a registry check to populate this column"
        };

        // Initialisation of results table JTable GUI element.
        results_table = new JTable(new DefaultTableModel()) {
            @Override
            public String getToolTipText(MouseEvent e) {
                String tip = null;
                java.awt.Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);
                int realColumnIndex = convertColumnIndexToModel(colIndex);
                int realRowIndex = convertRowIndexToModel(rowIndex);

                TableModel model = getModel();
                String editable = (String) model.getValueAt(realRowIndex, 4);
                String name = (String) model.getValueAt(realRowIndex, 1);

                switch (realColumnIndex) {
                    case 0:
                        if (profile_deleter.getCannotDeleteList().contains(name.toLowerCase())) {
                            tip = "User is in the cannot delete list, you cannot delete users in this list";
                        } else if (editable.compareTo("Uneditable") == 0) {
                            tip = "Cannot delete if state is not Editable";
                        } else if (profile_deleter.getShouldNotDeleteList().contains(name.toLowerCase())) {
                            tip = "User is in the should not delete list. It is recommended you do not delete this account unless it is necessary";
                        } else if (editable.compareTo("Editable") != 0) {
                            tip = "Cannot delete if state is not Editable";
                        }
                        break;
                    case 1:
                        if (profile_deleter.getCannotDeleteList().contains(name.toLowerCase())) {
                            tip = "User is in the cannot delete list, you cannot delete users in this list";
                        }
                        break;
                    case 4:
                        if (profile_deleter.getCannotDeleteList().contains(name.toLowerCase())) {
                            tip = "User is in the cannot delete list, you cannot delete users in this list";
                        } else if (editable.compareTo("Uneditable") == 0) {
                            tip = "User may be logged in or PC may need to be restarted";
                        }
                        break;
                }
                return tip;
            }

            @Override
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    @Override
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
        createTableData();
        results_scroll_pane = new JScrollPane(results_table);
        results_scroll_pane.setBorder(new LineBorder(Color.BLACK, 2));
        results_table_gc = new GridBagConstraints();
        results_table_gc.fill = GridBagConstraints.BOTH;
        results_table_gc.gridx = 0;
        results_table_gc.gridy = 1;
        results_table_gc.gridwidth = GridBagConstraints.REMAINDER;
        results_table_gc.weighty = 1;

        // Initialisation of system console GUI element.
        system_console_text_area = new JTextArea("System Console");
        system_console_text_area.setEditable(false);
        system_console_text_area.setBackground(Color.BLACK);
        system_console_text_area.setForeground(Color.WHITE);
        system_console_text_area.setSelectedTextColor(Color.YELLOW);
        system_console_text_area.setMargin(new Insets(0, 2, 0, 0));
        system_console_scroll_pane = new JScrollPane(system_console_text_area);
        system_console_scroll_pane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            // If the scrollbar is at the very bottom it will auto scroll as new text is added, if it is not at the very bottom it will stay in its current position.
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                if (system_console_scrollbar_previous_maximum - (system_console_scroll_pane.getViewport().getViewPosition().y + system_console_scroll_pane.getViewport().getViewRect().height) <= 1) {
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
        system_console_scroll_pane.setPreferredSize(new Dimension(100, 100));

        // Initialisation of computer name input field GUI element.
        computer_name_text_field = new JTextField();
        computer_name_text_field.setToolTipText("Enter the hostname or IP address of the target computer");
        computer_name_text_field.setActionCommand("SetComputer");
        computer_name_text_field.addActionListener(this);
        computer_name_text_field_gc = new GridBagConstraints();
        computer_name_text_field_gc.fill = GridBagConstraints.BOTH;
        computer_name_text_field_gc.gridx = 0;
        computer_name_text_field_gc.gridy = 0;
        computer_name_text_field_gc.gridwidth = 1;
        computer_name_text_field_gc.gridheight = 1;
        computer_name_text_field_gc.weightx = 1;
        computer_name_text_field_gc.insets = new Insets(2, 2, 2, 2);

        // Initialisation of set computer button GUI element.
        set_computer_button = new JButton("Set Computer");
        set_computer_button.setToolTipText("Loads the user list on the target computer and runs the specified checks. Will ping the computer first to ensure it is reachable on the network");
        set_computer_button.setActionCommand("SetComputer");
        set_computer_button.addActionListener(this);
        set_computer_button_gc = new GridBagConstraints();
        set_computer_button_gc.fill = GridBagConstraints.BOTH;
        set_computer_button_gc.gridx = 1;
        set_computer_button_gc.gridy = 0;
        set_computer_button_gc.gridwidth = 1;
        set_computer_button_gc.gridheight = 1;

        // Initialisation of size check checkbox GUI element.
        size_check_checkbox = new JCheckBox();
        size_check_checkbox.setToolTipText("If ticked will run a size check on each user found. This check is not necessary to run a deletion and can take a very long time if the users directory is large");
        size_check_checkbox.setSelected(profile_deleter.getSizeCheck());
        size_check_checkbox.setText("Size Check");
        size_check_checkbox.setActionCommand("SizeCheckToggle");
        size_check_checkbox.addActionListener(this);
        size_check_checkbox_gc = new GridBagConstraints();
        size_check_checkbox_gc.fill = GridBagConstraints.BOTH;
        size_check_checkbox_gc.gridx = 2;
        size_check_checkbox_gc.gridy = 0;
        size_check_checkbox_gc.gridwidth = 1;
        size_check_checkbox_gc.gridheight = 1;

        // Initialisation of state check checkbox GUI element.
        state_check_checkbox = new JCheckBox();
        state_check_checkbox.setToolTipText("If ticked will run a state check on each users local folder on the target computer to ensure you have access to delete it. This check must be run before a deletion can be done");
        state_check_checkbox.setSelected(profile_deleter.getStateCheck());
        state_check_checkbox.setText("State Check");
        state_check_checkbox.setActionCommand("StateCheckToggle");
        state_check_checkbox.addActionListener(this);
        state_check_checkbox_gc = new GridBagConstraints();
        state_check_checkbox_gc.fill = GridBagConstraints.BOTH;
        state_check_checkbox_gc.gridx = 3;
        state_check_checkbox_gc.gridy = 0;
        state_check_checkbox_gc.gridwidth = 1;
        state_check_checkbox_gc.gridheight = 1;

        // Initialisation of registry check checkbox GUI element.
        registry_check_checkbox = new JCheckBox();
        registry_check_checkbox.setToolTipText("If ticked will query the registry on the target computer to get the ProfileList and ProfileGuid information for each user. This check must be run before a deletion can be done");
        registry_check_checkbox.setSelected(profile_deleter.getRegistryCheck());
        registry_check_checkbox.setText("Registry Check");
        registry_check_checkbox.setActionCommand("RegistryCheckToggle");
        registry_check_checkbox.addActionListener(this);
        registry_check_checkbox_gc = new GridBagConstraints();
        registry_check_checkbox_gc.fill = GridBagConstraints.BOTH;
        registry_check_checkbox_gc.gridx = 4;
        registry_check_checkbox_gc.gridy = 0;
        registry_check_checkbox_gc.gridwidth = 1;
        registry_check_checkbox_gc.gridheight = 1;

        // Initialisation of delete all users checkbox GUI element.
        delete_all_users_checkbox = new JCheckBox();
        delete_all_users_checkbox.setToolTipText("If ticked all users with Editable state will be marked for deletion. Users in the should not delete list will not be marked automatically and must be marked manually");
        delete_all_users_checkbox.setSelected(profile_deleter.getDeleteAllUsers());
        delete_all_users_checkbox.setText("Delete All");
        delete_all_users_checkbox.setActionCommand("DeleteAllUsersToggle");
        delete_all_users_checkbox.addActionListener(this);
        delete_all_users_checkbox_gc = new GridBagConstraints();
        delete_all_users_checkbox_gc.fill = GridBagConstraints.BOTH;
        delete_all_users_checkbox_gc.gridx = 5;
        delete_all_users_checkbox_gc.gridy = 0;
        delete_all_users_checkbox_gc.gridwidth = 1;
        delete_all_users_checkbox_gc.gridheight = 1;

        // Initialisation of rerun checks button GUI element.
        rerun_checks_button = new JButton("Rerun Checks");
        rerun_checks_button.setToolTipText("Runs enabled checks. This button will only become active when a computer has been set");
        rerun_checks_button.setActionCommand("RerunChecks");
        rerun_checks_button.addActionListener(this);
        rerun_checks_button.setEnabled(false);
        rerun_checks_button_gc = new GridBagConstraints();
        rerun_checks_button_gc.fill = GridBagConstraints.BOTH;
        rerun_checks_button_gc.gridx = 6;
        rerun_checks_button_gc.gridy = 0;
        rerun_checks_button_gc.gridwidth = 1;
        rerun_checks_button_gc.gridheight = 1;

        // Initialisation of run deletion button GUI element.
        run_deletion_button = new JButton("Run Deletion");
        run_deletion_button.setToolTipText("Deletes all users marked for deletion. This process can take a long time depending on the total filesize of all users marked. This button will become active when a computer has been set and both a state and registry check have been done");
        run_deletion_button.setActionCommand("RunDeletion");
        run_deletion_button.addActionListener(this);
        run_deletion_button.setEnabled(false);
        run_deletion_button_gc = new GridBagConstraints();
        run_deletion_button_gc.fill = GridBagConstraints.BOTH;
        run_deletion_button_gc.gridx = 7;
        run_deletion_button_gc.gridy = 0;
        run_deletion_button_gc.gridwidth = 1;
        run_deletion_button_gc.gridheight = 1;

        // Initialisation of write log button GUI element.
        write_log_button = new JButton("Write Log");
        write_log_button.setToolTipText("Creates a log file of all logged events");
        write_log_button.setActionCommand("WriteLog");
        write_log_button.addActionListener(this);
        write_log_button_gc = new GridBagConstraints();
        write_log_button_gc.fill = GridBagConstraints.BOTH;
        write_log_button_gc.gridx = 8;
        write_log_button_gc.gridy = 0;
        write_log_button_gc.gridwidth = 1;
        write_log_button_gc.gridheight = 1;

        // Initialisation of help button GUI element.
        help_button = new JButton("Help");
        help_button.setToolTipText("Displays/hides help instructions");
        help_button.setActionCommand("Help");
        help_button.addActionListener(this);
        help_button_gc = new GridBagConstraints();
        help_button_gc.fill = GridBagConstraints.BOTH;
        help_button_gc.gridx = 9;
        help_button_gc.gridy = 0;
        help_button_gc.gridwidth = 1;
        help_button_gc.gridheight = 1;

        // Initialisation of show tooltips checkbox GUI element.
        tooltips_checkbox = new JCheckBox();
        tooltips_checkbox.setToolTipText("Turns tooltips on or off");
        tooltips_checkbox.setSelected(show_tooltips);
        tooltips_checkbox.setText("Show Tooltips");
        tooltips_checkbox.setActionCommand("TooltipsToggle");
        tooltips_checkbox.addActionListener(this);
        tooltips_checkbox_gc = new GridBagConstraints();
        tooltips_checkbox_gc.fill = GridBagConstraints.BOTH;
        tooltips_checkbox_gc.gridx = 10;
        tooltips_checkbox_gc.gridy = 0;
        tooltips_checkbox_gc.gridwidth = 1;
        tooltips_checkbox_gc.gridheight = 1;

        // Initialisation of exit button GUI element.
        exit_button = new JButton("Exit");
        exit_button.setToolTipText("Exits the program");
        exit_button.setActionCommand("Exit");
        exit_button.addActionListener(this);
        exit_button_gc = new GridBagConstraints();
        exit_button_gc.fill = GridBagConstraints.BOTH;
        exit_button_gc.gridx = 11;
        exit_button_gc.gridy = 0;
        exit_button_gc.gridwidth = 1;
        exit_button_gc.gridheight = 1;

        // Initialisation of help instructions display GUI element.
        help_frame = new JFrame("Profile Deleter Help");
        help_frame_editor_pane = new JEditorPane();
        help_frame_editor_pane.setEditable(false);
        help_frame_editor_pane.setContentType("text/html");
        try {
            File help_file = new File(help_location + "\\profile_deleter_help.html");
            if (!help_file.exists()) {
                throw new IOException("Help file does not exist");
            }
            help_frame_editor_pane.setPage((new File(help_location + "\\profile_deleter_help.html").toURI().toURL()));
        } catch (IOException e) {
            help_frame_html_editor_kit = new HTMLEditorKit();
            help_frame_editor_pane.setEditorKit(help_frame_html_editor_kit);
            help_frame_editor_pane.setDocument(help_frame_html_editor_kit.createDefaultDocument());
            help_frame_editor_pane.setText(help_text);
        }
        help_frame_scroll_pane = new JScrollPane(help_frame_editor_pane);
        help_frame.getContentPane().add(help_frame_scroll_pane);
        help_frame.setDefaultCloseOperation(HIDE_ON_CLOSE);
        help_frame.setMinimumSize(new Dimension(1200, 600));
        help_frame.pack();
        help_frame.setVisible(false);

        // Initialisation of deletion report display GUI element.
        deletion_report_frame = new JFrame("Deletion Report");
        deletion_report_frame.getContentPane().setLayout(new GridBagLayout());
        deletion_report_frame_heading_label = new JLabel("<html><h2>Deletion Report</h2></html>");
        deletion_report_frame_heading_label.setOpaque(true);
        deletion_report_frame_heading_label.setBackground(Color.WHITE);
        deletion_report_frame_heading_label_gc = new GridBagConstraints();
        deletion_report_frame_heading_label_gc.fill = GridBagConstraints.BOTH;
        deletion_report_frame_heading_label_gc.gridwidth = GridBagConstraints.REMAINDER;
        deletion_report_frame_heading_label_gc.gridx = 0;
        deletion_report_frame_heading_label_gc.gridy = 0;
        deletion_report_frame_computer_label = new JLabel();
        deletion_report_frame_computer_label.setOpaque(true);
        deletion_report_frame_computer_label.setBackground(Color.WHITE);
        deletion_report_frame_computer_label_gc = new GridBagConstraints();
        deletion_report_frame_computer_label_gc.fill = GridBagConstraints.BOTH;
        deletion_report_frame_computer_label_gc.gridx = 0;
        deletion_report_frame_computer_label_gc.gridy = 1;
        deletion_report_frame_computer_label_gc.weightx = 1;
        deletion_report_frame_copy_to_clipboard_button = new JButton("Copy Deletion Report to Clipboard");
        deletion_report_frame_copy_to_clipboard_button.setActionCommand("CopyDeletionReport");
        deletion_report_frame_copy_to_clipboard_button.addActionListener(this);
        deletion_report_frame_copy_to_clipboard_button_gc = new GridBagConstraints();
        deletion_report_frame_copy_to_clipboard_button_gc.fill = GridBagConstraints.BOTH;
        deletion_report_frame_copy_to_clipboard_button_gc.gridx = 1;
        deletion_report_frame_copy_to_clipboard_button_gc.gridy = 1;
        deletion_report_frame_table = new JTable();
        deletion_report_frame_scroll_pane = new JScrollPane(deletion_report_frame_table);
        deletion_report_frame_scroll_pane_gc = new GridBagConstraints();
        deletion_report_frame_scroll_pane_gc.fill = GridBagConstraints.BOTH;
        deletion_report_frame_scroll_pane_gc.gridwidth = GridBagConstraints.REMAINDER;
        deletion_report_frame_scroll_pane_gc.gridx = 0;
        deletion_report_frame_scroll_pane_gc.gridy = 2;
        deletion_report_frame_scroll_pane_gc.weightx = 1;
        deletion_report_frame_scroll_pane_gc.weighty = 1;
        deletion_report_frame.getContentPane().add(deletion_report_frame_heading_label, deletion_report_frame_heading_label_gc);
        deletion_report_frame.getContentPane().add(deletion_report_frame_computer_label, deletion_report_frame_computer_label_gc);
        deletion_report_frame.getContentPane().add(deletion_report_frame_copy_to_clipboard_button, deletion_report_frame_copy_to_clipboard_button_gc);
        deletion_report_frame.getContentPane().add(deletion_report_frame_scroll_pane, deletion_report_frame_scroll_pane_gc);
        deletion_report_frame.setDefaultCloseOperation(HIDE_ON_CLOSE);
        deletion_report_frame.setMinimumSize(new Dimension(1200, 600));
        deletion_report_frame.pack();
        deletion_report_frame.setVisible(false);

        // Add all GUI elements to top level JFrame and display the GUI.
        getContentPane().add(computer_name_text_field, computer_name_text_field_gc);
        getContentPane().add(set_computer_button, set_computer_button_gc);
        getContentPane().add(size_check_checkbox, size_check_checkbox_gc);
        getContentPane().add(state_check_checkbox, state_check_checkbox_gc);
        getContentPane().add(registry_check_checkbox, registry_check_checkbox_gc);
        getContentPane().add(rerun_checks_button, rerun_checks_button_gc);
        getContentPane().add(delete_all_users_checkbox, delete_all_users_checkbox_gc);
        getContentPane().add(run_deletion_button, run_deletion_button_gc);
        getContentPane().add(write_log_button, write_log_button_gc);
        getContentPane().add(help_button, help_button_gc);
        getContentPane().add(tooltips_checkbox, tooltips_checkbox_gc);
        getContentPane().add(exit_button, exit_button_gc);
        getContentPane().add(results_scroll_pane, results_table_gc);
        getContentPane().add(system_console_scroll_pane, system_console_gc);
        pack();
        setVisible(true);
    }

    /**
     * Converts a double value to a formatted String value.
     * <p>
     * The value passed is converted to a long value and has , added every 3rd
     * number.
     *
     * @param format_value the value to format
     * @return the value with the decimal place removed and , added every 3rd
     * number
     */
    private String doubleToFormattedString(Double format_value) {
        long double_to_long = Math.round(format_value);
        String double_to_long_string = Long.toString(double_to_long);
        String foramatted_string = "";
        int count = 0;
        for (int i = double_to_long_string.length() - 1; i >= 0; i--) {
            if (count == 3) {
                foramatted_string = "," + foramatted_string;
                count = 0;
            }
            foramatted_string = double_to_long_string.charAt(i) + foramatted_string;
            count++;
        }
        return foramatted_string;
    }

    /**
     * Initialises the table model and populates the data for the results table.
     * <p>
     * This needs to be run anytime the amount of users in the user list
     * changes.
     */
    private void createTableData() {
        results_table.getModel().removeTableModelListener(this);
        results_table.setModel(new DefaultTableModel(profile_deleter.convertUserListTo2DObjectArray(), UserData.headingsToStringArray()) {

            @Override
            public Class getColumnClass(int col) {
                String class_name = "";
                switch (col) {
                    case 0:
                        class_name = "java.lang.Boolean";
                        break;
                    case 2:
                        class_name = "java.util.Date";
                        break;
                    case 3:
                        class_name = "java.lang.Integer";
                        break;
                    default:
                        class_name = "java.lang.String";
                        break;
                }
                try {
                    return Class.forName(class_name);
                } catch (ClassNotFoundException ex) {
                    System.out.println("Couldn't find class " + class_name);
                }
                return null;
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 0 && getValueAt(row, 4) == "Editable" && !profile_deleter.getCannotDeleteList().contains(getValueAt(row, 1).toString().toLowerCase());
            }
        });
        // Default renderer for table columns.
        TableCellRenderer default_renderer = new DefaultTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean is_selected, boolean has_focus, int row, int column) {
                Component tableCellRendererComponent = super.getTableCellRendererComponent(table, value, is_selected, has_focus, row, column);
                if ((table.getModel().getValueAt(table.convertRowIndexToModel(row), 4)).toString().equals("Uneditable")) {
                    setBackground(uneditable_color);
                } else {
                    setBackground(Color.WHITE);
                }
                return tableCellRendererComponent;
            }
        };
        // Determines how the delete column should be displayed.
        TableCellRenderer boolean_renderer = new DefaultTableCellRenderer() {
            JCheckBox checkbox = new JCheckBox();

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean is_selected, boolean has_focus, int row, int column) {
                //Component tableCellRendererComponent = super.getTableCellRendererComponent(table, value, is_selected, has_focus, row, column);
                if (value instanceof Boolean) {
                    checkbox.setSelected((Boolean) value);
                }
                if ((table.getModel().getValueAt(table.convertRowIndexToModel(row), 4)).toString().equals("Uneditable")) {
                    checkbox.setBackground(uneditable_color);
                } else {
                    checkbox.setBackground(Color.WHITE);
                }
                checkbox.setHorizontalAlignment(CENTER);
                return checkbox;
            }
        };
        // Determines how the last updated column should be displayed.
        TableCellRenderer date_renderer = new DefaultTableCellRenderer() {
            DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

            @Override
            public void setValue(Object value) {
                setText((value == null) ? "" : formatter.format(value));
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean is_selected, boolean has_focus, int row, int column) {
                Component tableCellRendererComponent = super.getTableCellRendererComponent(table, value, is_selected, has_focus, row, column);
                if ((table.getModel().getValueAt(table.convertRowIndexToModel(row), 4)).toString().equals("Uneditable")) {
                    setBackground(uneditable_color);
                } else {
                    setBackground(Color.WHITE);
                }
                return tableCellRendererComponent;
            }
        };
        // Determines how the size column should be displayed.
        TableCellRenderer size_renderer = new DefaultTableCellRenderer() {
            @Override
            public void setValue(Object value) {
                String output = "";
                if (value != null && !value.toString().isEmpty()) {
                    output = doubleToFormattedString(Double.parseDouble(value.toString()) / (1024.0 * 1024.0)) + " MB";
                }
                setText(output);
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean is_selected, boolean has_focus, int row, int column) {
                Component tableCellRendererComponent = super.getTableCellRendererComponent(table, value, is_selected, has_focus, row, column);
                ((DefaultTableCellRenderer) tableCellRendererComponent).setHorizontalAlignment(DefaultTableCellRenderer.RIGHT);
                if ((table.getModel().getValueAt(table.convertRowIndexToModel(row), 4)).toString().equals("Uneditable")) {
                    setBackground(uneditable_color);
                } else {
                    setBackground(Color.WHITE);
                }
                return tableCellRendererComponent;
            }
        };
        results_table.getColumnModel().getColumn(0).setCellRenderer(boolean_renderer);
        results_table.getColumnModel().getColumn(1).setCellRenderer(default_renderer);
        results_table.getColumnModel().getColumn(2).setCellRenderer(date_renderer);
        results_table.getColumnModel().getColumn(3).setCellRenderer(size_renderer);
        results_table.getColumnModel().getColumn(4).setCellRenderer(default_renderer);
        results_table.getColumnModel().getColumn(5).setCellRenderer(default_renderer);
        results_table.getColumnModel().getColumn(6).setCellRenderer(default_renderer);
        results_table.setAutoCreateRowSorter(true);
        results_table.getModel().addTableModelListener(this);
    }

    /**
     * Updates the data in the current table model for the results table.
     * <p>
     * This needs to be run anytime a change is made to the users in the current
     * user list.
     */
    private void updateTableData() {
        for (int i = 0; i < ((DefaultTableModel) results_table.getModel()).getRowCount(); i++) {
            Object[] matching_user = null;
            for (Object[] user : profile_deleter.convertUserListTo2DObjectArray()) {
                if (user[1].toString().equals(results_table.getModel().getValueAt(i, 1).toString())) {
                    matching_user = user;
                    break;
                }
            }
            if (matching_user != null) {
                results_table.getModel().setValueAt(matching_user[0], i, 0);
                results_table.getModel().setValueAt(matching_user[2], i, 2);
                results_table.getModel().setValueAt(matching_user[3], i, 3);
                results_table.getModel().setValueAt(matching_user[4], i, 4);
                results_table.getModel().setValueAt(matching_user[5], i, 5);
                results_table.getModel().setValueAt(matching_user[6], i, 6);
            }
        }
    }

    /**
     * Overridden ActionListener function that runs the relevant functions based
     * on GUI elements pressed.
     *
     * @param e ActionEvent received from UI elements
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        switch (e.getActionCommand()) {
            case "LogWritten":
                if (system_console_text_area != null) {
                    writeLogToSystemConsole();
                }
                break;
            case "SetComputer":
                setComputerButton();
                break;
            case "RerunChecks":
                rerunChecksButton();
                break;
            case "RunDeletion":
                runDeletionButton();
                break;
            case "WriteLog":
                writeLogButton();
                break;
            case "SizeCheckToggle":
                sizeCheckCheckbox();
                break;
            case "StateCheckToggle":
                stateCheckCheckbox();
                break;
            case "RegistryCheckToggle":
                registryCheckCheckbox();
                break;
            case "DeleteAllUsersToggle":
                deleteAllUsersCheckbox();
                break;
            case "TooltipsToggle":
                show_tooltips = tooltips_checkbox.isSelected();
                ToolTipManager.sharedInstance().setEnabled(show_tooltips);
                break;
            case "Help":
                helpButton();
                break;
            case "Exit":
                exitButton();
                break;
            case "CopyDeletionReport":
                copyDeletionReportToClipboardButton();
                break;
        }
    }

    /**
     * Run when set computer button is pressed.
     * <p>
     * Enables/disables GUI elements as needed and runs the set computer
     * SwingWorker thread.
     */
    private void setComputerButton() {
        computer_name_text_field.setEnabled(false);
        set_computer_button.setEnabled(false);
        size_check_checkbox.setEnabled(false);
        state_check_checkbox.setEnabled(false);
        registry_check_checkbox.setEnabled(false);
        rerun_checks_button.setEnabled(false);
        delete_all_users_checkbox.setEnabled(false);
        run_deletion_button.setEnabled(false);
        write_log_button.setEnabled(false);
        results_table.setEnabled(false);
        (set_computer_thread = new setComputerThread()).execute();
    }

    /**
     * Run when rerun checks button is pressed.
     * <p>
     * Enables/disables GUI elements as needed and runs the rerun checks
     * SwingWorker thread.
     */
    private void rerunChecksButton() {
        computer_name_text_field.setEnabled(false);
        set_computer_button.setEnabled(false);
        size_check_checkbox.setEnabled(false);
        state_check_checkbox.setEnabled(false);
        registry_check_checkbox.setEnabled(false);
        rerun_checks_button.setEnabled(false);
        delete_all_users_checkbox.setEnabled(false);
        run_deletion_button.setEnabled(false);
        write_log_button.setEnabled(false);
        results_table.setEnabled(false);
        (rerun_checks_thread = new rerunChecksThread()).execute();
    }

    /**
     * Run when run deletion button is pressed.
     * <p>
     * Enables/disables GUI elements as needed and runs the run deletion
     * SwingWorker thread.
     */
    private void runDeletionButton() {
        computer_name_text_field.setEnabled(false);
        set_computer_button.setEnabled(false);
        size_check_checkbox.setEnabled(false);
        state_check_checkbox.setEnabled(false);
        registry_check_checkbox.setEnabled(false);
        rerun_checks_button.setEnabled(false);
        delete_all_users_checkbox.setEnabled(false);
        run_deletion_button.setEnabled(false);
        write_log_button.setEnabled(false);
        results_table.setEnabled(false);
        (run_deletion_thread = new runDeletionThread()).execute();
    }

    /**
     * Run when write log button is pressed.
     * <p>
     * Enables/disables GUI elements as needed and runs the write log
     * SwingWorker thread.
     */
    private void writeLogButton() {
        computer_name_text_field.setEnabled(false);
        set_computer_button.setEnabled(false);
        size_check_checkbox.setEnabled(false);
        state_check_checkbox.setEnabled(false);
        registry_check_checkbox.setEnabled(false);
        rerun_checks_button.setEnabled(false);
        delete_all_users_checkbox.setEnabled(false);
        run_deletion_button.setEnabled(false);
        write_log_button.setEnabled(false);
        results_table.setEnabled(false);
        (write_log_thread = new writeLogThread()).execute();
    }

    /**
     * Run when size check checkbox is pressed.
     * <p>
     * Changes size check attribute to the value of size check checkbox.
     */
    private void sizeCheckCheckbox() {
        profile_deleter.setSizeCheck(size_check_checkbox.isSelected());
    }

    /**
     * Run when state check checkbox is pressed.
     * <p>
     * Changes state check attribute to the value of state check checkbox.
     */
    private void stateCheckCheckbox() {
        profile_deleter.setStateCheck(state_check_checkbox.isSelected());
    }

    /**
     * Run when registry check checkbox is pressed.
     * <p>
     * Changes registry check attribute to the value of registry check checkbox.
     */
    private void registryCheckCheckbox() {
        profile_deleter.setRegistryCheck(registry_check_checkbox.isSelected());
    }

    /**
     * Run when delete all users checkbox is pressed.
     * <p>
     * Changes delete attribute of ProfileDeleter user list to the value of
     * delete all users checkbox.
     */
    private void deleteAllUsersCheckbox() {
        profile_deleter.setDeleteAllUsers(delete_all_users_checkbox.isSelected());
        updateTableData();
    }

    /**
     * Run when help button is pressed.
     * <p>
     * Display help message.
     */
    private void helpButton() {
        help_frame.setVisible(!help_frame.isVisible());
        /*
        profile_deleter.setRemoteComputer("CITSZTESTPC0001");
        List<String> deletion_report = new ArrayList<>();
        deletion_report.add("Deletion Report");
        deletion_report.add("User\tDeleted Successfully?\tFolder Deleted?\tSID Deleted?\tGUID Deleted?\tSID\tGUID\tSize");
        deletion_report.add("test1\tYes\tYes\tYes\tYes\tS-1-1-1234\t{abcdefg}\t1089765");
        deletion_report.add("test2\tNo\tUser logged in\tYes\tYes\tS-1-1-7654\t{lmnopqr}\t53827354");
        deletion_report.add("test3\tYes\tYes\tYes\tYes\tS-1-1-4321\t{mcgsjry}\t2736253465");
        deletion_report.add("test4\tYes\tYes\tSID is blank\tGUID is blank\t\t\t12280089012");
        deletion_report.add("test5\tYes\tYes\tYes\tYes\tS-1-1-3254\t{cjsufht}\t5273167");
        deletion_report.add("test6\tNo\tYes\tFailed to delete\tFailed to delete\tS-1-1-3509\t{plkjhtr}\t102789345");
        displayDeletionReport(deletion_report);
        */
    }

    /**
     * Run when exit button is pressed.
     * <p>
     * Closes the application.
     */
    private void exitButton() {
        System.exit(0);
    }
    
    /**
     * Run when the copy deletion report to clipboard button is pressed on the deletion report JFrame.
     * <p>
     * Copies the deletion report to the clipboard
     */
    private void copyDeletionReportToClipboardButton() {
        if (deletion_report_string != null && !deletion_report_string.isEmpty()) {
            StringSelection selection = new StringSelection(deletion_report_string);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
            profile_deleter.logMessage("Copied deletion report to clipboard", ProfileDeleter.LOG_TYPE.INFO, true);
        }
    }

    /**
     * Appends log to system console when ProfileDeleter log is updated.
     */
    private void writeLogToSystemConsole() {
        system_console_text_area.append('\n' + profile_deleter.getLogList().get(profile_deleter.getLogList().size() - 1));
    }

    /**
     * Displays the deletion report from ProfileDeleter process deletion function on a new JFrame using a JTable to display the data.
     *
     * @param deletion_report the deletion report returned from ProfileDeleter
     * after a deletion is processed
     */
    private void displayDeletionReport(List<String> deletion_report) {
        if(deletion_report.size() > 2) {
            String deletion_report_header_as_string = "";
            String deletion_report_headings_as_string = "";
            String deletion_report_content_as_string = "";
            Double total_size = 0.0;
            deletion_report_frame.setTitle("Deletion Report - " + profile_deleter.getRemoteComputer());
            String[] deletion_report_headings = new String[deletion_report.get(1).split("\t").length];
            Object[][] deletion_report_content = new Object[deletion_report.size()-2][];
            for (int i = 0; i < deletion_report.size(); i++) {
                if(i > 0) {
                    if(i == 1) {
                        deletion_report_headings = deletion_report.get(i).split("\t");
                        deletion_report_headings_as_string += deletion_report.get(i);
                    } else {
                        deletion_report_content[i-2] = deletion_report.get(i).split("\t");
                        String size_formatted = "";
                        if(deletion_report_content[i-2][7] != null && !deletion_report_content[i-2][7].toString().isEmpty()) {
                            if(deletion_report_content[i-2][7].toString().compareTo("Not calculated") != 0) {
                                total_size += Double.parseDouble(deletion_report_content[i-2][7].toString());
                                size_formatted = doubleToFormattedString(Double.parseDouble(deletion_report_content[i-2][7].toString()) / (1024 * 1024)) + " MB";
                            } else {
                                size_formatted = deletion_report_content[i-2][7].toString();
                            }
                        }
                        deletion_report_content_as_string += deletion_report_content[i-2][0].toString() + '\t' + deletion_report_content[i-2][1].toString() + '\t' + deletion_report_content[i-2][2].toString() + '\t' + deletion_report_content[i-2][3].toString() + '\t' + deletion_report_content[i-2][4].toString() + '\t' + deletion_report_content[i-2][5].toString() + '\t' + deletion_report_content[i-2][6].toString() + '\t' + size_formatted;
                        if(i != deletion_report.size()-1) {
                            deletion_report_content_as_string += '\n';
                        }
                    }
                } else {
                    deletion_report_header_as_string += deletion_report.get(i) + '\n';
                }
            }
            String total_size_formatted = "";
            if(total_size > 0.0) {
                total_size_formatted = doubleToFormattedString(total_size / (1024 * 1024)) + " MB";
            } else {
                total_size_formatted = "Not calculated";
            }
            deletion_report_frame_computer_label.setText("<html><strong>Computer:</strong> " + profile_deleter.getRemoteComputer() + "<br>" + "<strong>Total Size Deleted:</strong> " + total_size_formatted + "</html>");
            deletion_report_header_as_string += "Computer:" + '\t' + profile_deleter.getRemoteComputer() + '\n';
            deletion_report_header_as_string += "Total Size Deleted:" + '\t' + total_size_formatted + '\n';
            deletion_report_string = deletion_report_header_as_string + deletion_report_headings_as_string + '\n' + deletion_report_content_as_string;
            
            // Default renderer for table columns.
            TableCellRenderer default_renderer = new DefaultTableCellRenderer() {

                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean is_selected, boolean has_focus, int row, int column) {
                    Component tableCellRendererComponent = super.getTableCellRendererComponent(table, value, is_selected, has_focus, row, column);
                    if (!(table.getModel().getValueAt(table.convertRowIndexToModel(row), 1)).toString().equals("Yes")) {
                        tableCellRendererComponent.setForeground(Color.WHITE);
                        tableCellRendererComponent.setBackground(Color.DARK_GRAY);
                    } else {
                        
                        tableCellRendererComponent.setForeground(Color.BLACK);
                        tableCellRendererComponent.setBackground(Color.WHITE);
                    }
                    return tableCellRendererComponent;
                }
            };
            // Determines how the Successfully Deleted? column shoud be displayed.
            TableCellRenderer deletion_successful_renderer = new DefaultTableCellRenderer() {

                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean is_selected, boolean has_focus, int row, int column) {
                    Component tableCellRendererComponent = super.getTableCellRendererComponent(table, value, is_selected, has_focus, row, column);
                    if (!(table.getModel().getValueAt(table.convertRowIndexToModel(row), 1)).toString().equals("Yes")) {
                        tableCellRendererComponent.setForeground(Color.WHITE);
                        tableCellRendererComponent.setBackground(Color.DARK_GRAY);
                    } else {
                        tableCellRendererComponent.setForeground(Color.BLACK);
                        tableCellRendererComponent.setBackground(uneditable_color);
                    }
                    return tableCellRendererComponent;
                }
            };
            // Determines how the Size column shoud be displayed.
            TableCellRenderer size_renderer = new DefaultTableCellRenderer() {
                boolean right_align = true;
                
                @Override
                public void setValue(Object value) {
                    String output = "";
                    if (value != null && !value.toString().isEmpty()) {
                        if(value.toString().compareTo("Not calculated") != 0) {
                            output = doubleToFormattedString(Double.parseDouble(value.toString()) / (1024.0 * 1024.0)) + " MB";
                        } else {
                            output = value.toString();
                            right_align = false;
                        }
                    }
                    setText(output);
                }
                
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean is_selected, boolean has_focus, int row, int column) {
                    Component tableCellRendererComponent = super.getTableCellRendererComponent(table, value, is_selected, has_focus, row, column);
                    if (!(table.getModel().getValueAt(table.convertRowIndexToModel(row), 1)).toString().equals("Yes")) {
                        tableCellRendererComponent.setForeground(Color.WHITE);
                        tableCellRendererComponent.setBackground(Color.DARK_GRAY);
                    } else {
                        tableCellRendererComponent.setForeground(Color.BLACK);
                        tableCellRendererComponent.setBackground(Color.WHITE);
                    }
                    if(right_align) {
                        ((DefaultTableCellRenderer) tableCellRendererComponent).setHorizontalAlignment(DefaultTableCellRenderer.RIGHT);
                    } else {
                        ((DefaultTableCellRenderer) tableCellRendererComponent).setHorizontalAlignment(DefaultTableCellRenderer.LEFT);
                    }
                    return tableCellRendererComponent;
                }
            };
            
            deletion_report_frame_table.setModel(new DefaultTableModel(deletion_report_content, deletion_report_headings));
            deletion_report_frame_table.getColumnModel().getColumn(0).setCellRenderer(default_renderer);
            deletion_report_frame_table.getColumnModel().getColumn(1).setCellRenderer(deletion_successful_renderer);
            deletion_report_frame_table.getColumnModel().getColumn(2).setCellRenderer(default_renderer);
            deletion_report_frame_table.getColumnModel().getColumn(3).setCellRenderer(default_renderer);
            deletion_report_frame_table.getColumnModel().getColumn(4).setCellRenderer(default_renderer);
            deletion_report_frame_table.getColumnModel().getColumn(5).setCellRenderer(default_renderer);
            deletion_report_frame_table.getColumnModel().getColumn(6).setCellRenderer(default_renderer);
            deletion_report_frame_table.getColumnModel().getColumn(7).setCellRenderer(size_renderer);
            deletion_report_frame_table.setAutoCreateRowSorter(true);
            deletion_report_frame.setVisible(true);
        }
    }

    /**
     * Overridden from TableModelListener.
     * <p>
     * Used to track changes to the results table JTable.
     *
     * @param e the event detailing the change in table model
     */
    @Override
    public void tableChanged(TableModelEvent e) {
        int row = e.getFirstRow();
        int column = e.getColumn();
        TableModel model = (TableModel) e.getSource();
        Object data = model.getValueAt(row, column);

        if (column == 0) {
            profile_deleter.getUserList().get(row).setDelete(Boolean.parseBoolean(data.toString()));
        }
    }

    /**
     * SwingWorker thread used to run the setComputer function from the GUI.
     */
    private class setComputerThread extends SwingWorker<Object, Object> {

        boolean ping_success = false;

        @Override
        protected Object doInBackground() throws Exception {
            ping_success = profile_deleter.pingPC(computer_name_text_field.getText());
            if (ping_success) {
                profile_deleter.setSizeCheckComplete(false);
                profile_deleter.setStateCheckComplete(false);
                profile_deleter.setRegistryCheckComplete(false);
                profile_deleter.setRemoteComputer(computer_name_text_field.getText());
                profile_deleter.generateUserList();
                profile_deleter.checkAll();
                if (profile_deleter.getSizeCheckComplete()) {
                    double total_size = 0.0;
                    for (UserData user : profile_deleter.getUserList()) {
                        total_size += Double.parseDouble(user.getSize());
                    }
                    setTitle("Profile Deleter - " + profile_deleter.getRemoteComputer() + " - Total Users Size: " + doubleToFormattedString(total_size / (1024.0 * 1024.0)) + " MB");
                } else {
                    setTitle("Profile Deleter - " + profile_deleter.getRemoteComputer());
                }
            } else {
                profile_deleter.logMessage("Unable to ping computer, computer not set", ProfileDeleter.LOG_TYPE.WARNING, true);
            }
            return new Object();
        }

        @Override
        public void done() {
            createTableData();
            if (ping_success || (profile_deleter.getRemoteComputer() != null && !profile_deleter.getRemoteComputer().isEmpty())) {
                rerun_checks_button.setEnabled(true);
            }
            if (profile_deleter.getStateCheckComplete() && profile_deleter.getRegistryCheckComplete()) {
                run_deletion_button.setEnabled(true);
            }
            computer_name_text_field.setEnabled(true);
            set_computer_button.setEnabled(true);
            size_check_checkbox.setEnabled(true);
            state_check_checkbox.setEnabled(true);
            registry_check_checkbox.setEnabled(true);
            delete_all_users_checkbox.setEnabled(true);
            write_log_button.setEnabled(true);
            results_table.setEnabled(true);
        }
    }

    /**
     * SwingWorker thread used to run the checkAll function from the GUI.
     */
    private class rerunChecksThread extends SwingWorker<Object, Object> {

        @Override
        protected Object doInBackground() throws Exception {
            if (profile_deleter.getRemoteComputer() != null && !profile_deleter.getRemoteComputer().isEmpty()) {
                profile_deleter.checkAll();
                if (profile_deleter.getSizeCheckComplete()) {
                    double total_size = 0.0;
                    for (UserData user : profile_deleter.getUserList()) {
                        total_size += Double.parseDouble(user.getSize());
                    }
                    setTitle("Profile Deleter - " + profile_deleter.getRemoteComputer() + " - Total Users Size: " + doubleToFormattedString(total_size / (1024.0 * 1024.0)) + " MB");
                }
            }
            return new Object();
        }

        @Override
        public void done() {
            updateTableData();
            if (profile_deleter.getStateCheckComplete() && profile_deleter.getRegistryCheckComplete()) {
                run_deletion_button.setEnabled(true);
            }
            rerun_checks_button.setEnabled(true);
            computer_name_text_field.setEnabled(true);
            set_computer_button.setEnabled(true);
            size_check_checkbox.setEnabled(true);
            state_check_checkbox.setEnabled(true);
            registry_check_checkbox.setEnabled(true);
            delete_all_users_checkbox.setEnabled(true);
            write_log_button.setEnabled(true);
            results_table.setEnabled(true);
        }
    }

    /**
     * SwingWorker thread used to run the writeLog function from the GUI.
     */
    private class writeLogThread extends SwingWorker<Object, Object> {

        @Override
        protected Object doInBackground() throws Exception {
            try {
                profile_deleter.logMessage("Successfully wrote log to file " + profile_deleter.writeLog(), ProfileDeleter.LOG_TYPE.INFO, true);
            } catch (IOException | NotInitialisedException e) {
                profile_deleter.logMessage("Failed to write log to file. Error is " + e.getMessage(), ProfileDeleter.LOG_TYPE.ERROR, true);
            }
            return new Object();
        }

        @Override
        public void done() {
            if (profile_deleter.getStateCheckComplete() && profile_deleter.getRegistryCheckComplete()) {
                run_deletion_button.setEnabled(true);
            }
            rerun_checks_button.setEnabled(true);
            computer_name_text_field.setEnabled(true);
            set_computer_button.setEnabled(true);
            size_check_checkbox.setEnabled(true);
            state_check_checkbox.setEnabled(true);
            registry_check_checkbox.setEnabled(true);
            delete_all_users_checkbox.setEnabled(true);
            write_log_button.setEnabled(true);
            results_table.setEnabled(true);
        }
    }

    /**
     * SwingWorker thread used to run the processDeletion function from the GUI.
     */
    private class runDeletionThread extends SwingWorker<Object, Object> {

        @Override
        protected Object doInBackground() throws Exception {
            List<String> deleted_users = profile_deleter.processDeletion();
            deleted_users.add(0, "Deletion report:");
            if (deleted_users.size() > 2) {
                for (String deleted_user : deleted_users) {
                    system_console_text_area.append('\n' + deleted_user);
                }
                displayDeletionReport(deleted_users);
                if (profile_deleter.getSizeCheckComplete()) {
                    double total_size = 0.0;
                    for (UserData user : profile_deleter.getUserList()) {
                        total_size += Double.parseDouble(user.getSize());
                    }
                    setTitle("Profile Deleter - " + profile_deleter.getRemoteComputer() + " - Total Users Size: " + doubleToFormattedString(total_size / (1024.0 * 1024.0)) + " MB");
                }
            } else {
                profile_deleter.logMessage("Nothing was flagged for deletion", ProfileDeleter.LOG_TYPE.WARNING, true);
            }
            return new Object();
        }

        @Override
        public void done() {
            createTableData();
            if (profile_deleter.getStateCheckComplete() && profile_deleter.getRegistryCheckComplete()) {
                run_deletion_button.setEnabled(true);
            }
            rerun_checks_button.setEnabled(true);
            computer_name_text_field.setEnabled(true);
            set_computer_button.setEnabled(true);
            size_check_checkbox.setEnabled(true);
            state_check_checkbox.setEnabled(true);
            registry_check_checkbox.setEnabled(true);
            delete_all_users_checkbox.setEnabled(true);
            write_log_button.setEnabled(true);
            results_table.setEnabled(true);
        }
    }
}
