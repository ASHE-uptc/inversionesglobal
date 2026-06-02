package co.edu.uptc.service;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.google.gson.reflect.TypeToken;

import co.edu.uptc.exception.IncompatibleRiskProfileException;
import co.edu.uptc.exception.InsufficientCapitalException;
import co.edu.uptc.model.Asset;
import co.edu.uptc.model.Investment;
import co.edu.uptc.model.Investor;
import co.edu.uptc.model.enums.AssetType;
import co.edu.uptc.model.enums.RiskProfile;
import co.edu.uptc.persistence.JsonRepository;

/**
 * Servicio de inversiones individuales: creación y liquidación con validaciones de negocio 
 * (capital y perfil de riesgo frente al activo) y cálculos de rendimiento en tiempo real.
 */
public class InvestmentService {
    private final JsonRepository<Investment> repo;
    private final AssetService assetService;
    private final InvestorService investorService; // Inyectado para garantizar atomicidad global
    private static InvestmentService instance;
    public static synchronized InvestmentService getInstance() {
    if (instance == null) {
        instance = new InvestmentService(AssetService.getInstance(), InvestorService.getInstance());
    }
    return instance;
    }

    /**
     * Constructor principal unificado. Recibe los servicios core compartidos por la UI.
     */
    public InvestmentService(AssetService assetService, InvestorService investorService) {
        Type type = new TypeToken<List<Investment>>() {}.getType();
        // Corregido: Ruta universal con '/' compatible con cualquier S.O.
        this.repo = new JsonRepository<>("src\\main\\resources\\data\\investment.json", type);
        this.assetService = assetService;
        this.investorService = investorService;
    }

    /**
     * Constructor secundario útil para entornos de pruebas o mocks.
     */
    public InvestmentService(JsonRepository<Investment> repo, AssetService assetService, InvestorService investorService) {
        this.repo = repo;
        this.assetService = assetService;
        this.investorService = investorService;
    }

    /**
     * Crea y registra una nueva inversión aplicando reglas de negocio financieras atómicas.
     * Descuenta automáticamente el dinero de la billetera del inversionista.
     */
    public Investment createInvestment(String investorId, String assetId, double amount) {
        if (investorId == null || investorId.isBlank() || assetId == null || assetId.isBlank()) {
            throw new IllegalArgumentException("IDENTIFIERS_CANNOT_BE_EMPTY");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("INVALID_AMOUNT");
        }

        String cleanedInvestorId = investorId.trim();
        String cleanedAssetId = assetId.trim();

        // 1. Recuperar el estado fresco del inversionista desde la persistencia única
        Investor investorRef = investorService.findById(cleanedInvestorId);
        if (investorRef == null) {
            throw new IllegalArgumentException("INVESTOR_NOT_FOUND");
        }

        // 2. Recuperar el activo para capturar su precio fluctuado de este milisegundo
        Asset assetRef = assetService.findById(cleanedAssetId);
        if (assetRef == null) {
            throw new IllegalArgumentException("ASSET_NOT_FOUND");
        }

        // 3. Inversión Inicial = Precio actual del activo en mercado * Cantidad comprada
        double totalInvestmentCost = calculatePurchasePrice(assetRef.getActualPrice(), amount);
        
        // 4. Validación de Capital disponible contra la billetera del objeto Investor
        if (!validateAvailableCapital(investorRef.getAvailableCapital(), totalInvestmentCost)) {
            throw new InsufficientCapitalException("Capital insuficiente para registrar la inversión.");
        }

        // 5. Validación de Perfil de Riesgo
        validateRiskProfile(investorRef.getRiskProfile(), assetRef.getAssetType());

        // 6. Generación automática del ID de la transacción
        String generatedId = UUID.randomUUID().toString();

        Investment investment = new Investment(
            generatedId, 
            cleanedInvestorId, 
            cleanedAssetId, 
            amount, 
            totalInvestmentCost, 
            LocalDate.now(), 
            LocalTime.now()
        );

        // 7. PERSISTENCIA ATÓMICA Y COMPARTIDA
        try {
            // Guardar en el historial de transacciones globales (investment.json)
            repo.save(investment);

            // Descontar capital de la instancia viva del inversionista
            investorRef.setAvailableCapital(investorRef.getAvailableCapital() - totalInvestmentCost);
            
            if (investorRef.getInvestments() == null) {
                investorRef.setInvestments(new java.util.ArrayList<>());
            }
            investorRef.getInvestments().add(investment);
            
            // Forzar la reescritura en investor.json sincronizado
            investorService.updateInvestor(investorRef);

        } catch (RuntimeException e) {
            throw new RuntimeException("Error trying to process and synchronize the financial transaction.", e);
        }

        return investment;
    }

    /**
     * Ejecuta la venta de una inversión específica.
     * Calcula el valor actual en el mercado, le devuelve el efectivo al inversionista
     * y remueve la inversión de su portafolio activo de forma atómica.
     */
    public void sellInvestment(String investorId, String investmentId) {
        if (investorId == null || investmentId == null) {
            throw new IllegalArgumentException("IDENTIFIERS_CANNOT_BE_EMPTY");
        }

        // 1. Validar que el inversionista exista
        Investor investorRef = investorService.findById(investorId.trim());
        if (investorRef == null) {
            throw new IllegalArgumentException("INVESTOR_NOT_FOUND");
        }

        // 2. Buscar la inversión dentro del historial global utilizando predicados genéricos
        Investment investmentRef = repo.findBy(inv -> inv.getId().equalsIgnoreCase(investmentId.trim()))
                                       .orElse(null);
        if (investmentRef == null) {
            throw new IllegalArgumentException("INVESTMENT_NOT_FOUND");
        }

        // 3. Buscar el activo para saber a qué precio se cotiza HOY en el mercado
        Asset assetRef = assetService.findById(investmentRef.getAssetId());
        if (assetRef == null) {
            throw new IllegalArgumentException("ASSET_NOT_FOUND_IN_MARKET");
        }

        // 4. CÁLCULO FINANCIERO: Dinero a retornar = Unidades poseídas * Precio actual fluctuado
        double cashToReturn = investmentRef.getAmount() * assetRef.getActualPrice();

        // 5. SINCRONIZACIÓN ATÓMICA DE BASES DE DATOS (JSONs)
        try {
            // A. Eliminar la inversión del historial de transacciones activas (investment.json)
            repo.deleteBy(inv -> inv.getId().equalsIgnoreCase(investmentId.trim()));

            // B. Devolverle el dinero a la billetera del inversionista
            investorRef.setAvailableCapital(investorRef.getAvailableCapital() + cashToReturn);

            // C. Quitar la inversión de su lista interna de portafolio
            if (investorRef.getInvestments() != null) {
                investorRef.getInvestments().removeIf(inv -> inv.getId().equalsIgnoreCase(investmentId.trim()));
            }

            // D. Guardar los cambios actualizados en investor.json
            investorService.updateInvestor(investorRef);

        } catch (RuntimeException e) {
            throw new RuntimeException("Critical error synchronizing data during investment sale.", e);
        }
    }

    /**
     * Devuelve todas las inversiones registradas en persistencia.
     */
    public List<Investment> listInvestments() {
        try {
            return repo.findAll();
        } catch (RuntimeException e) {
            throw new RuntimeException("Error trying to list the inversions.", e);
        }
    }

    public double calculatePurchasePrice(double assetPrice, double amount) {
        return assetPrice * amount;
    }

    public double calculateEarnings(double actualValue, double initialInvestment) {
        return actualValue - initialInvestment;
    }

    public List<Investment> getInvestmentsByInvestorId(String investorId) {
        if (investorId == null) return List.of();
        String target = investorId.trim();
        return repo.findAll().stream()
                .filter(inv -> inv.getInversionistId().equalsIgnoreCase(target))
                .toList();
    }

    public boolean validateAvailableCapital(double availableCapital, double initialInvestment) {
        return availableCapital >= initialInvestment;
    }

    public void validateRiskProfile(RiskProfile riskProfile, AssetType assetType) {
        if (riskProfile == null || assetType == null) {
            throw new IllegalArgumentException("RISK_PROFILE_AND_ASSET_TYPE_REQUIRED");
        }
        if (assetType.getRiskLevel() > riskProfile.getMaxRisk()) {
            throw new IncompatibleRiskProfileException("Risk Profile " + riskProfile + " does not allow investing in " + assetType);
        }
    }

    public void updateAssetPriceProcess(String assetId, double newPrice) {
        assetService.updateAssetPrice(assetId, newPrice);
    }

    public double calculateCurrentValue(Investment investment) {
        if (investment == null) return 0.0;
        Asset asset = assetService.findById(investment.getAssetId());
        
        if (asset != null) {
            return investment.getAmount() * asset.getActualPrice();
        }
        return 0.0;
    }

    public double calculateYieldPercentage(Investment investment) {
        if (investment == null || investment.getPurchasePrice() <= 0) return 0.0;
        
        double currentVal = calculateCurrentValue(investment);
        double initialInvestment = investment.getPurchasePrice();
        
        return ((currentVal - initialInvestment) / initialInvestment) * 100;
    }
}