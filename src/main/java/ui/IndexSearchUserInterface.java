package ui;

import indexsearcher.IndexSearch;
import indexsearcher.MetricDoc;
import indexsearcher.SearchOption;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
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
import java.util.Vector;

public class IndexSearchUserInterface {
    private int notEmptyFields = 0;
    private boolean intervalSearchs = false;
    private IndexSearch indexSearch;
    private Map<String, JTextField> textFields;

    private Map<String,SearchOption> searchOptionMap;
    private Map<String, JComboBox<String>> comboBoxes;
    private Map<String,BooleanClause.Occur> comboBoxesValues;

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

    private Vector<Field> fields = generateFieldsInfo();

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

        boolean validIndex = false;
        while(!validIndex){
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Open Index Folder");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            int result = fileChooser.showOpenDialog(null);

            if (result == JFileChooser.APPROVE_OPTION) { // si se selecciona un archivo
                indexPath = fileChooser.getSelectedFile().getAbsolutePath();
                if (isLuceneIndex(indexPath)) {
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


        for(Field field : fields) {
            JLabel label = new JLabel(field.getText() + ":");
            label.setFont(new Font("Arial", Font.BOLD, 13));
            JComboBox<String> booleanComboBox = new JComboBox<>(new String[]{"MUST", "SHOULD"}); // 1 -> must 0 -> should

            //---- pintando
            mainPanel.add(label);
            if(!field.hasBounds)
                mainPanel.add(field.getTextField());
            else {
                mainPanel.add(new JLabel());
            }

            mainPanel.add(booleanComboBox);

            if(field.hasBounds){
                JLabel from = new JLabel("From:");
                from.setHorizontalAlignment(JLabel.RIGHT);
                JLabel to = new JLabel("To:");
                to.setHorizontalAlignment(JLabel.RIGHT);

                mainPanel.add(from);

                mainPanel.add(field.getFrom());
                mainPanel.add(field.getFromInclusive());

                mainPanel.add(to);
                mainPanel.add(field.getTo());
                mainPanel.add(field.getToInclusive());
            }
        }

        // preparacion de la busqueda
        searchButton.addActionListener(e -> {

            boolean oneHasText = false;

            for(Field field : fields){
                if(!field.hasBounds && !field.getTextField().getText().isEmpty()){
                    oneHasText = true;
                    try {
                        indexSearch.addQuery(field.getTextField().getText(), getSearchOptionByField(field.getText()));
                    } catch (IOException | ParseException | java.text.ParseException ex) {
                        throw new RuntimeException(ex);
                    }
                } else if (!field.getFrom().getText().isEmpty() && !field.getTo().getText().isEmpty()) {
                    oneHasText = true;

                    String fromZero = field.getFrom().getText(),
                            toZero = field.getTo().getText();

                    if(field.getFrom().getText().length() == 1) fromZero = "00" + field.getFrom().getText();
                    if(field.getTo().getText().length() == 1) toZero = "00" + field.getTo().getText();
                    if(field.getFrom().getText().length() == 2) fromZero = "0" + field.getFrom().getText();
                    if(field.getTo().getText().length() == 2) toZero = "0" + field.getTo().getText();

                    String[] bounds = {fromZero, toZero};
                    try {
                        indexSearch.addQuery(bounds, getSearchOptionByField(field.getText()), field.getFromInclusive().isSelected(), field.getToInclusive().isSelected());
                    } catch (IOException | ParseException | java.text.ParseException ex) {
                        throw new RuntimeException(ex);
                    }
                }



            }


            if(oneHasText){ // si hay al menos un campo relleno hace la búsqueda
                try {
                    search(false);
                } catch (IOException | ParseException ex) {
                    throw new RuntimeException(ex);
                }

                for (Field field : fields) {
                    field.getTextField().setText("");
                    field.getTo().setText("");
                    field.getFrom().setText("");
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

            results = indexSearch.search(BooleanClause.Occur.MUST);

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

    private static SearchOption getSearchOptionByField(String field) {
        switch (field) {
            case "spoken_words":
                return SearchOption.SPOKEN_WORDS;

            case "character":
                return SearchOption.CHARACTER;

            case "location":
                return SearchOption.LOCATION;

            case "title":
                return SearchOption.TITLE;

            case "episode_number":
                return SearchOption.EPISODE_NUMBER;

            case "release_date":
                return SearchOption.RELEASE_DATE;

            case "imdb_rating":
                return SearchOption.IMDB_RATING;


            case "season":
                return SearchOption.SEASON;

            default:
                throw new IllegalArgumentException("Campo no reconocido: " + field);
        }
    }

    private static Vector<Field> generateFieldsInfo(){
        Vector<Field> fields = new Vector<>();
        fields.add(new Field("spoken_words", false));
        fields.add(new Field( "character",false));
        fields.add(new Field( "location", false));
        fields.add(new Field( "title",false));
        fields.add(new Field( "episode_number", true));
        fields.add(new Field( "release_date", true));
        fields.add(new Field("imdb_rating", true));
        fields.add(new Field( "season", true));
        return fields;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(IndexSearchUserInterface::new);
    }
}


