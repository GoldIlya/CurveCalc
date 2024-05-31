package ru.itinbiz.curvecalc.service;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;

import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.LineAndPointRenderer;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.PointLabeler;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.util.List;

public class MyLineAndPointRenderer extends LineAndPointRenderer<MyLineAndPointFormatter> {

    public MyLineAndPointRenderer(XYPlot plot) {
        super(plot);
    }

    // Basically just copy the entire renderPoints implementation and add a rotation as shown below
    @Override
    protected void renderPoints(Canvas canvas, RectF plotArea, XYSeries series, int iStart, int iEnd, List<PointF> points,
                                LineAndPointFormatter formatter) {
        if (formatter.hasVertexPaint() || formatter.hasPointLabelFormatter()) {
            final Paint vertexPaint = formatter.hasVertexPaint() ? formatter.getVertexPaint() : null;
            final boolean hasPointLabelFormatter = formatter.hasPointLabelFormatter();
            final PointLabelFormatter plf = hasPointLabelFormatter ? formatter.getPointLabelFormatter() : null;
            final PointLabeler pointLabeler = hasPointLabelFormatter ? formatter.getPointLabeler() : null;
            for(int i = iStart; i < iEnd; i++) {
                PointF p = points.get(i);
                if(p != null) {

                    if (vertexPaint != null) {
                        canvas.drawPoint(p.x, p.y, vertexPaint);
                    }

                    if (pointLabeler != null) {
                        // this is where we rotate the text:
                        final int canvasState = canvas.save();
                        try {
                            canvas.rotate(180, p.x, p.y);
                            canvas.scale(-1, 1);
                            canvas.drawText(pointLabeler.getLabel(series, i),
                                    -p.x + plf.hOffset, p.y + plf.vOffset, plf.getTextPaint());
                        } finally {
                            canvas.restoreToCount(canvasState);
                        }
                    }
                }
            }
        }
    }

}
