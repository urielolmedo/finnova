package ar.edu.usal.finnova.service;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
public class GraficoService {

    // Grafico de barras: Ingresos vs Egresos (usado en CU-025 y como parte de otros reportes)
    public byte[] generarGraficoBarras(BigDecimal ingresos, BigDecimal egresos) throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(ingresos, "Monto", "Ingresos");
        dataset.addValue(egresos, "Monto", "Egresos");

        JFreeChart chart = ChartFactory.createBarChart(
                "Ingresos vs Egresos", "", "Monto ($)", dataset,
                PlotOrientation.VERTICAL, false, false, false);

        // Colores diferenciados por categoria: verde para Ingresos, rojo para Egresos
        org.jfree.chart.plot.CategoryPlot plot = chart.getCategoryPlot();
        org.jfree.chart.renderer.category.BarRenderer renderer = new org.jfree.chart.renderer.category.BarRenderer() {
            @Override
            public Paint getItemPaint(int row, int column) {
                return column == 0 ? new Color(46, 125, 50) : new Color(198, 40, 40);
            }
        };
        plot.setRenderer(renderer);

        return chartToBytes(chart, 500, 300);
    }

    // Grafico de torta: distribucion por categoria (usado en CU-024)
    public byte[] generarGraficoTorta(List<String> nombres, List<BigDecimal> valores) throws IOException {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        for (int i = 0; i < nombres.size(); i++) {
            if (valores.get(i).signum() > 0) {
                dataset.setValue(nombres.get(i), valores.get(i));
            }
        }
        JFreeChart chart = ChartFactory.createPieChart("Distribución por categoría", dataset, true, false, false);
        return chartToBytes(chart, 500, 350);
    }

    // Grafico de lineas: evolucion mensual (usado en CU-026)
    public byte[] generarGraficoEvolucion(List<String> periodos, List<BigDecimal> ingresos,
                                          List<BigDecimal> egresos, List<BigDecimal> balance) throws IOException {
        XYSeries serieIngresos = new XYSeries("Ingresos");
        XYSeries serieEgresos = new XYSeries("Egresos");
        XYSeries serieBalance = new XYSeries("Balance");

        for (int i = 0; i < periodos.size(); i++) {
            serieIngresos.add(i, ingresos.get(i));
            serieEgresos.add(i, egresos.get(i));
            serieBalance.add(i, balance.get(i));
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(serieIngresos);
        dataset.addSeries(serieEgresos);
        dataset.addSeries(serieBalance);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Evolución financiera", "Período", "Monto ($)", dataset,
                PlotOrientation.VERTICAL, true, false, false);

        return chartToBytes(chart, 550, 300);
    }

    private byte[] chartToBytes(JFreeChart chart, int width, int height) throws IOException {
        chart.setBackgroundPaint(Color.WHITE);
        java.awt.image.BufferedImage image = chart.createBufferedImage(width, height);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
}