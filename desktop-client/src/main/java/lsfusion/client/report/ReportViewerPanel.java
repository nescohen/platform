package lsfusion.client.report;

import net.sf.jasperreports.swing.JRViewerController;
import net.sf.jasperreports.swing.JRViewerPanel;

public class ReportViewerPanel extends JRViewerPanel {
    public ReportViewerPanel(JRViewerController viewerContext) {
        super(viewerContext);
    }
    
    public double getRealZoom() {
        return realZoom;
    }
}
