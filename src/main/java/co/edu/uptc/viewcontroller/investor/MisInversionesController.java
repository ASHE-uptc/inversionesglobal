package co.edu.uptc.viewcontroller.investor;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;

import co.edu.uptc.app.App;
import co.edu.uptc.model.Asset;
import co.edu.uptc.model.Investment;
import co.edu.uptc.model.Investor;
import co.edu.uptc.model.User;
import co.edu.uptc.service.AssetService;
import co.edu.uptc.service.InvestmentService;
import co.edu.uptc.service.InvestorService;
import co.edu.uptc.service.PortfolioService;
import co.edu.uptc.exception.IncompatibleRiskProfileException;
import co.edu.uptc.exception.InsufficientCapitalException;
import co.edu.uptc.util.I18nManager;

public class MisInversionesController {

    // --- COMPONENTES FXML ---
    @FXML private Label saldoLabel;
    @FXML private VBox containerEstadoVacio;
    @FXML private VBox containerListaInversiones;
    @FXML private VBox listaInversionesVBox;

    // --- COMPONENTES MODAL FLOTANTE ---
    @FXML private StackPane modalOverlay;
    @FXML private ComboBox<Asset> comboActivos; 
    @FXML private TextField txtMonto;

    // --- INSTANCIACIÓN DE SERVICIOS REALES ---
    private final InvestorService investorService = new InvestorService();
    private final AssetService assetService = new AssetService(); 
    private final InvestmentService investmentService = new InvestmentService(assetService, investorService);
    private final PortfolioService portfolioService = new PortfolioService(investmentService, assetService, investorService);

    private ResourceBundle bundle;

    @FXML
    public void initialize() {
        System.out.println("Inicializando panel de control de inversiones reales con persistencia JSON e I18n...");
        
        // Cargar el gestor de idiomas de la aplicación
        try {
            bundle = I18nManager.getInstance().getBundle();
        } catch (Exception e) {
            System.err.println("No se pudo cargar el ResourceBundle en MisInversionesController.");
        }
        
        // 1. Cargar el ComboBox con los activos reales de asset.json
        if (comboActivos != null) {
            try {
                List<Asset> listaActivosMercado = assetService.listAssets();
                comboActivos.setItems(FXCollections.observableArrayList(listaActivosMercado));
                
                // Formatear visualmente el ComboBox para mostrar Nombre y Precio
                comboActivos.setCellFactory(param -> new ListCell<Asset>() {
                    @Override
                    protected void updateItem(Asset item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item.getName() + " ($" + item.getActualPrice() + " USD)");
                        }
                    }
                });
                comboActivos.setButtonCell(comboActivos.getCellFactory().call(null));
            } catch (Exception e) {
                System.err.println("Error al cargar los activos en el ComboBox: " + e.getMessage());
            }
        }

        // 2. Renderizar y sincronizar la interfaz con los JSONs
        actualizarPantalla();
    }

    /**
     * Sincroniza la UI buscando al Investor real a través del User logueado en App
     */
    private void actualizarPantalla() {
        User usuarioLogueado = App.getUsuarioLogueado();
        
        if (usuarioLogueado == null) {
            System.err.println("❌ No hay ningún usuario autenticado en la sesión de App.");
            return;
        }

        // Buscar los datos financieros frescos directamente en el investor.json
        Investor investorActual = investorService.findByEmail(usuarioLogueado.getEmail());
        
        if (investorActual == null) {
            System.err.println("⚠️ El usuario está autenticado pero no posee un perfil en investor.json");
            if (saldoLabel != null) saldoLabel.setText("$0.00 USD");
            ocultarListaInversiones();
            return;
        }

        // Pintar el saldo real del inversionista
        if (saldoLabel != null) {
            saldoLabel.setText(String.format("$%,.2f USD", investorActual.getAvailableCapital()));
        }

        // Cargar historial de transacciones reales desde investment.json
        List<Investment> inversionesReales = investmentService.getInvestmentsByInvestorId(investorActual.getId());

        if (inversionesReales == null || inversionesReales.isEmpty()) {
            ocultarListaInversiones();
        } else {
            containerEstadoVacio.setVisible(false);
            containerEstadoVacio.setManaged(false);
            containerListaInversiones.setVisible(true);
            containerListaInversiones.setManaged(true);

            renderizarListaInversiones(inversionesReales);
        }
    }

    /**
     * Inyecta dinámicamente las filas en el contenedor de JavaFX leyendo la persistencia
     */
    /**
     * Inyecta dinámicamente las filas en el contenedor de JavaFX leyendo la persistencia
     */
    private void renderizarListaInversiones(List<Investment> inversiones) {
        // 1. Limpiamos SOLO el sub-contenedor de las tarjetas, dejando el encabezado a salvo
        listaInversionesVBox.getChildren().clear(); 

        // Recuperar traducciones básicas para la fila dinámica
        String etiquetaActiva = getTraducido("investments.status.active");
        String etiquetaUnidades = getTraducido("investments.units");
        String etiquetaRendimiento = getTraducido("investments.yield");

        for (Investment inv : inversiones) {
            Asset asset = assetService.findById(inv.getAssetId());
            String nombreActivo = (asset != null) ? asset.getName() : "Activo Desconocido";

            HBox fila = new HBox();
            fila.getStyleClass().add("investment-item-row");
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.setSpacing(20.0);
            fila.setPadding(new Insets(20, 25, 20, 25));

            // Columna Izquierda: Información del activo (Traducción aplicada aquí)
            VBox infoIzquierda = new VBox(5.0);
            HBox.setHgrow(infoIzquierda, Priority.ALWAYS);
            Label lblTitulo = new Label(nombreActivo);
            lblTitulo.getStyleClass().add("item-title");
            
            // Reemplazo de texto quemado: "Activa • X unidades" -> Traducido
            Label lblSub = new Label(String.format("%s • %.4f %s", etiquetaActiva, inv.getAmount(), etiquetaUnidades));
            lblSub.getStyleClass().add("item-subtitle");
            infoIzquierda.getChildren().addAll(lblTitulo, lblSub);

            // Columna Derecha: Rendimiento real calculado con fluctuaciones de mercado
            VBox infoDerecha = new VBox(5.0);
            infoDerecha.setAlignment(Pos.CENTER_RIGHT);
            Label lblPerfLabel = new Label(etiquetaRendimiento); // Reemplazo de "Rendimiento"
            lblPerfLabel.getStyleClass().add("item-perf-label");
            
            double rendimientoPorcentaje = investmentService.calculateYieldPercentage(inv);
            Label lblPerfValue = new Label(String.format("%s%.2f%%", (rendimientoPorcentaje >= 0 ? "+" : ""), rendimientoPorcentaje));
            
            if (rendimientoPorcentaje >= 0) {
                lblPerfValue.getStyleClass().add("item-perf-value-positive");
            } else {
                lblPerfValue.getStyleClass().add("item-perf-value-negative"); 
            }
            infoDerecha.getChildren().addAll(lblPerfLabel, lblPerfValue);

            Label flecha = new Label(">");
            flecha.getStyleClass().add("item-arrow");

            fila.getChildren().addAll(infoIzquierda, infoDerecha, flecha);

            // Evento Click: Modificado para pasar el objeto Investment completo al controlador de destino
            fila.setOnMouseClicked(event -> irAPantallaDetalles(inv, nombreActivo));

            // 2. Agregamos la fila (tarjeta) al sub-contenedor, no al padre
            listaInversionesVBox.getChildren().add(fila);
        }
    }
    /**
     * Confirma la orden de compra validando reglas de negocio financieras e internacionales
     */
    @FXML
    private void confirmarInversion() {
        Asset activoSeleccionado = comboActivos.getValue();
        String montoTexto = txtMonto.getText();

        if (activoSeleccionado == null || montoTexto.isEmpty()) {
            mostrarAlertaI18n("investments.alert.incomplete", "investments.alert.fields.msg", Alert.AlertType.WARNING);
            return; 
        }

        try {
            double montoDinero = Double.parseDouble(montoTexto);
            if (montoDinero <= 0) {
                mostrarAlertaI18n("investments.alert.invalid.monto", "investments.alert.monto.msg", Alert.AlertType.ERROR);
                return;
            }

            User usuarioLogueado = App.getUsuarioLogueado();
            Investor investorActual = investorService.findByEmail(usuarioLogueado.getEmail());

            if (investorActual == null) {
                mostrarAlertaI18n("investments.alert.profile.error", "investments.alert.profile.msg", Alert.AlertType.ERROR);
                return;
            }

            double unidadesAComprar = montoDinero / activoSeleccionado.getActualPrice();

            // Ejecución segura del negocio
            investmentService.createInvestment(investorActual.getId(), activoSeleccionado.getId(), unidadesAComprar);

            cerrarModal();
            actualizarPantalla(); 
            mostrarAlertaI18n("investments.alert.success", "investments.alert.success.msg", Alert.AlertType.INFORMATION);

        } catch (NumberFormatException e) {
            mostrarAlertaI18n("investments.alert.format.error", "investments.alert.format.msg", Alert.AlertType.ERROR);
        } catch (InsufficientCapitalException e) {
            mostrarAlertaI18n("investments.alert.capital.error", "investments.alert.capital.msg", Alert.AlertType.ERROR);
        } catch (IncompatibleRiskProfileException e) {
            mostrarAlertaI18n("investments.alert.risk.error", "investments.alert.risk.msg", Alert.AlertType.ERROR);
        } catch (Exception e) {
            String tituloError = getTraducido("investments.alert.system.error");
            String patronMensaje = getTraducido("investments.alert.system.msg");
            mostrarAlertaLiteral(tituloError, String.format(patronMensaje, e.getMessage()), Alert.AlertType.ERROR);
        }
    }

    /**
     * Navegación a la pantalla de detalles enviando la inversión seleccionada
     */
   private void irAPantallaDetalles(Investment inversion, String nombreActivo) {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/co/edu/uptc/view/investor/detalleInversion.fxml"));
        
        if (bundle != null) {
            loader.setResources(bundle);
        }

        // 1. Cargar la vista (Esto ejecuta el initialize() automático de JavaFX de forma segura)
        Parent detalleView = loader.load();
        
        // 2. Obtener el controlador de la vista cargada
        DetalleInversionController controller = loader.getController();
        
        // 3. Pasar los datos (El método setInversion ya se encarga de pintar todo de forma reactiva)
        controller.setInversion(inversion);

        // 4. Cambiar la vista central del Dashboard
        if (DashboardController.getInstancia() != null) {
            DashboardController.getInstancia().setVistaCentral(detalleView);
        }
        
    } catch (IOException e) {
        System.err.println("Error abriendo pantalla de detalles: " + e.getMessage());
        e.printStackTrace();
    }
}
    private void ocultarListaInversiones() {
        containerEstadoVacio.setVisible(true);
        containerEstadoVacio.setManaged(true);
        containerListaInversiones.setVisible(false);
        containerListaInversiones.setManaged(false);
    }

    @FXML
    private void abrirModal() {
        if (modalOverlay != null) modalOverlay.setVisible(true);
    }

    @FXML
    private void cerrarModal() {
        if (modalOverlay != null) {
            modalOverlay.setVisible(false);
            txtMonto.clear();
            comboActivos.setValue(null);
        }
    }

    // --- MÉTODOS TRADUCTORES AUXILIARES ---
    private String getTraducido(String llave) {
        if (bundle != null && bundle.containsKey(llave)) {
            return bundle.getString(llave);
        }
        return llave;
    }

    private void mostrarAlertaI18n(String llaveTitulo, String llaveMensaje, Alert.AlertType tipo) {
        mostrarAlertaLiteral(getTraducido(llaveTitulo), getTraducido(llaveMensaje), tipo);
    }

    private void mostrarAlertaLiteral(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}