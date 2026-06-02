package co.edu.uptc.service;

import java.time.LocalDate;
import java.util.List;

import co.edu.uptc.model.Asset;
import co.edu.uptc.model.Investment;
import co.edu.uptc.model.Investor;

/**
 * Servicio de agregación de portafolio: cálculos avanzados sobre conjuntos de inversiones,
 * reportes por periodos temporales, gestión de riesgo ponderado y rankings globales.
 */
public class PortfolioService {
    private final InvestorService investorService;
    private final InvestmentService inversionService;
    private final AssetService assetService;
    private static PortfolioService instance;
    public static synchronized PortfolioService getInstance() {
    if (instance == null) {
        instance = new PortfolioService(InvestmentService.getInstance(), AssetService.getInstance(), InvestorService.getInstance());
    }
    return instance;
    }

    /**
     * Construye el servicio de portafolio inyectando las dependencias core del sistema.
     */
    public PortfolioService(InvestmentService inversionService, AssetService assetService, InvestorService investorService) {
        this.inversionService = inversionService;
        this.assetService = assetService;
        this.investorService = investorService; 
    }

    /**
     * Calcula la suma de ganancias o pérdidas monetarias en un intervalo de fechas (inclusive).
     */
    public double calculateEarningsByPeriod(List<Investment> inversions, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("DATES_CANNOT_BE_NULL");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }

        double total = 0;

        for (Investment inv : inversions) {
            // Validación inclusiva del rango de fechas
            if ((inv.getDate().isEqual(startDate) || inv.getDate().isAfter(startDate))
                    && (inv.getDate().isEqual(endDate) || inv.getDate().isBefore(endDate))) {

                double actualValue = inversionService.calculateCurrentValue(inv);
                double initialInvestment = inv.getPurchasePrice();

                total += inversionService.calculateEarnings(actualValue, initialInvestment);
            }
        }
        return total;
    }

    /**
     * Devuelve los 5 inversionistas con mayor rendimiento porcentual histórico o activo.
     * Corregido: Ya no discrimina a los usuarios que vendieron todo y tienen su capital en efectivo.
     */
    public List<Investor> getTop5InvestorsByYield() {
        List<Investor> allInvestors = investorService.listInversionists();

        return allInvestors.stream()
                // Se ordenan de forma descendente evaluando su rendimiento total
                .sorted((inv1, inv2) -> Double.compare(calculateYieldPercentage(inv2), calculateYieldPercentage(inv1)))
                .limit(5)
                .toList();
    }

    /**
     * Calcula el monto total que el usuario ha desembolsado en sus posiciones activas.
     */
    public double calculateTotalInvested(Investor investor) {
        if (investor == null || investor.getInvestments() == null || investor.getInvestments().isEmpty()) {
            return 0.0;
        }
        return investor.getInvestments().stream()
                .mapToDouble(Investment::getPurchasePrice)
                .sum();
    }

    /**
     * Calcula el valor de liquidación actual de todo el portafolio en base al mercado flotante.
     */
    public double calculateCurrentPortfolioValue(Investor investor) {
        if (investor == null || investor.getInvestments() == null || investor.getInvestments().isEmpty()) {
            return 0.0;
        }
        
        return investor.getInvestments().stream()
                .mapToDouble(inversionService::calculateCurrentValue) // Reutiliza la fórmula unificada de InvestmentService
                .sum();
    }

    /**
     * Calcula el rendimiento porcentual total acumulado del inversionista.
     * Soporta de manera segura portafolios temporalmente liquidados.
     */
    public double calculateYieldPercentage(Investor investor) {
        if (investor == null) return 0.0;
        
        double totalInvested = calculateTotalInvested(investor); 
        double currentValue = calculateCurrentPortfolioValue(investor);

        if (totalInvested == 0) return 0.0;
        
        // Formula: (($Valor Actual - Inversion Inicial) / Inversion Inicial) * 100$
        return ((currentValue - totalInvested) / totalInvested) * 100.0;
    }

    /**
     * Calcula el riesgo (volatilidad promedio) ponderado del portafolio.
     * Si un activo representa el 80% de tu portafolio, su volatilidad impactará en un 80% al riesgo total.
     */
    public double calculatePortfolioRisk(Investor investor) {
        if (investor == null) return 0.0;
        
        double totalValue = calculateCurrentPortfolioValue(investor);
        if (totalValue == 0) return 0.0;

        double weightedRiskSum = 0.0;
        List<Investment> investments = investor.getInvestments();
        
        if (investments != null) {
            for (Investment inv : investments) {
                Asset asset = assetService.findById(inv.getAssetId());
                if (asset != null) {
                    double currentInvValue = inversionService.calculateCurrentValue(inv);
                    // Ponderación: (Valor de la inversión individual * Volatilidad del activo)
                    weightedRiskSum += (currentInvValue * asset.getVolatility());
                }
            }
        }
        return weightedRiskSum / totalValue;
    }
}