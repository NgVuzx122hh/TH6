import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class RenderSequenceDiagram {
    private static final int WIDTH = 1024;
    private static final int HEIGHT = 1100;
    private static final int[] LANES = {90, 300, 500, 720, 930};
    private static final String[] PARTICIPANTS = {
            "Doctor", "Appointment /\nExamination", "Appointment DB", "Laboratory", "Laboratory DB"
    };

    public static void main(String[] args) throws IOException {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, WIDTH, HEIGHT);
        graphics.setFont(new Font("Segoe UI", Font.BOLD, 22));
        graphics.setColor(new Color(28, 48, 74));
        graphics.drawString("Appointment and Laboratory Service Integration", 185, 42);

        drawParticipants(graphics);
        int y = 172;
        message(graphics, 0, 1, y, "POST /api/v1/examinations (JWT)"); y += 37;
        message(graphics, 1, 2, y, "Check active patient and appointment"); y += 32;
        message(graphics, 2, 1, y, "Valid"); y += 37;
        message(graphics, 1, 3, y, "POST /api/v1/lab-orders (JWT)"); y += 32;
        message(graphics, 3, 4, y, "Validate tests; save RECEIVED order"); y += 32;
        reply(graphics, 4, 3, y, "orderId"); y += 32;
        reply(graphics, 3, 1, y, "201 Created: orderId, RECEIVED"); y += 37;
        message(graphics, 1, 2, y, "Save examination to labOrderId link"); y += 32;
        reply(graphics, 2, 1, y, "examinationId"); y += 32;
        reply(graphics, 1, 0, y, "201 Created"); y += 55;
        message(graphics, 0, 3, y, "PUT /lab-orders/{id}/results (technician JWT)"); y += 32;
        message(graphics, 3, 4, y, "Save results; mark COMPLETED"); y += 32;
        reply(graphics, 4, 3, y, "Updated"); y += 32;
        reply(graphics, 3, 0, y, "200 OK"); y += 55;
        message(graphics, 0, 1, y, "GET examination results by labOrderId (JWT)"); y += 32;
        message(graphics, 1, 2, y, "Verify examination ↔ labOrder link"); y += 32;
        reply(graphics, 2, 1, y, "Link exists"); y += 32;
        message(graphics, 1, 3, y, "GET /lab-orders/{id}/results (JWT)"); y += 32;
        message(graphics, 3, 4, y, "Read results"); y += 32;
        reply(graphics, 4, 3, y, "Result data"); y += 32;
        reply(graphics, 3, 1, y, "200 OK"); y += 32;
        reply(graphics, 1, 0, y, "200 OK: examination and lab results");

        graphics.setColor(new Color(120, 71, 33));
        graphics.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        graphics.drawString("Compensation: if saving the Consumer link fails, request DELETE /lab-orders/{id}.",
                75, 1050);
        graphics.drawString("If cancellation also fails, return RECONCILIATION_REQUIRED for manual follow-up.",
                75, 1072);
        graphics.dispose();
        ImageIO.write(image, "png", new File("integration/sequence-diagram/integration-flow.png"));
    }

    private static void drawParticipants(Graphics2D graphics) {
        graphics.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        for (int i = 0; i < LANES.length; i++) {
            int x = LANES[i] - 82;
            graphics.setColor(new Color(234, 241, 250));
            graphics.fillRoundRect(x, 70, 164, 54, 10, 10);
            graphics.setColor(new Color(55, 79, 110));
            graphics.drawRoundRect(x, 70, 164, 54, 10, 10);
            String[] lines = PARTICIPANTS[i].split("\n");
            for (int line = 0; line < lines.length; line++) {
                int textWidth = graphics.getFontMetrics().stringWidth(lines[line]);
                graphics.drawString(lines[line], LANES[i] - textWidth / 2, 94 + line * 17);
            }
            graphics.setColor(new Color(145, 158, 177));
            graphics.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10, new float[]{5, 5}, 0));
            graphics.drawLine(LANES[i], 125, LANES[i], 1015);
        }
        graphics.setStroke(new BasicStroke(1.4f));
    }

    private static void message(Graphics2D graphics, int from, int to, int y, String label) {
        arrow(graphics, LANES[from], LANES[to], y, label, new Color(38, 91, 145), false);
    }

    private static void reply(Graphics2D graphics, int from, int to, int y, String label) {
        arrow(graphics, LANES[from], LANES[to], y, label, new Color(97, 112, 129), true);
    }

    private static void arrow(Graphics2D graphics, int from, int to, int y, String label, Color color,
                              boolean dashed) {
        graphics.setColor(color);
        graphics.setStroke(dashed
                ? new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                        10, new float[]{5, 4}, 0)
                : new BasicStroke(1.4f));
        int start = from < to ? from + 4 : from - 4;
        int end = from < to ? to - 8 : to + 8;
        graphics.drawLine(start, y, end, y);
        Polygon head = from < to
                ? new Polygon(new int[]{end, end - 8, end - 8}, new int[]{y, y - 5, y + 5}, 3)
                : new Polygon(new int[]{end, end + 8, end + 8}, new int[]{y, y - 5, y + 5}, 3);
        graphics.fillPolygon(head);
        graphics.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        int labelWidth = graphics.getFontMetrics().stringWidth(label);
        int labelX = (start + end - labelWidth) / 2;
        graphics.setColor(color);
        graphics.drawString(label, labelX, y - 5);
    }
}
