# Respuestas a las preguntas de criterio de decisión

## 1. ArrayList vs. LinkedList: eliminación frecuente de eventos antiguos

Si los eventos antiguos se eliminan **con frecuencia**, elegiría **LinkedList** (o, mejor aún, `ArrayDeque`, que es la opción que recomienda Java para colas).

- En un `ArrayList`, quitar el primer elemento obliga a desplazar todos los demás una posición: **O(n)** por cada eliminación.
- En una `LinkedList`, quitar el primer nodo solo reasigna la referencia de la cabeza: **O(1)**.
- El costo de la `LinkedList` es mayor memoria (cada nodo guarda dos referencias extra) y que `get(i)` es O(n). Como aquí no se consulta por posición, ese costo no pesa.

En nuestra implementación usamos `ArrayList` en `limpiarHistorialAntiguo`, pero **sin borrar uno por uno**: calculamos cuántos sobran y los quitamos en una sola operación con `subList(0, sobrantes).clear()`. Como la limpieza se hace por lotes y no con cada evento, el costo se paga una vez y el `ArrayList` se justifica por su acceso rápido a los eventos recientes. Si la limpieza fuera continua, cambiaríamos a `ArrayDeque`.

## 2. Deque vs. Queue y el NodoDoble

- **Queue** es una cola FIFO: agrega por un extremo (`offer`) y extrae por el otro (`poll`). Solo se puede mirar o sacar el frente.
- **Deque** (*double-ended queue*) extiende a `Queue` y permite operar en **ambos extremos**: `addFirst`, `addLast`, `pollFirst`, `pollLast`. Además sirve como pila (`push`/`pop`).

La relación con el `NodoDoble` es directa: un nodo doble tiene una referencia `anterior` y otra `siguiente`, y así puede recorrerse en ambos sentidos. Un `Deque` ofrece esa misma capacidad de acceso por los dos extremos sin que el programador maneje punteros.

Para la navegación de alertas necesitamos un **cursor en el medio**, algo que un `Deque` no ofrece por sí solo. Por eso usamos dos `Deque`: `alertasAtras` (pila de lo que quedó antes del cursor) y `navegadorAlertas` (cola de lo que queda después). Navegar hacia adelante mueve la alerta actual a `alertasAtras` y saca la siguiente de `navegadorAlertas`; navegar hacia atrás hace lo contrario.

## 3. Colas y equidad en el monitoreo

Una **cola** es apropiada porque el monitoreo es un *round-robin*: cada sensor se consulta, pasa al final y espera su turno. Con `obtenerSiguienteSensor` (`poll` seguido de `offer`) todos los sensores reciben atención en el mismo orden, y ninguno se queda sin consultar.

Si usáramos una **pila (LIFO)**, se consultaría siempre el último sensor agregado, y los que están abajo nunca llegarían al tope mientras entren sensores nuevos. Es decir, habría **inanición**: los sensores antiguos dejarían de monitorearse. Es el mismo principio por el que la pila sirve para deshacer acciones (lo último hecho es lo primero que se revierte), pero no para repartir turnos equitativamente.

## 4. Lista de adyacencia vs. matriz de adyacencia

`Map<String, List<String>>` es una **lista de adyacencia**: cada vértice (sensor) es una clave, y su valor es la lista de vecinos. Es exactamente la idea del grafo de la Semana 5, pero con un mapa para encontrar el vértice por su identificador.

Frente a una matriz de adyacencia:

- **Ventaja de memoria:** la matriz ocupa **O(n²)** aunque cada sensor tenga solo 2 o 3 vecinos. La lista ocupa **O(n + m)**, donde *m* es el número de conexiones. En una red dispersa como la nuestra, eso es una diferencia enorme.
- **Ventaja de crecimiento:** agregar un sensor nuevo no requiere redimensionar una matriz.
- **Desventaja:** verificar si existe una conexión entre dos sensores cuesta **O(grado)** (recorrer la lista de vecinos), mientras que en la matriz cuesta **O(1)**.
- **Desventaja:** la matriz permite recorrer rápido todas las combinaciones, pero en una red dispersa eso desperdicia casi todas las celdas.

Como la mayoría de las consultas son "dame los vecinos de este sensor" (`obtenerVecinos`), la lista de adyacencia es la estructura más adecuada.

## 5. ArrayDeque vs. ConcurrentLinkedQueue en multihilo

La diferencia clave es la **seguridad para concurrencia (*thread-safety*)**: una estructura es segura para hilos si su comportamiento sigue siendo correcto cuando varios hilos la usan al mismo tiempo, sin que el programador tenga que sincronizarla.

- **`ArrayDeque` no es thread-safe.** Si dos hilos insertan o extraen a la vez, pueden corromperse sus índices internos, perderse elementos o producirse excepciones. Sirve solo para un único hilo, o si el acceso se protege con `synchronized` o un `Lock` externo.
- **`ConcurrentLinkedQueue` sí es thread-safe.** Usa operaciones atómicas sin bloqueos (*lock-free*), así que varios productores y consumidores pueden operar a la vez. El costo es que `size()` es O(n) y no hay operaciones bloqueantes.

Para nuestro sistema **hoy** el procesamiento es de un solo hilo, así que `ArrayDeque` es suficiente y más rápido. Si el volumen crece y se procesan lecturas en paralelo, cambiaría a `ConcurrentLinkedQueue` si basta con sondear la cola, o a `LinkedBlockingQueue` si los consumidores deben esperar cuando la cola está vacía, que es el caso típico de productor–consumidor.
