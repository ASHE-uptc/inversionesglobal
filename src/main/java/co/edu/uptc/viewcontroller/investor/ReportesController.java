package co.edu.uptc.viewcontroller.investor;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
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

public class ReportesController {

    // --- COMPONENTES FXML ---
    @FXML private Label lblNumeroReporte;
    @FXML private Label lblCapitalInicial;
    @FXML private Label lblValorActual;
    @FXML private Label lblRendimiento;
    @FXML private Label lblRiesgo;
    @FXML private ResourceBundle resources;

    // --- SERVICIOS ---
    private final InvestorService investorService = new InvestorService();
    private final AssetService assetService = new AssetService();
    private final InvestmentService investmentService = new InvestmentService(assetService, investorService);

    @FXML
    public void initialize() {
        System.out.println("Inicializando pantalla de Reportes...");
        generarReporte();
    }

    /**
     * Calcula los totales del portafolio y actualiza la tarjeta visual.
     */
    private void generarReporte() {
        User usuarioLogueado = App.getUsuarioLogueado();
        
        if (usuarioLogueado == null) {
            System.err.println("No hay usuario logueado. No se puede generar el reporte.");
            return;
        }

        Investor investorActual = investorService.findByEmail(usuarioLogueado.getEmail());
        
        if (investorActual == null) {
            System.err.println("No se encontró el perfil de inversionista.");
            return;
        }

        // Obtener todas las inversiones del usuario
        List<Investment> inversiones = investmentService.getInvestmentsByInvestorId(investorActual.getId());

        double capitalInicialTotal = 0.0;
        double valorActualTotal = 0.0;

        // Recorrer las inversiones para sumar los valores
        if (inversiones != null) {
            for (Investment inv : inversiones) {
                Asset asset = assetService.findById(inv.getAssetId());
                if (asset != null) {
                    // Valor actual de esta inversión = cantidad de unidades * precio actual del mercado
                    double valorActualInversion = inv.getAmount() * asset.getActualPrice();
                    valorActualTotal += valorActualInversion;

                    // Para calcular el capital inicial, usamos el rendimiento actual para "echar hacia atrás" la fórmula.
                    // (Si en tu modelo Investment tienes un getPurchasePrice(), puedes multiplicarlo directo por el amount).
                    double rendimientoPorcentaje = investmentService.calculateYieldPercentage(inv);
                    double capitalInicialInversion = valorActualInversion / (1 + (rendimientoPorcentaje / 100.0));
                    
                    capitalInicialTotal += capitalInicialInversion;
                }
            }
        }

        // Calcular el rendimiento neto general del portafolio completo
        double rendimientoNetoGeneral = 0.0;
        if (capitalInicialTotal > 0) {
            rendimientoNetoGeneral = ((valorActualTotal - capitalInicialTotal) / capitalInicialTotal) * 100.0;
        }

        // --- ACTUALIZAR LA INTERFAZ (UI) ---
        
        // 1. Número de reporte (podemos poner uno estático o aleatorio por ahora)
        lblNumeroReporte.setText("01"); 

        // 2. Valores de dinero
        lblCapitalInicial.setText(String.format("$ %,.2f", capitalInicialTotal));
        lblValorActual.setText(String.format("$ %,.2f", valorActualTotal));

        // 3. Riesgo del usuario
        // Convertimos el enum a minúsculas para que coincida con la clave del properties
// Ejemplo: "AGGRESSIVE" se convierte en "risk.aggressive"
String riskKey = "risk." + investorActual.getRiskProfile().name().toLowerCase();

// Obtenemos el texto traducido y lo ponemos en el Label
lblRiesgo.setText(resources.getString(riskKey));

        // 4. Rendimiento y colores
        lblRendimiento.setText(String.format("%s%.2f%%", (rendimientoNetoGeneral >= 0 ? "+" : ""), rendimientoNetoGeneral));
        
        // Limpiamos las clases de estilo anteriores por si acaso
        lblRendimiento.getStyleClass().removeAll("report-value-positive", "report-value-negative", "report-value");
        
        // Asignamos el color dependiendo de si hay ganancias (verde) o pérdidas (rojo)
        if (rendimientoNetoGeneral >= 0) {
            lblRendimiento.getStyleClass().add("report-value-positive");
        } else {
            lblRendimiento.getStyleClass().add("report-value-negative");
        }
    }
}