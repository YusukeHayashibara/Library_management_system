//
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class LoanPanel extends JPanel {

    private Connection connection;
    private JTable table;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private JFrame mainFrame;

    public LoanPanel(Connection connection, JFrame mainFrame) {
        this.connection = connection;
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        initUI();
    }

    private void initUI() {
        tableModel = new DefaultTableModel(new Object[]{"Book ID", "Patron ID", "Loan Date", "Return Date", "Returned"}, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                return column == 4 ? Boolean.class : super.getColumnClass(column);
            }
        };
        table = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        loadLoanData();

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BorderLayout());

        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Add");
        JButton deleteButton = new JButton("Delete");
        JButton updateButton = new JButton("Update");
        JButton backButton = new JButton("Back");

        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(backButton);

        controlPanel.add(buttonPanel, BorderLayout.SOUTH);

        JTextField searchField = new JTextField();
        JButton searchButton = new JButton("Search");

        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BorderLayout());
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        controlPanel.add(searchPanel, BorderLayout.NORTH);

        add(controlPanel, BorderLayout.SOUTH);

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addNewLoan();
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedRow();
            }
        });

        updateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateSelectedRow();
            }
        });

        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainFrame.setVisible(true);
                ((JFrame) SwingUtilities.getWindowAncestor(LoanPanel.this)).dispose();
            }
        });

        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchLoan(searchField.getText());
            }
        });
    }

    private void loadLoanData() {
        tableModel.setRowCount(0); // Clear existing data
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM loan")) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("id_book"),
                        rs.getInt("id_patron"),
                        rs.getDate("loan_date"),
                        rs.getDate("return_date"),
                        rs.getBoolean("returned")
                });
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteSelectedRow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow != -1) {
            int bookId = (int) tableModel.getValueAt(selectedRow, 0);
            int patronId = (int) tableModel.getValueAt(selectedRow, 1);
            java.sql.Date loanDate = (java.sql.Date) tableModel.getValueAt(selectedRow, 2);

            try (Statement stmt = connection.createStatement()) {
                String query = String.format("DELETE FROM loan WHERE id_book=%d AND id_patron=%d AND loan_date='%s'", bookId, patronId, loanDate);
                stmt.executeUpdate(query);
                tableModel.removeRow(selectedRow);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateSelectedRow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow != -1) {
            int bookId = (int) tableModel.getValueAt(selectedRow, 0);
            int patronId = (int) tableModel.getValueAt(selectedRow, 1);
            java.sql.Date loanDate = (java.sql.Date) tableModel.getValueAt(selectedRow, 2);
            java.sql.Date returnDate = (java.sql.Date) tableModel.getValueAt(selectedRow, 3);
            boolean returned = (boolean) tableModel.getValueAt(selectedRow, 4);

            JTextField bookIdField = new JTextField(String.valueOf(bookId));
            JTextField patronIdField = new JTextField(String.valueOf(patronId));
            JTextField loanDateField = new JTextField(String.valueOf(loanDate));
            JTextField returnDateField = new JTextField(String.valueOf(returnDate));
            JCheckBox returnedCheckBox = new JCheckBox("Returned", returned);

            JPanel panel = new JPanel(new GridLayout(0, 1));
            panel.add(new JLabel("Book ID:"));
            panel.add(bookIdField);
            panel.add(new JLabel("Patron ID:"));
            panel.add(patronIdField);
            panel.add(new JLabel("Loan Date:"));
            panel.add(loanDateField);
            panel.add(new JLabel("Return Date:"));
            panel.add(returnDateField);
            panel.add(returnedCheckBox);

            int result = JOptionPane.showConfirmDialog(this, panel, "Update Loan", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                int newBookId = Integer.parseInt(bookIdField.getText());
                int newPatronId = Integer.parseInt(patronIdField.getText());
                java.sql.Date newLoanDate = java.sql.Date.valueOf(loanDateField.getText());
                java.sql.Date newReturnDate = java.sql.Date.valueOf(returnDateField.getText());
                boolean newReturned = returnedCheckBox.isSelected();

                try (Statement stmt = connection.createStatement()) {
                    String query = String.format("UPDATE loan SET id_book=%d, id_patron=%d, loan_date='%s', return_date='%s', returned=%b WHERE id_book=%d AND id_patron=%d AND loan_date='%s'",
                            newBookId, newPatronId, newLoanDate, newReturnDate, newReturned, bookId, patronId, loanDate);
                    stmt.executeUpdate(query);
                    tableModel.setValueAt(newBookId, selectedRow, 0);
                    tableModel.setValueAt(newPatronId, selectedRow, 1);
                    tableModel.setValueAt(newLoanDate, selectedRow, 2);
                    tableModel.setValueAt(newReturnDate, selectedRow, 3);
                    tableModel.setValueAt(newReturned, selectedRow, 4);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void addNewLoan() {
        JTextField bookIdField = new JTextField();
        JTextField patronIdField = new JTextField();
        JTextField loanDateField = new JTextField();
        JTextField returnDateField = new JTextField();
        JCheckBox returnedCheckBox = new JCheckBox("Returned");

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Book ID:"));
        panel.add(bookIdField);
        panel.add(new JLabel("Patron ID:"));
        panel.add(patronIdField);
        panel.add(new JLabel("Loan Date (YYYY-MM-DD):"));
        panel.add(loanDateField);
        panel.add(new JLabel("Return Date (YYYY-MM-DD):"));
        panel.add(returnDateField);
        panel.add(returnedCheckBox);

        int result = JOptionPane.showConfirmDialog(null, panel, "Add Loan", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                int bookId = Integer.parseInt(bookIdField.getText());
                int patronId = Integer.parseInt(patronIdField.getText());
                Date loanDate = Date.valueOf(loanDateField.getText());
                Date returnDate = returnDateField.getText().isEmpty() ? null : Date.valueOf(returnDateField.getText());
                boolean returned = returnedCheckBox.isSelected();

                String query = String.format("INSERT INTO loan (id_book, id_patron, loan_date, return_date, returned) VALUES (%d, %d, '%s', %s, %b)", bookId, patronId, loanDate, returnDate == null ? "NULL" : "'" + returnDate + "'", returned);

                try (Statement stmt = connection.createStatement()) {
                    stmt.executeUpdate(query);
                    loadLoanData(); // Refresh table data
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(null, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Please enter valid numbers for Book ID and Patron ID.", "Invalid Input", JOptionPane.WARNING_MESSAGE);
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(null, "Please enter valid dates in the format YYYY-MM-DD.", "Invalid Date Format", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void searchLoan(String query) {
        tableModel.setRowCount(0); // Clear existing data

        String sql = "SELECT * FROM loan WHERE CAST(id_book AS TEXT) ILIKE '%" + query + "%' OR CAST(id_patron AS TEXT) ILIKE '%" + query + "%'";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("id_book"),
                        rs.getInt("id_patron"),
                        rs.getDate("loan_date"),
                        rs.getDate("return_date"),
                        rs.getBoolean("returned")
                });
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
