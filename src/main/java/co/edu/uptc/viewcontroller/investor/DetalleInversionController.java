package co.edu.uptc.viewcontroller.investor;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class DetalleInversionController {

    // --- COMPONENTES INYECTADOS DESDE FXML ---
    @FXML private Label lblNombreActivo;
    @FXML private Label lblUnidades;
    @FXML private Label lblMontoInicial;
    @FXML private Label lblValorActual;
    @FXML private LineChart<String, Number> chartRendimiento;

    @FXML
    public void initialize() {
        // Aquí puedes realizar ajustes de inicio si lo requieres.
        // Nota: El truco de quitar los círculos de los puntos de la gráfica se maneja desde el CSS.
    }

    /**
     * Este método es invocado por el controlador anterior pasando los datos de la inversión
     */
    public void setDatosInversion(String nombre, String unidades, double montoInicial) {
        // 1. Asignar los textos a las etiquetas correspondientes
        if (lblNombreActivo != null) lblNombreActivo.setText(nombre);
        if (lblUnidades != null) lblUnidades.setText(unidades);
        if (lblMontoInicial != null) lblMontoInicial.setText(String.format("$%,.2f", montoInicial));

        // 2. Calcular el valor actual simulando el +17% de rendimiento de tu captura
        double rendimientoFactor = 1.17; 
        double valorActual = montoInicial * rendimientoFactor;
        if (lblValorActual != null) lblValorActual.setText(String.format("$%,.2f", valorActual));

        // 3. Dibujar la línea de tiempo en el LineChart basados en el monto inicial y final
        generarDatosGrafico(montoInicial, valorActual);
    }

    /**
     * Genera una curva de comportamiento financiero realista (subidas y bajadas) 
     * que termina exactamente en el valor actual de la inversión.
     */
    private void generarDatosGrafico(double base, double actual) {
        if (chartRendimiento == null) return;

        chartRendimiento.getData().clear(); // Limpiar comportamientos de gráficos anteriores

        XYChart.Series<String, Number> serieFinanciera = new XYChart.Series<>();

        // Puntos de la curva simulando comportamiento del mercado (Semana a Semana)
        serieFinanciera.getData().add(new XYChart.Data<>("Sem 1", base));
        serieFinanciera.getData().add(new XYChart.Data<>("Sem 2", base * 0.94));  // Caída de mercado
        serieFinanciera.getData().add(new XYChart.Data<>("Sem 3", base * 1.08));  // Recuperación
        serieFinanciera.getData().add(new XYChart.Data<>("Sem 4", base * 1.03));  // Corrección lateral
        serieFinanciera.getData().add(new XYChart.Data<>("Sem 5", actual));       // Cierre en profit (+17%)

        // Inyectar la serie de datos al componente gráfico
        chartRendimiento.getData().add(serieFinanciera);
    }

    /**
     * Acción vinculada al botón "← Volver". Reempuja la vista de Mis Inversiones al contentArea.
     */
@FXML
private void volverAInversiones() {
    try {
        System.out.println("Regresando a Mis Inversiones...");
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/co/edu/uptc/view/investor/misInversiones.fxml"));
        Parent inversionesView = loader.load();

        if (DashboardController.getInstancia() != null) {
            DashboardController.getInstancia().setVistaCentral(inversionesView);
        }
        
    } catch (IOException e) {
        System.err.println("Error al regresar a la lista de inversiones: " + e.getMessage());
        e.printStackTrace();
    }
}
}