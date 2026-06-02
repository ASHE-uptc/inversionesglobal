package co.edu.uptc.viewcontroller.investor;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import java.util.ResourceBundle;
import co.edu.uptc.model.Investment;
import co.edu.uptc.model.Asset;
import co.edu.uptc.service.AssetService;
import co.edu.uptc.util.I18nManager;

public class DetalleInversionController {

    // --- COMPONENTES ASIGNADOS EXPRESAMENTE EN TU FXML ---
    @FXML private Label lblNombreActivo;    
    @FXML private Label lblMontoInicial;    
    @FXML private Label lblValorActual;     
    @FXML private Label lblPorcentajeRendimiento; 
    @FXML private Label lblUnidades;        

    // --- SERVICIOS Y CONTROL DE DATOS ---
    private Investment inversionSeleccionada;
    private ResourceBundle bundle;
    private final AssetService assetService = new AssetService();

    @FXML
    public void initialize() {
        // 1. Cargar el diccionario de idiomas configurado globalmente
        try {
            bundle = I18nManager.getInstance().getBundle();
        } catch (Exception e) {
            System.err.println("Error al cargar ResourceBundle en DetalleInversionController: " + e.getMessage());
        }

        // 2. Control de renderizado seguro por si el objeto se inyectó antes del load()
        if (inversionSeleccionada != null) {
            inyectarDatosReales();
        }
    }

    /**
     * Recibe la inversión de la pantalla anterior. Si los componentes gráficos 
     * ya fueron inicializados por JavaFX, inyecta los datos de inmediato.
     */
    public void setInversion(Investment investment) {
        this.inversionSeleccionada = investment;
        if (lblNombreActivo != null && investment != null) {
            inyectarDatosReales();
        }
    }

    /**
     * Mapea y procesa los atributos del JSON hacia la interfaz gráfica de usuario
     */
    private void inyectarDatosReales() {
        // 1. Buscar el activo en asset.json para descifrar su precio de mercado actual y nombre
        Asset activoAsociado = assetService.findById(inversionSeleccionada.getAssetId());
        String nombreAMostrar = (activoAsociado != null) ? activoAsociado.getName() : inversionSeleccionada.getAssetId();
        lblNombreActivo.setText(nombreAMostrar);

        // 2. Variables matemáticas y de negocio basadas en tus modelos exactos
        double capitalInvertido = inversionSeleccionada.getPurchasePrice();
        double cantidadUnidades = inversionSeleccionada.getAmount();
        
        double precioActualActivo = (activoAsociado != null) ? activoAsociado.getActualPrice() : 0.0;
        double valorActualMercado = cantidadUnidades * precioActualActivo;

        // 3. Modificar textos de métricas de capitales formateados a 2 decimales
        lblMontoInicial.setText(String.format("$%,.2f USD", capitalInvertido));
        lblValorActual.setText(String.format("$%,.2f USD", valorActualMercado));

        // 4. Calcular el rendimiento porcentual real dinámicamente
        double rendimientoPorcentaje = 0.0;
        if (capitalInvertido > 0) {
            rendimientoPorcentaje = ((valorActualMercado - capitalInvertido) / capitalInvertido) * 100;
        }

        // Estilizar la métrica de rendimiento según el resultado financiero
        if (rendimientoPorcentaje >= 0) {
            lblPorcentajeRendimiento.setText(String.format("+ %,.2f%% ▲", rendimientoPorcentaje));
            lblPorcentajeRendimiento.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold; -fx-font-size: 16px;");
        } else {
            lblPorcentajeRendimiento.setText(String.format("%,.2f%% ▼", rendimientoPorcentaje));
            lblPorcentajeRendimiento.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 16px;");
        }

        // 5. Adaptar el texto de las unidades aplicando la internacionalización (i18n)
        String palabraUnidades = (bundle != null && bundle.containsKey("investments.units")) 
                ? bundle.getString("investments.units") 
                : "unidades";
        lblUnidades.setText(String.format("%.4f %s", cantidadUnidades, palabraUnidades));
    }

    /**
     * Maneja el regreso a la lista principal reutilizando el setVistaCentral de tu Dashboard
     */
    @FXML
    private void volverAInversiones() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/co/edu/uptc/view/investor/misInversiones.fxml"));
            if (bundle != null) {
                loader.setResources(bundle);
            }
            
            Parent listaView = loader.load();
            
            if (DashboardController.getInstancia() != null) {
                DashboardController.getInstancia().setVistaCentral(listaView);
            }
        } catch (Exception e) {
            System.err.println("Error crítico al intentar volver al panel de Mis Inversiones: " + e.getMessage());
            e.printStackTrace();
        }
    }
}