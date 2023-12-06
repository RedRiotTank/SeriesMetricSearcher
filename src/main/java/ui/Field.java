package ui;

import indexsearcher.SearchOption;
import org.apache.lucene.search.BooleanClause;

import javax.swing.*;

public class Field {
    private String text;
    boolean hasBounds; //true = must, false = should

    private JTextField textField;
    private JTextField from, to;

    private JRadioButton fromInclusive, toInclusive;
    private JComboBox<String> mustOrShouldComboBox;

    public Field(String text, boolean hasBounds){
        this.text = text;
        this.hasBounds = hasBounds;
        textField = new JTextField();
        from = new JTextField();
        to = new JTextField();
        fromInclusive = new JRadioButton("Included");
        toInclusive = new JRadioButton("Included");
        mustOrShouldComboBox = new JComboBox<>(new String[]{"MUST","SHOULD"});
    }

    public String getText() {
        return text;
    }

    public boolean hetHasBounds() {
        return hasBounds;
    }

    public JTextField getTextField() {
        return textField;
    }

    public JTextField getFrom() {
        return from;
    }

    public JTextField getTo() {
        return to;
    }

    public JRadioButton getFromInclusive() {
        return fromInclusive;
    }

    public JRadioButton getToInclusive() {
        return toInclusive;
    }

    public JComboBox<String> getMustOrShouldComboBox() {
        return mustOrShouldComboBox;
    }


    public BooleanClause.Occur getValueMustOrShould(){
        if(mustOrShouldComboBox.getSelectedIndex() == 0)
            return BooleanClause.Occur.MUST;
        else
            return BooleanClause.Occur.SHOULD;
    }

}
