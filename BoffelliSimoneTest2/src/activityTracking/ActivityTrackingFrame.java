package activityTracking;

import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author simone.boffelli
 */
public class ActivityTrackingFrame extends javax.swing.JFrame {

    //costanti interne per definire le colonne della jTable
    private static final int ACTIVITY_COLUMN_ID = 0;
    private static final int DESCRIPTION_COLUMN_ID = 1;
    private static final int DEADLINE_COLUMN_ID = 2;

    //costanti interne per definire le opzioni di riordinamento
    private static final int ACTIVITY_DOWN_ID = 0;
    private static final int ACTIVITY_UP_ID = 1;
    private static final int DESCRIPTION_DOWN_ID = 2;
    private static final int DESCRIPTION_UP_ID = 3;
    private static final int DEADLINE_DOWN_ID = 4;
    private static final int DEADLINE_UP_ID = 5;
    private static final int STATE_DOWN_ID = 6;
    private static final int STATE_UP_ID = 7;

    //variabile per gestire la connessione al db
    private Connection conn;

    /**
     * Creates new form ActivityTrackingFrame
     */
    public ActivityTrackingFrame() {
        initComponents(); //ciamata al metodo per inizializzare i componenti della gui

        this.openDB(); //chiamata al metodo per aprire la connessione con il db

        // Creazione e configurazione del modello di selezione per una tabella (JTable).
        ListSelectionModel selectionModel = this.activityTable.getSelectionModel();
        selectionModel.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {

                // Se l'evento di selezione è in corso di modifica, non fare nulla.
                if (e.getValueIsAdjusting()) {
                    return;
                }

                // Ottiene l'indice della riga selezionata.
                int selRow = activityTable.getSelectedRow();

                // Se nessuna riga è selezionata, non fare nulla.
                if (selRow < 0) {
                    return;
                }

                // Ottiene il modello della tabella e lo casta a DefaultTableModel.
                DefaultTableModel dtm = (DefaultTableModel) activityTable.getModel();

                // Imposta i valori dei campi di testo in base alla riga selezionata nella tabella.
                activityTextField.setText((String) dtm.getValueAt(selRow,
                        ACTIVITY_COLUMN_ID));
                descriptionTextField.setText((String) dtm.getValueAt(selRow,
                        DESCRIPTION_COLUMN_ID));
                deadlineTextField.setText((String) dtm.getValueAt(selRow,
                        DEADLINE_COLUMN_ID));

                // Chiamata a un metodo per aggiornare l'interfaccia grafica.
                updateGUI();
            }
            // Chiusura della definizione del ListSelectionListener.
        });

        // Chiamata a un metodo per popolare la tabella con dati 
        this.populateTable();
    }

    //----------------------- METODI ------------------------------------------------
    private void updateGUI() {
        //ottiene l'indice della riga selezionata della tabella
        int selRow = this.activityTable.getSelectedRow();

        //ottiene il conteggio tatale delle righe della tabella
        int rowCount = this.activityTable.getRowCount();

        //controlla se i campi di testo non sono vuoti
        boolean isValidEntity = !this.activityTextField.getText().isBlank()
                && !this.descriptionTextField.getText().isBlank()
                && !this.deadlineTextField.getText().isBlank();

        //abilita il pulsante add solo se tutti i campi di testo sono validi
        this.addButton.setEnabled(isValidEntity);

        //abilita il pulsante delete solo se c'e' una righa selezionata nella tabella
        this.deleteButton.setEnabled(selRow >= 0);

        //abilita il pulsante deleteAll solo se ci sono righe nella tabella
        this.deleteAllButton.setEnabled(rowCount > 0);

    }

    private void openDB() {
        try {
            // Carica la classe del driver JDBC per SQLite
            Class.forName("org.sqlite.JDBC");

            // Stabilisce una connessione con il database SQLite specificato
            this.conn = DriverManager.getConnection("jdbc:sqlite:ActivityTracking.db");

            // Crea un oggetto Statement per eseguire query SQL.
            try (Statement stmt = this.conn.createStatement()) {
                // Definisce una stringa SQL per creare una tabella se non esiste già
                String sql = "CREATE TABLE IF NOT EXISTS activityTracking ("
                        + " activity TEXT NOT NULL,"
                        + " description TEXT NOT NULL,"
                        + " deadline TEXT NOT NULL,"
                        + " state TEXT NOT NULL,"
                        + " PRIMARY KEY (activity))";
                // Esegue l'aggiornamento SQL per creare la tabella
                stmt.executeUpdate(sql);
            }
        } catch (ClassNotFoundException | SQLException ex) {
            // Cattura eccezioni relative alla connessione al database o al caricamento del driver

            // Ottiene il messaggio di errore e lo modifica se necessario
            String err = ex.getMessage().trim();
            if (ex instanceof ClassNotFoundException) {
                err = "Class '" + err + "' not found.";
            }

            // Mostra un messaggio di errore all'utente
            JOptionPane.showMessageDialog(null,
                    err + "\n\nTerminate the application!",
                    "Activity Tracking 2 Error",
                    JOptionPane.ERROR_MESSAGE);

            // Termina l'applicazione in caso di errore
            this.dispatchEvent(new WindowEvent(this,
                    WindowEvent.WINDOW_CLOSING));
        }
    }

    private void populateTable() {
        // Inizializza la stringa SQL per la selezione di tutti i dati della tabella
        String sql = "SELECT * FROM activityTracking ORDER BY ";

        //ottiene l'indice dell'elemento selezionato nel combobox per determinare ordinamento
        int selItem = this.sortingComboBox.getSelectedIndex();

        //switch per modificare la stringa SQL in base all'elemento selezionato
        switch (selItem) {
            case ACTIVITY_DOWN_ID:
                sql += "activity ASC";
                break;
            case ACTIVITY_UP_ID:
                sql += "activity DESC";
                break;
            case DESCRIPTION_DOWN_ID:
                sql += "description ASC";
                break;
            case DESCRIPTION_UP_ID:
                sql += "description DESC";
                break;
            case DEADLINE_DOWN_ID:
                sql += "deadline ASC";
                break;
            case DEADLINE_UP_ID:
                sql += "deadline DESC";
                break;
            case STATE_DOWN_ID:
                sql += "state ASC";
                break;
            case STATE_UP_ID:
                sql += "state DESC";
                break;
            default:
                sql += "state ASC";
        }

        //ottiene il modello della tabella e resetta il numero di righe a zero
        DefaultTableModel dtm = (DefaultTableModel) this.activityTable.getModel();
        dtm.setRowCount(0);

        //prepara la quarry SQL e la esegue
        try (PreparedStatement pstmt = this.conn.prepareStatement(sql)) {
            try (ResultSet rs = pstmt.executeQuery()) {
                // Itera sui risultati della query.
                while (rs.next()) {
                    // Crea un array con i dati di una riga
                    String[] rowData = {
                        rs.getString("activity"),
                        rs.getString("description"),
                        rs.getString("deadline"),
                        rs.getString("state")
                    };
                    // Aggiunge la riga di dati al modello della tabella
                    dtm.addRow(rowData);
                }
            }
        } catch (SQLException ex) {
            // Mostra un messaggio di errore in caso di eccezione SQL
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Activity Tracking Manager Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        // Aggiorna i componenti dell'interfaccia utente
        this.updateGUI();
    }

    private void emptyTextField() {
        //svuota i campi di testo (da usare per esempio con il bottone reload)
        this.activityTextField.setText("");
        this.descriptionTextField.setText("");
        this.deadlineTextField.setText("");
    }

    private void insert(String campo1, String campo2, String campo3, String campo4) {

        // Verifica che nessuno dei campi di testo sia vuoto.
        if (campo1.isBlank() || campo2.isBlank() || campo3.isBlank() || campo4.isBlank()) {
            return;
        }

        // Prepara una query SQL per inserire i nuovi dati nella tabella
        String sql = "INSERT INTO activityTracking (activity, description, deadline, state) VALUES(?, ?, ?, ?)";
        try (PreparedStatement pstmt = this.conn.prepareStatement(sql)) {

            // Imposta i valori dei parametri nella query SQL.
            pstmt.setString(1, campo1);
            pstmt.setString(2, campo2);
            pstmt.setString(3, campo3);
            pstmt.setString(4, campo4);
            //adattare al numero di colonne della tabella

            // Esegue l'aggiornamento del database
            pstmt.executeUpdate();

            // Aggiorna i dati visualizzati nella tabella.
            this.populateTable();

        } catch (SQLException ex) {
            // Mostra un messaggio di errore in caso di eccezione SQL.
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Activity Tracking Manager Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void delete(String entityName) {

        // Prepara la query SQL per eliminare il record.
        String sql = "DELETE FROM activityTracking WHERE activity = ?";

        try (PreparedStatement pstmt = this.conn.prepareStatement(sql)) {

            // Imposta il parametro della query (campo del listener) e esegue l'aggiornamento.
            pstmt.setString(1, entityName);
            pstmt.executeUpdate();

            // Aggiorna i dati visualizzati nella tabella.
            this.populateTable();

        } catch (SQLException ex) {

            // Mostra un messaggio di errore in caso di eccezione SQL.
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Activity Tracking Manager Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteAll() {

        // Prepara e esegue una query SQL per eliminare tutti i record dalla tabella
        try (Statement stmt = this.conn.createStatement()) {

            stmt.executeUpdate("DELETE FROM activityTracking");

            // Aggiorna i dati visualizzati nella tabella.
            this.populateTable();

        } catch (SQLException ex) {

            // Mostra un messaggio di errore in caso di eccezione SQL.
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Activity Tracking Manager Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    //-------------------------------------------------------------------------------
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        activityScrollPane = new javax.swing.JScrollPane();
        activityTable = new javax.swing.JTable();
        activityLabel = new javax.swing.JLabel();
        descriptionLabel = new javax.swing.JLabel();
        deadlineLabel = new javax.swing.JLabel();
        activityTextField = new javax.swing.JTextField();
        descriptionTextField = new javax.swing.JTextField();
        deadlineTextField = new javax.swing.JTextField();
        addButton = new javax.swing.JButton();
        deleteButton = new javax.swing.JButton();
        deleteAllButton = new javax.swing.JButton();
        stateLabel = new javax.swing.JLabel();
        stateComboBox = new javax.swing.JComboBox<>();
        sortingLabel = new javax.swing.JLabel();
        sortingComboBox = new javax.swing.JComboBox<>();
        reloadButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Activity Tracking");
        setMinimumSize(new java.awt.Dimension(700, 400));

        activityTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Activity", "Description", "Deadline", "State"
            }
        ));
        activityTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        activityScrollPane.setViewportView(activityTable);

        activityLabel.setText("Activity:");

        descriptionLabel.setText("Description:");

        deadlineLabel.setText("Deadline:");

        activityTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                unlokButtons(evt);
            }
        });

        descriptionTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                unlokButtons(evt);
            }
        });

        deadlineTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                unlokButtons(evt);
            }
        });

        addButton.setText("Add");
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });

        deleteButton.setText("Delete");
        deleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteButtonActionPerformed(evt);
            }
        });

        deleteAllButton.setText("Delete All");
        deleteAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteAllButtonActionPerformed(evt);
            }
        });

        stateLabel.setText("State:");

        stateComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "In progress", "Completed", "Waiting" }));

        sortingLabel.setText("Sorted by:");

        sortingComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Activity ▼", "Activity ▲", "Description ▼", "Description▲", "Deadline ▼", "Deadline ▲", "State ▼", "State ▲" }));
        sortingComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sortingComboBoxActionPerformed(evt);
            }
        });

        reloadButton.setText("Reload");
        reloadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reloadButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(deadlineLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(descriptionLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 73, Short.MAX_VALUE)
                            .addComponent(activityLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(activityTextField)
                            .addComponent(descriptionTextField)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(deadlineTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(stateLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(stateComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 145, Short.MAX_VALUE)))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(deleteAllButton, javax.swing.GroupLayout.DEFAULT_SIZE, 90, Short.MAX_VALUE)
                            .addComponent(deleteButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(addButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 17, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(activityScrollPane)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(sortingComboBox, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(reloadButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(sortingLabel))
                .addGap(15, 15, 15))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(activityScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 358, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(sortingLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sortingComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(reloadButton)))
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(activityLabel)
                    .addComponent(activityTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(addButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(descriptionLabel)
                    .addComponent(descriptionTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(deleteButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(deadlineLabel)
                    .addComponent(deadlineTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(deleteAllButton)
                    .addComponent(stateLabel)
                    .addComponent(stateComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(16, 16, 16))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
        // Ottiene i valori dai campi di testo della GUI. (da usare nel metodo)                                        
        String tl = this.activityTextField.getText();
        String t2 = this.descriptionTextField.getText();
        String t3 = this.deadlineTextField.getText();
        String t4 = this.stateComboBox.getSelectedItem().toString();

        // chiamata al metodo
        insert(tl, t2, t3, t4);
    }//GEN-LAST:event_addButtonActionPerformed

    private void deleteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteButtonActionPerformed
        // Mostra un dialogo di conferma per assicurarsi che l'utente voglia davvero eliminare il record
        int response = JOptionPane.showConfirmDialog(this,
                "Do you really want to delete the selected record from the database?",
                "Activity Tracking Manager",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        // Interrompe l'azione se l'utente sceglie "No"
        if (response != JOptionPane.YES_OPTION) {
            return;
        }

        // Ottiene l'indice della riga selezionata nella tabella
        int selectedRow = this.activityTable.getSelectedRow();

        // Interrompe l'azione se nessuna riga è selezionata
        if (selectedRow < 0) {
            return;
        }

        // Ottiene il titolo della riga selezionata, che è usato come chiave per l'eliminazione
        String entityName = (String) this.activityTable.getValueAt(selectedRow, ACTIVITY_COLUMN_ID);

        // Chiama il metodo deleteTask per eliminare il record selezionato
        delete(entityName);
    }//GEN-LAST:event_deleteButtonActionPerformed

    private void deleteAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteAllButtonActionPerformed
        // Mostra un dialogo di conferma per assicurarsi che l'utente voglia davvero eliminare tutti i record
        int response = JOptionPane.showConfirmDialog(this,
                "Do you really want to delete all records from the database?",
                "Activity Tracking Manager",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        // Interrompe l'azione se l'utente sceglie "No"
        if (response != JOptionPane.YES_OPTION) {
            return;
        }

        // Chiama il metodo per eliminare tutti i record
        deleteAll();
    }//GEN-LAST:event_deleteAllButtonActionPerformed

    private void reloadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reloadButtonActionPerformed
        // Questo metodo viene chiamato quando l'utente clicca sul pulsante di ricaricamento

        // Ricarica i dati nella tabella
        this.populateTable();

        // resetta i campi di testo svuotandoli
        this.emptyTextField();

        // aggiorna l'interfaccia grafica
        this.updateGUI();
    }//GEN-LAST:event_reloadButtonActionPerformed

    private void unlokButtons(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_unlokButtons
        // Questo metodo viene chiamato quando l'utente rilascia un tasto mentre scrive in uno dei campi di testo

        // Aggiorna l'interfaccia utente per riflettere i cambiamenti nei campi di testo
        this.updateGUI();
    }//GEN-LAST:event_unlokButtons

    private void sortingComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sortingComboBoxActionPerformed
        // Questo metodo viene chiamato quando l'utente cambia la selezione nella ComboBox di ordinamento

        // Ricarica e riordina i dati nella tabella in base alla nuova selezione
        this.populateTable();
    }//GEN-LAST:event_sortingComboBoxActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;

                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ActivityTrackingFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ActivityTrackingFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ActivityTrackingFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);

        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ActivityTrackingFrame.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ActivityTrackingFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel activityLabel;
    private javax.swing.JScrollPane activityScrollPane;
    private javax.swing.JTable activityTable;
    private javax.swing.JTextField activityTextField;
    private javax.swing.JButton addButton;
    private javax.swing.JLabel deadlineLabel;
    private javax.swing.JTextField deadlineTextField;
    private javax.swing.JButton deleteAllButton;
    private javax.swing.JButton deleteButton;
    private javax.swing.JLabel descriptionLabel;
    private javax.swing.JTextField descriptionTextField;
    private javax.swing.JButton reloadButton;
    private javax.swing.JComboBox<String> sortingComboBox;
    private javax.swing.JLabel sortingLabel;
    private javax.swing.JComboBox<String> stateComboBox;
    private javax.swing.JLabel stateLabel;
    // End of variables declaration//GEN-END:variables
}
