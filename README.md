# gKoTH - Plugin de King of the Hill para Minecraft 1.8

![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)
![Minecraft](https://img.shields.io/badge/minecraft-1.8-green.svg)
![License](https://img.shields.io/badge/license-MIT-yellow.svg)

## Descripción

**gKoTH** es un plugin completo de King of the Hill (KoTH) para servidores Minecraft 1.8 que permite a los administradores crear y gestionar eventos de captura de puntos. Incluye sistema de captura con tiempo configurable, integración con clanes (gClans), recompensas configurables con items y comandos, scoreboard animado anti-flicker, sistema de horarios programados, y estadísticas de capturas con soporte para SQLite y MySQL.

## Tabla de Contenidos

- [Características Principales](#características-principales)
- [Comandos](#comandos)
- [Placeholders](#placeholders)
- [Archivos de Configuración](#archivos-de-configuración)
- [Dependencias](#dependencias)
- [Permisos](#permisos)
- [Instalación](#instalación)
- [Base de Datos](#base-de-datos)
- [Funcionamiento Interno](#funcionamiento-interno)
- [Ejemplos de Uso](#ejemplos-de-uso)
- [Reporte de Problemas](#reporte-de-problemas)
- [Licencia](#licencia)
- [Autor](#autor)
- [Agradecimientos](#agradecimientos)
- [Enlaces Útiles](#enlaces-útiles)

## Características Principales

### Sistema de Captura

- Captura individual: solo un jugador puede capturar a la vez.
- Tiempo de captura configurable en segundos.
- Tiempo máximo de ejecución del KoTH configurable.
- Mensajes de notificación en tiempo real (`ATTEMPTING_CAPTURE`, `LOST_CONTROL`, `TIME_LEFT`).
- Intervalo de notificación configurable.

### Integración con Clanes (gClans)

- Suma de puntos al clan del ganador.
- Distribución de recompensas: solo ganador o todos los miembros del clan online.
- Distribución de comandos: solo ganador o todos los miembros del clan online.
- Placeholders de clan disponibles en todos los mensajes.

### Sistema de Recompensas

- Items configurables mediante GUI interactiva.
- Comandos ejecutables al capturar (con soporte de placeholders).
- Sistema de reclamación de recompensas pendientes (`/koth claim`).
- Tiempo de expiración de recompensas configurable.
- Acción al inventario lleno: guardar para reclamar o dropear al suelo.
- Menú de recompensas con protección anti-duplicación.

### Scoreboard Animado

- Sistema anti-flicker con teams y entradas únicas.
- Título animado con múltiples frames.
- Actualización en tiempo real (intervalo configurable).
- Placeholders dinámicos: jugador capturando, tiempo restante, coordenadas, clan.
- Soporte para múltiples líneas animadas.
- Toggle para activar/desactivar.

### Sistema de Horarios (Schedule)

- Programación por día específico (`MONDAY` a `SUNDAY`).
- Programación diaria (`DAILY`).
- Visualización del tiempo restante en formato "3h 07m 05s".
- Validación de horarios duplicados.
- Muestra solo el horario más cercano por KoTH.
- Zona horaria configurable.

### Integración con Lunar Client (Apollo)

- Muestra un waypoint en el mapa/HUD de los jugadores que usan Lunar Client cuando un KoTH se activa.
- El waypoint se actualiza automáticamente al centro del área del KoTH (calculado a partir de los dos puntos seleccionados con la wand).
- Se elimina automáticamente en los tres casos que finalizan un KoTH: captura exitosa, tiempo máximo alcanzado, y detención manual (`/koth stop`).
- Los waypoints se reenvían automáticamente a los jugadores que se reconectan mientras un KoTH sigue activo.
- Funciona de forma opcional: si el jugador no usa Lunar Client, o si el plugin Apollo no está instalado en el servidor, esta función simplemente se desactiva sin afectar el resto del plugin.

### Estadísticas y Top

- Registro de capturas por jugador y por clan.
- Placeholders para top 10 de jugadores y clanes.
- Consultas asíncronas a base de datos.

### Base de Datos

- Soporte SQLite y MySQL.
- Pool de conexiones HikariCP para MySQL.
- DAO Pattern con operaciones asíncronas.
- ExecutorService compartido para evitar concurrencia en SQLite.

## Comandos

### Comandos de Administración

| Comando | Descripción | Permiso |
|---|---|---|
| `/koth create <nombre>` | Crea un nuevo KoTH (inicia desactivado) | `koth.admin` |
| `/koth remove <koth\|id>` | Elimina un KoTH | `koth.admin` |
| `/koth list` | Lista todos los KoTHs registrados | `koth.admin` |
| `/koth enable <koth\|id>` | Activa/desactiva un KoTH | `koth.admin` |
| `/koth reload` | Recarga configuraciones y base de datos | `koth.admin` |
| `/koth wand <koth\|id>` | Otorga la varita de selección con lore personalizable | `koth.admin` |
| `/koth rewards <koth\|id>` | Abre GUI para configurar recompensas | `koth.admin` |
| `/koth loot <koth\|id>` | Muestra recompensas en modo solo lectura | `koth.admin` |
| `/koth cmd <koth\|id> <comando>` | Añade un comando ejecutable al ganar | `koth.admin` |
| `/koth removecmd <koth\|id> <índice\|comando>` | Elimina un comando del KoTH | `koth.admin` |
| `/koth start <koth\|id> <captura> <max>` | Inicia manualmente un KoTH (tiempos en segundos) | `koth.admin` |
| `/koth stop <koth\|id>` | Detiene forzosamente un KoTH activo | `koth.admin` |
| `/koth tp <koth\|id>` | Teletransporta al centro del KoTH | `koth.admin` |
| `/koth info <koth\|id>` | Muestra información detallada del KoTH | `koth.admin` |

### Comandos de Horarios

| Comando | Descripción | Permiso |
|---|---|---|
| `/koth schedule` | Muestra el calendario público con tiempo restante | `koth.schedule` |
| `/koth schedule list` | Lista administrativa con IDs | `koth.schedule.admin` |
| `/koth schedule create <koth\|id> <día\|daily> <HH:MM> <cap> <max>` | Programa un evento | `koth.schedule.admin` |
| `/koth schedule remove <id>` | Elimina un horario | `koth.schedule.admin` |

### Comandos de Jugador

| Comando | Descripción | Permiso |
|---|---|---|
| `/koth claim` | Abre el menú de recompensas pendientes | `koth.claim` |
| `/koth help [página]` | Muestra la ayuda del plugin | `koth.player` |

> Días válidos para `schedule`: `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY`, `DAILY`

## Placeholders

> Requiere [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)

### Placeholders del KoTH

| Placeholder | Descripción |
|---|---|
| `%koth_captures%` | Capturas del jugador |
| `%koth_clan_captures%` | Capturas del clan del jugador |
| `%koth_top_player_1%` al `%koth_top_player_10%` | Nombre del jugador en posición N |
| `%koth_top_player_captures_1%` al `%koth_top_player_captures_10%` | Capturas del jugador en posición N |
| `%koth_top_clan_1%` al `%koth_top_clan_10%` | Nombre del clan en posición N |
| `%koth_top_clan_captures_1%` al `%koth_top_clan_captures_10%` | Capturas del clan en posición N |
| `%koth_active%` | Nombre del KoTH activo |
| `%koth_active_time%` | Tiempo restante del KoTH activo |
| `%koth_capturing%` | Jugador que está capturando |

### Placeholders de gClans Disponibles

| Placeholder | Descripción |
|---|---|
| `%gclan_name%` | Nombre del clan |
| `%gclan_name_raw%` | Nombre del clan sin color |
| `%gclan_display%` | Display del clan (prefijo o nombre) |
| `%gclan_tag%` | Tag del clan |
| `%gclan_prefix%` | Prefijo del clan |
| `%gclan_points%` | Puntos del jugador |
| `%gclan_clan_points%` | Puntos del clan |
| `%gclan_clan_kills%` | Kills del clan |
| `%gclan_clan_deaths%` | Muertes del clan |
| `%gclan_clan_level%` | Nivel del clan |
| `%gclan_clan_members%` | Miembros del clan |
| `%gclan_clan_slots%` | Slots del clan |

### Placeholders de LuckPerms

| Placeholder | Descripción |
|---|---|
| `%luckperms_prefix%` | Prefijo del rango del jugador |

## Archivos de Configuración

### Estructura de Archivos

```
/gKoTH/
├── config.yml       # Configuración principal
├── messages.yml     # Todos los mensajes
├── scoreboard.yml   # Configuración del scoreboard
├── koths.yml        # KoTHs registrados (gestionado automáticamente)
├── schedules.yml    # Horarios programados (gestionado automáticamente)
├── koth.db          # Base de datos SQLite (generada automáticamente)
└── plugin.yml       # Descriptor del plugin
```

### config.yml

```yaml
# ========================================== #
#      KoTH System Main Configuration        #
# ========================================== #

settings:
  time-zone: "America/Lima"
  check-interval-ticks: 5
  capture-notification-interval: 15  # Segundos entre notificaciones de captura
  rewards-gui-size: 27
  claim-gui-size: 27
  wand-material: "GOLD_AXE"
  wand-name: "&6&lKoTH Selection Wand"
  wand-lore:
    - "&7KoTH: &f<koth>"
    - "&7ID: &f<id>"
    - "&eClick Izquierdo: &fEstablece Punto 1"
    - "&eClick Derecho: &fEstablece Punto 2"
  blacklisted-worlds:
    - "world_nether"
    - "world_the_end"

scoreboard:
  enabled: true  # Activar o desactivar la scoreboard

rewards:
  target-distribution: "WINNER_ONLY" # "WINNER_ONLY" o "ALL_CLAN_MEMBERS"
  command-distribution: "WINNER_ONLY" # "WINNER_ONLY" o "ALL_CLAN_MEMBERS"
  full-inventory-action: "CLAIM_MENU"  # "DROP_ON_GROUND" o "CLAIM_MENU"
  expiration-minutes: 30

clan-integration:
  enabled: true
  points-per-koth: 50
  hook-plugin-name: "gClans"

database:
  type: "SQLITE" # "SQLITE" o "MYSQL"
  mysql:
    host: "localhost"
    port: 3306
    database: "koth_db"
    username: "root"
    password: ""
    pool-size: 10
```

### Placeholders Disponibles en Wand

| Placeholder | Descripción |
|---|---|
| `<koth>` | Nombre del KoTH |
| `<id>` | ID del KoTH |

### Placeholders Disponibles en Mensajes

| Placeholder | Descripción |
|---|---|
| `%prefix%` | Prefijo del plugin |
| `%player%` | Nombre del jugador |
| `%winner%` | Nombre del ganador |
| `%koth%` | Nombre del KoTH |
| `%time%` | Tiempo restante de captura |
| `%x%`, `%y%`, `%z%` | Coordenadas del centro |
| `%gclan_name%` | Nombre del clan |
| `%gclan_display%` | Display del clan |
| `%gclan_tag%` | Tag del clan |
| `%gclan_prefix%` | Prefijo del clan |
| `%capture_time%` | Tiempo total de captura (formato "3h 07m 05s") |
| `%luckperms_prefix%` | Prefijo del rango del jugador |

## Dependencias

| Dependencia | Tipo | Versión | Enlace |
|---|---|---|---|
| Spigot/Bukkit | Obligatorio | 1.8.8 | [SpigotMC](https://www.spigotmc.org) |
| WorldEdit | Opcional | 6.x | [WorldEdit](https://dev.bukkit.org/projects/worldedit) |
| gClans | Opcional | 2.0.0 | [gClans](https://github.com/gamma-ok/gClans) |
| PlaceholderAPI | Opcional | 2.10.6+ | [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) |
| Vault | Opcional | 1.7 | [Vault](https://www.spigotmc.org/resources/vault.34315/) |
| LuckPerms | Opcional | 5.x | [LuckPerms](https://luckperms.net/) |
| Apollo (Lunar Client) | Opcional | 1.2.8+ | [Apollo](https://lunarclient.dev/apollo/introduction) |

> El plugin funciona sin gClans, pero las funcionalidades de clan estarán desactivadas.
> Los placeholders de clan (`%gclan_name%`, etc.) mostrarán "N/A" o vacío.
> El plugin funciona sin Apollo, pero los KoTHs no mostrarán waypoints a los jugadores con Lunar Client.

## Permisos

| Permiso | Descripción | Default |
|---|---|---|
| `koth.admin` | Acceso a todos los comandos administrativos | `op` |
| `koth.player` | Acceso a comandos básicos (help, schedule) | `true` |
| `koth.create` | Crear KoTHs | `op` |
| `koth.remove` | Eliminar KoTHs | `op` |
| `koth.list` | Listar KoTHs | `op` |
| `koth.enable` | Activar/desactivar KoTHs | `op` |
| `koth.reload` | Recargar configuraciones | `op` |
| `koth.wand` | Obtener varita de selección | `op` |
| `koth.rewards` | Administrar recompensas | `op` |
| `koth.cmd` | Agregar/eliminar comandos | `op` |
| `koth.start` | Iniciar KoTHs manualmente | `op` |
| `koth.stop` | Detener KoTHs | `op` |
| `koth.tp` | Teletransportarse a KoTHs | `op` |
| `koth.info` | Ver información de KoTHs | `op` |
| `koth.loot` | Ver recompensas | `true` |
| `koth.claim` | Reclamar recompensas | `true` |
| `koth.schedule` | Ver horarios | `true` |
| `koth.schedule.admin` | Administrar horarios | `op` |

## Instalación

1. Descarga el archivo `gKoTH.jar`.
2. Colócalo en la carpeta `plugins/` de tu servidor.
3. Reinicia el servidor. Se generarán automáticamente los archivos de configuración.
4. Configura `config.yml`, `messages.yml` y `scoreboard.yml` según tus necesidades.
5. Crea tu primer KoTH con `/koth create <nombre>`.
6. Usa `/koth wand <nombre>` y selecciona el área con click izquierdo y derecho.
7. Configura recompensas con `/koth rewards <nombre>`.
8. Activa el KoTH con `/koth enable <nombre>`.
9. Inicia el KoTH con `/koth start <nombre> <captura> <max>`.

## Base de Datos

### Tablas

```sql
-- Estadísticas de jugadores
CREATE TABLE koth_player_stats (
    uuid VARCHAR(36) PRIMARY KEY,
    player_name VARCHAR(32) NOT NULL,
    captures_count INT DEFAULT 0
);

-- Estadísticas de clanes
CREATE TABLE koth_clan_stats (
    clan_name VARCHAR(32) PRIMARY KEY,
    captures_count INT DEFAULT 0
);

-- Recompensas no reclamadas
CREATE TABLE koth_unclaimed_rewards (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    uuid VARCHAR(36) NOT NULL,
    item_base64 TEXT NOT NULL,
    created_at BIGINT NOT NULL
);
```

## Funcionamiento Interno

### Sistema de Captura

- **Detección rápida** (cada 5 ticks / 250ms): Verifica si un jugador entró o salió del área.
- **Reducción de tiempo** (cada 20 ticks / 1 segundo): Descuenta el tiempo de captura y el tiempo máximo.
- **Reinicio de captura**: Si el jugador sale del área, el tiempo de captura se reinicia.
- **Notificaciones**: `ATTEMPTING_CAPTURE` al entrar, `LOST_CONTROL` al salir, `TIME_LEFT` cada X segundos.

### Sistema de Recompensas

- **Items**: Se guardan en `koths.yml` serializados en Base64.
- **GUI de recompensas**: Permite agregar/eliminar items visualmente.
- **Reclamación**: Al cerrar el menú de claim, se eliminan de la BD y los items restantes se dropean.
- **Anti-duplicación**: Los items solo se pueden tomar con click izquierdo, no se pueden mover ni dropear.

### Waypoints de Lunar Client (Apollo)

- **Detección automática**: al iniciar, el plugin verifica si Apollo está instalado y disponible; si no lo está, la función se desactiva silenciosamente.
- **Mostrar waypoint**: cuando un KoTH se activa, se calcula la posición del suelo más cercana al centro del área y se envía un waypoint nombrado `KoTH: <nombre>` a todos los jugadores online que usan Lunar Client.
- **Sincronización al reconectar**: Apollo confirma el registro de un jugador de forma asíncrona tras el login, por lo que el envío de waypoints activos a jugadores que se reconectan se maneja mediante el evento `ApolloRegisterPlayerEvent` de Apollo, no directamente en el join de Bukkit.
- **Limpieza centralizada**: la eliminación del waypoint está centralizada en `KoTHManager.stopKoTH()`, de forma que cualquier camino que detenga un KoTH (comando manual, captura, tiempo máximo, recarga del plugin) limpia el waypoint correctamente.

### Scoreboard Anti-Flicker

- **Teams fijos**: Se crean una sola vez con nombres únicos.
- **Entradas únicas**: Usan caracteres invisibles para evitar conflictos.
- **Actualización diferencial**: Solo se actualizan las líneas que cambiaron.
- **Manejo de códigos de color**: División segura de prefijo/sufijo (16+16 caracteres).

### Base de Datos

- **ExecutorService compartido**: Un solo hilo para todas las operaciones SQLite.
- **Conexión compartida**: No se cierra en los DAOs, solo en `shutdown()`.
- **Operaciones asíncronas**: No bloquean el hilo principal.

## Ejemplos de Uso

### Crear un KoTH

```
/koth create Playa
/koth wand Playa
# Click izquierdo en una esquina
# Click derecho en la esquina opuesta
/koth rewards Playa
# Colocar items en la GUI y cerrar
/koth cmd Playa eco give %winner% 1000
/koth enable Playa
/koth start Playa 30 300
```

### Programar un KoTH

```
# Diario a las 18:00, captura 30 segundos, máximo 5 minutos
/koth schedule create Playa daily 18:00 30 300

# Lunes a las 20:00, captura 60 segundos, máximo 10 minutos
/koth schedule create Playa monday 20:00 60 600

# Ver horarios
/koth schedule
```

### Configurar recompensas

```
/koth rewards Playa
# Colocar 64 esmeraldas en la GUI
# Cerrar la GUI para guardar

# Ver recompensas (solo lectura)
/koth loot Playa
```

### Reclamar recompensas

```
/koth claim
# Tomar items con click izquierdo
# Cerrar el menú (los items restantes se dropean)
```

## Reporte de Problemas

Si encuentras algún problema o tienes sugerencias:

1. Revisa la consola del servidor en busca de errores.
2. Verifica que las dependencias estén instaladas correctamente.
3. Asegúrate de estar usando Minecraft 1.8.8.
4. Crea un [issue](https://github.com/gamma-ok/gKoTH/issues) en el repositorio con:
   - Versión del plugin
   - Versión del servidor
   - Descripción del problema
   - Logs/errores relevantes

## Licencia

Este proyecto está bajo la licencia **MIT**. Para más información, consulta el archivo [LICENSE](LICENSE).

## Autor

**gamma** — GitHub: [@gamma-ok](https://github.com/gamma-ok)

## Agradecimientos

- SpigotMC por la API
- PlaceholderAPI por el sistema de placeholders
- LuckPerms por el sistema de permisos
- HikariCP por el pool de conexiones
- A la comunidad de Minecraft por el soporte y feedback

## Enlaces Útiles

- [Documentación de Spigot](https://www.spigotmc.org/wiki/index/)
- [PlaceholderAPI Wiki](https://wiki.placeholderapi.com)
- [LuckPerms Wiki](https://luckperms.net/wiki)
- [SQLite JDBC](https://github.com/xerial/sqlite-jdbc)
- [HikariCP](https://github.com/brettwooldridge/HikariCP)
