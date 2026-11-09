package structures.management;

import model.LecturaSensor;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GestorDeRed - Módulo Integrador de Estructuras de Datos
 *
 * Objetivo: Implementar la gestión de la red utilizando las colecciones de Java (JCF).
 * El éxito de este módulo no es solo que el código compile, sino que elijas
 * la estructura correcta para cada problema.
 */
public class GestorDeRed {
    // 1. Historial de Eventos: Registro lineal de todas las actividades.
    //    ArrayList: los eventos se agregan al final y se borran desde el inicio
    //    solo en la limpieza periódica (ver limpiarHistorialAntiguo).
    private List<String> historialEventos;

    // 2. Navegador de Alertas: Permite moverse adelante y atrás entre alertas.
    //    El cursor es 'alertaActual'. Lo que queda antes del cursor vive en
    //    'alertasAtras' (pila: el más cercano está al frente) y lo que queda
    //    después vive en 'navegadorAlertas' (cola: el siguiente está al frente).
    private Deque<String> navegadorAlertas;
    private Deque<String> alertasAtras;
    private String alertaActual;

    // 3. Ciclo de Monitoreo: Gestión de la rotación de sensores a consultar.
    //    'sensoresProgramados' evita duplicados en O(1) antes de encolar.
    private Queue<String> colaMonitoreo;
    private Set<String> sensoresProgramados;

    // 4. Topología de la Red: Grafo de conexiones entre estaciones (Lista de adyacencia)
    private Map<String, List<String>> topologiaRed;
    // Todos los sensores que el gestor conoce (aunque no tengan conexiones).
    private Set<String> sensoresRegistrados;

    // 5. Buffer de Ingesta: Cola de espera para procesar lecturas entrantes
    private Queue<LecturaSensor> bufferIngesta;

    public GestorDeRed() {
        this.historialEventos = new ArrayList<>();
        this.navegadorAlertas = new ArrayDeque<>();
        this.alertasAtras = new ArrayDeque<>();
        this.colaMonitoreo = new ArrayDeque<>();
        this.sensoresProgramados = new HashSet<>();
        this.topologiaRed = new HashMap<>();
        this.sensoresRegistrados = new LinkedHashSet<>();
        this.bufferIngesta = new ArrayDeque<>();
    }

    // =========================================================================
    // SECCIÓN 1: HISTORIAL Y EVENTOS
    // =========================================================================

    public void agregarEvento(String evento) {
        historialEventos.add("[" + new java.util.Date() + "] " + evento);
    }

    public List<String> obtenerEventosRecientes(int limite) {
        if (historialEventos.size() <= limite) return new ArrayList<>(historialEventos);
        return historialEventos.subList(historialEventos.size() - limite, historialEventos.size());
    }

    public void limpiarHistorialAntiguo(int mantener) {
        int sobrantes = historialEventos.size() - Math.max(0, mantener);
        if (sobrantes > 0) {
            // Quita los más antiguos (inicio de la lista) en una sola operación.
            historialEventos.subList(0, sobrantes).clear();
        }
    }

    // =========================================================================
    // SECCIÓN 2: NAVEGACIÓN DE ALERTAS
    // =========================================================================

    public void registrarAlerta(String alerta) {
        if (alertaActual == null) {
            // Primera alerta: el cursor queda sobre ella.
            alertaActual = alerta;
        } else {
            // Las nuevas alertas se encolan después del cursor.
            navegadorAlertas.addLast(alerta);
        }
    }

    public String navegarAlSiguiente() {
        if (navegadorAlertas.isEmpty()) return null;
        alertasAtras.push(alertaActual);          // la actual queda atrás
        alertaActual = navegadorAlertas.pollFirst();
        return alertaActual;
    }

    public String navegarAlAnterior() {
        if (alertasAtras.isEmpty()) return null;
        navegadorAlertas.addFirst(alertaActual);  // la actual queda adelante
        alertaActual = alertasAtras.pop();
        return alertaActual;
    }

    public String getAlertaActual() { return alertaActual; }

    // =========================================================================
    // SECCIÓN 3: CICLO DE MONITOREO
    // =========================================================================

    public void programarConsulta(String idSensor) {
        sensoresRegistrados.add(idSensor);
        if (sensoresProgramados.add(idSensor)) {
            colaMonitoreo.offer(idSensor);
        }
    }

    public String obtenerSiguienteSensor() {
        String sensor = colaMonitoreo.poll();
        if (sensor != null) {
            colaMonitoreo.offer(sensor); // vuelve al final: rotación
        }
        return sensor;
    }

    // =========================================================================
    // SECCIÓN 4: TOPOLOGÍA DE RED
    // =========================================================================

    public void registrarConexion(String sensorA, String sensorB) {
        sensoresRegistrados.add(sensorA);
        sensoresRegistrados.add(sensorB);
        agregarVecino(sensorA, sensorB);
        agregarVecino(sensorB, sensorA);
    }

    private void agregarVecino(String origen, String destino) {
        List<String> vecinos = topologiaRed.computeIfAbsent(origen, k -> new ArrayList<>());
        if (!vecinos.contains(destino)) {
            vecinos.add(destino);
        }
    }

    public List<String> obtenerVecinos(String idSensor) {
        return new ArrayList<>(topologiaRed.getOrDefault(idSensor, new ArrayList<>()));
    }

    public List<String> detectarEstacionesAisladas() {
        List<String> aisladas = new ArrayList<>();
        for (String sensor : sensoresRegistrados) {
            if (topologiaRed.getOrDefault(sensor, new ArrayList<>()).isEmpty()) {
                aisladas.add(sensor);
            }
        }
        return aisladas;
    }

    // =========================================================================
    // SECCIÓN 5: BUFFER DE INGESTA
    // =========================================================================

    public void recibirLectura(LecturaSensor lectura) {
        bufferIngesta.add(lectura);
    }

    public LecturaSensor procesarSiguienteLectura() {
        return bufferIngesta.poll(); // FIFO: la más antigua, o null si está vacío
    }

    public List<LecturaSensor> filtrarAlertasCriticas(double limitePm25) {
        // TODO EXTRA: Usar Streams para filtrar lecturas con PM2.5 superior al límite.
        // Debe retornar una lista con las lecturas filtradas.
        return null;
    }
}
