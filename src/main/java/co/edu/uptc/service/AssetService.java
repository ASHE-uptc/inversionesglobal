package co.edu.uptc.service;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Random;

import com.google.gson.reflect.TypeToken;

import co.edu.uptc.exception.AssetNotFoundException;
import co.edu.uptc.model.Asset;
import co.edu.uptc.model.enums.AssetType;
import co.edu.uptc.persistence.JsonRepository;

public class AssetService {
    private java.util.Timer fluctuationTimer;

    private final JsonRepository<Asset> repo;

    /**
     * Crea el servicio usando la ruta de persistencia correcta del proyecto
     * modular.
     */
    public AssetService() {
        Type type = new TypeToken<List<Asset>>() {
        }.getType();
        // Mantenemos la ruta relativa limpia que soluciona el conflicto de carpetas
        this.repo = new JsonRepository<>("src\\main\\resources\\data\\asset.json", type);
    }

    /**
     * Crea el servicio con un repositorio inyectado (útil para pruebas).
     *
     * @param repo repositorio JSON de activos
     */
    public AssetService(JsonRepository<Asset> repo) {
        this.repo = repo;
    }

    /**
     * Registra un activo con código (identificador), nombre, tipo, precio actual y
     * volatilidad.
     * Soporta cualquier formato de ID alfanumérico de manera ilimitada y elástica.
     */
    public void createAsset(String name, AssetType assetType, double actualPrice, double volatility) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("ASSET_NAME_CANNOT_BE_EMPTY");
        }
        if (assetType == null) {
            throw new IllegalArgumentException("ASSET_TYPE_REQUIRED");
        }
        if (actualPrice <= 0) { // Un activo no debería costar 0 al crearse
            throw new IllegalArgumentException("PRICE_MUST_BE_GREATER_THAN_ZERO");
        }
        if (volatility < 0 || volatility > 1.0) { // Rango porcentual (ej: 0.15 para 15%)
            throw new IllegalArgumentException("VOLATILITY_MUST_BE_BETWEEN_0_AND_1");
        }

        try {
            String generatedId = java.util.UUID.randomUUID().toString();
            repo.save(new Asset(generatedId, name.trim(), assetType, actualPrice, volatility));
        } catch (RuntimeException e) {
            throw new RuntimeException("Error trying to save the asset.", e);
        }
    }

    /**
     * Devuelve todos los activos almacenados (consulta general).
     *
     * @return lista de {@link Asset}; puede estar vacía
     */
    public List<Asset> listAssets() {
        try {
            return repo.findAll();
        } catch (RuntimeException e) {
            throw new RuntimeException("Error trying to list the assets.", e);
        }
    }

    /**
     * Devuelve todos los activos almacenados. 
     * Alias de listAssets() usado por el controlador de administración.
     */
    public List<Asset> findAll() {
        return listAssets();
    }

    /**
     * Consulta activos filtrando por tipo (Acción, Bono, ETF).
     */
    public List<Asset> findByType(AssetType t) {
        if (t == null)
            return List.of();
        return repo.findAll().stream()
                .filter(asset -> asset.getAssetType() == t)
                .toList();
    }

    /**
     * Busca un activo por su identificador de manera segura y agnóstica a espacios.
     *
     * @param id identificador del activo
     * @return el {@link Asset} encontrado, o {@code null} si no existe
     */
    public Asset findById(String id) {
        if (id == null || id.isBlank())
            return null;
        String target = id.trim();

        // Forzar a leer el JSON actualizado en lugar de usar una lista estática vieja
        return repo.findAll().stream()
                .filter(asset -> asset.getId().equalsIgnoreCase(target))
                .findFirst()
                .orElse(null);
    }

    /**
     * Consulta activos filtrando por un rango de precios inclusivo.
     */
    public List<Asset> findByPriceRange(double minPrice, double maxPrice) {
        return repo.findAll().stream()
                .filter(asset -> asset.getActualPrice() >= minPrice && asset.getActualPrice() <= maxPrice)
                .toList();
    }

    /**
     * Actualiza el precio del activo en el mercado.
     * Al consultar las inversiones, estas recalcularán sus rendimientos
     * automáticamente
     * en base a este nuevo valor.
     */
    public void updateAssetPrice(String assetId, double newPrice) {

        if (assetId == null)
            throw new IllegalArgumentException("ASSET_ID_REQUIRED");
        if (newPrice < 0) {
            throw new IllegalArgumentException("NEGATIVE_PRICE_OR_VOLATILITY");
        }

        List<Asset> assets = repo.findAll();

        boolean isUpdated = false;

        String targetId = assetId.trim();

        for (Asset asset : assets) {
            if (asset.getId().equalsIgnoreCase(targetId)) {
                asset.setActualPrice(newPrice);
                isUpdated = true;
                break;
            }
        }
        if (isUpdated) {
            repo.replaceAll(assets);
        } else {
            throw new AssetNotFoundException(assetId);
        }
    }

    /**
     * Simula la fluctuación aleatoria del mercado aplicando rangos fijos de dinero
     * según el nivel de riesgo del activo (Regla solicitada por el docente).
     * Los valores están convertidos a la moneda base (USD) para mantener la
     * consistencia.
     */
    public void simulateMarketFluctuation() {
        List<Asset> assets = repo.findAll();
        if (assets.isEmpty())
            return;

        java.util.Random random = new java.util.Random();

        for (Asset asset : assets) {
            int riskLevel = asset.getAssetType().getRiskLevel();
            double maxDeltaUSD = 0.0;

            // Clasificación según la regla del profesor (Convertido proporcionalmente a
            // USD)
            switch (riskLevel) {
                case 1:
                case 2:
                    // Conservadores / Bajo Riesgo (Variación de max $500 COP -> $0.125 USD)
                    maxDeltaUSD = 0.125;
                    break;
                case 3:
                    // Moderados (Variación de max $1000 COP -> $0.25 USD)
                    maxDeltaUSD = 0.25;
                    break;
                case 4:
                case 5:
                    // Arriesgados (Variación mayor a $1000 COP, ej: max $3000 COP -> $0.75 USD)
                    maxDeltaUSD = 0.75;
                    break;
                default:
                    maxDeltaUSD = 0.10;
            }

            // random.nextDouble() * 2 - 1 genera un número entre -1.0 y +1.0
            // Al multiplicarlo por maxDeltaUSD, el precio subirá o bajará dentro del rango
            // permitido
            double priceChange = (random.nextDouble() * 2 - 1) * maxDeltaUSD;
            double newPrice = asset.getActualPrice() + priceChange;

            // Protección obligatoria: El activo no puede valer cero o ser negativo
            if (newPrice < 0.01) {
                newPrice = 0.01;
            }

            // Guardamos el precio redondeado a dos decimales
            asset.setActualPrice(Math.round(newPrice * 100.0) / 100.0);
        }

        // Guardar los precios fluctuados en el JSON
        repo.replaceAll(assets);
    }
    /**
     * Inicia un temporizador en segundo plano que actualiza los precios
     * automáticamente cada cierto tiempo.
     * * @param intervalSeconds Cada cuántos segundos cambiarán los precios.
     */
    public void startAutoFluctuation(int intervalSeconds) {
        // Si ya hay un timer corriendo, lo detenemos primero
        if (fluctuationTimer != null) {
            fluctuationTimer.cancel();
        }
        
        // El parámetro 'true' indica que es un hilo "Daemon" (se cerrará si cierras la app)
        fluctuationTimer = new java.util.Timer(true); 
        
        fluctuationTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                // Ejecutamos tu método existente
                simulateMarketFluctuation();
                System.out.println("Fluctuación de mercado ejecutada. Archivo JSON actualizado.");
            }
        }, 0, intervalSeconds * 1000L); // Convertimos segundos a milisegundos
    }

    /**
     * Detiene la fluctuación automática.
     */
    public void stopAutoFluctuation() {
        if (fluctuationTimer != null) {
            fluctuationTimer.cancel();
            fluctuationTimer = null;
        }
    }
    private static AssetService instance;

// 2. Añade este método estático para obtener la instancia única
public static synchronized AssetService getInstance() {
    if (instance == null) {
        instance = new AssetService();
    }
    return instance;
}

}
