# Migración de `SuggestionBarView` a Otro Proyecto

## Resumen
`SuggestionBarView` es una barra de sugerencias de aplicaciones que permite navegar rápidamente por las aplicaciones instaladas usando un teclado físico o toques táctiles.

## 1. Estructura de Clases Necesarias

### Clase Principal
- **SuggestionBarView.java** - Vista principal (GridLayout)
  - Ubicación original: `app/src/main/java/com/termux/app/SuggestionBarView.java`

### Clases de Soporte
```java
// Modelos de datos
com.termux.app.launcher.model.AppRef;
com.termux.app.launcher.model.LauncherAppEntry;
com.termux.app.launcher.model.PinnedAppItem;
com.termux.app.launcher.model.PinnedFolderItem;
com.termux.app.launcher.model.PinnedItem;

// Proveedor de datos
com.termux.app.launcher.data.LauncherAppDataProvider;

// Repositorio de configuración
com.termux.app.launcher.data.LauncherConfigRepository;

// Motor de ranking
com.termux.app.launcher.data.LauncherRankingEngine;

// Callbacks
com.termux.app.SuggestionBarCallback;
com.termux.app.SuggestionBarButton;
```

## 2. Dependencias Necesarias

### Gradle (app/build.gradle)
```gradle
dependencies {
    // AndroidX
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.recyclerview:recyclerview:1.3.0'
    implementation 'androidx.core:core:1.10.0'
    
    // Material Design
    implementation 'com.google.android.material:material:1.9.0'
    
    // Navegación
    implementation 'androidx.navigation:navigation-fragment:2.5.3'
    
    // Nuevos componentes
    implementation 'androidx.lifecycle:lifecycle-viewmodel:2.6.1'
}
```

## 3. Archivos XML Necesarios

### 1. Layout del Activity Principal
`res/layout/activity_main.xml`
```xml
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- Área de contenido principal -->
    <FrameLayout
        android:id="@+id/content_area"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_above="@id/suggestion_bar" />

    <!-- Barra de sugerencias (launcher) -->
    <com.termux.app.SuggestionBarView
        android:id="@+id/suggestion_bar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_alignParentBottom="true"
        android:background="#20000000" />

</RelativeLayout>
```

### 2. Configuración del Activity
`AndroidManifest.xml`
```xml
<activity
    android:name=".MainActivity"
    android:theme="@style/Theme.AppCompat.NoActionBar">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

## 4. Implementación Paso a Paso

### Paso 1: Crear el Activity Principal
```java
public class MainActivity extends AppCompatActivity 
    implements SuggestionBarCallback {
    
    private SuggestionBarView suggestionBar;
    private LauncherAppDataProvider appDataProvider;
    private LauncherConfigRepository configRepository;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Inicializar vistas
        suggestionBar = findViewById(R.id.suggestion_bar);
        
        // Configurar proveedores de datos
        appDataProvider = new LauncherAppDataProvider(this);
        configRepository = new LauncherConfigRepository(this);
        
        // Configurar la barra de sugerencias
        setupSuggestionBar();
    }
    
    private void setupSuggestionBar() {
        suggestionBar.setAppDataProvider(appDataProvider);
        suggestionBar.setConfigRepository(configRepository);
        suggestionBar.setDefaultButtons(getDefaultButtons());
        
        // Cargar aplicaciones
        suggestionBar.reloadAllApps();
    }
    
    private List<String> getDefaultButtons() {
        List<String> buttons = new ArrayList<>();
        buttons.add("Terminal");
        buttons.add("Editor");
        buttons.add("Settings");
        return buttons;
    }
    
    // Implementación de SuggestionBarCallback
    @Override
    public void reloadSuggestionBar(char inputChar) {
        suggestionBar.previewAzLetter(inputChar, 0, false);
    }
    
    @Override
    public void reloadSuggestionBar(boolean delete, boolean enter) {
        if (delete) {
            // Manejar borrado
        }
        if (enter) {
            // Manejar enter
        }
    }
}
```

### Paso 2: Configurar el Teclado
```java
// En tu Activity principal
@Override
public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (event.getUnicodeChar() > 0) {
        char inputChar = (char) event.getUnicodeChar();
        if (Character.isLetterOrDigit(inputChar) || inputChar == '#') {
            suggestionBar.previewAzLetter(inputChar, 0, false);
            return true;
        }
    }
    return super.onKeyDown(keyCode, event);
}
```

## 5. Archivos de Modelo (Data Models)

### AppRef.java
```java
package com.termux.app.launcher.model;

public class AppRef {
    public final String packageName;
    public final String activityName;
    
    public AppRef(String packageName, String activityName) {
        this.packageName = packageName;
        this.activityName = activityName;
    }
    
    public String stableId() {
        return packageName + "/" + (activityName != null ? activityName : "");
    }
}
```

### LauncherAppEntry.java
```java
package com.termux.app.launcher.model;

import android.graphics.drawable.Drawable;

public class LauncherAppEntry {
    public final AppRef appRef;
    public final String label;
    public final Drawable icon;
    
    public LauncherAppEntry(AppRef appRef, String label, Drawable icon) {
        this.appRef = appRef;
        this.label = label;
        this.icon = icon;
    }
}
```

## 6. Configuración de Proveedor de Datos

### LauncherAppDataProvider.java (Núcleo)
```java
package com.termux.app.launcher.data;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.util.*;

public class LauncherAppDataProvider {
    private final Context context;
    private final List<LauncherAppEntry> cachedApps = new ArrayList<>();
    private final Map<Character, List<LauncherAppEntry>> letterBuckets = new HashMap<>();
    
    public LauncherAppDataProvider(Context context) {
        this.context = context;
    }
    
    public List<LauncherAppEntry> getAllApps() {
        if (cachedApps.isEmpty()) {
            loadAppsLocked();
        }
        return new ArrayList<>(cachedApps);
    }
    
    private void loadAppsLocked() {
        PackageManager pm = context.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN, null);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(main, 0);
        Collections.sort(apps, new ResolveInfo.DisplayNameComparator(pm));
        
        for (ResolveInfo info : apps) {
            if (info.activityInfo == null) continue;
            String label = info.activityInfo.loadLabel(pm).toString();
            AppRef ref = new AppRef(info.activityInfo.packageName, info.activityInfo.name);
            Drawable icon = info.activityInfo.loadIcon(pm);
            LauncherAppEntry entry = new LauncherAppEntry(ref, label, icon);
            cachedApps.add(entry);
            
            char key = normalizeLetter(label);
            letterBuckets.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);
        }
    }
    
    public List<LauncherAppEntry> getAppsForLetter(char letter) {
        char normalized = normalizeLetter(letter);
        return letterBuckets.getOrDefault(normalized, new ArrayList<>());
    }
    
    private char normalizeLetter(String label) {
        if (label.isEmpty()) return '#';
        char c = Character.toUpperCase(label.charAt(0));
        return (c >= 'A' && c <= 'Z') ? c : '#';
    }
}
```

## 7. Configuración de Layout Personalizado

### Opción 1: Usar GridLayout directamente
```xml
<GridLayout
    android:id="@+id/suggestion_bar"
    android:layout_width="match_parent"
    android:layout_height="60dp"
    android:alignmentMode="alignBounds"
    android:columnCount="4"
    android:rowCount="1"
    android:background="#80000000" />
```

### Opción 2: Estilos personalizados
`res/values/styles.xml`
```xml
<style name="SuggestionBarStyle" parent="Widget.AppCompat.Button">
    <item name="android:textColor">#C0B18B</item>
    <item name="android:textSize">12sp</item>
    <item name="android:background">@android:color/transparent</item>
</style>
```

## 8. Personalización Opcional

### Cambiar número de botones visibles
```java
suggestionBar.setMaxButtonCount(7); // 7 botones por defecto
```

### Configurar apariencia
```java
suggestionBar.setTextSize(14f); // Tamaño de texto
suggestionBar.setShowIcons(true); // Mostrar iconos
suggestionBar.setAppBarOpacity(80); // Opacidad del fondo
suggestionBar.setBlurConfig(true, 10); // Desenfoque de fondo
```

## 9. Manejo de Permisos

### En AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

### Verificación de permisos en runtime (API 23+)
```java
private void checkPermissions() {
    String[] permissions = {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        requestPermissions(permissions, 100);
    }
}
```

## 10. Pruebas Básicas

### Test unitario para proveedor de datos
```java
@Test
public void testGetAppsForLetter() {
    LauncherAppDataProvider provider = new LauncherAppDataProvider(context);
    List<LauncherAppEntry> appsA = provider.getAppsForLetter('A');
    assertNotNull(appsA);
    // Verificar que todas empiezan con A
    for (LauncherAppEntry app : appsA) {
        assertTrue(app.label.toUpperCase().startsWith("A"));
    }
}
```

## 11. Consideraciones de Migración

### Importante:
1. **Paquetes:** Asegúrate de cambiar los paquetes de `com.termux` a tu paquete personalizado
2. **Recursos:** Copiar drawables, layouts y estilos relacionados
3. **Strings:** Copiar recursos de strings.xml de Termux si los necesitas
4. **Permisos:** Verificar permisos necesarios según tu aplicación

### Archivos a Copiar:
- `SuggestionBarView.java`
- `LauncherAppDataProvider.java`
- `LauncherAppEntry.java`
- `AppRef.java`
- Cualquier clase relacionada con `PinnedItem`, `LauncherConfigRepository`, etc.

## 12. Ejemplo Completo de Activity

```java
public class LauncherActivity extends AppCompatActivity 
    implements SuggestionBarCallback {
    
    private SuggestionBarView suggestionBar;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        
        suggestionBar = findViewById(R.id.suggestion_bar);
        suggestionBar.setAppDataProvider(new LauncherAppDataProvider(this));
        
        // Configurar callback para teclado
        setupKeyboardListener();
    }
    
    private void setupKeyboardListener() {
        getWindow().getDecorView().setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                char inputChar = (char) event.getUnicodeChar();
                if (Character.isLetterOrDigit(inputChar) || inputChar == '#') {
                    suggestionBar.previewAzLetter(inputChar, 0, false);
                    return true;
                }
            }
            return false;
        });
    }
    
    @Override
    public void reloadSuggestionBar(char inputChar) {
        suggestionBar.previewAzLetter(inputChar, 0, false);
    }
    
    @Override
    public void reloadSuggestionBar(boolean delete, boolean enter) {
        // Implementar lógica de borrado/enter
    }
}
```

## 13. Depuración Común

### Problema: No se muestran aplicaciones
- Verificar permisos de lectura de paquetes
- Asegurar que `queryIntentActivities` devuelve resultados
- Verificar que el activity tenga la categoría `LAUNCHER`

### Problema: Layout incorrecto
- Verificar dimensiones en `createSlotParams()`
- Ajustar `maxButtonCount` según pantalla
- Revisar `columnCount` en GridLayout

Esta guía te permite migrar completamente la vista de `SuggestionBarView` a tu proyecto. ¡Asegúrate de adaptar los paquetes y recursos a tu estructura de proyecto!
