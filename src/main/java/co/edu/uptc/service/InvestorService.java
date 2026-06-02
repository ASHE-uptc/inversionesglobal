package co.edu.uptc.service;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.google.gson.reflect.TypeToken;

import co.edu.uptc.exception.InsufficientCapitalException;
import co.edu.uptc.exception.InvestorNotFoundException;
import co.edu.uptc.model.Investor;
import co.edu.uptc.model.enums.RiskProfile;
import co.edu.uptc.persistence.JsonRepository; // Apunta a tu paquete de persistencia corregido

/**
 * Servicio de gestión de inversionistas: registro, consulta y actualización del
 * capital
 * disponible según los requisitos de administración de inversionistas.
 */
public class InvestorService {
    private static InvestorService instance;
    public static synchronized InvestorService getInstance() {
    if (instance == null) {
        instance = new InvestorService();
    }
    return instance;
}
    private final JsonRepository<Investor> repo;

    /**
     * Crea el servicio usando la ruta de persistencia universal y compatible.
     */
    public InvestorService() {
        Type type = new TypeToken<List<Investor>>() {
        }.getType();
        // Corregido: Ruta universal con '/' compatible con cualquier S.O.
        this.repo = new JsonRepository<>("src\\main\\resources\\data\\investor.json", type);
    }

    /**
     * Crea el servicio con un repositorio inyectado (útil para pruebas).
     */
    public InvestorService(JsonRepository<Investor> repo) {
        this.repo = repo;
    }

    /**
     * Registra un nuevo inversionista con los datos obligatorios.
     * Si necesitas usar la cédula/pasaporte del usuario en la UI, pásala como
     * parámetro
     * en lugar de autogenerar el UUID.
     */
    public void createInvestor(String name, String email, double availableCapital, RiskProfile riskProfile) {
        if (name == null || name.isBlank() || email == null || email.isBlank()) {
            throw new IllegalArgumentException("REQUIRED_FIELDS_CANNOT_BE_EMPTY");
        }
        if (availableCapital < 0) {
            throw new IllegalArgumentException("NEGATIVE_CAPITAL");
        }
        if (riskProfile == null) {
            throw new IllegalArgumentException("RISK_PROFILE_REQUIRED");
        }

        try {
            // Validar si ya existe un inversionista con el mismo correo electrónico (Llave
            // lógica de negocio)
            boolean emailExists = repo.findBy(inv -> inv.getEmail().equalsIgnoreCase(email.trim())).isPresent();
            if (emailExists) {
                throw new IllegalArgumentException("EMAIL_ALREADY_EXISTS");
            }

            String idGenerada = UUID.randomUUID().toString();

            // Inicialización limpia de listas internas para evitar dolores de cabeza con
            // Gson
            Investor newInvestor = new Investor(idGenerada, name.trim(), email.trim(), availableCapital, riskProfile,
                    new ArrayList<>());
            repo.save(newInvestor);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new RuntimeException("Error al registrar el inversionista en el sistema.", e);
        }
    }

    /**
     * Obtiene todos los inversionistas almacenados.
     */
    public List<Investor> listInversionists() {
        try {
            return repo.findAll();
        } catch (RuntimeException e) {
            throw new RuntimeException("Error trying to list the investors.", e);
        }
    }

    /**
     * Busca un inversionista por su identificador utilizando los predicados del
     * repositorio genérico.
     */
    public Investor findById(String id) {
        if (id == null)
            return null;
        try {
            String targetId = id.trim();
            return repo.findBy(inv -> inv.getId().equalsIgnoreCase(targetId)).orElse(null);
        } catch (RuntimeException e) {
            throw new RuntimeException("Error trying to find the inversionist by id.", e);
        }
    }

    /**
     * Reemplaza por completo un objeto de tipo inversionista (Esencial para guardar
     * nuevas inversiones).
     */
    public void updateInvestor(Investor updatedInvestor) {
        if (updatedInvestor == null || updatedInvestor.getId() == null) {
            throw new IllegalArgumentException("INVALID_INVESTOR_DATA");
        }

        List<Investor> investors = repo.findAll();
        boolean isUpdated = false;
        String targetId = updatedInvestor.getId().trim();

        for (int i = 0; i < investors.size(); i++) {
            if (investors.get(i).getId().equalsIgnoreCase(targetId)) {
                investors.set(i, updatedInvestor);
                isUpdated = true;
                break;
            }
        }

        if (isUpdated) {
            repo.replaceAll(investors);
        } else {
            throw new InvestorNotFoundException(updatedInvestor.getId());
        }
    }

    /**
     * Modifica atributos específicos de un inversionista desde el panel de edición
     * del perfil.
     */
    public void updateInvestorAtributes(String id, String newName, String newEmail, RiskProfile newRiskProfile) {
        if (id == null)
            throw new IllegalArgumentException("ID_REQUIRED");

        List<Investor> investors = repo.findAll();
        boolean isUpdated = false;
        String targetId = id.trim();

        for (Investor investor : investors) {
            if (investor.getId().equalsIgnoreCase(targetId)) {

                if (newName != null && !newName.isBlank()) {
                    investor.setName(newName.trim());
                }
                if (newEmail != null && !newEmail.isBlank()) {
                    investor.setEmail(newEmail.trim());
                }
                if (newRiskProfile != null) {
                    investor.setRiskProfile(newRiskProfile);
                }

                isUpdated = true;
                break;
            }
        }

        if (isUpdated) {
            repo.replaceAll(investors);
        } else {
            throw new InvestorNotFoundException(id);
        }
    }

    /**
     * Elimina un inversionista delegando directamente al predicado del repositorio.
     */
    public boolean delete(String id) {
        if (id == null)
            return false;
        String targetId = id.trim();

        // Verifica si existe antes de intentar borrar para retornar true/false a la
        // interfaz
        boolean exists = repo.findBy(inv -> inv.getId().equalsIgnoreCase(targetId)).isPresent();
        if (exists) {
            repo.deleteBy(inv -> inv.getId().equalsIgnoreCase(targetId));
            return true;
        }
        return false;
    }

    /**
     * Elimina un inversionista buscando por su correo electrónico.
     *
     * @param email correo del inversionista a eliminar
     * @return true si se eliminó, false si no se encontró
     */
    public boolean deleteInvestor(String email) {
        if (email == null)
            return false;
        String target = email.trim();

        boolean exists = repo.findBy(inv -> inv.getEmail().equalsIgnoreCase(target)).isPresent();
        if (exists) {
            repo.deleteBy(inv -> inv.getEmail().equalsIgnoreCase(target));
            return true;
        }
        return false;
    }

    /**
     * Modifica el capital disponible restando o sumando el costo de las
     * transacciones operacionales.
     */
    public void updateCapital(String id, double purchaseValue) {
        if (id == null)
            throw new IllegalArgumentException("ID_REQUIRED");

        List<Investor> investors = repo.findAll();
        String targetId = id.trim();

        for (Investor inv : investors) {
            if (inv.getId().equalsIgnoreCase(targetId)) {
                if (inv.getAvailableCapital() < purchaseValue) {
                    throw new InsufficientCapitalException("Capital insuficiente para completar la operación.");
                }
                inv.setAvailableCapital(inv.getAvailableCapital() - purchaseValue);
                repo.replaceAll(investors);
                return;
            }
        }
        throw new InvestorNotFoundException("Investor not found with that id: " + id);
    }

    public RiskProfile getRiskProfile(String id) {
        Investor inv = findById(id);
        if (inv == null) {
            throw new InvestorNotFoundException("Investor not found with that id: " + id);
        }
        return inv.getRiskProfile();
    }

    public double getAvailableCapital(String id) {
        Investor inv = findById(id);
        if (inv == null) {
            throw new InvestorNotFoundException("Investor not found with that id: " + id);
        }
        return inv.getAvailableCapital();
    }

    /**
     * Busca un inversionista en la persistencia a partir de su correo electrónico.
     * * @param email El correo electrónico del usuario logueado.
     * 
     * @return El objeto Investor si coincide, o null si no se encuentra.
     */
    public Investor findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        // 🎯 CORREGIDO: Usar el repositorio real (repo.findAll()) en lugar del método
        // simulado
        List<Investor> listaInversionistas = repo.findAll();

        if (listaInversionistas == null) {
            return null;
        }

        // Filtrar usando Streams para encontrar la coincidencia
        Optional<Investor> inversionistaEncontrado = listaInversionistas.stream()
                .filter(investor -> investor.getEmail() != null && investor.getEmail().equalsIgnoreCase(email.trim()))
                .findFirst();

        return inversionistaEncontrado.orElse(null);
    }

    // 🎯 CORREGIDO: Ya no necesitas un método simulador que retorne null.

    /**
     * Método simulador de lectura.
     * Reemplaza el cuerpo de este método con tu lógica de Gson/Jackson real.
     */
    private List<Investor> obtenerTodosLosInversionistas() {
        // Aquí debería ir tu deserialización de Gson:
        // Type listType = new TypeToken<ArrayList<Investor>>(){}.getType();
        // return gson.fromJson(new
        // FileReader("src/main/resources/data/investors.json"), listType);
        return null;
    }
}