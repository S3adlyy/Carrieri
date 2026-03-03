package services.goffre;

import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Service pour afficher des cartes interactives avec OpenStreetMap + Leaflet.js
 * 100% gratuit, pas de clé API nécessaire
 */
public class MapService {

    private static MapService instance;

    private MapService() {}

    public static MapService getInstance() {
        if (instance == null) {
            instance = new MapService();
        }
        return instance;
    }

    /**
     * Ouvre une carte interactive dans une nouvelle fenêtre
     * @param location Localisation à afficher (ex: "Tunis, Tunisie")
     * @param title Titre de la fenêtre
     */
    public void showMap(String location, String title) {
        if (location == null || location.trim().isEmpty()) {
            location = "Paris, France"; // Localisation par défaut
        }

        Stage mapStage = new Stage();
        mapStage.setTitle(title != null ? title : "Localisation - " + location);
        mapStage.initModality(Modality.NONE);

        // Créer WebView pour afficher la carte
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();

        // Charger la page HTML avec la carte Leaflet
        String mapHtml = generateMapHtml(location);
        webEngine.loadContent(mapHtml);

        Scene scene = new Scene(webView, 900, 650);
        mapStage.setScene(scene);
        mapStage.show();

        System.out.println("✓ Carte interactive ouverte pour : " + location);
    }

    /**
     * Génère le HTML avec Leaflet.js et OpenStreetMap
     */
    private String generateMapHtml(String location) {
        String escapedLocation = escapeJavaScript(location);
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Carte Interactive</title>
    
    <!-- Leaflet CSS -->
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
          integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY="
          crossorigin=""/>
    
    <!-- Leaflet JS -->
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"
            integrity="sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo="
            crossorigin=""></script>
    
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: #f5f0fa;
        }
        
        #map {
            width: 100%%;
            height: 100vh;
            z-index: 1;
        }
        
        .location-header {
            position: absolute;
            top: 20px;
            left: 50%%;
            transform: translateX(-50%%);
            z-index: 1000;
            background: linear-gradient(135deg, #7c3aed, #a78bfa);
            color: white;
            padding: 12px 28px;
            border-radius: 20px;
            box-shadow: 0 8px 20px rgba(124, 58, 237, 0.3);
            font-weight: 600;
            font-size: 16px;
            backdrop-filter: blur(10px);
        }
        
        .location-header::before {
            content: '📍';
            margin-right: 10px;
            font-size: 20px;
        }
        
        /* Custom marker popup */
        .leaflet-popup-content-wrapper {
            background: white;
            border-radius: 12px;
            box-shadow: 0 4px 15px rgba(0,0,0,0.1);
        }
        
        .leaflet-popup-content {
            margin: 16px;
            font-size: 14px;
            color: #374151;
        }
        
        .leaflet-popup-content h3 {
            color: #7c3aed;
            font-size: 16px;
            margin-bottom: 8px;
        }
        
        .leaflet-popup-tip {
            background: white;
        }
    </style>
</head>
<body>
    <div class="location-header" id="locationHeader">Chargement...</div>
    <div id="map"></div>
    
    <script>
        const locationQuery = "%s";
        
        // Initialiser la carte (centre par défaut)
        const map = L.map('map').setView([36.8065, 10.1815], 13);
        
        // Ajouter les tuiles OpenStreetMap
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© OpenStreetMap contributors',
            maxZoom: 19,
        }).addTo(map);
        
        // Créer un marqueur personnalisé violet
        const purpleIcon = L.icon({
            iconUrl: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzIiIGhlaWdodD0iNDgiIHZpZXdCb3g9IjAgMCAzMiA0OCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KICA8cGF0aCBkPSJNMTYgNDhDMTYgNDggMzIgMjggMzIgMTZDMzIgNy4xNjM0NCAyNC44MzY2IDAgMTYgMEM3LjE2MzQ0IDAgMCA3LjE2MzQ0IDAgMTZDMCAyOCAxNiA0OCAxNiA0OFoiIGZpbGw9IiM3YzNhZWQiLz4KICA8Y2lyY2xlIGN4PSIxNiIgY3k9IjE2IiByPSI4IiBmaWxsPSJ3aGl0ZSIvPgo8L3N2Zz4=',
            iconSize: [32, 48],
            iconAnchor: [16, 48],
            popupAnchor: [0, -48]
        });
        
        let marker = null;
        
        // Géocoder la localisation avec Nominatim (API gratuite d'OpenStreetMap)
        async function geocodeLocation(query) {
            try {
                document.getElementById('locationHeader').textContent = 'Recherche de ' + query + '...';
                
                const response = await fetch(
                    `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}&limit=1`
                );
                
                const data = await response.json();
                
                if (data && data.length > 0) {
                    const result = data[0];
                    const lat = parseFloat(result.lat);
                    const lon = parseFloat(result.lon);
                    
                    // Centrer la carte sur la localisation
                    map.setView([lat, lon], 14);
                    
                    // Ajouter un marqueur
                    if (marker) {
                        map.removeLayer(marker);
                    }
                    
                    marker = L.marker([lat, lon], { icon: purpleIcon }).addTo(map);
                    marker.bindPopup(`
                        <div style="text-align: center;">
                            <h3>${result.display_name.split(',')[0]}</h3>
                            <p style="margin-top: 8px; color: #6b7280;">
                                ${result.display_name}
                            </p>
                        </div>
                    `).openPopup();
                    
                    document.getElementById('locationHeader').textContent = query;
                    
                    console.log('✓ Localisation trouvée:', result.display_name);
                } else {
                    document.getElementById('locationHeader').textContent = query + ' (non trouvé)';
                    console.warn('⚠ Aucun résultat pour:', query);
                }
            } catch (error) {
                console.error('❌ Erreur géocodage:', error);
                document.getElementById('locationHeader').textContent = query;
            }
        }
        
        // Lancer la recherche au chargement
        if (locationQuery) {
            geocodeLocation(locationQuery);
        }
    </script>
</body>
</html>
""".formatted(escapedLocation);
    }

    /**
     * Échappe les caractères spéciaux pour JavaScript
     */
    private String escapeJavaScript(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("'", "\\'")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r");
    }
}

