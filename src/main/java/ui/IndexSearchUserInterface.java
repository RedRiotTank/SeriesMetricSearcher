package ui;

import indexsearcher.IndexSearch;
import indexsearcher.MetricDoc;
import indexsearcher.SearchOption;
import org.apache.lucene.queryparser.classic.ParseException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
        openIndexButton.addActionListener(e -> {
            initialFrame.dispose();
            showFileChooser();
        });

        initialPanel.add(initialLabel, BorderLayout.NORTH);
        initialPanel.add(openIndexButton, BorderLayout.CENTER);

        initialFrame.getContentPane().add(initialPanel);
        initialFrame.pack();
        initialFrame.setSize(200,75);
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
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        JPanel mainPanel = new JPanel(new GridLayout(0, 2));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel titleAndSearchPanel = new JPanel(new BorderLayout()); // panel aux

        JLabel titleLabel = new JLabel("Fields Search");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleAndSearchPanel.add(titleLabel, BorderLayout.NORTH);

        globalSearchButton.addActionListener(e -> {
            createGlobalSearchWindow(IndexSearchUserInterface.this);
        });
        mainPanel.add(titleAndSearchPanel);
        mainPanel.add(globalSearchButton);

        String[] campos = {"Temática del episodio", "Nº del episodio", "Diálogo", "Personaje", "Localización del diálogo", "Título del episodio", "Fecha de lanzamiento del episodio", "Puntuación en IMDB", "Votos en IMDB", "Nº de temporada"};

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
                search(false);
            } catch (IOException | ParseException | java.text.ParseException ex) {
                throw new RuntimeException(ex);
            }

        });

        mainPanel.add(new JLabel());
        mainPanel.add(searchButton);

        // para cerrar el índice cuando se cierre la ventana principal (fields search)
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeIndex();
                System.exit(0);
            }
        });

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
                search(true);
            } catch (IOException | ParseException | java.text.ParseException ex) {
                throw new RuntimeException(ex);
            }
        });

        JButton backButton = new JButton("Back to Fields Search");
        backButton.addActionListener(e -> {
            globalSearchFrame.dispose(); // cierra la ventana
            mainWindow.showMainWindow(); // muestra la ventana principal
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

    private void search(boolean global) throws IOException, ParseException, java.text.ParseException {
        if(global){
            // TODO: hacer la busqueda global
            System.out.println("Búsqueda global");
        }else{
            // TODO: hacerlo con x campos no solo con uno
            System.out.println("Búsqueda por campos");
            String[] query = {textFields.get("Personaje").getText()};
           // results = indexSearch.search(query, SearchOption.CHARACTER, true, true);
            createResultsWindow();
        }

    }

    private void createResultsWindow() {
        JFrame resultsFrame = new JFrame("Query Results");
        resultsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("Query Results");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        resultsPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel resultsGridPanel = getResultsGridPanel();

        // se aniade este componente para los casos en los que sea mas grande el contenido que la ventana
        JScrollPane scrollPane = new JScrollPane(resultsGridPanel);
        resultsPanel.add(scrollPane, BorderLayout.CENTER);

        JButton newSearchButton = new JButton("New query");
        newSearchButton.addActionListener(e -> {
            resultsFrame.dispose(); // se cierra
            showMainWindow(); // se abre la principal
        });

        resultsPanel.add(newSearchButton, BorderLayout.SOUTH);

        resultsFrame.getContentPane().add(resultsPanel);
        resultsFrame.pack();
        resultsFrame.setSize(1100, 550); // tam ventana resultados
        resultsFrame.setLocationRelativeTo(frame);
        resultsFrame.setVisible(true);
    }

    private JPanel getResultsGridPanel() { // se encarga de la ventana donde se muestran los resultados
        JPanel resultsGridPanel = new JPanel(new GridLayout(0, 2)); // columnas y filas
        resultsGridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (int i = 0; i < results.size(); i++) { // para cada documento
            MetricDoc doc = results.get(i);

            String resultText = "Documento nº " + (i + 1) + "\n" +
                    "Título: " + doc.getTitle() +
                    "\nNúmero de Episodio: " + doc.getEpisode_number() +
                    "\nPersonaje: " + doc.getCharacter();

            JTextArea resultTextArea = new JTextArea(resultText);
            resultTextArea.setBorder(BorderFactory.createEtchedBorder() );
            resultTextArea.setEditable(false);
            resultTextArea.setLineWrap(true);
            resultTextArea.setWrapStyleWord(true);

            JButton detailsButton = new JButton("More details");
            detailsButton.addActionListener(e -> showDetailsWindow(doc));

            JPanel resultDetailsPanel = new JPanel(new BorderLayout());
            resultDetailsPanel.add(resultTextArea, BorderLayout.CENTER);
            resultDetailsPanel.add(detailsButton, BorderLayout.SOUTH);

            resultsGridPanel.add(resultDetailsPanel);
        }
        return resultsGridPanel;
    }

    private void showDetailsWindow(MetricDoc doc) {
        JFrame detailsFrame = new JFrame("Chapter details");
        detailsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextArea detailsTextArea = new JTextArea();
        detailsTextArea.setEditable(false);

        String detailsText = "Title: " + doc.getTitle() +
                "\nNº de Episodio: " + doc.getEpisode_number() +
                "\nCharacter: " + doc.getCharacter() +
                "\nDialog: " + doc.getSpoken_words() +
                "\nLocation: " + doc.getLocation() +
                "\nRating IMDB: " + doc.getImdb_rating() +
                //"\nVotes in IMDB: " + doc.getImdb_votes() +
                "\nRelease date: " + doc.getRelease_date() +
                "\nSeason: " + doc.getSeason();
        detailsTextArea.setText(detailsText);

        detailsPanel.add(new JScrollPane(detailsTextArea), BorderLayout.CENTER);

        detailsFrame.getContentPane().add(detailsPanel);
        detailsFrame.pack();
        detailsFrame.setSize(600, 400);
        detailsFrame.setLocationRelativeTo(null);
        detailsFrame.setVisible(true);
    }


    private void initializeIndexSearch() throws IOException {
        indexSearch = new IndexSearch(indexPath);
    }

    private void closeIndex() {
        try {
            if (indexSearch != null) {
                indexSearch.closeIndex();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(IndexSearchUserInterface::new);
    }
}


