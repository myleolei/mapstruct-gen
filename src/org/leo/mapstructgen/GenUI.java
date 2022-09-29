package org.leo.mapstructgen;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.*;
import java.util.*;
import java.util.stream.Collectors;

public class GenUI implements Configurable {

    private JPanel main;

    private JLabel sourceLabel;

    private JLabel targetLabel;

    private JTable mappingTable;

    private JList sourceContent;

    private ApplyCallback callback;


    public GenUI(String sourceClassName, String targetClassName, Collection<String> sourceFields, Collection<String> targetFields, ApplyCallback callback) {

        List<String> sources = sourceFields.stream().collect(Collectors.toList());
        List<String> targets = targetFields.stream().collect(Collectors.toList());
        this.sourceLabel.setText(sourceClassName);
        this.targetLabel.setText(targetClassName);

        DefaultListModel listModel = new DefaultListModel();
        listModel.add(0, "NONE");
        listModel.addAll(sourceFields);
        this.sourceContent.setModel(listModel);
        Object[][] cellDatas = new Object[targets.size()][2];
        for (int i = 0; i < targets.size(); i++) {
            String fieldName = targets.get(i);
            cellDatas[i][0] = fieldName;
            if (sourceFields.contains(fieldName)) {
                cellDatas[i][1] = fieldName;
            } else {
                cellDatas[i][1] = "";
            }
        }
        this.mappingTable.setModel(new DefaultTableModel(cellDatas, new Object[]{"Target(" + targetClassName + ") Field", "Source(" + sourceClassName + ") Field"}) {
            @Override
            public boolean isCellEditable(int row, int column) {

//                return column == 1;
                return false;
            }
        });
//        String[] options = new String[sources.size() + 1];
//        options[0] = "";
//        for (int i = 0; i < sources.size(); i++) {
//            options[i + 1] = sources.get(i);
//        }
//        AutoCompleteComboBox comboBox = new AutoCompleteComboBox(options);
//        DefaultCellEditor cellEditor = new DefaultCellEditor(comboBox);
//        this.mappingTable.getColumnModel().getColumn(1).setCellEditor(cellEditor);
        this.sourceContent.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

                if (e.getClickCount() == 2) {
                    if (mappingTable.getSelectedColumn() == 1) {
                        String val = (String) sourceContent.getSelectedValue();
                        mappingTable.setValueAt(val, mappingTable.getSelectedRow(), 1);
                    }
                }
            }
        });
        this.callback = callback;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title)
    String getDisplayName() {

        return "Mapping Config";
    }

    @Override
    public @Nullable
    JComponent createComponent() {

        return main;
    }

    @Override
    public boolean isModified() {

        return true;
    }

    @Override
    public void apply() throws ConfigurationException {

        TreeMap<String, String> sourceToTargetMapping = new TreeMap<>();
        int rowCount = this.mappingTable.getRowCount();
        for (int row = 0; row < rowCount; row++) {
            String target = this.mappingTable.getValueAt(row, 0).toString();
            String source = this.mappingTable.getValueAt(row, 1).toString();
            if (!target.equals(source) && !source.equals("") && !source.equals("NONE")) {
                sourceToTargetMapping.put(source, target);
            }
        }
        if (callback != null) {
            callback.apply(sourceToTargetMapping);
        }
    }

    public static class AutoCompleteComboBox extends JComboBox {

        private AutoCompleter completer;

        public AutoCompleteComboBox() {

            super();
            addCompleter();
        }

        public AutoCompleteComboBox(ComboBoxModel cm) {

            super(cm);
            addCompleter();
        }

        public AutoCompleteComboBox(Object[] items) {

            super(items);
            addCompleter();
        }

        public AutoCompleteComboBox(List v) {

            super((Vector) v);
            addCompleter();
        }

        private void addCompleter() {

            setEditable(true);
            completer = new AutoCompleter(this);
        }

        public void autoComplete(String str) {

            this.completer.autoComplete(str, str.length());
        }

        public String getText() {

            return ((JTextField) getEditor().getEditorComponent()).getText();
        }

        public void setText(String text) {

            ((JTextField) getEditor().getEditorComponent()).setText(text);
        }

        public boolean containsItem(String itemString) {

            for (int i = 0; i < this.getModel().getSize(); i++) {
                String _item = " " + this.getModel().getElementAt(i);
                if (_item.equals(itemString))
                    return true;
            }
            return false;
        }

    }

    public static class AutoCompleter implements KeyListener, ItemListener {

        private JComboBox owner = null;

        private JTextField editor = null;

        private ComboBoxModel model = null;

        public AutoCompleter(JComboBox comboBox) {

            owner = comboBox;
            editor = (JTextField) comboBox.getEditor().getEditorComponent();
            editor.addKeyListener(this);
            model = comboBox.getModel();

            owner.addItemListener(this);
        }

        public void keyTyped(KeyEvent e) {

        }

        public void keyPressed(KeyEvent e) {

        }

        public void keyReleased(KeyEvent e) {

            char ch = e.getKeyChar();
            if (ch == KeyEvent.CHAR_UNDEFINED || Character.isISOControl(ch)
                    || ch == KeyEvent.VK_DELETE)
                return;

            int caretPosition = editor.getCaretPosition();
            String str = editor.getText();
            if (str.length() == 0)
                return;
            autoComplete(str, caretPosition);
        }

        /**
         * 自动完成。根据输入的内容，在列表中找到相似的项目.
         */
        protected void autoComplete(String strf, int caretPosition) {

            Object[] opts;
            opts = getMatchingOptions(strf.substring(0, caretPosition));
            if (owner != null) {
                model = new DefaultComboBoxModel(opts);
                owner.setModel(model);
            }
            if (opts.length > 0) {
                String str = opts[0].toString();
                editor.setCaretPosition(caretPosition);
                if (owner != null) {
                    try {
                        owner.showPopup();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }

        /**
         * 找到相似的项目, 并且将之排列到数组的最前面。
         *
         * @param str
         * @return 返回所有项目的列表。
         */
        protected Object[] getMatchingOptions(String str) {

            List v = new Vector();
            List v1 = new Vector();

            for (int k = 0; k < model.getSize(); k++) {
                Object itemObj = model.getElementAt(k);
                if (itemObj != null) {
                    String item = itemObj.toString().toLowerCase();
                    if (item.startsWith(str.toLowerCase()))
                        v.add(model.getElementAt(k));
                    else
                        v1.add(model.getElementAt(k));
                } else
                    v1.add(model.getElementAt(k));
            }
            for (int i = 0; i < v1.size(); i++) {
                v.add(v1.get(i));
            }
            if (v.isEmpty())
                v.add(str);
            return v.toArray();
        }

        public void itemStateChanged(ItemEvent event) {

            if (event.getStateChange() == ItemEvent.SELECTED) {
                int caretPosition = editor.getCaretPosition();
                if (caretPosition != -1) {
                    try {
                        editor.moveCaretPosition(caretPosition);
                    } catch (IllegalArgumentException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    public interface ApplyCallback {

        public void apply(Map<String, String> sourceToTargetMapping);
    }

    public static void main(String[] args) {

        JFrame frame = new JFrame("Mapping Config");
        frame.setContentPane(new GenUI("TestA", "TestB", Arrays.asList("abc", "def"), Arrays.asList("def"), null).main);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
