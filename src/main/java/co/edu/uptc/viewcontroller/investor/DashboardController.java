package co.edu.uptc.viewcontroller.investor;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    // Guarda la instancia activa del Dashboard para acceso global
    private static DashboardController instanciaGlobal;

    @FXML
    private VBox warningBox;
    @FXML
    private ComboBox<String> idiomaComboBox;
    @FXML
    private Button btnCerrarSesion;
    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private StackPane contentArea; // Vinculado al contenedor dinámico central derecho

    @FXML
    private void mostrarMisInversiones() {
        cambiarCentro("/co/edu/uptc/view/investor/misInversiones.fxml");
    }

    @FXML
    private void mostrarReportes() {
        cambiarCentro("/co/edu/uptc/view/investor/reportes.fxml");
    }

    @FXML
    private void mostrarActivos() {
        cambiarCentro("/co/edu/uptc/view/investor/activosView.fxml");
    }

    /**
     * MÉTODOS ESTÁTICOS DE NAVEGACIÓN GLOBAL
     * Permiten cambiar el contenido de la derecha de forma segura desde subcontroladores
     */
    public static DashboardController getInstancia() {
        return instanciaGlobal;
    }

    public void setVistaCentral(Node nodoVista) {
        if (contentArea != null) {
            contentArea.getChildren().setAll(nodoVista);
        } else {
            // Respaldo por si el nodo contentArea fue sobrescrito
            mainBorderPane.setCenter(nodoVista);
        }
    }

    @FXML
    public void handleCerrarSesion(ActionEvent event) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Cerrar Sesión");
        alerta.setHeaderText("¿Estás seguro de que deseas salir?");
        alerta.setContentText("Cualquier cambio no guardado en la sesión actual podría perderse.");

        ButtonType botonSi = new ButtonType("Sí, salir");
        ButtonType botonNo = new ButtonType("Cancelar");
        alerta.getButtonTypes().setAll(botonSi, botonNo);

        Optional<ButtonType> resultado = alerta.showAndWait();

        if (resultado.isPresent() && resultado.get() == botonSi) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/co/edu/uptc/view/auth/login.fxml"));
                Parent loginRoot = loader.load();

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                boolean estabaMaximizada = stage.isMaximized();

                Scene loginScene = new Scene(loginRoot);
                stage.setScene(loginScene);

                if (estabaMaximizada) {
                    stage.setMaximized(false);
                    stage.setMaximized(true);
                } else {
                    stage.centerOnScreen();
                }
                stage.show();

            } catch (IOException e) {
                e.printStackTrace();
            }
        } 
    }

    private void cambiarCentro(String rutaFxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(rutaFxml));
            Parent nuevaVista = loader.load();
            
            // OPTIMIZACIÓN: Inyectamos la vista DENTRO del contentArea en vez de reemplazar el centro completo
            setVistaCentral(nuevaVista);
            
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error al cargar la sub-vista: " + rutaFxml);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Inicializamos la referencia estática
        instanciaGlobal = this;

        idiomaComboBox.getItems().addAll("es", "en");
        idiomaComboBox.setCellFactory(param -> createCustomCell());
        idiomaComboBox.setButtonCell(createCustomCell());
        idiomaComboBox.getSelectionModel().selectFirst();

        if (warningBox != null) {
            warningBox.setVisible(false);
            warningBox.setManaged(false);
        }
    }

    private ListCell<String> createCustomCell() {
        return new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox container = new HBox(10);
                    container.setAlignment(Pos.CENTER_LEFT);

                    ImageView flagView = new ImageView();
                    flagView.setFitWidth(24);
                    flagView.setFitHeight(16);
                    flagView.setPreserveRatio(true);

                    Label lblName = new Label(item.equals("es") ? "Español" : "English");
                    lblName.setStyle("-fx-text-fill: white; -fx-font-size: 20px;");

                    String imagePath = item.equals("es") ? "/co/edu/uptc/images/colflag.png"
                            : "/co/edu/uptc/images/usaflag.jpg";

                    try {
                        java.io.InputStream stream = getClass().getResourceAsStream(imagePath);
                        if (stream != null) {
                            flagView.setImage(new Image(stream));
                            container.getChildren().addAll(flagView, lblName);
                        } else {
                            container.getChildren().add(lblName);
                        }
                    } catch (Exception e) {
                        container.getChildren().add(lblName);
                    }

                    setGraphic(container);
                }
            }
        };
    }
}