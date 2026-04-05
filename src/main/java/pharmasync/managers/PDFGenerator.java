package pharmasync.managers;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import pharmasync.models.CartItem;
import pharmasync.models.Medicine;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PDFGenerator {

    private static final String INVOICES_DIR = "invoices";

    public static void generateAndOpenReceipt(List<CartItem<Medicine, Integer>> cart, double totalAmount) {
        try {
            File dir = new File(INVOICES_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filePath = INVOICES_DIR + "/Receipt_" + timestamp + ".pdf";

            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            // Make a premium looking header
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, BaseColor.DARK_GRAY);
            Paragraph title = new Paragraph("MANIPAL MEDICALS", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.GRAY);
            Paragraph subtitle = new Paragraph("PharmaSync Billing System - Official Receipt", subTitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20);
            document.add(subtitle);

            // Add basic info
            Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);
            document.add(new Paragraph("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), infoFont));
            document.add(new Paragraph("Transaction ID: " + timestamp, infoFont));
            document.add(new Paragraph(" "));

            // Add Table
            PdfPTable table = new PdfPTable(5); // Item, Type, Qty, Unit, Line Total
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);
            
            float[] columnWidths = {3f, 1.5f, 1f, 1.5f, 1.5f};
            table.setWidths(columnWidths);

            // Table Header
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE);
            BaseColor headerBg = new BaseColor(74, 78, 140); // Neumorphic dark blue

            String[] headers = {"Item Name", "Category", "Qty", "Unit ₹", "Total ₹"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(headerBg);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(8f);
                table.addCell(cell);
            }

            // Table Body
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.DARK_GRAY);
            for (CartItem<Medicine, Integer> item : cart) {
                Medicine m = item.getItem();
                int qty = item.getQuantity();
                double lineTotal = m.getPrice() * qty; // Note: tax/discount skipped for basic PDF columns for simplicity
                
                table.addCell(new Phrase(m.getName(), bodyFont));
                table.addCell(new Phrase(m.getCategory().name(), bodyFont));
                
                PdfPCell qtyCell = new PdfPCell(new Phrase(String.valueOf(qty), bodyFont));
                qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(qtyCell);
                
                PdfPCell priceCell = new PdfPCell(new Phrase(String.format("%.2f", m.getPrice()), bodyFont));
                priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(priceCell);
                
                // Real line total with taxes/discount logic
                double taxAmount  = lineTotal * m.getCategory().getBaseTaxRate();
                double discount   = m.calculateDiscount() * qty; // assuming calculateDiscount is per item? 
                double finalLine  = lineTotal + taxAmount - discount;

                PdfPCell totalCell = new PdfPCell(new Phrase(String.format("%.2f", finalLine), bodyFont));
                totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(totalCell);
            }
            document.add(table);

            // Add Grand Total
            Font grandTotalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.RED);
            Paragraph totalP = new Paragraph(String.format("Grand Total: ₹%.2f", totalAmount), grandTotalFont);
            totalP.setAlignment(Element.ALIGN_RIGHT);
            document.add(totalP);

            // Footer
            Paragraph footer = new Paragraph("\n\nThank you for choosing Manipal Medicals. wishing you good health!", infoFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();

            // Open the PDF automatically
            if (Desktop.isDesktopSupported()) {
                File pdfFile = new File(filePath);
                if (pdfFile.exists()) {
                    Desktop.getDesktop().open(pdfFile);
                }
            }

        } catch (Exception e) {
            System.err.println("Error generating PDF Receipt: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
