# Termux:Launcher

> **Fork de [termux/termux-float](https://github.com/termux/termux-float)**

[![Build status](https://github.com/itskreisler/termux-float/workflows/Build/badge.svg)](https://github.com/itskreisler/termux-float/actions)
[![Join the chat at https://gitter.im/termux/termux](https://badges.gitter.im/termux/termux.svg)](https://gitter.im/termux/termux)

**Termux:Launcher** es un fork de Termux:Float que convierte la ventana flotante en un **lanzador de aplicaciones completo**. En lugar de mostrar únicamente una terminal, muestra todas las apps instaladas en el dispositivo, permite fijarlas, organizarlas en carpetas y lanzarlas directamente desde una interfaz flotante siempre accesible.

### ¿Qué cambia respecto a Termux:Float?

- 📱 **Lista de apps instaladas** — muestra todas las aplicaciones del dispositivo con su icono y nombre.
- 📌 **Apps fijadas** — fija tus apps favoritas en la barra superior para acceso rápido.
- 📁 **Carpetas** — organiza apps en carpetas personalizadas con nombre y color de tinte.
- 🔍 **Búsqueda** — filtra apps mientras escribes, con índice alfabético A–Z lateral.
- 🌐 **Interfaz en español** — toda la UI traducida al español.
- 🎨 **Tema oscuro** — interfaz oscura (#121212) con texto dorado (#C0B18B).

##



### Contents
- [Instalación](#Instalación)
- [Configuración](#Configuración)
- [Depuración](#Depuración)
- [Créditos](#Créditos)
##



### Instalación

Última versión: `v0.17.0`.

### GitHub Releases

Descarga el APK directamente desde [GitHub Releases](https://github.com/itskreisler/termux-float/releases/tag/v0.17.0).

El APK está firmado con la clave de depuración del proyecto, por lo que **no es compatible** con builds de F-Droid ni del Play Store.

> **Nota:** Esta app requiere que [Termux](https://github.com/termux/termux-app) esté instalado en el dispositivo.

##



### Configuración

Al igual que Termux:Float, soporta configuración vía `~/.termux/termux.float.properties`. Las propiedades disponibles son: `enforce-char-based-input`, `ctrl-space-workaround`, `bell-character`, `terminal-cursor-style`, `terminal-transcript-rows`, `back-key`, `default-working-directory`, `volume-keys`.

```
mkdir -p ~/.termux
nano ~/.termux/termux.float.properties
```

##






### Depuración

Para depurar, usa `logcat` en la terminal de Termux:

```
logcat | grep TermuxLauncher
# o volcar a fichero:
logcat -d > logcat.txt
```

También puedes conectar por ADB y revisar los logs desde el PC.

##



## Créditos

- Basado en [termux/termux-float](https://github.com/termux/termux-float) — Copyright (C) Termux contributors.
- Fork desarrollado por [itskreisler](https://github.com/itskreisler).

##



[Termux]: https://termux.dev

