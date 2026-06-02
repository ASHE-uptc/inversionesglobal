package co.edu.uptc.viewcontroller.investor;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;
import co.edu.uptc.app.App;
import co.edu.uptc.model.User;
import co.edu.uptc.model.Investor;
import co.edu.uptc.model.enums.RiskProfile;
import co.edu.uptc.service.InvestorService;
import co.edu.uptc.service.UserService;
import co.edu.uptc.util.I18nManager;

public class CustomizeController {

    UserService userService = new UserService();
    
    @FXML private Button btnGuardarUsername;
    @FXML private Button btnGuardarRiesgo;
    @FXML private ImageView imgPerfilPersonalizacion;
    @FXML private TextField txtUsername;
    @FXML private Button btnEditarUsername;
    @FXML private Button btnGuardarFoto;
    @FXML private Button btnCargarFoto;
    @FXML private ToggleGroup grupoPerfilRiesgo;
    @FXML private RadioButton radioConservador;
    @FXML private RadioButton radioModerado;
    @FXML private RadioButton radioAgresivo;
    @FXML private TextField txtMontoDeposito;
    @FXML private Button btnDepositar;

    private boolean editandoUsername = false;
    private String rutaImagenTemporal = null;
    private ResourceBundle bundle;

    @FXML
    public void initialize() {
        // Inicializar el bundle de idioma global
        try {
            bundle = I18nManager.getInstance().getBundle();
        } catch (Exception e) {
            System.err.println("No se pudo cargar el I18nManager, usando bundle por defecto.");
        }

        User usuarioLogueado = App.getUsuarioLogueado();
        if (usuarioLogueado != null) {
            InvestorService investorService = new InvestorService();
            Investor inversionista = investorService.findByEmail(usuarioLogueado.getEmail());

            if (inversionista != null && inversionista.getName() != null) {
                txtUsername.setText(inversionista.getName());
            } else {
                txtUsername.setText(usuarioLogueado.getEmail());
            }

            cargarFotoPerfil(usuarioLogueado.getProfileImagePath());

            if (inversionista != null && inversionista.getRiskProfile() != null) {
                switch (inversionista.getRiskProfile()) {
                    case CONSERVATIVE -> radioConservador.setSelected(true);
                    case MODERATE -> radioModerado.setSelected(true);
                    case AGGRESSIVE -> radioAgresivo.setSelected(true);
                }
            }
        }
    }

    private void cargarFotoPerfil(String path) {
        try {
            Image avatar;
            if (path != null && path.startsWith("/")) {
                avatar = new Image(getClass().getResourceAsStream(path));
            } else if (path != null && new File(path).exists()) {
                avatar = new Image(new File(path).toURI().toString());
            } else {
                avatar = new Image(getClass().getResourceAsStream("/co/edu/uptc/images/userIconDefault.jpg"));
            }

            imgPerfilPersonalizacion.setImage(avatar);

            double ancho = 140;
            double alto = 140;
            imgPerfilPersonalizacion.setFitWidth(ancho);
            imgPerfilPersonalizacion.setFitHeight(alto);
            imgPerfilPersonalizacion.setPreserveRatio(false);

            Circle clip = new Circle(ancho / 2, alto / 2, ancho / 2);
            imgPerfilPersonalizacion.setClip(clip);
        } catch (Exception e) {
            System.err.println("Error al cargar foto en personalización: " + e.getMessage());
        }
    }

    @FXML
    private void handleDepositarSaldo() {
        String montoTexto = txtMontoDeposito.getText();

        if (montoTexto == null || montoTexto.trim().isEmpty()) {
            mostrarAlertaI18n("customize.alert.empty.fields", "customize.alert.deposit.empty", Alert.AlertType.WARNING);
            return;
        }

        try {
            double montoADepositar = Double.parseDouble(montoTexto.trim());

            if (montoADepositar <= 0) {
                mostrarAlertaI18n("customize.alert.invalid.amount", "customize.alert.deposit.positive", Alert.AlertType.ERROR);
                return;
            }

            User usuarioLogueado = App.getUsuarioLogueado();
            if (usuarioLogueado != null) {
                InvestorService investorService = new InvestorService();
                Investor inversionista = investorService.findByEmail(usuarioLogueado.getEmail());

                if (inversionista != null) {
                    double saldoAnterior = inversionista.getAvailableCapital();
                    double nuevoSaldo = saldoAnterior + montoADepositar;
                    inversionista.setAvailableCapital(nuevoSaldo);

                    investorService.updateInvestor(inversionista);

                    txtMontoDeposito.clear();

                    String patronMensaje = getTraducido("customize.alert.deposit.ok");
                    String mensajeFormateado = String.format(patronMensaje, montoADepositar);
                    
                    mostrarAlertaLiteral(getTraducido("customize.alert.success"), mensajeFormateado, Alert.AlertType.INFORMATION);

                    if (DashboardController.getInstancia() != null) {
                        DashboardController.getInstancia().initialize(null, null);
                    }
                } else {
                    mostrarAlertaI18n("customize.alert.no.profile", "customize.alert.no.json", Alert.AlertType.ERROR);
                }
            }
        } catch (NumberFormatException e) {
            mostrarAlertaI18n("customize.alert.format.error", "customize.alert.format.nan", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleCargarFoto() throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Foto de Perfil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));

        Stage stage = (Stage) imgPerfilPersonalizacion.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            rutaImagenTemporal = file.getAbsolutePath();
            cargarFotoPerfil(rutaImagenTemporal);
        }
    }

    @FXML
    private void handleEditarUsername() {
        if (!editandoUsername) {
            txtUsername.setEditable(true);
            txtUsername.requestFocus();
            txtUsername.selectAll();
            btnEditarUsername.setText(getTraducido("customize.btn.lock"));
            editandoUsername = true;
        } else {
            txtUsername.setEditable(false);
            btnEditarUsername.setText(getTraducido("customize.btn.edit"));
            editandoUsername = false;
        }
    }

    @FXML
    private void handleGuardarFoto() {
        if (rutaImagenTemporal == null) {
            mostrarAlertaI18n("customize.alert.info", "customize.alert.noimage", Alert.AlertType.INFORMATION);
            return;
        }

        User usuarioLogueado = App.getUsuarioLogueado();
        if (usuarioLogueado != null) {
            usuarioLogueado.setProfileImagePath(rutaImagenTemporal);
            userService.updateUserInPersistence(usuarioLogueado);

            mostrarAlertaI18n("customize.alert.success", "customize.alert.avatar.ok", Alert.AlertType.INFORMATION);

            if (DashboardController.getInstancia() != null) {
                DashboardController.getInstancia().initialize(null, null);
            }
        }
    }

    @FXML
    private void handleGuardarUsername() {
        String nuevoNombre = txtUsername.getText();
        User usuarioLogueado = App.getUsuarioLogueado();

        if (usuarioLogueado != null && !nuevoNombre.trim().isEmpty()) {
            InvestorService investorService = new InvestorService();
            Investor inversionista = investorService.findByEmail(usuarioLogueado.getEmail());

            if (inversionista != null) {
                inversionista.setName(nuevoNombre);
                investorService.updateInvestor(inversionista);

                mostrarAlertaI18n("customize.alert.success", "customize.alert.username.ok", Alert.AlertType.INFORMATION);

                txtUsername.setEditable(false);
                btnEditarUsername.setText(getTraducido("customize.btn.edit"));
                editandoUsername = false;

                if (DashboardController.getInstancia() != null) {
                    DashboardController.getInstancia().initialize(null, null);
                }
            }
        }
    }

    @FXML
    private void handleGuardarRiesgo() {
        User usuarioLogueado = App.getUsuarioLogueado();
        RadioButton seleccionado = (RadioButton) grupoPerfilRiesgo.getSelectedToggle();

        if (usuarioLogueado != null && seleccionado != null) {
            InvestorService investorService = new InvestorService();
            Investor inversionista = investorService.findByEmail(usuarioLogueado.getEmail());

            if (inversionista != null) {
                // Evaluamos los IDs de los radio buttons o textos traducidos
                RiskProfile perfilReal;
                if (seleccionado == radioModerado) {
                    perfilReal = RiskProfile.MODERATE;
                } else if (seleccionado == radioAgresivo) {
                    perfilReal = RiskProfile.AGGRESSIVE;
                } else {
                    perfilReal = RiskProfile.CONSERVATIVE;
                }

                inversionista.setRiskProfile(perfilReal);
                investorService.updateInvestor(inversionista);

                mostrarAlertaI18n("customize.alert.success", "customize.alert.risk.ok", Alert.AlertType.INFORMATION);

                if (DashboardController.getInstancia() != null) {
                    DashboardController.getInstancia().initialize(null, null);
                }
            }
        }
    }

    // --- MÉTODOS AUXILIARES DE TRADUCCIÓN ---
    private String getTraducido(String llave) {
        if (bundle != null && bundle.containsKey(llave)) {
            return bundle.getString(llave);
        }
        return llave; // Fallback para depuración
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