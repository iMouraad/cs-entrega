package ec.edu.uteq.microservicios.msusuarios.service;

import ec.edu.uteq.microservicios.msusuarios.model.Entrega;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class PdfService {

    public byte[] generateDeliveryNote(Entrega entrega) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A6);
        
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Paragraph title = new Paragraph("GUÍA DE ENTREGA", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            
            document.add(new Paragraph(" ")); // Espacio
            
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            
            addText(document, "Nro. Entrega: ", String.valueOf(entrega.getId()), labelFont, valueFont);
            addText(document, "Orden: ", "#" + entrega.getOrderId(), labelFont, valueFont);
            addText(document, "Cliente: ", entrega.getCustomerName() != null ? entrega.getCustomerName() : "Consumidor Final", labelFont, valueFont);
            addText(document, "Dirección: ", entrega.getAddress(), labelFont, valueFont);
            addText(document, "Email: ", entrega.getEmail() != null ? entrega.getEmail() : "N/A", labelFont, valueFont);
            addText(document, "Track ID: ", entrega.getTrackingNumber() != null ? entrega.getTrackingNumber() : "PENDIENTE", labelFont, valueFont);
            
            document.add(new Paragraph(" "));
            Paragraph footer = new Paragraph("¡Gracias por su compra!", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);
            
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return out.toByteArray();
    }
    
    private void addText(Document doc, String label, String value, Font labelFont, Font valueFont) throws DocumentException {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label, labelFont));
        p.add(new Chunk(value, valueFont));
        doc.add(p);
    }
}
