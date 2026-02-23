package services;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import entities.OffreEmploi;
import entities.Postulation;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service pour exporter les statistiques en PDF et Excel
 */
public class StatsExportService {

    private static final DeviceRgb PURPLE_COLOR = new DeviceRgb(124, 58, 237);
    private static final DeviceRgb LIGHT_PURPLE = new DeviceRgb(243, 232, 255);

    /**
     * Exporte les statistiques en PDF
     */
    public File exportToPDF(OffreEmploi offre, OffreAnalyticsService.OffreStatistics stats,
                            List<Postulation> postulations) throws Exception {

        String fileName = "Analytics_" + sanitizeFileName(offre.getTitre()) + "_" +
                         LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";

        File file = new File(System.getProperty("user.home") + "/Downloads/" + fileName);

        PdfWriter writer = new PdfWriter(file);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Titre
        Paragraph title = new Paragraph("📊 Analytics - " + offre.getTitre())
            .setFontSize(24)
            .setBold()
            .setFontColor(PURPLE_COLOR)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20);
        document.add(title);

        // Informations de l'offre
        document.add(new Paragraph("Entreprise: " + offre.getEntreprise()).setFontSize(12));
        document.add(new Paragraph("Type de contrat: " + offre.getTypeContrat()).setFontSize(12));
        document.add(new Paragraph("Localisation: " + offre.getLocalisation()).setFontSize(12));
        document.add(new Paragraph("Date de génération: " +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).setFontSize(10));
        document.add(new Paragraph("\n"));

        // Section KPIs Principaux
        document.add(new Paragraph("📈 KPIs Principaux")
            .setFontSize(18)
            .setBold()
            .setFontColor(PURPLE_COLOR)
            .setMarginTop(10));

        Table kpiTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}))
            .useAllAvailableWidth();

        addKpiCell(kpiTable, "👁️ Vues Totales", String.valueOf(stats.getTotalVues()));
        addKpiCell(kpiTable, "📤 Candidatures", String.valueOf(stats.getTotalCandidatures()));
        addKpiCell(kpiTable, "🎯 Taux Conversion", String.format("%.1f%%", stats.getTauxConversion()));
        addKpiCell(kpiTable, "⭐ Score Qualité", stats.getScoreQualite() + "/100");

        document.add(kpiTable);
        document.add(new Paragraph("\n"));

        // Section KPIs Secondaires
        document.add(new Paragraph("📊 Détails des Candidatures")
            .setFontSize(18)
            .setBold()
            .setFontColor(PURPLE_COLOR)
            .setMarginTop(10));

        Table detailTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}))
            .useAllAvailableWidth();

        addKpiCell(detailTable, "⏳ En Attente", String.valueOf(stats.getEnAttente()));
        addKpiCell(detailTable, "✅ Acceptées", String.valueOf(stats.getAcceptees()));
        addKpiCell(detailTable, "📊 Taux Acceptation", String.format("%.1f%%",
            stats.getTotalCandidatures() > 0 ? (stats.getAcceptees() * 100.0 / stats.getTotalCandidatures()) : 0));
        addKpiCell(detailTable, "📈 Tendance", stats.getTendance() + " (" +
            String.format("%+.1f%%", stats.getVariationPourcentage()) + ")");

        document.add(detailTable);
        document.add(new Paragraph("\n"));

        // Analyse Salariale
        document.add(new Paragraph("💰 Analyse Salariale")
            .setFontSize(18)
            .setBold()
            .setFontColor(PURPLE_COLOR)
            .setMarginTop(10));

        String salaireText = stats.isSalaireCompetitif() ?
            "✅ Salaire compétitif (" + offre.getSalaire() + " DT vs " +
            String.format("%.0f", stats.getSalaireMoyenSecteur()) + " DT moyenne)" :
            "⚠️ Salaire en dessous du marché (" + offre.getSalaire() + " DT vs " +
            String.format("%.0f", stats.getSalaireMoyenSecteur()) + " DT moyenne)";

        document.add(new Paragraph(salaireText).setFontSize(12));
        document.add(new Paragraph("\n"));

        // Recommandations
        if (!stats.getRecommandations().isEmpty()) {
            document.add(new Paragraph("💡 Recommandations")
                .setFontSize(18)
                .setBold()
                .setFontColor(PURPLE_COLOR)
                .setMarginTop(10));

            for (OffreAnalyticsService.Recommendation rec : stats.getRecommandations()) {
                document.add(new Paragraph(rec.getIcon() + " " + rec.getTitre() + ": " + rec.getDescription())
                    .setFontSize(11)
                    .setMarginLeft(10));
            }
            document.add(new Paragraph("\n"));
        }

        // Liste des postulations
        if (!postulations.isEmpty()) {
            document.add(new Paragraph("📋 Postulations Récentes")
                .setFontSize(18)
                .setBold()
                .setFontColor(PURPLE_COLOR)
                .setMarginTop(10));

            Table postTable = new Table(UnitValue.createPercentArray(new float[]{1, 2, 1}))
                .useAllAvailableWidth();

            // Header
            postTable.addHeaderCell(createHeaderCell("Candidat"));
            postTable.addHeaderCell(createHeaderCell("Date"));
            postTable.addHeaderCell(createHeaderCell("Statut"));

            // Rows
            for (Postulation p : postulations.subList(0, Math.min(10, postulations.size()))) {
                postTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("ID: " + p.getCandidatId()).setFontSize(10)));
                postTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(
                    p.getDatePostulation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).setFontSize(10)));
                postTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(p.getStatut()).setFontSize(10)));
            }

            document.add(postTable);
        }

        // Footer
        document.add(new Paragraph("\n\n"));
        document.add(new Paragraph("Généré par Goffres - Système de Gestion des Offres d'Emploi")
            .setFontSize(8)
            .setTextAlignment(TextAlignment.CENTER)
            .setFontColor(ColorConstants.GRAY));

        document.close();

        return file;
    }

    /**
     * Exporte les statistiques en Excel
     */
    public File exportToExcel(OffreEmploi offre, OffreAnalyticsService.OffreStatistics stats,
                              List<Postulation> postulations) throws Exception {

        String fileName = "Analytics_" + sanitizeFileName(offre.getTitre()) + "_" +
                         LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

        File file = new File(System.getProperty("user.home") + "/Downloads/" + fileName);

        Workbook workbook = new XSSFWorkbook();

        // Style pour les headers
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.VIOLET.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        // Sheet 1: Informations Générales
        Sheet infoSheet = workbook.createSheet("Informations");
        createInfoSheet(infoSheet, offre, stats, headerStyle);

        // Sheet 2: KPIs
        Sheet kpiSheet = workbook.createSheet("KPIs");
        createKpiSheet(kpiSheet, stats, headerStyle);

        // Sheet 3: Postulations
        Sheet postSheet = workbook.createSheet("Postulations");
        createPostulationsSheet(postSheet, postulations, headerStyle);

        // Auto-size columns
        for (int i = 0; i < 3; i++) {
            Sheet sheet = workbook.getSheetAt(i);
            for (int j = 0; j < 5; j++) {
                sheet.autoSizeColumn(j);
            }
        }

        // Save
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            workbook.write(fileOut);
        }

        workbook.close();

        return file;
    }

    // Helper methods

    private void addKpiCell(Table table, String label, String value) {
        com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell()
            .setBackgroundColor(LIGHT_PURPLE)
            .setTextAlignment(TextAlignment.CENTER)
            .setPadding(10);

        cell.add(new Paragraph(label).setFontSize(10).setBold());
        cell.add(new Paragraph(value).setFontSize(16).setBold().setFontColor(PURPLE_COLOR));

        table.addCell(cell);
    }

    private com.itextpdf.layout.element.Cell createHeaderCell(String text) {
        return new com.itextpdf.layout.element.Cell()
            .add(new Paragraph(text).setBold().setFontColor(ColorConstants.WHITE))
            .setBackgroundColor(PURPLE_COLOR)
            .setTextAlignment(TextAlignment.CENTER)
            .setPadding(5);
    }

    private void createInfoSheet(Sheet sheet, OffreEmploi offre,
                                 OffreAnalyticsService.OffreStatistics stats, CellStyle headerStyle) {
        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("📊 Analytics - " + offre.getTitre());
        titleCell.setCellStyle(headerStyle);

        rowNum++;

        // Info
        addInfoRow(sheet, rowNum++, "Entreprise", offre.getEntreprise());
        addInfoRow(sheet, rowNum++, "Type de contrat", offre.getTypeContrat());
        addInfoRow(sheet, rowNum++, "Localisation", offre.getLocalisation());
        addInfoRow(sheet, rowNum++, "Salaire", offre.getSalaire() + " DT");
        addInfoRow(sheet, rowNum++, "Date de génération",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
    }

    private void createKpiSheet(Sheet sheet, OffreAnalyticsService.OffreStatistics stats, CellStyle headerStyle) {
        int rowNum = 0;

        // Headers
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"KPI", "Valeur"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data
        addKpiRow(sheet, rowNum++, "👁️ Vues Totales", String.valueOf(stats.getTotalVues()));
        addKpiRow(sheet, rowNum++, "📤 Candidatures", String.valueOf(stats.getTotalCandidatures()));
        addKpiRow(sheet, rowNum++, "🎯 Taux Conversion", String.format("%.1f%%", stats.getTauxConversion()));
        addKpiRow(sheet, rowNum++, "⭐ Score Qualité", stats.getScoreQualite() + "/100");
        addKpiRow(sheet, rowNum++, "⏳ En Attente", String.valueOf(stats.getEnAttente()));
        addKpiRow(sheet, rowNum++, "✅ Acceptées", String.valueOf(stats.getAcceptees()));
        addKpiRow(sheet, rowNum++, "📊 Taux Acceptation", String.format("%.1f%%",
            stats.getTotalCandidatures() > 0 ? (stats.getAcceptees() * 100.0 / stats.getTotalCandidatures()) : 0));
        addKpiRow(sheet, rowNum++, "📈 Tendance", stats.getTendance() + " (" +
            String.format("%+.1f%%", stats.getVariationPourcentage()) + ")");
    }

    private void createPostulationsSheet(Sheet sheet, List<Postulation> postulations, CellStyle headerStyle) {
        int rowNum = 0;

        // Headers
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"ID", "Candidat ID", "Date", "Statut", "Lettre de motivation"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data
        for (Postulation p : postulations) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(p.getId());
            row.createCell(1).setCellValue(p.getCandidatId());
            row.createCell(2).setCellValue(p.getDatePostulation().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            row.createCell(3).setCellValue(p.getStatut());
            row.createCell(4).setCellValue(p.getMotivationCandidature());
        }
    }

    private void addInfoRow(Sheet sheet, int rowNum, String label, String value) {
        Row row = sheet.createRow(rowNum);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        Font font = sheet.getWorkbook().createFont();
        font.setBold(true);
        CellStyle style = sheet.getWorkbook().createCellStyle();
        style.setFont(font);
        labelCell.setCellStyle(style);

        row.createCell(1).setCellValue(value);
    }

    private void addKpiRow(Sheet sheet, int rowNum, String kpi, String value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(kpi);
        row.createCell(1).setCellValue(value);
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}

