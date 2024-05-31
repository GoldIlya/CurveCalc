package ru.itinbiz.curvecalc.service;

import android.content.Context;

import com.androidplot.ui.SeriesRenderer;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;


public class MyLineAndPointFormatter extends LineAndPointFormatter {

    // if you dont use configurator you can omit this constructor.  this example uses it
    // tho so here it is.
    public MyLineAndPointFormatter(Context context, int xmlCfgId) {
        super(context, xmlCfgId);
    }

    @Override
    public Class<? extends SeriesRenderer> getRendererClass() {
        return MyLineAndPointRenderer.class;
    }

    @Override
    public SeriesRenderer doGetRendererInstance(XYPlot plot) {
        return new MyLineAndPointRenderer(plot);
    }
}
