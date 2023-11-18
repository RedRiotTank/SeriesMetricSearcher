package ui;

import indexsearcher.IndexSearch;
import indexsearcher.MetricDoc;
import indexsearcher.SearchOption;
import org.apache.lucene.queryparser.classic.ParseException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class IndexSearchUserInterface {
    private IndexSearch indexSearch;
    private Map<String, JTextField> textFields;
    private Map<String, String> queryPerFields;
    private ArrayList<MetricDoc> results;
    private JFrame frame;
    private final JButton globalSearchButton = new JButton("Global Search");
    private final JButton searchButton = new JButton("Search");

    private String indexPath;
    public IndexSearchUserInterface(){
        showInitialWindow(); // muestra la ventana para ir a abrir el indice
    }

    private void showInitialWindow() {
        JFrame initialFrame = new JFrame("Searching index...");
        initialFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel initialPanel = new JPanel(new BorderLayout());
        initialPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel initialLabel = new JLabel("Open an index");
        initialLabel.setFont(new Font("Arial", Font.BOLD, 18));
        initialLabel.setHorizontalAlignment(JLabel.CENTER);

        JButton openIndexButton = new JButton("Explore");
        openIndexButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                initialFrame.dispose();
                showFileChooser();
            }
        });

        initialPanel.add(initialLabel, BorderLayout.NORTH);
        initialPanel.add(openIndexButton, BorderLayout.CENTER);

        initialFrame.getContentPane().add(initialPanel);
        initialFrame.pack();
        initialFrame.setLocationRelativeTo(null);
        initialFrame.setVisible(true);
    }

    private void showFileChooser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Open Index Folder");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) { // si se selecciona un archivo
            indexPath = fileChooser.getSelectedFile().getAbsolutePath();
            initializeUI();
        } else {
            System.exit(0);
        }
    }

    private void initializeUI() { // inicializa la interfaz grafica (fields search & global search)
        try{
            initializeIndexSearch();
        }catch (IOException e){
            e.printStackTrace();
        }
        frame = new JFrame("Index Search");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new GridLayout(0, 2));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel titleAndSearchPanel = new JPanel(new BorderLayout()); // panel aux

        JLabel titleLabel = new JLabel("Fields Search");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleAndSearchPanel.add(titleLabel, BorderLayout.NORTH);

        globalSearchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Funciona");
                createGlobalSearchWindow(IndexSearchUserInterface.this);
            }
        });
        mainPanel.add(titleAndSearchPanel);
        mainPanel.add(globalSearchButton);

        String[] campos = {"Temática del episodio", "Nº del episodio", "Diálogo", "Character", "Localización del diálogo", "Título del episodio", "Fecha de lanzamiento del episodio", "Puntuación en IMDB", "Votos en IMDB", "Nº de temporada"};

        textFields = new HashMap<>();
        queryPerFields = new HashMap<>();


        for (String campo : campos) {
            JLabel label = new JLabel(campo + ":");
            JTextField textField = new JTextField(20);

            mainPanel.add(label);
            mainPanel.add(textField);

            textFields.put(campo, textField);
        }

        searchButton.addActionListener(e -> {
            for(String campo : campos){
                if(!textFields.get(campo).getText().isEmpty()){ // nos quedamos con los campos que no estén vacíos
                    queryPerFields.put(campo, textFields.get(campo).getText());
                    System.out.println(campo + ": " + textFields.get(campo).getText());
                }
            }
            try {
                search();
            } catch (IOException | ParseException ex) {
                throw new RuntimeException(ex);
            }

        });

        mainPanel.add(new JLabel());
        mainPanel.add(searchButton);

        System.out.println(indexPath);
        frame.getContentPane().add(mainPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void showMainWindow() {
        frame.setVisible(true);
    }

    private void createGlobalSearchWindow(IndexSearchUserInterface mainWindow) {
        frame.setVisible(false);
        JFrame globalSearchFrame = new JFrame("Global Search");
        globalSearchFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel globalSearchPanel = new JPanel(new GridLayout(3, 1));
        globalSearchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("Global Search");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);

        JTextField searchTextField = new JTextField(20);
        JButton globalSearchButton = new JButton("Global Search");

        globalSearchButton.addActionListener(e -> {
            String globalQuery = searchTextField.getText();
            System.out.println("Global Search Term: " + globalQuery);
            try {
                search();
            } catch (IOException | ParseException ex) {
                throw new RuntimeException(ex);
            }
        });

        JButton backButton = new JButton("Back to Fields Search");
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                globalSearchFrame.dispose(); // cierra la ventana
                mainWindow.showMainWindow(); // muestra la ventana principal
            }
        });

        globalSearchPanel.add(titleLabel);
        globalSearchPanel.add(searchTextField);
        globalSearchPanel.add(globalSearchButton);
        globalSearchPanel.add(backButton);

        globalSearchFrame.getContentPane().add(globalSearchPanel);
        globalSearchFrame.pack();
        globalSearchFrame.setLocationRelativeTo(mainWindow.frame);
        globalSearchFrame.setVisible(true);
    }

    private void search() throws IOException, ParseException {
        indexSearch = new IndexSearch(indexPath);
        results = indexSearch.search("mariom", SearchOption.CHARACTER);
        indexSearch.closeIndex();
        createResultsWindow();
    }

    private void createResultsWindow(){
        JFrame resultsFrame = new JFrame("Query Results");
        resultsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel resultsPanel = new JPanel(new GridLayout());
        resultsPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        JLabel titleLabel = new JLabel("Query Results");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);

        resultsPanel.add(titleLabel);

        for(MetricDoc doc: results){
            // TODO: mejorar la presentacion: no se ve bien.
            String resultText = "Título: " + doc.getTitle() +
                    "\nNúmero de Episodio: " + doc.getEpisode_number() +
                    "\nPersonaje: " + doc.getCharacter() +
                    "\nDiálogo: " + doc.getSpoken_words() +
                    "\nRating IMDB: " + doc.getImdb_rating();

            JTextArea resultTextArea = new JTextArea(resultText);
            resultTextArea.setLineWrap(true);
            resultTextArea.setWrapStyleWord(true);
            resultTextArea.setEditable(false);

            resultsPanel.add(resultTextArea);
        }

        resultsFrame.getContentPane().add(resultsPanel);
        resultsFrame.pack();
        resultsFrame.setLocationRelativeTo(frame);
        resultsFrame.setVisible(true);
    }

    private void initializeIndexSearch() throws IOException {
        indexSearch = new IndexSearch(indexPath);
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(IndexSearchUserInterface::new);
    }
}


