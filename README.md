# 🐻💗 BearHealthPlus

**BearHealthPlus** es un plugin avanzado para Minecraft que mejora la gestión de la salud de los jugadores, permitiendo aumentar corazones, personalizar ítems con ItemsAdder y agregar recetas personalizadas. Compatible con **Spigot/Paper 1.20.4**.

## ✨ Características  
✅ **Aumento de corazones**: Usa ítems personalizados para ganar más vida.  
✅ **Compatibilidad con ItemsAdder**: Soporta materiales de ItemsAdder para ítems y recetas (opcional).  
✅ **Crafteos personalizados**: Registra nuevas recetas directamente desde `items.yml`.  
✅ **Sistema de comandos**: Administra la salud de los jugadores con `/bhp`.  
✅ **Gestión de experiencia**: Algunos ítems requieren experiencia para ser usados.  
✅ **Fácil configuración**: Todo configurable en `config.yml` y `items.yml`.  

---

## 📥 Instalación
1. Descarga el archivo `BearHealthPlus.jar` desde la pestaña de [Releases](https://github.com/eloso1506/BearHealthPlus/releases).  
2. Coloca el archivo en la carpeta `plugins/` de tu servidor.  
3. **(Opcional)** Instala [ItemsAdder](https://www.spigotmc.org/resources/itemsadder.73355/) si quieres usar ítems personalizados.  
4. Reinicia el servidor y edita `config.yml` y `items.yml` según tus necesidades.  

---
🎮 Comandos y Permisos
🔹 Comandos:
Comando	Descripción
/bhp help                        	         | Muestra ayuda sobre el plugin.
/bhp give {item} {cantidad} {jugador}      |	Da un ítem a un jugador.
/bhp get {item}                            |	Obtiene un ítem del plugin.
/bhp reset {jugador}	                     |  Restablece la vida de un jugador.
/bhp set {jugador} {corazones}             |	Ajusta la cantidad exacta de corazones.
/bhp reload	                               |  Recarga la configuración del plugin.
🔹 Permisos:
Permiso	
bhp.admin |	Acceso a todos los comandos de administración.
bhp.use  	| Permite a los jugadores usar ítems del plugin.


🔧 Integración con ItemsAdder
Si ItemsAdder está instalado y activado en config.yml, los ítems con IA: en items.yml serán compatibles.

💬 Soporte
Para reportar errores o sugerencias, crea un [Issues](https://github.com/eloso1506/BearHealthPlus/issues) en GitHub.

🎉 ¡Gracias por usar BearHealthPlus! Si te gusta el plugin, ⭐ ¡déjale un star en GitHub!
