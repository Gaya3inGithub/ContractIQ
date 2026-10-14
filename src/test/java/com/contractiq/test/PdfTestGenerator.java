package com.contractiq.test;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;

public class PdfTestGenerator {

    public static void main(String[] args) throws IOException {
        File dir = new File("sample");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, "synthetic_enterprise_saas_agreement.pdf");
        createSampleContractPdf(file);
        System.out.println("Generated sample contract PDF: " + file.getAbsolutePath() + " (" + file.length() + " bytes)");
    }

    public static File createSampleContractPdf(File targetFile) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream cos = new PDPageContentStream(document, page)) {
                cos.beginText();
                cos.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                cos.newLineAtOffset(50, 720);
                cos.showText("ENTERPRISE CLOUD SERVICES AGREEMENT");
                
                cos.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                cos.newLineAtOffset(0, -25);
                cos.showText("This Agreement is entered into by and between CloudScale Systems Inc. and Horizon Logistics Corp.");

                cos.newLineAtOffset(0, -20);
                cos.showText("Effective as of 2025-02-01. Expiration date: 2026-02-01.");

                cos.newLineAtOffset(0, -20);
                cos.showText("This agreement shall automatically renew for successive 12-month periods.");

                cos.newLineAtOffset(0, -20);
                cos.showText("Either party may terminate by providing at least 60 days prior written notice of non-renewal.");

                cos.newLineAtOffset(0, -20);
                cos.showText("Section 4. Payment Terms: Customer shall pay quarterly invoices Net 30 days from receipt.");

                cos.newLineAtOffset(0, -20);
                cos.showText("Section 8. Termination: Either party may terminate immediately for uncured material breach.");

                cos.newLineAtOffset(0, -20);
                cos.showText("Section 12. Provider shall maintain SOC 2 compliance and deliver annual audit reports.");

                cos.endText();
            }

            document.save(targetFile);
        }
        return targetFile;
    }
}
