package ui;

import indexsearcher.*;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.FSDirectory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class IndexSearchUserInterface {
    // TODO: revisar variables y hacer locales las que hagan falta
    private JTextField globalSearchTextField; // campo para busqueda global
    private IndexSearch indexSearch;
    private boolean isOriginalResultsAdded;
    private ArrayList<MetricDoc> resultsFiltered;
    private SearchResult resultsWithFacets;
    private JFrame fieldsSearchframe;
    private JFrame globalSearchFrame;
    private final JButton globalSearchButton = new JButton(" <-- Back to Global Search");
    private String indexPath;
    private final Vector<Field> fields = generateFieldsInfo();

    private int currentPage = 1;
    private final int resultsPerPage = 10;

    // Botones y panel de botones (para poder añadirlos o quitarlos en facetas)
    private JPanel buttonsPanel;
    private JButton newSearchButton;
    private JButton previousDocsButton;
    private JButton nextDocsButton;

    private boolean originalResults = true; // TODO: revisar nombre de la variable

    private boolean globalSearch = false;
    private JDialog resultsDialog;
    private JPanel resultsGridPanel;

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
        initialFrame.setSize(400,150);
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
                    // set ruta para poder acceder desde otras clases
                    GlobalVals.setRutaIndex(indexPath);
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
        IndexSearchUserInterface mainWindow = IndexSearchUserInterface.this;

        globalSearchFrame = new JFrame("Global Search");
        globalSearchFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel globalSearchPanel = new JPanel(new GridLayout(2, 1));
        globalSearchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("Global Search");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);

        globalSearchTextField = new JTextField(20);
        JButton globalSearchButton = new JButton("Search");
        globalSearchButton.setBackground(new Color(51,239,100));
        globalSearchButton.setForeground(new Color(34,42,45));

        globalSearchButton.addActionListener(e -> {
            globalSearch = true; // param para usarlo en el showMainWindow
            if(!globalSearchTextField.getText().isEmpty()){
                String globalQuery = globalSearchTextField.getText();
                System.out.println("Global Search Term: " + globalQuery);
                try {
                    search(true);
                } catch (IOException | ParseException | java.text.ParseException ex) {
                    throw new RuntimeException(ex);
                }

            } else{
                JOptionPane.showMessageDialog(globalSearchFrame, "Must search something", "Global search error", JOptionPane.WARNING_MESSAGE);
            }

        });

        JButton fieldSearchButton = new JButton("Fields Search -->");
        fieldSearchButton.setBackground(new Color(249,162,86));
        fieldSearchButton.setForeground(new Color(34,42,45));

        fieldSearchButton.addActionListener(e -> {
            createFieldsSearchWindow();
            globalSearchFrame.setVisible(false); // cierra la ventana
        });

        globalSearchPanel.add(titleLabel);
        globalSearchPanel.add(globalSearchTextField);
        globalSearchPanel.add(fieldSearchButton);
        globalSearchPanel.add(globalSearchButton);

        globalSearchFrame.getContentPane().add(globalSearchPanel);
        globalSearchFrame.pack();
        globalSearchFrame.setLocationRelativeTo(mainWindow.fieldsSearchframe);
        globalSearchFrame.setVisible(true);


        // para cerrar el índice cuando se cierre la ventana principal (fields search)
        globalSearchFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeIndex();
                System.exit(0);
            }
        });
    }

    public void showMainWindow(boolean global) {
        if(global)
            globalSearchFrame.setVisible(true);
        else
            fieldsSearchframe.setVisible(true);
    }

    private void createFieldsSearchWindow() {
        if(fieldsSearchframe==null){ // solo se crea si es null
            System.out.println("Se crea la ventana de fields search");
            fieldsSearchframe = new JFrame("Fields Search");
            fieldsSearchframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel mainPanel = new JPanel(new GridLayout(0, 3));
            mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JPanel titleAndSearchPanel = new JPanel(new BorderLayout()); // panel aux

            JLabel titleLabel = new JLabel("Fields Search");
            titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
            titleLabel.setHorizontalAlignment(JLabel.CENTER);
            titleLabel.setBorder(BorderFactory.createEmptyBorder());
            titleAndSearchPanel.add(titleLabel, BorderLayout.NORTH);


            globalSearchButton.setBackground(new Color(249,162,86));
            globalSearchButton.setForeground(new Color(34,42,45));
            globalSearchButton.addActionListener(e ->{
                fieldsSearchframe.setVisible(false);
                globalSearchFrame.setVisible(true);
            });

            mainPanel.add(new JLabel());
            mainPanel.add(titleAndSearchPanel);

            mainPanel.add(new JLabel());

            for(Field field : fields) {
                JLabel label = new JLabel(field.getText() + ":");
                label.setFont(new Font("Arial", Font.BOLD, 13));

                //---- pintando
                mainPanel.add(label);
                if(!field.hasBounds)
                    mainPanel.add(field.getTextField());
                else {
                    mainPanel.add(new JLabel());
                }

                mainPanel.add(field.getMustOrShouldComboBox());

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
            JButton searchButton = new JButton("Search");
            searchButton.setBackground(new Color(51,239,100));
            searchButton.setForeground(new Color(34,42,45));
            // preparacion de la busqueda
            searchButton.addActionListener(e -> {
                globalSearch = false;
                boolean oneHasText = false;

                for(Field field : fields){
                    // para acceder a los combos (MUST/SHOULD) y (AND/OR)
                    System.out.println(field.getValueMustOrShould());
                    if(!field.hasBounds && !field.getTextField().getText().isEmpty()){
                        oneHasText = true;
                        try {
                            indexSearch.addQuery(field.getTextField().getText(), getSearchOptionByField(field.getText()), field.getValueMustOrShould());
                        } catch (IOException | ParseException | java.text.ParseException ex) {
                            throw new RuntimeException(ex);
                        }
                    } else if (!field.getFrom().getText().isEmpty() && !field.getTo().getText().isEmpty()) {
                        oneHasText = true;

                        String fromZero = field.getFrom().getText(),
                                toZero = field.getTo().getText();

                        String[] bounds = {fromZero, toZero};
                        try {
                            indexSearch.addQuery(bounds, getSearchOptionByField(field.getText()), field.getFromInclusive().isSelected(), field.getToInclusive().isSelected(), field.getValueMustOrShould());
                        } catch (IOException | ParseException | java.text.ParseException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }

                if(oneHasText){ // si hay al menos un campo relleno hace la búsqueda
                    try {
                        search(false);
                    } catch (IOException | ParseException | java.text.ParseException ex) {
                        throw new RuntimeException(ex);
                    }

                    for (Field field : fields) {
                        field.getTextField().setText("");
                        field.getTo().setText("");
                        field.getFrom().setText("");
                    }
                } else{
                    JOptionPane.showMessageDialog(fieldsSearchframe, "You must fill in at least one field", "Warning", JOptionPane.WARNING_MESSAGE);
                    System.out.println("No hay ningún campo relleno");
                }

            });

            mainPanel.add(new JLabel());
            mainPanel.add(globalSearchButton);
            mainPanel.add(searchButton);

            fieldsSearchframe.getContentPane().add(mainPanel);
            fieldsSearchframe.pack();
            fieldsSearchframe.setLocationRelativeTo(null);
        }
        // se hace visible
        fieldsSearchframe.setVisible(true);
    }

    private void search(boolean global) throws IOException, ParseException, java.text.ParseException {
        if(global){
            String queryGlobal = globalSearchTextField.getText();
            // TODO: revisar la busqeuda global con facetas
            try{
                resultsWithFacets = indexSearch.globalSearchWithFacets(queryGlobal);
            }catch (NumberFormatException e) {
                System.err.println("Sintax error");
                resultsWithFacets = null;
            }
            //globalSearchFrame.dispose(); // cierra la ventana
        }else{
            try{
                resultsWithFacets = indexSearch.searchAndObtainFacets();

            }catch (NumberFormatException e){
                System.err.println("Sintax error");
                resultsWithFacets = null;
            }

        }

        currentPage = 1;
        createResultsWindow();
    }

    private void createResultsWindow() {
        currentPage = 1;
        // se cambia el frame por un dialog para poder actualizarlo en tiempo real
        if(resultsDialog == null){
            resultsDialog = new JDialog(fieldsSearchframe,"Query Results", true);
            resultsDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            resultsDialog.setSize(1100,550);
            resultsDialog.setLocationRelativeTo(fieldsSearchframe);
        }
        // si no hay resultados se muestra mensaje de error o si es null error por sintaxis
        if(resultsWithFacets.getDocs() == null){
            JOptionPane.showMessageDialog(resultsDialog, "Sintax error in query", "Sintax error", JOptionPane.ERROR_MESSAGE);
            return;
        } else if(resultsWithFacets.getDocs().isEmpty()){
            JOptionPane.showMessageDialog(resultsDialog, "There are no results for this query", "Without results", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JPanel facetsPanel = createFacetsPanel();
        facetsPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("Query Results");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        resultsPanel.add(titleLabel, BorderLayout.NORTH);

        // variables para la paginación de los resultados
        int startIndex = (currentPage - 1) * resultsPerPage;
        int endIndex = Math.min(startIndex + resultsPerPage,resultsWithFacets.getDocs().size());
        List<MetricDoc> currentPageResults = resultsWithFacets.getDocs().subList(startIndex,endIndex);

        resultsGridPanel = getResultsGridPanel(currentPageResults);

        // se aniade este componente para los casos en los que sea mas grande el contenido que la ventana
        JScrollPane scrollPane = new JScrollPane(resultsGridPanel);
        resultsPanel.add(scrollPane, BorderLayout.CENTER);

        // panel para los botones de abajo
        buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

        // NEW QUERY BUTTON
        newSearchButton = new JButton("New query");
        newSearchButton.addActionListener(e -> {
            resultsDialog.dispose(); // se cierra
            showMainWindow(globalSearch); // se abre la principal
        });

        buttonsPanel.add(newSearchButton);
        // PREVIOUS DOCS BUTTON
        previousDocsButton = new JButton("Previous 10 docs");
        previousDocsButton.addActionListener(e->{
            currentPage--;
            if(isValidPage(currentPage)) // para no mostrar una ventana sin resultados
                updateResultsWindow(getCurrentPageResults());
            else{
                JOptionPane.showMessageDialog(resultsDialog, "No hay más resultados");
                currentPage++;
            }
        });
        buttonsPanel.add(previousDocsButton);

        // NEXT DOCS BUTTON
        nextDocsButton = new JButton("Next 10 docs");
        nextDocsButton.addActionListener(e->{
            currentPage++;
            if(isValidPage(currentPage)) // para no mostrar una ventana sin resultados
                updateResultsWindow(getCurrentPageResults());
            else{
                JOptionPane.showMessageDialog(resultsDialog, "No hay más resultados");
                currentPage--;
            }
        });
        buttonsPanel.add(nextDocsButton);

        // se añade el panel de botones al panel principal de la ventana de resultados
        resultsPanel.add(buttonsPanel, BorderLayout.SOUTH);

        resultsDialog.getContentPane().removeAll();

        // crea un split para poder hacer dinamica la ventana segun gustemos
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, facetsPanel, resultsPanel);
        splitPane.setResizeWeight(0.2);
        resultsDialog.getContentPane().add(splitPane);
        resultsDialog.setVisible(true);
    }

    private List<MetricDoc> getCurrentPageResults() {
        int startIndex = (currentPage - 1) * resultsPerPage;
        int endIndex = Math.min(startIndex + resultsPerPage, resultsWithFacets.getDocs().size());

        return resultsWithFacets.getDocs().subList(startIndex, endIndex);
    }

    // para quitar los botones cuando se muestren los resultados filtrados
    private void removeButtonFromPanel(JButton buttonToRemove) {
        SwingUtilities.invokeLater(() -> {
            buttonsPanel.remove(buttonToRemove);
            buttonsPanel.revalidate();
            buttonsPanel.repaint();
        });
    }
    private void addButtonToPanel(JButton button) {
        SwingUtilities.invokeLater(() -> {
                buttonsPanel.add(button);
                buttonsPanel.revalidate();
                buttonsPanel.repaint();
        });
    }

    private JPanel createFacetsPanel() { // panel para las facetas (en resultsWindow)
        JPanel facetsPanel = new JPanel(new BorderLayout());

        JLabel titleLabel = new JLabel("Categories");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);


        JPanel innerPanel = new JPanel(new GridLayout(0, 1));
        innerPanel.add(titleLabel);

        JLabel rating = new JLabel("Rating");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setHorizontalAlignment(JLabel.LEFT);
        innerPanel.add(rating);

        RangeSlider rangeSlider = new RangeSlider();
        rangeSlider.setPreferredSize(new Dimension(240, rangeSlider.getPreferredSize().height));
        rangeSlider.setMinimum(0);
        rangeSlider.setMaximum(10);

        rangeSlider.setValue(0);
        rangeSlider.setUpperValue(10);

        JLabel minrat = new JLabel("Min Rating: " + rangeSlider.getValue());
        minrat.setFont(new Font("Arial", Font.BOLD, 11));
        minrat.setHorizontalAlignment(JLabel.LEFT);
        innerPanel.add(minrat);

        JLabel maxrat = new JLabel("Max Rating: " + rangeSlider.getUpperValue());
        maxrat.setFont(new Font("Arial", Font.BOLD, 11));
        maxrat.setHorizontalAlignment(JLabel.LEFT);
        innerPanel.add(maxrat);

        rangeSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                minrat.setText("Low Rating: " + rangeSlider.getValue());
                maxrat.setText("High Rating: " + rangeSlider.getUpperValue());
            }
        });

        innerPanel.add(rangeSlider);

        // si hay facetas para la busqueda:
        if(!resultsWithFacets.getDims().isEmpty() && resultsWithFacets.getDims() != null){


            // map con las facetas y sus subfacetas.
            Map<String, ArrayList<String>> facets = new HashMap<>();
            String facetName, subFacetName;
            ArrayList<String> facetsNames = new ArrayList<>();

            // se genera dinamicamente el apartado de las facetas
            for (FacetResult facetResult : resultsWithFacets.getDims()) {
                facetName = facetResult.dim;
                facetsNames.add(facetName);

                // solo si la faceta es jerarquica
                if(facetResult.labelValues != null && facetResult.labelValues.length > 0){
                    ArrayList<String> subFacets = new ArrayList<>();

                    for(LabelAndValue subFacet : facetResult.labelValues){
                        subFacetName = subFacet.label;
                        subFacets.add(subFacetName);
                    }

                    facets.put(facetName,subFacets);
                } else {
                    facets.put(facetName,new ArrayList<>());
                }
            }

            // para acceder a cada combo de la faceta
            Map<String,JComboBox<String>> comboFacets = new HashMap<>();

            for(String facet : facetsNames){
                ArrayList<String> subFacets = facets.get(facet);
                JComboBox<String> combo = null;
                if(subFacets != null && !subFacets.isEmpty()){
                    String[] subFacetsArray = new String[subFacets.size() + 1];
                    subFacetsArray[0] = "Not selected";
                    for(int i = 0; i < subFacets.size(); i++)
                        subFacetsArray[i+1] = subFacets.get(i);
                    combo = new JComboBox<>(subFacetsArray);
                }

                comboFacets.put(facet,combo);
                innerPanel.add(new JLabel(facet));
                innerPanel.add(combo);
                innerPanel.add(new JLabel());
            }


            JButton filterResultsButton = new JButton("Filter");

            isOriginalResultsAdded = true;
            filterResultsButton.addActionListener(e->{

                System.out.println("Filtrando facetas...");

                // guardamos los valores de cada combo
                Map<String, String> selectedFilters = new HashMap<>();
                for (Map.Entry<String, JComboBox<String>> entry : comboFacets.entrySet()) {
                    String facet = entry.getKey();
                    JComboBox<String> comboBox = entry.getValue();
                    String selectedValue = (String) comboBox.getSelectedItem();
                    if (!"Not selected".equals(selectedValue)) {
                        selectedFilters.put(facet, selectedValue);
                    }
                }

                // para sacar por pantalla lo seleccionado y rellenar los filtros para la busqueda con facets
                System.out.println("Filtros seleccionados:");
                List<String> selectedList = new ArrayList<>(); // lista con los filtros
                selectedList.add("rating:[" + rangeSlider.getValue() + ":" + rangeSlider.getUpperValue() + "]");
                for (Map.Entry<String, String> entry : selectedFilters.entrySet()) {
                    System.out.println("Faceta: " + entry.getKey() + ", Seleccionado: " + entry.getValue());
                    selectedList.add(entry.getKey() + ":" + entry.getValue());
                }

                // se pasa a array de strings
                // array que se usa para el filtrado
                String[] selected = selectedList.toArray(new String[0]);
                JButton originalResultsButton = new JButton("Repaint original results");
                originalResultsButton.addActionListener(e1->{
                    System.out.println("Pintando resultados originales...");
                    createResultsWindow();
                });
                // añadir y quitar botones (experiencia de usuario)
                if(isOriginalResultsAdded){ // bool para controlar que el boton solo se añade una vez
                    addButtonToPanel(originalResultsButton);
                    isOriginalResultsAdded = false;
                }
                removeButtonFromPanel(nextDocsButton);
                removeButtonFromPanel(previousDocsButton);

                try {
                    // obtenemos los resultados filtrados

                    resultsFiltered = indexSearch.searchDrillDown(selected, this.resultsWithFacets.getDocs());
                    originalResults = false;
                } catch (IOException | java.text.ParseException | ParseException ex) {
                    throw new RuntimeException(ex);
                }
                // se pintan los resultados
                updateResultsWindow(resultsFiltered);

            });

            filterResultsButton.setBackground(new Color(51,239,100));
            filterResultsButton.setForeground(new Color(34,42,45));

            JButton clearButton = new JButton("Clear filters");
            // boton para reestablecer los valores de los combos
            clearButton.addActionListener(e->{
                rangeSlider.setValue(0);
                rangeSlider.setUpperValue(10);
                for (JComboBox<String> combo : comboFacets.values()) {
                    if (combo != null) {
                        combo.setSelectedIndex(0);
                    }
                }
            });
            clearButton.setBackground(new Color(200,60,60));
            clearButton.setForeground(new Color(34,42,45));

            innerPanel.add(clearButton);
            innerPanel.add(filterResultsButton);
            facetsPanel.add(innerPanel, BorderLayout.CENTER);
        }

        return facetsPanel;
    }

    // metodo para actualizar los resultados
    private void updateResultsWindow(List<MetricDoc> newResults){

        if(newResults.size() > 10)
            currentPage = 1;
        resultsGridPanel.removeAll();
        if(!newResults.isEmpty() && newResults != null){
            int startIndex = (currentPage - 1) * resultsPerPage;
            for(int i = 0; i < newResults.size(); i++){
                MetricDoc doc = newResults.get(i);
                int documentNumber = startIndex + i  + 1;
                String resultText =
                        "Documento nº " + documentNumber+ "\n" +
                        "Título: " + doc.getTitle() +
                        "\nNúmero de Episodio: " + doc.getEpisode_number() +
                        "\nPersonaje: " + doc.getCharacter();

                JTextArea resultTextArea = new JTextArea(resultText);
                resultTextArea.setFont(new Font("Arial", Font.ITALIC, 12));
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
        } else JOptionPane.showMessageDialog(globalSearchFrame, "There are not results with these filters", "No results error", JOptionPane.ERROR_MESSAGE);

        resultsGridPanel.revalidate();
        resultsGridPanel.repaint();
    }

    // método para controlar que no hay mas resultados en la busqueda y saltar excepcion
    private boolean isValidPage(int page) {
        int startIndex = (page - 1) * resultsPerPage;
        return startIndex < resultsWithFacets.getDocs().size() && startIndex >= 0;
    }

    private JPanel getResultsGridPanel(List<MetricDoc> results) { // se encarga de la ventana donde se muestran los resultados
        JPanel resultsGridPanel = new JPanel(new GridLayout(0, 2)); // columnas y filas
        resultsGridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (int i = 0; i < results.size(); i++) { // para cada documento
            MetricDoc doc = results.get(i);

            // con las variables current page y results per page controlamos el numero de los docs en paginacion
            String resultText =
                    "Documento nº " + (i + 1 + ((currentPage - 1)* resultsPerPage)) + "\n" +
                    "Título: " + doc.getTitle() +
                    "\nNúmero de Episodio: " + doc.getEpisode_number() +
                    "\nPersonaje: " + doc.getCharacter();

            JTextArea resultTextArea = new JTextArea(resultText);
            resultTextArea.setFont(new Font("Arial", Font.ITALIC, 12));
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
        // se cambia a dialog para que se pueda ver por encima de la ventana de results
        JDialog detailsDialog = new JDialog(resultsDialog, "Chapter details", true);
        detailsDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextArea detailsTextArea = new JTextArea();
        detailsTextArea.setEditable(false);

        String detailsText = "Title: " + doc.getTitle() +
                "\nNº de Episodio: " + doc.getEpisode_number() +
                "\nCharacter: " + doc.getCharacter() +
                "\nDialog: " + doc.getSpoken_words_dialog() +
                //"\nSpoken_words: " + doc.getSpoken_words() +
                "\nLocation: " + doc.getLocation() +
                "\nRating IMDB: " + doc.getImdb_rating() +
                "\nRelease date: " + doc.getRelease_date() +
                "\nSeason: " + doc.getSeason();
        detailsTextArea.setText(detailsText);
        
        detailsPanel.add(new JScrollPane(detailsTextArea), BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> detailsDialog.dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);

        JButton showAllEpisodeText = new JButton("Show full episode text");

        // boton para mostrar el episodio del texto completo
        showAllEpisodeText.addActionListener(e -> {

            String fullEpisodeText = doc.getSpoken_words();
            JDialog fullTextDialog = new JDialog(detailsDialog, "Full Episode Text", true);
            fullTextDialog.setSize(600, 400);
            fullTextDialog.setLocationRelativeTo(detailsDialog);

            JTextArea fullTextArea = new JTextArea(fullEpisodeText);
            fullTextArea.setEditable(false);
            fullTextArea.setLineWrap(true);
            fullTextArea.setWrapStyleWord(true);

            JScrollPane fullScrollPane = new JScrollPane(fullTextArea);
            fullScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

            fullTextDialog.add(fullScrollPane);

            JButton closeFullTextButton = new JButton("Close");
            closeFullTextButton.addActionListener(closeEvent -> fullTextDialog.dispose());

            fullTextDialog.add(closeFullTextButton, BorderLayout.SOUTH);

            fullTextDialog.setVisible(true);
        });

        buttonPanel.add(showAllEpisodeText);

        detailsPanel.add(buttonPanel, BorderLayout.SOUTH);

        detailsDialog.getContentPane().add(detailsPanel);
        detailsDialog.pack();
        detailsDialog.setSize(400, 350);
        detailsDialog.setLocationRelativeTo(null);
        detailsDialog.setVisible(true);
    }


    private void initializeIndexSearch() throws IOException {
        indexSearch = new IndexSearch(indexPath);
        System.out.println("Indice abierto");
    }

    private void closeIndex() {
        try {
            if (indexSearch != null) {
                indexSearch.closeIndex();
                System.out.println("Indice cerrado");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static SearchOption getSearchOptionByField(String field) {
        return switch (field) {
            case "spoken_words_dialog" -> SearchOption.SPOKEN_WORDS_DIALOG;
            case "spoken_words" -> SearchOption.SPOKEN_WORDS;
            case "character" -> SearchOption.CHARACTER;
            case "location" -> SearchOption.LOCATION;
            case "title" -> SearchOption.TITLE;
            case "episode_number" -> SearchOption.EPISODE_NUMBER;
            case "release_date" -> SearchOption.RELEASE_DATE;
            case "imdb_rating" -> SearchOption.IMDB_RATING;
            case "season" -> SearchOption.SEASON;
            default -> throw new IllegalArgumentException("Campo no reconocido: " + field);
        };
    }

    private static Vector<Field> generateFieldsInfo(){
        Vector<Field> fields = new Vector<>();
        fields.add(new Field("spoken_words_dialog", false));
        fields.add(new Field( "character",false));
        fields.add(new Field( "location", false));
        fields.add(new Field("spoken_words", false));
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


