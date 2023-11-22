package ui;

import indexsearcher.IndexSearch;
import indexsearcher.MetricDoc;
import indexsearcher.SearchOption;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.store.FSDirectory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class IndexSearchUserInterface {
    private int notEmptyFields = 0;
    private boolean intervalSearchs = false;
    private IndexSearch indexSearch;
    private Map<String, JTextField> textFields;

    private Map<String,SearchOption> searchOptionMap;
    private Map<String, JComboBox<String>> comboBoxes;
    private Map<String,BooleanClause.Occur> comboBoxesValues;
    private Map<String, String> queryPerFields;
    private ArrayList<MetricDoc> results;
    private JFrame frame;
    private final JButton globalSearchButton = new JButton("Global Search");
    private final JButton searchButton = new JButton("Search");
    private JLabel ratingFrom = new JLabel("From (min 1): ");
    private JLabel ratingTo = new JLabel("To (max 10): ");
    private JLabel episodeNumberFrom = new JLabel("From (min 1): ");
    private JLabel episodeNumberTo = new JLabel("To (max 560): ");

    private JRadioButton ratingFromIncluded = new JRadioButton();
    private JRadioButton ratingToIncluded = new JRadioButton();

    private JRadioButton episodeNumberFromIncluded = new JRadioButton();
    private JRadioButton episodeNumberToIncluded = new JRadioButton();

    private JLabel releaseDateFrom = new JLabel("From (dd-mm-yy dd/mm/yy d/m/yy): ");
    private JLabel releaseDateTo = new JLabel("To (dd-mm-yy dd/mm/yy d/m/yy): ");

    private JRadioButton releaseDatoFromIncluded = new JRadioButton();
    private JRadioButton releaseDateToIncluded = new JRadioButton();
    private String indexPath;
    public IndexSearchUserInterface(){
        loadSearchOptionMap();
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

        boolean validIndex = false;
        while(!validIndex){
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Open Index Folder");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            int result = fileChooser.showOpenDialog(null);

            if (result == JFileChooser.APPROVE_OPTION) { // si se selecciona un archivo
                indexPath = fileChooser.getSelectedFile().getAbsolutePath();
                if (isLuceneIndex(indexPath)) {
                    loadSearchOptionMap(); // se cargan en memoria las searchOptions mapeadas
                    validIndex = true;
                    initializeUI();
                } else {
                    JOptionPane.showMessageDialog(null, "La carpeta seleccionada no es un índice de Lucene válido.", "Error opening index...", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                System.exit(0);
            }
        }

    }

    private boolean isLuceneIndex(String indexPath) {
        try {
            DirectoryReader.open(FSDirectory.open(Path.of(indexPath))).close();
            return true;
        } catch (IOException e) {
            return false;
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

        JPanel mainPanel = new JPanel(new GridLayout(0, 3));
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
        mainPanel.add(new JLabel());

        String[] campos = {"Dialogo", "Personaje", "Localizacion", "Titulo", "Capitulo(nº)",
                "Fecha de lanzamiento", "Puntuacion(IMDB)", "Temporada"};

        textFields = new HashMap<>();
        queryPerFields = new HashMap<>();
        comboBoxes = new HashMap<>();
        comboBoxesValues = new HashMap<>();

        for (String campo : campos) {
            JLabel label = new JLabel(campo + ":");
            label.setFont(new Font("Arial", Font.BOLD, 13));
            JTextField textField = new JTextField(20);

            JComboBox<String> booleanComboBox = new JComboBox<>(new String[]{"Seleccionar","MUST", "SHOULD"}); // 1 -> must 0 -> should


            if(campo.equals("Puntuacion(IMDB)")){
                JTextField from = new JTextField(20);
                JTextField to = new JTextField(20);
                ratingFrom.setHorizontalAlignment(JLabel.RIGHT); // cambiar fuentes y posicion del texto
                ratingFrom.setFont(new Font("Arial", Font.BOLD, 11));
                ratingTo.setHorizontalAlignment(JLabel.RIGHT);
                ratingTo.setFont(new Font("Arial", Font.BOLD, 11));

                mainPanel.add(label);
                mainPanel.add(new JLabel()); mainPanel.add(booleanComboBox);

                mainPanel.add(ratingFrom); mainPanel.add(from);

                mainPanel.add(ratingFromIncluded);
                mainPanel.add(ratingTo); mainPanel.add(to);
                mainPanel.add(ratingToIncluded);
                // añadir al textFields 2 valores, el from y el to
                System.out.println(from.getText());
                textFields.put(campo + "_from",from);
                textFields.put(campo + "_to", to);

            } else if(campo.equals("Capitulo(nº)")){
                JTextField from = new JTextField(20);
                JTextField to = new JTextField(20);
                episodeNumberFrom.setHorizontalAlignment(JLabel.RIGHT); // cambiar fuentes y posicion del texto
                episodeNumberFrom.setFont(new Font("Arial", Font.BOLD, 11));
                episodeNumberTo.setHorizontalAlignment(JLabel.RIGHT);
                episodeNumberTo.setFont(new Font("Arial", Font.BOLD, 11));

                mainPanel.add(label);
                mainPanel.add(new JLabel()); mainPanel.add(booleanComboBox);

                mainPanel.add(episodeNumberFrom); mainPanel.add(from);

                mainPanel.add(episodeNumberFromIncluded);
                mainPanel.add(episodeNumberTo); mainPanel.add(to);
                mainPanel.add(episodeNumberToIncluded);

                // añadir al textFields 2 valores, el from y el to
                textFields.put(campo + "_from",from);
                textFields.put(campo + "_to", to);
            } else if(campo.equals("Fecha de lanzamiento")){
                JTextField from = new JTextField(20);
                JTextField to = new JTextField(20);
                releaseDateFrom.setHorizontalAlignment(JLabel.RIGHT); // cambiar fuentes y posicion del texto
                releaseDateFrom.setFont(new Font("Arial", Font.BOLD, 11));
                releaseDateTo.setHorizontalAlignment(JLabel.RIGHT);
                releaseDateTo.setFont(new Font("Arial", Font.BOLD, 11));

                mainPanel.add(label);
                mainPanel.add(new JLabel()); mainPanel.add(booleanComboBox);

                mainPanel.add(releaseDateFrom); mainPanel.add(from);

                mainPanel.add(releaseDatoFromIncluded);
                mainPanel.add(releaseDateTo); mainPanel.add(to);
                mainPanel.add(releaseDateToIncluded);
                // añadir al textFields 2 valores, el from y el to
                textFields.put(campo + "_from",from);
                textFields.put(campo + "_to", to);
            } else {
                mainPanel.add(label);
                mainPanel.add(textField);
                mainPanel.add(booleanComboBox);
                textFields.put(campo, textField);
            }
            comboBoxes.put(campo,booleanComboBox);
            booleanComboBox.addActionListener(e -> {
                String selectedValue = (String) booleanComboBox.getSelectedItem();
                BooleanClause.Occur occur;

                if (selectedValue.equals("MUST")) {
                    occur = BooleanClause.Occur.MUST;
                } else if (selectedValue.equals("SHOULD")) {
                    occur = BooleanClause.Occur.SHOULD;
                } else {
                    occur = BooleanClause.Occur.SHOULD;
                }

                comboBoxesValues.put(campo, occur);
            });
        }
        // preparacion de la busqueda
        searchButton.addActionListener(e -> {
            String[] campos_map = {"Dialogo", "Personaje", "Localizacion", "Titulo", "Capitulo(nº)_from","Capitulo(nº)_to","Fecha de lanzamiento_from","Fecha de lanzamiento_to", "Puntuacion(IMDB)_from","Puntuacion(IMDB)_to", "Temporada"};
            String[] episode_bounds = {"", ""};
            boolean includeLower=false, includeUpper = false;
            SearchOption option = SearchOption.EPISODE_VIEWS; // por inicializar algo
            for(String campo : campos_map) {
                String fieldValue = textFields.get(campo).getText().trim();
                queryPerFields.put(campo, fieldValue);
                if (!fieldValue.isEmpty()) { // nos quedamos con los campos que no estén vacíos
                    notEmptyFields++; // para saber si la busqueda es individual
                    option = getSearchOptionByField(campo);
                    try {
                        indexSearch.addQuery(fieldValue,option);
                    } catch (IOException | ParseException | java.text.ParseException ex) {
                        throw new RuntimeException(ex);
                    }

                    // para numeros de capitulos
                    /*
                    if (campo.equals("Capitulo(nº)_from")) {
                        String value = textFields.get(campo).getText().trim();
                        includeLower = (episodeNumberFromIncluded.isSelected());
                        episode_bounds[0] = value;
                    } else if (campo.equals("Capitulo(nº)_to")) {
                        String value = textFields.get(campo).getText().trim();
                        includeUpper = (episodeNumberToIncluded.isSelected());
                        episode_bounds[1] = value;
                        try {
                            indexSearch.addQuery(episode_bounds, option, includeLower, includeUpper);
                        } catch (IOException | ParseException | java.text.ParseException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                    */



                    /*
                    queryPerFields.put(campo, fieldValue);
                    comboBoxesValues.put(campo, String.valueOf(comboBoxes.get(campo).getSelectedIndex()));
                    System.out.println(comboBoxesValues.get(campo));
                    System.out.println(campo + ": " + textFields.get(campo).getText());
                    */
                    //indexSearch.addQuery();
                }
            }

            if(!queryPerFields.isEmpty()){ // si hay al menos un campo relleno hace la búsqueda
                try {
                    search(false);
                } catch (IOException | ParseException ex) {
                    throw new RuntimeException(ex);
                }

                for (String campo : campos) {
                    textFields.get(campo).setText("");
                }
            } else{
                JOptionPane.showMessageDialog(frame, "Debes rellenar al menos un campo", "Advertencia", JOptionPane.WARNING_MESSAGE);
                System.out.println("No hay ningún campo relleno");
            }




        });

        mainPanel.add(new JLabel());
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
            } catch (IOException | ParseException ex) {
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

    private void search(boolean global) throws IOException, ParseException {
        if(global){
            // TODO: hacer la busqueda global
            System.out.println("Búsqueda global");
        }else{
            if(notEmptyFields == 1){
                results = indexSearch.search(comboBoxesValues.get("Dialogo"));
            }
            // TODO: Mandar el campo que está relleno y accederlo con el searchOptionMap
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

    private void loadSearchOptionMap(){
        searchOptionMap = new HashMap<>();
        String[] campos = {"Dialogo", "Personaje", "Localizacion", "Titulo", "Capitulo(nº)_from",
                "Capitulo(nº)_to","Fecha de lanzamiento_from","Fecha de lanzamiento_to", "Puntuacion(IMDB)_from",
                "Puntuacion(IMDB)_to", "Temporada"};

        //String[] campos = {"Nº del episodio", "Diálogo", "Personaje", "Localización del diálogo", "Título del episodio", "Fecha de lanzamiento del episodio", "Puntuación en IMDB", "Nº de temporada"};

        for (String campo : campos) {
            SearchOption searchOption = getSearchOptionByField(campo);
            searchOptionMap.put(campo, searchOption);
        }

    }

    private static SearchOption getSearchOptionByField(String field) {
        switch (field) {
            case "Capitulo(nº)_from":
                return SearchOption.EPISODE_NUMBER;
            case "Capitulo(nº)_to":
                return SearchOption.EPISODE_NUMBER;
            case "Dialogo":
                return SearchOption.SPOKEN_WORDS_DIALOG;
            case "Personaje":
                return SearchOption.CHARACTER;
            case "Localizacion":
                return SearchOption.LOCATION;
            case "Titulo":
                return SearchOption.TITLE;
            case "Fecha de lanzamiento_from":
                return SearchOption.RELEASE_DATE;
            case "Fecha de lanzamiento_to":
                return SearchOption.RELEASE_DATE;
            case "Puntuacion(IMDB)_to":
                return SearchOption.IMDB_RATING;
            case "Puntuacion(IMDB)_from":
                return SearchOption.IMDB_RATING;
            case "Temporada":
                return SearchOption.SEASON;

            default:
                throw new IllegalArgumentException("Campo no reconocido: " + field);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(IndexSearchUserInterface::new);
    }
}


