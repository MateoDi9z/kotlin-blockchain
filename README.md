# Blockchain CLI

Blockchain peer-to-peer con interfaz de línea de comandos (CLI).

## Requisitos previos

- Python 3.x
- Flask
- Requests

## Instalación

```bash
python -m venv venv
source ./venv/bin/activate
```


```bash
pip install -r requirements.txt
```

## Iniciar el nodo

Para iniciar un nodo de blockchain, ejecuta:

```bash
python main.py [opciones]
```

### Opciones de línea de comandos

| Opción | Tipo | Default | Descripción |
|--------|------|---------|-------------|
| `--host` | string | `0.0.0.0` | Host donde escucha el servidor |
| `--port` | int | `5001` | Puerto donde escucha el servidor |
| `--peers` | string | `""` | Lista de URLs de peers separadas por comas |

### Ejemplos de inicio

**Nodo simple en puerto 5001:**
```bash
python main.py
```

**Nodo en puerto 5002:**
```bash
python main.py --port 5002
```

**Nodo con peers preconfigurados:**
```bash
python main.py --port 5003 --peers http://localhost:5001,http://localhost:5002
```

## Comandos del CLI

Una vez iniciado el nodo, puedes usar los siguientes comandos en la interfaz interactiva:

### `tx` - Crear transacción

Agrega una nueva transacción a la lista de transacciones pendientes.

**Sintaxis:**
```
tx <from> <to> <amount> <signature>
```

**Parámetros:**
- `from` (string): Dirección del remitente
- `to` (string): Dirección del destinatario
- `amount` (float): Cantidad a transferir
- `signature` (string): Firma de la transacción

**Ejemplo:**
```
tx alice bob 10 signature123
```

---

### `pt` - Mostrar transacciones pendientes

Muestra todas las transacciones pendientes que aún no han sido minadas.

**Sintaxis:**
```
pt
```

**Parámetros:** Ninguno

**Ejemplo:**
```
pt
```

---

### `mine` - Minar bloque

Inicia el proceso de minado para crear un nuevo bloque con las transacciones pendientes.

**Sintaxis:**
```
mine
```

**Parámetros:** Ninguno

**Ejemplo:**
```
mine
```

---

### `chain` - Mostrar cadena completa

Muestra todos los bloques de la blockchain.

**Sintaxis:**
```
chain
```

**Parámetros:** Ninguno

**Ejemplo:**
```
chain
```

---

### `r` - Resolver conflictos

Ejecuta el algoritmo de consenso para resolver conflictos entre peers y sincronizar con la cadena más larga.

**Sintaxis:**
```
r
```

**Parámetros:** Ninguno

**Ejemplo:**
```
r
```

---

### `reg` - Registrar peer

Registra un nuevo peer en la red y se registra a sí mismo en ese peer.

**Sintaxis:**
```
reg <peer_url>
```

**Parámetros:**
- `peer_url` (string): URL completa del peer a registrar (debe incluir protocolo y puerto)

**Ejemplo:**
```
reg http://localhost:5002
```

---

### `peers` - Mostrar peers

Lista todos los peers registrados en el nodo actual.

**Sintaxis:**
```
peers
```

**Parámetros:** Ninguno

**Ejemplo:**
```
peers
```

---

### `s` - Mostrar estado

Muestra un resumen del estado actual del nodo (longitud de cadena, transacciones pendientes, último hash de bloque y peers).

**Sintaxis:**
```
s
```

**Parámetros:** Ninguno

**Ejemplo:**
```
s
```

**Salida ejemplo:**
```
Current chain length: 5, pending transactions: 2
Latest block hash: 00001a2b3c4d5e6f..., nonce: 12345
Peers: ['http://localhost:5002', 'http://localhost:5003']
```

---

### `port` - Mostrar puerto

Muestra el puerto en el que está escuchando el nodo actual.

**Sintaxis:**
```
port
```

**Parámetros:** Ninguno

**Ejemplo:**
```
port
```

---

### `help` - Mostrar ayuda

Muestra el estado actual y un mensaje de ayuda con todos los comandos disponibles.

**Sintaxis:**
```
help
```

**Parámetros:** Ninguno

**Ejemplo:**
```
help
```

---

### `q` - Salir

Cierra el nodo y sale del CLI.

**Sintaxis:**
```
q
```

**Parámetros:** Ninguno

**Ejemplo:**
```
q
```

## Ejemplo de sesión completa

```bash
# Terminal 1: Iniciar primer nodo
python main.py --port 5001

# En el CLI del nodo 1:
Enter command: tx alice bob 10 sig1
Enter command: pt
Enter command: mine
Enter command: s
Enter command: q
```

```bash
# Terminal 2: Iniciar segundo nodo conectado al primero
python main.py --port 5002 --peers http://localhost:5001

# En el CLI del nodo 2:
Enter command: peers
Enter command: r
Enter command: chain
Enter command: q
```

## API HTTP

El nodo también expone endpoints HTTP (documentar según sea necesario):

- `GET /chain` - Obtener la cadena completa
- `GET /peers` - Obtener lista de peers
- `POST /peers` - Registrar un peer
- `GET /pending` - Obtener transacciones pendientes
- `POST /transaction` - Crear una transacción
- `POST /mine` - Minar un bloque
- `POST /block` - Recibir un bloque nuevo
- `POST /consensus` - Ejecutar consenso

## Notas

- El consenso se ejecuta automáticamente cada 30 segundos
- La dificultad de minado está definida por la constante `DIFFICULTY`
- El bloque génesis se crea automáticamente al iniciar el nodo
