package co.edu.uptc.viewcontroller.investor;

import co.edu.uptc.model.Asset;
import co.edu.uptc.model.enums.AssetType;
import co.edu.uptc.service.AssetService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ActivosController implements Initializable {

    @FXML private TextField txtBuscar;
    @FXML private VBox contenedorActivos;

    private AssetService assetService;
    private List<Asset> todosLosActivos;
    
    private AssetType categoriaActual = null; 

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        assetService = new AssetService();
        
        cargarDatosDesdeJson();
        renderizarLista(todosLosActivos);

        txtBuscar.textProperty().addListener((observable, oldValue, newValue) -> {
            aplicarFiltros();
        });
    }

    private void cargarDatosDesdeJson() {
        try {
            todosLosActivos = assetService.listAssets();
        } catch (Exception e) {
            System.err.println("Error al cargar los activos del JSON: " + e.getMessage());
        }
    }

    private void renderizarLista(List<Asset> listaActivos) {
        contenedorActivos.getChildren().clear();

        if (listaActivos == null || listaActivos.isEmpty()) {
            return;
        }

        for (Asset asset : listaActivos) { 
            
            HBox fila = new HBox();
            fila.setAlignment(Pos.CENTER_LEFT);
            
            String estiloNormal = "-fx-padding: 15; -fx-border-color: transparent transparent #1e2330 transparent; -fx-border-width: 1; -fx-background-color: transparent;";
            String estiloHover = "-fx-padding: 15; -fx-border-color: transparent transparent #1e2330 transparent; -fx-border-width: 1; -fx-background-color: #1e2330; -fx-cursor: hand;";
            
            fila.setStyle(estiloNormal);

            fila.setOnMouseEntered(e -> fila.setStyle(estiloHover));
            fila.setOnMouseExited(e -> fila.setStyle(estiloNormal));

            fila.setOnMouseClicked(e -> {
                System.out.println("Clic registrado en: " + asset.getName());
            });

            Label lblNombre = new Label(asset.getName());
            lblNombre.setPrefWidth(200.0);
            lblNombre.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffffff;");

            Region spacer1 = new Region();
            HBox.setHgrow(spacer1, Priority.ALWAYS);

            Label lblPrecio = new Label(String.format("$ %,.2f", asset.getActualPrice()));
            lblPrecio.setPrefWidth(120.0);
            lblPrecio.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffffff; -fx-font-weight: bold;");

            Region spacer2 = new Region();
            HBox.setHgrow(spacer2, Priority.ALWAYS);

            double valorVolatilidad = asset.getVolatility();
            
            Label lblVolatilidad = new Label(String.format("%.2f %%", valorVolatilidad));
            lblVolatilidad.setPrefWidth(100.0);
            lblVolatilidad.setAlignment(Pos.CENTER_RIGHT);
            
            String colorVolatilidad;
            if (valorVolatilidad < 5.0) { 
                colorVolatilidad = "#10b981"; // Verde (Baja)
            } else if (valorVolatilidad < 15.0) { 
                colorVolatilidad = "#f59e0b"; // Naranja (Media)
            } else { 
                colorVolatilidad = "#ef4444"; // Rojo (Alta)
            }
            
            lblVolatilidad.setStyle("-fx-font-size: 14px; -fx-text-fill: " + colorVolatilidad + "; -fx-font-weight: bold;");

            fila.getChildren().addAll(lblNombre, spacer1, lblPrecio, spacer2, lblVolatilidad);
            contenedorActivos.getChildren().add(fila);
        }
    }

    // --- MÉTODOS DE FILTRADO ACTUALIZADOS ---

    @FXML 
    private void filtrarTodos() { 
        categoriaActual = null; 
        aplicarFiltros(); 
    }
    
    @FXML 
    private void filtrarBond() { 
        categoriaActual = AssetType.BOND; 
        aplicarFiltros(); 
    }
    
    @FXML 
    private void filtrarBadge() { 
        categoriaActual = AssetType.BADGE; 
        aplicarFiltros(); 
    }
    
    @FXML 
    private void filtrarEtf() { 
        categoriaActual = AssetType.ETF; 
        aplicarFiltros(); 
    }

    @FXML 
    private void filtrarProperty() { 
        categoriaActual = AssetType.PROPERTY; 
        aplicarFiltros(); 
    }

    @FXML 
    private void filtrarStock() { 
        categoriaActual = AssetType.STOCK; 
        aplicarFiltros(); 
    }

    @FXML 
    private void filtrarCrypto() { 
        categoriaActual = AssetType.CRYPTO; 
        aplicarFiltros(); 
    }

    @FXML 
    private void filtrarNft() { 
        categoriaActual = AssetType.NFT; 
        aplicarFiltros(); 
    }

    private void aplicarFiltros() {
        String busqueda = txtBuscar.getText().toLowerCase().trim();

        List<Asset> listaFiltrada = todosLosActivos.stream()
                .filter(a -> categoriaActual == null || a.getAssetType() == categoriaActual)
                .filter(a -> a.getName().toLowerCase().contains(busqueda))
                .collect(Collectors.toList());

        renderizarLista(listaFiltrada);
    }
}