package co.edu.uptc.viewcontroller.investor;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MisInversionesController {

    // --- COMPONENTES PRINCIPALES ---
    @FXML private Label saldoLabel;
    @FXML private VBox containerEstadoVacio;
    @FXML private VBox containerListaInversiones;
    @FXML private Button btnNuevaInversion;

    // --- COMPONENTES DEL MODAL FLOTANTE ---
    @FXML private StackPane modalOverlay;
    @FXML private ComboBox<String> comboActivos;
    @FXML private TextField txtMonto;

    // Lista simulada de inversiones activas
    private final List<InversionSimulada> listaDeInversiones = new ArrayList<>();
    private double saldoDisponible = 20000.00;

    @FXML
    public void initialize() {
        System.out.println("Inicializando panel de control de inversiones...");
        
        // 1. Llenar el ComboBox del modal con activos de prueba
        if (comboActivos != null) {
            comboActivos.setItems(FXCollections.observableArrayList(
                "Bitcoin (BTC)", "Ethereum (ETH)", "Solana (SOL)", "S&P 500 ETF", "Acciones Apple (AAPL)"
            ));
        }

        // 2. Renderizar la UI inicial basada en si hay datos o no
        actualizarPantalla();
    }

    /**
     * Evalúa el estado de la lista y decide qué componentes mostrar u ocultar
     */
    private void actualizarPantalla() {
        // Actualizar el texto del saldo disponible
        if (saldoLabel != null) {
            saldoLabel.setText(String.format("$%,.2f", saldoDisponible));
        }

        if (listaDeInversiones.isEmpty()) {
            // ESTADO VACÍO: Muestra la lupa, oculta la lista
            containerEstadoVacio.setVisible(true);
            containerEstadoVacio.setManaged(true);
            
            containerListaInversiones.setVisible(false);
            containerListaInversiones.setManaged(false);
        } else {
            // ESTADO CON DATOS: Oculta la lupa, muestra la lista y la dibuja
            containerEstadoVacio.setVisible(false);
            containerEstadoVacio.setManaged(false);
            
            containerListaInversiones.setVisible(true);
            containerListaInversiones.setManaged(true);

            renderizarListaInversiones();
        }
    }

    /**
     * Construye dinámicamente las filas (cards) en JavaFX con el estilo de la captura 2
     */
    private void renderizarListaInversiones() {
        containerListaInversiones.getChildren().clear(); // Limpiar UI previa

        for (InversionSimulada inv : listaDeInversiones) {
            // Crear contenedor de la fila (HBox)
            HBox fila = new HBox();
            fila.getStyleClass().add("investment-item-row");
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.setSpacing(20.0);
            fila.setPadding(new Insets(20, 25, 20, 25));

            // Bloque Izquierdo: Nombre y detalles del activo
            VBox infoIzquierda = new VBox(5.0);
            HBox.setHgrow(infoIzquierda, Priority.ALWAYS);
            Label lblTitulo = new Label(inv.getNombre());
            lblTitulo.getStyleClass().add("item-title");
            Label lblSub = new Label("Activa • " + inv.getDetalleUnidades());
            lblSub.getStyleClass().add("item-subtitle");
            infoIzquierda.getChildren().addAll(lblTitulo, lblSub);

            // Bloque Derecho: Rendimiento estático simulado
            VBox infoDerecha = new VBox(5.0);
            infoDerecha.setAlignment(Pos.CENTER_RIGHT);
            Label lblPerfLabel = new Label("Rendimiento");
            lblPerfLabel.getStyleClass().add("item-perf-label");
            Label lblPerfValue = new Label("+17%"); // Simulado estático para coincidir con tu diseño
            lblPerfValue.getStyleClass().add("item-perf-value-positive");
            infoDerecha.getChildren().addAll(lblPerfLabel, lblPerfValue);

            // Flecha indicadora de navegación ">"
            Label flecha = new Label(">");
            flecha.getStyleClass().add("item-arrow");

            // Meter todo a la fila
            fila.getChildren().addAll(infoIzquierda, infoDerecha, flecha);

            // EVENTO CLICK: Ir a la pantalla de detalles de esta inversión
            fila.setOnMouseClicked(event -> irAPantallaDetalles(inv));

            // Agregar la fila construida al contenedor principal de la vista
            containerListaInversiones.getChildren().add(fila);
        }
    }

    // --- ACCIONES DEL MODAL FLOTANTE (OPCIÓN A) ---

    @FXML
    private void abrirModal() {
        if (modalOverlay != null) {
            modalOverlay.setVisible(true); // Hace aparecer el formulario encima
        }
    }

    @FXML
    private void cerrarModal() {
        if (modalOverlay != null) {
            modalOverlay.setVisible(false); // Oculta el formulario
            // Limpiar campos
            txtMonto.clear();
            comboActivos.setValue(null);
        }
    }

    @FXML
    private void confirmarInversion() {
        String activoSeleccionado = comboActivos.getValue();
        String montoTexto = txtMonto.getText();

        if (activoSeleccionado == null || montoTexto.isEmpty()) {
            System.out.println("Por favor completa los campos del formulario.");
            return; 
        }

        try {
            double monto = Double.parseDouble(montoTexto);
            if (monto > saldoDisponible) {
                System.out.println("Saldo insuficiente.");
                return;
            }

            // Restar del saldo e ingresar inversión simulada
            saldoDisponible -= monto;
            double unidadesSimuladas = monto / 42000.0; // Simulación de precio base para unidades
            
            listaDeInversiones.add(new InversionSimulada(
                activoSeleccionado, 
                String.format("%.4f unidades", unidadesSimuladas),
                monto
            ));

            cerrarModal();
            actualizarPantalla(); // Recarga la UI automáticamente cambiando de estado
            
        } catch (NumberFormatException e) {
            System.out.println("Monto ingresado no válido.");
        }
    }

    /**
     * Carga el FXML de detalles en el contenedor principal de tu Dashboard
     */
    private void irAPantallaDetalles(InversionSimulada inversion) {
    try {
        System.out.println("Navegando a detalles de: " + inversion.getNombre());
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/co/edu/uptc/view/investor/detalleInversion.fxml"));
        Parent detalleView = loader.load();
        
        DetalleInversionController controller = loader.getController();
        controller.setDatosInversion(inversion.getNombre(), inversion.getDetalleUnidades(), inversion.getMontoInicial());

        // Lógica unificada: Le enviamos la nueva vista al manejador central del Dashboard
        if (DashboardController.getInstancia() != null) {
            DashboardController.getInstancia().setVistaCentral(detalleView);
        }
        
    } catch (IOException e) {
        System.err.println("Error al redirigir al panel de detalles: " + e.getMessage());
        e.printStackTrace();
    }
}
    // --- CLASE ANIDADA AUXILIAR PARA PASAR DATOS ---
    public static class InversionSimulada {
        private final String nombre;
        private final String detalleUnidades;
        private final double montoInicial;

        public InversionSimulada(String nombre, String detalleUnidades, double montoInicial) {
            this.nombre = nombre;
            this.detalleUnidades = detalleUnidades;
            this.montoInicial = montoInicial;
        }
        public String getNombre() { return nombre; }
        public String getDetalleUnidades() { return detalleUnidades; }
        public double getMontoInicial() { return montoInicial; }
    }
}