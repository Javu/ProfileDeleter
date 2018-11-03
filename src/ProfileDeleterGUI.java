
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
import java.io.IOException;
import java.util.List;
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

/**
 * Implementation of a Swing GUI for the ProfileDeleter class.
 */
public class ProfileDeleterGUI extends JFrame implements TableModelListener, ActionListener {

    // The ProfileDeleter class that handles all the logic of the application.
    private ProfileDeleter profile_deleter;

    /**
     * Swing GUI elements.
     */
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

    /**
     * SwingWorker threads for GUI.
     */
    private setComputerThread set_computer_thread;
    private rerunChecksThread rerun_checks_thread;
    private runDeletionThread run_deletion_thread;
    private writeLogThread write_log_thread;

    public static void main(String args[]) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new ProfileDeleterGUI();
            }
        });
    }

    public ProfileDeleterGUI() {
        super("Profile Deleter");

        // The ProfileDeleter class that handles all the logic of the application.
        profile_deleter = new ProfileDeleter(this);

        // Configurations for the top level JFrame of the GUI.
        setMinimumSize(new Dimension(950, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(new GridBagLayout());

        // Column header tooltips in the results table JTable GUI element.
        String[] columnToolTips = {
            "Cannot delete if state is not Editable and cannot delete the Public account",
            null,
            null,
            null,
            null,
            null,
            null
        };

        // Initialisation of results table JTable GUI element.
        results_table = new JTable(new DefaultTableModel()) {
            public String getToolTipText(MouseEvent e) {
                String tip = null;
                java.awt.Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);
                int realColumnIndex = convertColumnIndexToModel(colIndex);
                int realRowIndex = convertRowIndexToModel(rowIndex);

                if (realColumnIndex == 0) {
                    TableModel model = getModel();
                    String editable = (String) model.getValueAt(realRowIndex, 4);
                    String name = (String) model.getValueAt(realRowIndex, 1);
                    if (editable.compareTo("Editable") != 0 || profile_deleter.getCannotDeleteList().contains(name)) {
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
        updateTableData();
        results_scroll_pane = new JScrollPane(results_table);
        results_scroll_pane.setBorder(new LineBorder(Color.BLACK, 2));
        results_gc = new GridBagConstraints();
        results_gc.fill = GridBagConstraints.BOTH;
        results_gc.gridx = 0;
        results_gc.gridy = 1;
        results_gc.gridwidth = GridBagConstraints.REMAINDER;
        results_gc.weighty = 1;

        // Initialisation of system console GUI element.
        system_console_text_area = new JTextArea("System Console");
        system_console_text_area.setEditable(false);
        system_console_text_area.setBackground(Color.BLACK);
        system_console_text_area.setForeground(Color.WHITE);
        system_console_text_area.setSelectedTextColor(Color.YELLOW);
        system_console_text_area.setMargin(new Insets(0, 2, 0, 0));
        system_console_scroll_pane = new JScrollPane(system_console_text_area);
        system_console_scroll_pane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
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
        computer_name_gc = new GridBagConstraints();
        computer_name_gc.fill = GridBagConstraints.BOTH;
        computer_name_gc.gridx = 0;
        computer_name_gc.gridy = 0;
        computer_name_gc.gridwidth = 1;
        computer_name_gc.gridheight = 1;
        computer_name_gc.weightx = 1;
        computer_name_gc.insets = new Insets(2, 2, 2, 2);

        // Initialisation of set computer button GUI element.
        set_computer_button = new JButton("Set Computer");
        set_computer_button.setActionCommand("setComputer");
        set_computer_button.addActionListener(this);
        set_computer_gc = new GridBagConstraints();
        set_computer_gc.fill = GridBagConstraints.BOTH;
        set_computer_gc.gridx = 1;
        set_computer_gc.gridy = 0;
        set_computer_gc.gridwidth = 1;
        set_computer_gc.gridheight = 1;

        // Initialisation of size check checkbox GUI element.
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

        // Initialisation of state check checkbox GUI element.
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

        // Initialisation of registry check checkbox GUI element.
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

        // Initialisation of rerun checks button GUI element.
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

        // Initialisation of run deletion button GUI element.
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

        // Initialisation of write log button GUI element.
        write_log_button = new JButton("Write Log");
        write_log_button.setActionCommand("writeLog");
        write_log_button.addActionListener(this);
        write_log_gc = new GridBagConstraints();
        write_log_gc.fill = GridBagConstraints.BOTH;
        write_log_gc.gridx = 7;
        write_log_gc.gridy = 0;
        write_log_gc.gridwidth = 1;
        write_log_gc.gridheight = 1;

        // Initialisation of exit button GUI element.
        exit_button = new JButton("Exit");
        exit_button.setActionCommand("Exit");
        exit_button.addActionListener(this);
        exit_gc = new GridBagConstraints();
        exit_gc.fill = GridBagConstraints.BOTH;
        exit_gc.gridx = 8;
        exit_gc.gridy = 0;
        exit_gc.gridwidth = 1;
        exit_gc.gridheight = 1;

        // Add all GUI elements to top level JFrame and display the GUI.
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
     * Refreshes the data in the results table JTable.
     * <p>
     * This needs to be run anytime the user list attribute is changed otherwise
     * the data in the results table will be outdated.
     */
    public void updateTableData() {
        results_table.getModel().removeTableModelListener(this);
        results_table.setModel(new AbstractTableModel() {
            private String[] columnNames = UserData.headingsToStringArray();
            private Object[][] rowData = profile_deleter.convertUserListTo2DObjectArray();

            public String getColumnName(int col) {
                return columnNames[col].toString();
            }

            public int getRowCount() {
                return rowData.length;
            }

            public int getColumnCount() {
                return columnNames.length;
            }

            public Object getValueAt(int row, int col) {
                return rowData[row][col];
            }

            public Class getColumnClass(int col) {
                if (col == 0) {
                    try {
                        return Class.forName("java.lang.Boolean");
                    } catch (ClassNotFoundException ex) {
                        System.out.println("Couldn't find class");
                        try {
                            return Class.forName("java.lang.String");
                        } catch (ClassNotFoundException ex1) {
                            System.out.println("Couldn't find class");
                        }
                    }
                } else {
                    try {
                        return Class.forName("java.lang.String");
                    } catch (ClassNotFoundException ex) {
                        System.out.println("Couldn't find class");
                    }
                }
                return null;
            }

            public boolean isCellEditable(int row, int col) {
                if (col == 0 && getValueAt(row, 4) == "Editable" && !profile_deleter.getCannotDeleteList().contains(getValueAt(row, 1))) {
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
    }

    /**
     * Overridden ActionListener function that runs the relevant functions based
     * on GUI elements pressed.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if ("LogWritten" == e.getActionCommand()) {
            writeLogToSystemConsole();
        } else if ("setComputer" == e.getActionCommand()) {
            setComputerButton();
        } else if ("RerunChecks" == e.getActionCommand()) {
            rerunChecksButton();
        } else if ("RunDeletion" == e.getActionCommand()) {
            runDeletionButton();
        } else if ("writeLog" == e.getActionCommand()) {
            writeLogButton();
        } else if ("SizeCheckToggle" == e.getActionCommand()) {
            sizeCheckCheckbox();
        } else if ("StateCheckToggle" == e.getActionCommand()) {
            stateCheckCheckbox();
        } else if ("RegistryCheckToggle" == e.getActionCommand()) {
            registryCheckCheckbox();
        } else if ("Exit" == e.getActionCommand()) {
            exitButton();
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
     * Run when exit button is pressed.
     * <p>
     * Closes the application.
     */
    private void exitButton() {
        System.exit(0);
    }

    /**
     * Appends log to system console when ProfileDeleter log is updated.
     */
    private void writeLogToSystemConsole() {
        system_console_text_area.append('\n' + profile_deleter.getLogList().get(profile_deleter.getLogList().size() - 1));
    }

    /**
     * Overridden from TableModelListener.
     * <p>
     * Used to track changes to the results table JTable.
     */
    @Override
    public void tableChanged(TableModelEvent e) {
        int row = e.getFirstRow();
        int column = e.getColumn();
        TableModel model = (TableModel) e.getSource();
        Object data = model.getValueAt(row, column);

        profile_deleter.getUserList().get(row).setDelete(Boolean.parseBoolean(data.toString()));
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
                profile_deleter.setComputer(computer_name_text_field.getText());
                profile_deleter.generateUserList();
                profile_deleter.checkAll();
                if (profile_deleter.getSizeCheckComplete()) {
                    double total_size = 0.0;
                    for (UserData user : profile_deleter.getUserList()) {
                        total_size += Double.parseDouble(user.getSize());
                    }
                    setTitle("Profile Deleter - " + profile_deleter.getComputer() + " - Total Users Size: " + Math.round(total_size / (1024.0 * 1024.0)) + "MB");
                } else {
                    setTitle("Profile Deleter - " + profile_deleter.getComputer());
                }
                updateTableData();
            } else {
                profile_deleter.logMessage("Unable to ping computer, computer not set", ProfileDeleter.LOG_TYPE.WARNING, true);
            }
            return new Object();
        }

        @Override
        public void done() {
            if (ping_success || (profile_deleter.getComputer() != null && !profile_deleter.getComputer().isEmpty())) {
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
            if (profile_deleter.getComputer() != null && !profile_deleter.getComputer().isEmpty()) {
                profile_deleter.checkAll();
                if (profile_deleter.getSizeCheckComplete()) {
                    double total_size = 0.0;
                    for (UserData user : profile_deleter.getUserList()) {
                        total_size += Double.parseDouble(user.getSize());
                    }
                    setTitle("Profile Deleter - " + profile_deleter.getComputer() + " - Total Users Size: " + Math.round(total_size / (1024.0 * 1024.0)) + "MB");
                }
                updateTableData();
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
            try {
                List<String> deleted_users = profile_deleter.processDeletion();
                deleted_users.add(0, "Deletion report:");
                if (deleted_users.size() > 2) {
                    for (String deleted_user : deleted_users) {
                        system_console_text_area.append('\n' + deleted_user);
                    }
                    String suffix = profile_deleter.generateDateString();
                    profile_deleter.writeToFile("reports\\" + profile_deleter.getComputer() + "_deletion_report_" + suffix + ".txt", deleted_users);
                    profile_deleter.logMessage("Deletion report written to file reports\\" + profile_deleter.getComputer() + "_deletion_report_" + suffix + ".txt", ProfileDeleter.LOG_TYPE.INFO, true);
                    updateTableData();
                    if (profile_deleter.getSizeCheckComplete()) {
                        double total_size = 0.0;
                        for (UserData user : profile_deleter.getUserList()) {
                            total_size += Double.parseDouble(user.getSize());
                        }
                        setTitle("Profile Deleter - " + profile_deleter.getComputer() + " - Total Users Size: " + Math.round(total_size / (1024.0 * 1024.0)) + "MB");
                    }
                } else {
                    profile_deleter.logMessage("Nothing was flagged for deletion", ProfileDeleter.LOG_TYPE.WARNING, true);
                }
            } catch (NotInitialisedException | IOException e) {
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
            write_log_button.setEnabled(true);
            results_table.setEnabled(true);
        }
    }
}
