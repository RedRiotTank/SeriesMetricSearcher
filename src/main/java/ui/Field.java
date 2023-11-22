package ui;

import indexsearcher.SearchOption;
import org.apache.lucene.search.BooleanClause;

import javax.swing.*;

public class Field {
    String text;
    boolean hasBounds; //true = must, false = should

    JTextField textField;
    JTextField from, to;

    JRadioButton fromInclusive, toInclusive;


    public Field(String text, boolean hasBounds){
        this.text = text;
        this.hasBounds = hasBounds;
        textField = new JTextField();
        from = new JTextField();
        to = new JTextField();
        fromInclusive = new JRadioButton("Included");
        toInclusive = new JRadioButton("Included");
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
}
